import re

with open('app/src/main/java/com/safarparmar/app/ui/premium/PremiumPaywallScreen.kt', 'r') as f:
    content = f.read()

# Replace any Card(...) { that uses PlannerFlatColors.CardWhite
# We will find all indices of "Card(" and see if they contain CardWhite before the matching brace.
def replace_cards(text):
    result = ""
    idx = 0
    while True:
        start_idx = text.find("Card(", idx)
        if start_idx == -1:
            result += text[idx:]
            break
        
        result += text[idx:start_idx]
        
        # Find matching closing parenthesis for Card(
        paren_count = 0
        in_card = False
        end_paren = -1
        for i in range(start_idx, len(text)):
            if text[i] == '(':
                paren_count += 1
                in_card = True
            elif text[i] == ')':
                paren_count -= 1
                if in_card and paren_count == 0:
                    end_paren = i
                    break
        
        if end_paren != -1:
            card_args = text[start_idx:end_paren+1]
            if "PlannerFlatColors.CardWhite" in card_args:
                # Need to also find the "{" right after the Card()
                brace_start = text.find("{", end_paren)
                if brace_start != -1 and text[end_paren+1:brace_start].strip() == "":
                    result += "PremiumStyleGroup {"
                    idx = brace_start + 1
                    continue
            
        result += "Card("
        idx = start_idx + 5

    return result

content = replace_cards(content)

# We also need to fix PlanHairline -> GroupDivider() in the Main Column!
content = content.replace("PlanHairline(alpha = 0.5f)", "GroupDivider()")

with open('app/src/main/java/com/safarparmar/app/ui/premium/PremiumPaywallScreen.kt', 'w') as f:
    f.write(content)

