#!/usr/bin/env python3
"""Real process-death verification on a disposable offline Android emulator.

Install the prodDebug and prodDebugAndroidTest APKs first. This does not build,
install, clear application data, use credentials, or contact the backend.
The seed test is intentionally killed with SIGKILL; that instrumentation abort
is expected. Only the subsequent verification test must complete successfully.
"""

import argparse
import json
import pathlib
import re
import subprocess
import sys
import time

CASES = ("focus", "focus-completion", "stopwatch", "paused", "manual-break", "manual-break-completion", "pomodoro-chain")
TEST_CLASS = "com.safarparmar.app.ui.ekagra.EkagraProcessDeathInstrumentedTest"
TIMER_COMPONENT = "com.safarparmar.app.ui.ekagra.TimerService"


def foreground_timer_pid(services_dump, package):
    """Read the live TimerService record, excluding other flavors and restarting records."""
    records = list(re.finditer(r"^\s*\* ServiceRecord\{[^\n]+", services_dump, re.MULTILINE))
    for index, record in enumerate(records):
        header = record.group()
        if not any(component in header for component in (
                package + "/" + TIMER_COMPONENT, package + "/.ui.ekagra.TimerService")):
            continue
        block = services_dump[record.end():records[index + 1].start() if index + 1 < len(records) else len(services_dump)]
        if not re.search(r"\bisForeground=true\b", block):
            return None
        process = re.search(r"\bapp=ProcessRecord\{[^}\n]*\s(\d+):" + re.escape(package) + r"/", block)
        return int(process.group(1)) if process else None
    return None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--serial", required=True, help="Disposable emulator serial; physical devices are rejected")
    parser.add_argument("--package", default="com.safarparmar.app")
    parser.add_argument("--case", choices=CASES, action="append", dest="cases")
    parser.add_argument("--interruption", choices=("kill", "force-stop", "reboot"), default="kill")
    parser.add_argument("--require-background-restart", action="store_true",
                        help="For running focus/stopwatch/manual-break, require a stable foreground service within 30s, before opening the app")
    parser.add_argument("--output", type=pathlib.Path, required=True)
    options = parser.parse_args()
    if options.require_background_restart and options.interruption != "kill":
        parser.error("--require-background-restart only applies to ordinary process kills")
    prefix = [options.adb, "-s", options.serial]
    package = options.package

    def adb(*args, check=True, timeout=30):
        return subprocess.run(prefix + list(args), capture_output=True, text=True, check=check, timeout=timeout)

    def shell(*args, **kwargs):
        return adb("shell", *args, **kwargs)

    def read_private(name):
        output = shell("run-as", package, "cat", "files/" + name, check=False)
        if output.returncode:
            return None
        try:
            return json.loads(output.stdout)
        except json.JSONDecodeError:
            return None

    def wait_until(condition, timeout, description):
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            result = condition()
            if result:
                return result
            time.sleep(0.25)
        raise RuntimeError("Timed out waiting for " + description)

    def instrumentation(method, case):
        return prefix + ["shell", "am", "instrument", "-w", "-r", "-e", "class", TEST_CLASS + "#" + method,
                         "-e", "ekagraCase", case, "-e", "ekagraInterruption", options.interruption,
                         package + ".test/androidx.test.runner.AndroidJUnitRunner"]

    if not options.serial.startswith("emulator-") or shell("getprop", "ro.kernel.qemu").stdout.strip() != "1":
        raise RuntimeError("This destructive QA harness only accepts a disposable emulator")
    shell("svc", "wifi", "disable")
    shell("svc", "data", "disable")
    shell("input", "keyevent", "KEYCODE_WAKEUP")
    shell("input", "keyevent", "82")
    options.output.parent.mkdir(parents=True, exist_ok=True)
    results = []
    for case in options.cases or CASES:
        print(f"Starting {case} ({options.interruption})", flush=True)
        shell("run-as", package, "rm", "-f", "files/ekagra-process-death-seed.json", "files/ekagra-process-death-result.json")
        seed_log_path = options.output.with_name(options.output.stem + f"-{case}-seed.log")
        with seed_log_path.open("w") as seed_log:
            seed_process = subprocess.Popen(instrumentation("seedProcessDeath", case), stdout=seed_log, stderr=subprocess.STDOUT)
            try:
                seed = wait_until(lambda: read_private("ekagra-process-death-seed.json"), 45, "durable seed checkpoint")
                pid = seed["pid"]
                if not isinstance(pid, int) or pid <= 1:
                    raise RuntimeError("Invalid seed PID")
                shell("input", "keyevent", "KEYCODE_HOME")
                shell("input", "keyevent", "KEYCODE_SLEEP")
                interrupted_at = time.monotonic()
                if options.interruption == "kill":
                    shell("run-as", package, "kill", "-9", str(pid))
                elif options.interruption == "force-stop":
                    shell("am", "force-stop", package)
                else:
                    adb("reboot")
                    adb("wait-for-device", timeout=90)
                    wait_until(lambda: shell("getprop", "sys.boot_completed", check=False).stdout.strip() == "1", 90, "emulator reboot")
                try:
                    seed_process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    seed_process.terminate()
                    seed_process.wait(timeout=10)
                # Real elapsed downtime, without changing production timing constants.
                gap_seconds = (70 if case == "pomodoro-chain" else 10 if case.endswith("completion") else 4) if options.interruption == "kill" else 4
                time.sleep(gap_seconds)
                background = None
                background_error = None
                if options.require_background_restart and case in ("focus", "stopwatch", "manual-break"):
                    latest_dump = [""]
                    observed = {"pid": None, "since": 0.0}

                    def stable_background_restart():
                        latest_dump[0] = shell("dumpsys", "activity", "services", package).stdout
                        restored_pid = foreground_timer_pid(latest_dump[0], package)
                        if restored_pid is None or restored_pid == pid:
                            observed.update(pid=None, since=0.0)
                            return None
                        if restored_pid != observed["pid"]:
                            observed.update(pid=restored_pid, since=time.monotonic())
                            return None
                        if time.monotonic() - observed["since"] < 1.0:
                            return None
                        return {"backgroundRestarted": True, "backgroundPid": restored_pid}

                    service_log_path = options.output.with_name(options.output.stem + f"-{case}-background-services.log")
                    try:
                        background = wait_until(stable_background_restart, max(1, 30 - (time.monotonic() - interrupted_at)),
                                                "autonomous foreground timer restart without opening the app")
                    except RuntimeError as error:
                        # Still run verification to inspect recovery and clean up the fake owner.
                        background_error = str(error)
                    finally:
                        service_log_path.write_text(latest_dump[0])
                    if background:
                        print(f"Autonomous foreground restart observed in PID {background['backgroundPid']}", flush=True)
                exit_log_path = options.output.with_name(options.output.stem + f"-{case}-exit-info.log")
                exit_log_path.write_text(shell("dumpsys", "activity", "exit-info", package, check=False).stdout)
                shell("input", "keyevent", "KEYCODE_WAKEUP")
                shell("input", "keyevent", "82")
                verification = subprocess.run(instrumentation("verifyRecovery", case), capture_output=True, text=True, timeout=90)
                verify_log_path = options.output.with_name(options.output.stem + f"-{case}-verify.log")
                verify_log_path.write_text(verification.stdout + verification.stderr)
                if verification.returncode or "OK (1 test)" not in verification.stdout:
                    raise RuntimeError(f"Recovery verification failed for {case}; see {verify_log_path}")
                result = read_private("ekagra-process-death-result.json")
                if not result or not result.get("passed"):
                    raise RuntimeError("No passing recovery result for " + case)
                if background:
                    result.update(background)
                elif background_error:
                    result.update(backgroundRestarted=False, backgroundError=background_error)
                results.append(result)
                options.output.write_text(json.dumps(results, indent=2) + "\n")
                print(f"Passed {case}: {result['initialFocusSeconds']} -> {result['finalFocusSeconds']} focus seconds, one session", flush=True)
                if background_error:
                    raise RuntimeError(background_error + f"; manual recovery passed; see {service_log_path}")
            finally:
                if seed_process.poll() is None:
                    seed_process.terminate()
                    seed_process.wait(timeout=10)
    print(f"Verified {len(results)} actual {options.interruption} cases; report: {options.output}", flush=True)


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, subprocess.SubprocessError) as error:
        print(str(error), file=sys.stderr)
        sys.exit(1)
