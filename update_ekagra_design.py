with open('app/src/main/java/com/safarparmar/app/ui/ekagra/EkagraDesign.kt', 'r') as f:
    code = f.read()

code = code.replace(".height(EkagraChrome.size(46f))", ".height(EkagraChrome.size(42f))")
code = code.replace(".padding(horizontal = EkagraChrome.size(26f))", ".padding(horizontal = EkagraChrome.size(16f))")

with open('app/src/main/java/com/safarparmar/app/ui/ekagra/EkagraDesign.kt', 'w') as f:
    f.write(code)

