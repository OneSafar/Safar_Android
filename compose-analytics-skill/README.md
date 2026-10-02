# Compose Analytics Skill

Copy the `compose-analytics-skill` folder into the skills directory used by your coding agent, or provide `SKILL.md` directly as an agent skill/instruction file.

The skill is designed to let an agent inspect and use these upstream sources when network/GitHub access is available:

- Vico — https://github.com/patrykandpatrick/vico
- Drafter — https://github.com/AndroidPoet/Drafter
- JetCo — https://github.com/developerchunk/JetCo

It deliberately does not pin versions. The agent is instructed to verify current stable versions, APIs, examples, compatibility, and licenses at implementation time.

Recommended invocation examples:

- "Use the compose analytics visualization skill to build the Progress analytics screen."
- "Add a dynamic weekly study-time graph using real ViewModel data."
- "Implement a GitHub-style consistency heatmap for the last 90 days."
- "Replace this static analytics mockup with production Compose visualizations."

The skill prefers a dependency when appropriate and only adapts source code when necessary and license-compatible.
