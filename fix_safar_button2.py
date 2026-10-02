import re

with open('app/src/main/java/com/safarparmar/app/ui/dashboard/DashboardScreen.kt', 'r') as f:
    content = f.read()

safar_button_old = """                com.safarparmar.app.ui.components.SafarButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBg,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.dashboard_lets_begin),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp,
                    )
                }"""

safar_button_new = """                com.composables.ui.components.Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    style = com.composables.ui.components.ButtonStyle.Primary,
                ) {
                    com.composables.ui.components.Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    com.composables.ui.components.Text(
                        text = stringResource(R.string.dashboard_lets_begin),
                    )
                }"""
content = content.replace(safar_button_old, safar_button_new)

with open('app/src/main/java/com/safarparmar/app/ui/dashboard/DashboardScreen.kt', 'w') as f:
    f.write(content)

