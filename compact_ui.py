import re
import os

files_to_process = [
    "app/src/main/java/com/safarparmar/app/ui/settings/SettingsScreen.kt",
    "app/src/main/java/com/safarparmar/app/ui/profile/ProfileScreen.kt",
    "app/src/main/java/com/safarparmar/app/ui/premium/PremiumPaywallScreen.kt",
    "app/src/main/java/com/safarparmar/app/ui/nishtha/NishthaScreen.kt"
]

def reduce_sp(match):
    val = float(match.group(1))
    if val >= 24:
        new_val = val * 0.85
    elif 18 <= val <= 22:
        new_val = 16
    elif 14 <= val <= 16:
        new_val = 12
    elif 12 <= val <= 13.5:
        new_val = 11
    else:
        new_val = val
        
    if isinstance(new_val, float) and new_val.is_integer():
        new_val = int(new_val)
    elif isinstance(new_val, float):
        new_val = round(new_val, 1)
        if new_val.is_integer():
            new_val = int(new_val)
    new_val_str = f"{new_val:g}"
    return f"fontSize = {new_val_str}.sp"

def reduce_dp_size(match):
    pre = match.group(1)
    val = float(match.group(2))
    post = match.group(3)
    
    if val >= 24:
        new_val = round(val * 0.82)
    elif 20 <= val <= 22:
        new_val = 18
    elif 16 <= val <= 18:
        new_val = 14
    else:
        new_val = val
        
    if isinstance(new_val, float) and new_val.is_integer():
        new_val = int(new_val)
    new_val_str = f"{new_val:g}"
    return f"{pre}{new_val_str}{post}"

def reduce_dp_padding(match):
    pre = match.group(1)
    val = float(match.group(2))
    post = match.group(3)
    
    if val >= 20:
        new_val = round(val * 0.75)
    elif 12 <= val <= 16:
        new_val = 10
    elif 8 <= val <= 10:
        new_val = 6
    else:
        new_val = val
        
    if isinstance(new_val, float) and new_val.is_integer():
        new_val = int(new_val)
    new_val_str = f"{new_val:g}"
    return f"{pre}{new_val_str}{post}"

def reduce_shape(match):
    pre = match.group(1)
    val = float(match.group(2))
    post = match.group(3)
    
    if val >= 24:
        new_val = 16
    elif val == 16 or val == 18 or val == 20:
        new_val = 12
    else:
        new_val = val
        
    if isinstance(new_val, float) and new_val.is_integer():
        new_val = int(new_val)
    new_val_str = f"{new_val:g}"
    return f"{pre}{new_val_str}{post}"

def process_file(filepath):
    if not os.path.exists(filepath):
        print(f"Skipping {filepath}, does not exist")
        return
        
    with open(filepath, 'r') as f:
        content = f.read()
        
    # Replace fontSize = X.sp
    content = re.sub(r'fontSize\s*=\s*([0-9.]+)\.sp', reduce_sp, content)
    
    # Replace Icon sizes and other elements using .size(X.dp)
    content = re.sub(r'(\.size\s*\(\s*)([0-9.]+)(\.dp\s*\))', reduce_dp_size, content)
    
    # Replace heights
    content = re.sub(r'(\.height\s*\(\s*)([0-9.]+)(\.dp\s*\))', reduce_dp_size, content)
    
    # Replace padding arguments (horizontal = X.dp, vertical = Y.dp, etc)
    content = re.sub(r'((?:horizontal|vertical|start|end|top|bottom|all)\s*=\s*)([0-9.]+)(\.dp)', reduce_dp_padding, content)
    
    # Replace padding(X.dp) direct calls
    content = re.sub(r'(\.padding\s*\(\s*)([0-9.]+)(\.dp\s*\))', reduce_dp_padding, content)
    
    # Replace spacedBy
    content = re.sub(r'(Arrangement\.spacedBy\s*\(\s*)([0-9.]+)(\.dp\s*\))', reduce_dp_padding, content)
    
    # Replace corner shape radii
    content = re.sub(r'(RoundedCornerShape\s*\(\s*)([0-9.]+)(\.dp\s*\))', reduce_shape, content)

    with open(filepath, 'w') as f:
        f.write(content)
        
    print(f"Processed {filepath}")

for f in files_to_process:
    process_file(f)

