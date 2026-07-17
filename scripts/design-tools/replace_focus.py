import os
import re

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    new_content = content
    changed = False

    if filepath.endswith('.kt'):
        # Find all string literals
        # This regex finds "..." strings (not handling multiline """ perfectly, but usually fine)
        def replace_in_string(match):
            original_string = match.group(0)
            new_string = original_string.replace('Focus', 'Ekagra')
            # Uncomment the next line if lowercase replacement is also desired
            new_string = new_string.replace('focus', 'ekagra')
            return new_string
        
        # We need to replace inside "..."
        # But let's avoid replacing things that aren't strings.
        # A simpler way is to split by ", but escaped quotes are tricky.
        # Alternatively, we can just replace 'Focus' with 'Ekagra' if it's not preceded by a letter, to avoid breaking camelCase.
        # E.g. \bFocus\b -> Ekagra
        # Let's just do a regex replace for word boundary Focus and focus, but NOT if it's part of camelCase like 'hasFocus'
        # Actually, in Kotlin, \bFocus\b will match 'Focus' in 'Goal Focus', but not 'hasFocus' (it's hasFocus, wait, \b matches between s and F? No, \b is between non-word and word. 's' and 'F' are both word characters, so there is NO \b between them! Perfect!)
        # So \bFocus\b will NOT match 'hasFocus' or 'focusRequester' (because 'focus' is followed by 'R').
        # Wait, what if it's 'FocusManager'? \bFocusManager is a word. \bFocus\b will NOT match 'FocusManager'.
        # This is incredibly safe!
        
        new_content = re.sub(r'\bFocus\b', 'Ekagra', new_content)
        new_content = re.sub(r'\bfocus\b', 'ekagra', new_content)

    elif filepath.endswith('.xml'):
        new_content = re.sub(r'\bFocus\b', 'Ekagra', new_content)
        new_content = re.sub(r'\bfocus\b', 'ekagra', new_content)

    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {filepath}")

def main():
    target_dir = r'd:\SAFAR_PARENT\Safar_Android\Safar_Android\app\src\main'
    for root, dirs, files in os.walk(target_dir):
        for file in files:
            if file.endswith('.kt') or file.endswith('.xml'):
                process_file(os.path.join(root, file))

if __name__ == "__main__":
    main()
