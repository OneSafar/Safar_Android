#!/usr/bin/env python3
"""Read-only coverage gate for the Android Toppers Batch translation resources."""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
FEATURE = ROOT / "app/src/main/java/com/safarparmar/app/feature/toppersbatch"
LOCALES = ("values", "values-hi", "values-b+hi+Latn")
PLACEHOLDER = re.compile(r"(?<!%)%(?!%)(?:\d+\$)?[sdf]")
UI_FILES = {
    "StudyPlanDialog", "ToppersBatchComponents", "ToppersBatchProgressChart",
    "LectureWeekBrowser", "ToppersBatchScreen", "PersonalPlanPanel", "StudyModePanel",
    "ToppersBatchDashboard", "BatchStudentCalendar", "BatchCompletionFeedback",
    "BatchReleaseProgressPanel", "BatchWeeklyAgenda", "ToppersBatchActions",
}
# These are wire identifiers, date patterns, analytics/animation labels, URL checks,
# remote-copy keys or stable test keys. None is rendered as app-owned copy.
EXCEPTIONS = {
    "HH:mm", "reminderTime", "lectureId", "Batch button press", "Tab background",
    "Asia/Kolkata", "d MMM", "d MMMM yyyy", "d MMM yyyy", "h:mm a", "EEE, d MMM",
    "EEE, d MMM yyyy", "EEE", "d MMMM", "Calendar date press", "pulseScale", "pulseAlpha",
    "emptyLectures", "personalPlanToday", "progressTitle", "todayTitle",
    "calendarCompleted", "calendarScheduled", "calendarPlanned", "calendarRevision",
    "calendarMilestones", "calendarCompletedEmpty", "calendarScheduledEmpty",
    "liveAcademyOnly", "liveUpcoming", "liveEnded", "openYoutube", "liveUnavailable",
    "openAcademy", "openLecture",
}


def end_string(source, start):
    cursor = start + 1
    while cursor < len(source):
        if source[cursor] == "\\":
            cursor += 2
        elif source[cursor] == '"':
            return cursor + 1
        elif source[cursor:cursor + 2] == "${":
            cursor = end_brace(source, cursor + 1)
        else:
            cursor += 1
    raise ValueError("Unterminated Kotlin string")


def end_brace(source, start):
    cursor, depth = start + 1, 1
    while cursor < len(source):
        if source[cursor] == '"':
            cursor = end_string(source, cursor)
            continue
        depth += (source[cursor] == "{") - (source[cursor] == "}")
        cursor += 1
        if not depth:
            return cursor
    raise ValueError("Unterminated Kotlin interpolation")


def literals(source):
    cursor = 0
    while cursor < len(source):
        if source[cursor:cursor + 2] == "//":
            cursor = source.find("\n", cursor)
            if cursor < 0:
                return
        elif source[cursor:cursor + 2] == "/*":
            cursor = source.index("*/", cursor + 2) + 2
        elif source[cursor] == '"':
            end = end_string(source, cursor)
            yield cursor, source[cursor + 1:end - 1]
            cursor = end
        else:
            cursor += 1


def template(raw):
    cursor, body, arguments = 0, "", []
    while cursor < len(raw):
        if raw[cursor:cursor + 2] == "${":
            end = end_brace(raw, cursor + 1)
            arguments.append(raw[cursor + 2:end - 1])
            body += "{}"
            cursor = end
        elif raw[cursor] == "$" and re.match(r"[A-Za-z_]", raw[cursor + 1:cursor + 2]):
            match = re.match(r"\$[A-Za-z_]\w*", raw[cursor:])
            body += "{}"
            cursor += len(match[0])
        else:
            body += raw[cursor]
            cursor += 1
    return body, arguments


def main():
    issues, resources = [], {}
    for locale in LOCALES:
        entries = {}
        for path in (RES / locale).glob("*.xml"):
            for node in ET.parse(path).getroot():
                name = node.get("name", "")
                if node.tag not in ("string", "plurals") or not name.startswith("toppers_batch_"):
                    continue
                key = (node.tag, name)
                if key in entries:
                    issues.append(f"{locale}: duplicate {name}")
                values = {"string": "".join(node.itertext())} if node.tag == "string" else {
                    item.get("quantity"): "".join(item.itertext()) for item in node
                }
                entries[key] = values
                for quantity, value in values.items():
                    if not value.strip():
                        issues.append(f"{locale}: empty {name}/{quantity}")
                    if locale.endswith("Latn") and re.search(r"[\u0900-\u097f]", value):
                        issues.append(f"{locale}: Devanagari in {name}")
        resources[locale] = entries
    base = resources["values"]
    for locale in LOCALES[1:]:
        entries = resources[locale]
        for key in base.keys() ^ entries.keys():
            issues.append(f"{locale}: resource parity mismatch {key}")
        for key in base.keys() & entries.keys():
            if base[key].keys() != entries[key].keys():
                issues.append(f"{locale}: quantity parity mismatch {key}")
            for quantity in base[key].keys() & entries[key].keys():
                expected, actual = base[key][quantity], entries[key][quantity]
                if sorted(PLACEHOLDER.findall(expected)) != sorted(PLACEHOLDER.findall(actual)):
                    issues.append(f"{locale}: placeholder mismatch {key}/{quantity}")
                if expected.count("%%") != actual.count("%%"):
                    issues.append(f"{locale}: literal percent mismatch {key}/{quantity}")
    for path in FEATURE.glob("*.kt"):
        for kind, key in re.findall(r"R\.(string|plurals)\.(toppers_batch_\w+)", path.read_text()):
            if (kind, key) not in base:
                issues.append(f"{path.name}: missing {kind} {key}")
        if path.stem not in UI_FILES:
            continue
        source = path.read_text()
        for cursor, raw in literals(source):
            body, arguments = template(raw)
            # Inspect literals nested in interpolations, too.
            nested = [template(value)[0] for argument in arguments for _, value in literals(argument)]
            for value in [body] + nested:
                if not re.search(r"[A-Za-z]{2}", value) or value in EXCEPTIONS:
                    continue
                if re.fullmatch(r"[a-z0-9_./:#{}%-]+", value) or value.startswith(("https:", "^#", "#")):
                    continue
                issues.append(f"{path.name}:{source.count(chr(10), 0, cursor) + 1}: unlocalized literal {value!r}")
    if issues:
        print("\n".join(issues), file=sys.stderr)
        return 1
    strings = sum(kind == "string" for kind, _ in base)
    plurals = sum(kind == "plurals" for kind, _ in base)
    print(f"Toppers Batch: {strings} strings + {plurals} plurals; all three locales match; source audit passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
