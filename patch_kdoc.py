import re

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/focusshield/KavachAlwaysOnService.kt", "r") as f:
    content = f.read()

old_kdoc = """/**
 * Keeps KAVACH blocking apps outside Ekagra, after the student explicitly turns
 * Always On on.
 *
 * Same Usage Access + overlay path as timer-bound blocking, but it owns no timer
 * state: as long as this service is running and its notification is showing, the
 * chosen apps stay blocked whether or not a study session is in progress.
 *
 * Fully independent of Ekagra, and the *sole* blocker while it runs. A timer
 * running, paused, or on a break changes nothing, and no quick unlock is honoured:
 * the student chose this mode precisely because it has no way out.
 *
 * [com.safarparmar.app.ui.ekagra.TimerService] therefore does not activate its own
 * shield while Always On is enabled — it only records the study session. Letting it
 * take over was the bug: the student would be blocked by the timer's Normal-Mode
 * sheet, quick-unlock button and all, in the middle of an Always On session.
 */"""

new_kdoc = """/**
 * Unified service that handles KAVACH app blocking for both Normal Mode (timer-bound)
 * and Always On Mode (24/7). 
 *
 * It monitors the foreground app using Usage Access and shows an overlay block screen
 * if a restricted app is launched. In Normal Mode, it honors the quick unlock grace
 * period, but in Always On Mode it enforces strict continuous blocking without any
 * unlock windows.
 *
 * App blocking logic has been decoupled from the timer logic.
 */"""

content = content.replace(old_kdoc, new_kdoc)

with open("app/src/main/java/com/safarparmar/app/ui/ekagra/focusshield/KavachAlwaysOnService.kt", "w") as f:
    f.write(content)
