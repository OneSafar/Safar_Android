with open('app/src/main/java/com/safarparmar/app/ui/dashboard/DashboardScreen.kt', 'r') as f:
    text = f.read()

bad_str = """    }
}
}

private fun macTextColor"""

good_str = """    }
}

private fun macTextColor"""

text = text.replace(bad_str, good_str)

with open('app/src/main/java/com/safarparmar/app/ui/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(text)
