#!/usr/bin/env python3
"""Kill a normal debug app process outside instrumentation and verify automatic restart.

Only accepts an offline disposable emulator with no account credentials. The debug-only
probe Activity guards those conditions too. Install a prodDebug APK before running.
"""
import argparse
import json
import pathlib
import runpy
import subprocess
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--serial', required=True)
    parser.add_argument('--package', default='com.safarparmar.app')
    parser.add_argument('--case', action='append', dest='cases', choices=('focus', 'focus-completion', 'stopwatch', 'paused', 'manual-break', 'manual-break-completion', 'pomodoro-chain', 'goal', 'topic'))
    parser.add_argument('--interruption', choices=('kill', 'force-stop', 'reboot'), default='kill')
    parser.add_argument('--skip-background-check', action='store_true',
                        help='Verify durable recovery on reopen without waiting through Android restart backoff')
    parser.add_argument('--output', type=pathlib.Path, required=True)
    args = parser.parse_args()
    if not args.serial.startswith('emulator-'):
        raise RuntimeError('Only a disposable emulator is permitted')
    prefix = [args.adb, '-s', args.serial]
    def adb(*command, timeout=30, check=True):
        return subprocess.run(prefix + list(command), capture_output=True, text=True, timeout=timeout, check=check).stdout
    def shell(*command, **kwargs): return adb('shell', *command, **kwargs)
    def read(name):
        try: return json.loads(shell('run-as', args.package, 'cat', 'files/' + name))
        except (ValueError, subprocess.SubprocessError): return None
    def wait(check, seconds, label):
        deadline = time.monotonic() + seconds
        while time.monotonic() < deadline:
            result = check()
            if result: return result
            time.sleep(.25)
        raise RuntimeError('Timed out: ' + label)
    def probe(action, case):
        return shell('am', 'start', '-f', '0x18000000', '-n', args.package + '/com.safarparmar.app.ui.ekagra.EkagraRecoveryProbeActivity',
                     '--es', 'probe', action, '--es', 'case', case, '--es', 'interruption', args.interruption)
    if shell('getprop', 'ro.kernel.qemu').strip() != '1': raise RuntimeError('Not an emulator')
    shell('svc', 'wifi', 'disable')
    shell('svc', 'data', 'disable')
    parse_pid = runpy.run_path(str(pathlib.Path(__file__).with_name('ekagra-process-death.py')))['foreground_timer_pid']
    args.output.parent.mkdir(parents=True, exist_ok=True)
    results = []
    for case in args.cases or ('focus',):
        print('Normal-process probe:', case, args.interruption, flush=True)
        shell('run-as', args.package, 'rm', '-f', 'files/ekagra-process-death-seed.json', 'files/ekagra-process-death-result.json')
        shell('input', 'keyevent', 'KEYCODE_WAKEUP')
        shell('input', 'keyevent', '82')
        probe('seed', case)
        seed = wait(lambda: read('ekagra-process-death-seed.json'), 30, 'durable seed')
        time.sleep(.5)
        shell('input', 'keyevent', 'KEYCODE_HOME')
        shell('input', 'keyevent', 'KEYCODE_SLEEP')
        if args.interruption == 'kill': shell('run-as', args.package, 'kill', '-9', str(seed['pid']))
        elif args.interruption == 'force-stop': shell('am', 'force-stop', args.package)
        else:
            adb('reboot')
            adb('wait-for-device', timeout=90)
            wait(lambda: shell('getprop', 'sys.boot_completed', check=False).strip() == '1', 90, 'reboot')
            shell('svc', 'wifi', 'disable')
            shell('svc', 'data', 'disable')
        background_pid = None
        if not args.skip_background_check and args.interruption == 'kill' and case in ('focus', 'stopwatch', 'manual-break', 'goal', 'topic'):
            def background():
                current = shell('dumpsys', 'activity', 'services', args.package)
                args.output.with_name(args.output.stem + '-' + case + '-services.log').write_text(current)
                pid = parse_pid(current, args.package)
                return pid if pid and pid != seed['pid'] else None
            background_pid = wait(background, 45, 'autonomous foreground restart before opening app')
            print('Autonomous foreground PID:', background_pid, flush=True)
        gap = 70 if case == 'pomodoro-chain' else 10 if case.endswith('completion') else 4
        if args.interruption != 'kill': gap = 4
        time.sleep(gap)
        args.output.with_name(args.output.stem + '-' + case + '-exits.log').write_text(shell('dumpsys', 'activity', 'exit-info', args.package))
        shell('input', 'keyevent', 'KEYCODE_WAKEUP')
        shell('input', 'keyevent', '82')
        probe('verify', case)
        result = wait(lambda: read('ekagra-process-death-result.json'), 30, 'verification')
        if not result.get('passed'): raise RuntimeError(str(result))
        result.update(backgroundPid=background_pid, backgroundRestarted=background_pid is not None)
        results.append(result)
        args.output.write_text(json.dumps(results, indent=2) + '\n')
        print(f"Passed {case}: {result['initialFocusSeconds']} -> {result['finalFocusSeconds']} seconds", flush=True)
    print('Passed', len(results), 'normal-process cases; report:', args.output, flush=True)


if __name__ == '__main__': main()
