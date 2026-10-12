---
name: find-bugs
description: Locate likely bug locations in code by analyzing defect probability, hotspots, explicit markers, and complexity. Use when asked to find bugs, review risky code, or audit a codebase for issues.
usage: find-bugs [path]
arguments:
  - name: paths
    description: Paths to analyze
    required: false
    default: "."
metadata:
  author: shashankkumar
  source: ~/.codex/skills/find-bugs/SKILL.md
  last-updated: '2026-09-10'
  keywords:
  - bugs
  - debugging
  - code-quality
  - hotspots
  - defects
---

# Find Bugs Skill

Hunt for bugs in: `{{.paths}}`

## Quick Start

```bash
# Get high-risk files (threshold 0.8 = high risk)
omen -f json defect --risk-threshold 0.8

# Find hotspots
omen -f json hotspot --days 30

# Check for explicit markers
omen -f json satd | jq '.items[] | select(.category == "defect")'
```

## Priority Order

1. **Defect probability > 0.8**: Investigate first
2. **Hotspot score > 0.5**: Review complex logic
3. **BUG/FIXME markers**: Known issues
4. **Temporal coupling**: Check related files

## In Each File

- Look for functions with cognitive complexity > 20
- Check error handling paths
- Review boundary conditions
