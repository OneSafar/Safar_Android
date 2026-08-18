import re

with open("app/src/main/java/com/safarparmar/app/feature/youtubeinsights/YoutubeInsightsRepository.kt", "r") as f:
    content = f.read()

content = content.replace(
    "suspend fun shouldBlock(detection: YoutubeDetection, protectedNow: Boolean): Boolean = when {",
    "suspend fun shouldBlock(detection: YoutubeDetection, protectedNow: Boolean = com.safarparmar.app.ui.ekagra.focusshield.FocusShieldRepository.Snapshot.active): Boolean = when {"
)

with open("app/src/main/java/com/safarparmar/app/feature/youtubeinsights/YoutubeInsightsRepository.kt", "w") as f:
    f.write(content)
