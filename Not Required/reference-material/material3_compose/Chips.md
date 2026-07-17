# Chips

**Chips** are compact elements that allow users to enter information, select choices, filter content or trigger actions.  Material 3 defines four chip types: **assist**, **filter**, **input** and **suggestion**.  Compose provides a dedicated composable for each type, along with “elevated” variants for additional emphasis.

## Common parameters

All chip composables share a few key parameters【897895251418885†L870-L879】:

| Parameter | Description |
| --- | --- |
| `label` | A lambda that returns the text displayed on the chip. |
| `icon` / `leadingIcon` / `trailingIcon` | Optional icons displayed at the start or end of the chip. |
| `onClick` | Lambda invoked when the chip is pressed. |
| `selected` | For filter and input chips, controls the selected state. |

## Assist chips

Assist chips guide the user during a task.  They often appear temporarily in response to input.  Use the `AssistChip` composable; it includes a `leadingIcon` parameter for a small icon【897895251418885†L879-L899】.

```kotlin
@Composable
fun AssistChipExample() {
    AssistChip(
        onClick = { /* handle click */ },
        label = { Text("Assist chip") },
        leadingIcon = {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Settings",
                Modifier.size(AssistChipDefaults.IconSize)
            )
        }
    )
}
```

## Filter chips

Filter chips let users refine content from a set of options.  They can be selected or deselected, and may display a checkmark when selected.  The `FilterChip` composable requires you to track whether the chip is selected【897895251418885†L910-L937】.

```kotlin
@Composable
fun FilterChipExample() {
    var selected by remember { mutableStateOf(false) }
    FilterChip(
        onClick = { selected = !selected },
        label = { Text("Filter chip") },
        selected = selected,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Done",
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        }
    )
}
```

## Input chips

Input chips represent user‑entered information (for example, recipients in an email app).  They can show an avatar and a trailing icon for dismissal.  Use `InputChip` and manage the selected state; the chip should call an `onDismiss` lambda when the user removes it【897895251418885†L950-L995】.

```kotlin
@Composable
fun InputChipExample(text: String, onDismiss: () -> Unit) {
    var enabled by remember { mutableStateOf(true) }
    if (!enabled) return
    InputChip(
        onClick = {
            onDismiss()
            enabled = false
        },
        label = { Text(text) },
        selected = enabled,
        avatar = {
            Icon(
                Icons.Filled.Person,
                contentDescription = "Person",
                Modifier.size(InputChipDefaults.AvatarSize)
            )
        },
        trailingIcon = {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                Modifier.size(InputChipDefaults.AvatarSize)
            )
        }
    )
}
```

## Suggestion chips

Suggestion chips offer dynamically generated prompts, such as possible responses in a chat app.  They take only a `label` and an `onClick` lambda【897895251418885†L1003-L1021】.

```kotlin
@Composable
fun SuggestionChipExample() {
    SuggestionChip(
        onClick = { /* handle suggestion */ },
        label = { Text("Suggestion chip") }
    )
}
```

## Elevated chips

All of the base chip types have **elevated** variants that include a subtle shadow.  Use `ElevatedAssistChip`, `ElevatedFilterChip` and `ElevatedSuggestionChip` when you need more emphasis【897895251418885†L1030-L1037】.

## Best practices

* **Use chips sparingly:** Too many chips can clutter your UI.  Present a concise set of choices or actions.
* **Manage state externally:** For filter and input chips, hold the selected/dismissed state in a `remember` variable or in your view model.
* **Provide descriptive icons:** Use icons that clearly convey the chip’s function.  For example, a checkmark for a selected filter chip or a close icon for dismissal.
* **Consider elevated chips** when you need additional visual hierarchy or to separate chips from a busy background.