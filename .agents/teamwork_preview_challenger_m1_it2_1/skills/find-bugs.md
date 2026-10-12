# Find Bugs Skill

Hunt for bugs in: `{{.paths}}`

## Priority Order
1. Defect probability > 0.8: Investigate first
2. Hotspot score > 0.5: Review complex logic
3. BUG/FIXME markers: Known issues
4. Temporal coupling: Check related files

## In Each File
- Look for functions with cognitive complexity > 20
- Check error handling paths
- Review boundary conditions
