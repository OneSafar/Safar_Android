import re

with open("app/src/main/java/com/safarparmar/app/feature/youtubeinsights/YoutubeInsightsAccessibilityService.kt", "r") as f:
    content = f.read()

content = content.replace(
    "val protectedNow = com.safarparmar.app.ui.ekagra.TimerService.isFocusTimerRunning(this)",
    "val protectedNow = com.safarparmar.app.ui.ekagra.focusshield.FocusShieldRepository.Snapshot.active"
)

with open("app/src/main/java/com/safarparmar/app/feature/youtubeinsights/YoutubeInsightsAccessibilityService.kt", "w") as f:
    f.write(content)
