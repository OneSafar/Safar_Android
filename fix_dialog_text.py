import re

with open('app/src/main/java/com/safarparmar/app/ui/components/LanguageSelectionDialog.kt', 'r') as f:
    content = f.read()

# 1. Add dialogTitle variable
val_active_tag = 'val activeTag = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "en" }'
val_dialog_title = '\n    val dialogTitle = if (activeTag == "hi") "भाषा चुनें" else "Select Language"'
content = content.replace(val_active_tag, val_active_tag + val_dialog_title)

# 2. Replace title Text usage
old_title_text = 'text = stringResource(R.string.profile_language_dialog_title),'
new_title_text = 'text = dialogTitle,'
content = content.replace(old_title_text, new_title_text)

# 3. Remove subtitle Text usage
# Let's match the exact block to remove it safely
subtitle_block = """                                Text(
                                    text = stringResource(R.string.profile_language_subtitle),
                                    fontSize = 13.5.sp,
                                    color = if (isDark) Color(0xFFA0AAB0) else Color(0xFF6B7280),
                                    lineHeight = 18.sp
                                )"""
content = content.replace(subtitle_block, "")

with open('app/src/main/java/com/safarparmar/app/ui/components/LanguageSelectionDialog.kt', 'w') as f:
    f.write(content)

