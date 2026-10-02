with open('app/src/main/java/com/safarparmar/app/ui/nishtha/checkin/CheckInScreen.kt', 'r') as f:
    code = f.read()

code = code.replace('Regex("(?i)Tags:\\s*([^\\n]+)")', 'Regex("""(?i)Tags:\\s*([^\\n]+)""")')
code = code.replace('Regex("(?i)Tags:\\s*([^\n]+)")', 'Regex("""(?i)Tags:\\s*([^\\n]+)""")')
code = code.replace('Regex("(?i)Tags:\s*([^\n]+)")', 'Regex("""(?i)Tags:\\s*([^\\n]+)""")')

code = code.replace('Regex("(?i)Caused by:\\s*([^\\n]+)")', 'Regex("""(?i)Caused by:\\s*([^\\n]+)""")')
code = code.replace('Regex("(?i)Caused by:\\s*([^\n]+)")', 'Regex("""(?i)Caused by:\\s*([^\\n]+)""")')
code = code.replace('Regex("(?i)Caused by:\s*([^\n]+)")', 'Regex("""(?i)Caused by:\\s*([^\\n]+)""")')

code = code.replace('Regex("(?i)Due to:\\s*(.+)", RegexOption.DOT_MATCHES_ALL)', 'Regex("""(?i)Due to:\\s*(.+)""", RegexOption.DOT_MATCHES_ALL)')
code = code.replace('Regex("(?i)Due to:\s*(.+)", RegexOption.DOT_MATCHES_ALL)', 'Regex("""(?i)Due to:\\s*(.+)""", RegexOption.DOT_MATCHES_ALL)')

code = code.replace('Regex("\\n+")', 'Regex("""\\n+""")')
code = code.replace('Regex("\n+")', 'Regex("""\\n+""")')

with open('app/src/main/java/com/safarparmar/app/ui/nishtha/checkin/CheckInScreen.kt', 'w') as f:
    f.write(code)

