#!/usr/bin/env python3
"""Create a review sheet for the app's English, Hindi and Hinglish strings."""

import csv
import re
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
OUT = ROOT / "Docs/localization-review.csv"
LOCALES = {
    "English": RES / "values",
    "Hindi": RES / "values-hi",
    "Hinglish": RES / "values-b+hi+Latn",
}
PLACEHOLDER = re.compile(r"(?<!%)%(?!%)(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z]")
DEVANAGARI = re.compile(r"[\u0900-\u097f]")
LATIN_WORD = re.compile(r"[A-Za-z]{2,}")
SUSPECT = re.compile(r"\b(?:misa|rivaiza|taimara|pasavarda|notiphikesana|aipsa)\b", re.I)


def read_locale(directory):
    entries = {}
    duplicates = []
    for path in sorted(directory.glob("*.xml")):
        tree = ET.parse(path)
        for node in tree.getroot():
            if node.tag != "string" or not node.get("name") or node.get("translatable") == "false":
                continue
            key = node.attrib["name"]
            value = "".join(node.itertext()).strip()
            if key in entries:
                duplicates.append(key)
            entries[key] = (value, path.relative_to(ROOT).as_posix())
    return entries, duplicates


def screens_by_key():
    screens = defaultdict(set)
    source = ROOT / "app/src/main/java"
    for path in source.rglob("*.kt"):
        body = path.read_text(encoding="utf-8")
        for key in re.findall(r"\bR\.string\.([A-Za-z0-9_]+)", body):
            screens[key].add(path.stem)
    return screens


def main():
    data = {}
    duplicates = {}
    for locale, directory in LOCALES.items():
        data[locale], duplicates[locale] = read_locale(directory)
    screens = screens_by_key()
    keys = sorted(set().union(*(item.keys() for item in data.values())))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    flagged = 0
    with OUT.open("w", newline="", encoding="utf-8-sig") as stream:
        writer = csv.writer(stream)
        writer.writerow(["key", "screens", "English", "Hindi", "Hinglish", "audit flags", "review status"])
        for key in keys:
            values = {locale: data[locale].get(key, ("", ""))[0] for locale in LOCALES}
            flags = []
            for locale in LOCALES:
                if key not in data[locale]:
                    flags.append(f"missing {locale}")
            if all(key in data[locale] for locale in LOCALES):
                expected = sorted(PLACEHOLDER.findall(values["English"]))
                for locale in ("Hindi", "Hinglish"):
                    if sorted(PLACEHOLDER.findall(values[locale])) != expected:
                        flags.append(f"{locale} placeholders differ")
                    if values[locale].count("%%") != values["English"].count("%%"):
                        flags.append(f"{locale} literal percent signs differ")
            if DEVANAGARI.search(values["Hinglish"]):
                flags.append("Devanagari in Hinglish")
            if LATIN_WORD.search(values["Hindi"]):
                flags.append("Latin text in Hindi: inspect names/technical terms")
            if SUSPECT.search(values["Hinglish"]):
                flags.append("likely Hinglish spelling")
            if flags:
                flagged += 1
            writer.writerow([key, "; ".join(sorted(screens[key])), values["English"],
                             values["Hindi"], values["Hinglish"], "; ".join(flags), "Needs human review"])
    for locale, names in duplicates.items():
        if names:
            print(f"{locale}: {len(names)} duplicate keys: {', '.join(names[:10])}", file=sys.stderr)
    print(f"Wrote {len(keys)} strings to {OUT.relative_to(ROOT)}; {flagged} rows have automated flags.")
    return 1 if any(duplicates.values()) else 0


if __name__ == "__main__":
    raise SystemExit(main())
