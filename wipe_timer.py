import re

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/TimerService.kt", "r") as f:
    text = f.read()

# Replace the block from `    // ── Focus Shield state ─────────────────────────────────────────────────`
# up to `    // ── Theme persistence (SharedPreferences so it survives navigation/rebind) ─`

start_marker = r"    // ── Focus Shield state ─────────────────────────────────────────────────"
end_marker = r"    // ── Theme persistence \(SharedPreferences so it survives navigation/rebind\) ─"

match = re.search(f"{start_marker}.*?{end_marker}", text, re.DOTALL)
if match:
    replacement = """    // ── Focus Shield state ─────────────────────────────────────────────────
    private val _focusShieldActive  = kotlinx.coroutines.flow.MutableStateFlow(false)
    val focusShieldActive: kotlinx.coroutines.flow.StateFlow<Boolean> = _focusShieldActive

    // ── Theme persistence (SharedPreferences so it survives navigation/rebind) ─"""
    text = text[:match.start()] + replacement + text[match.end()-len("    // ── Theme persistence (SharedPreferences so it survives navigation/rebind) ─"):]

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/TimerService.kt", "w") as f:
    f.write(text)
