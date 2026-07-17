# Additional Compose Material 3 Components

Material 3 provides a wide range of interactive components beyond buttons, cards and navigation.  This file explains how to implement dialogs, menus, selection controls (checkboxes, switches and radio buttons) and date pickers using Jetpack Compose.  Each section summarises the API surface, lists the most important parameters and shows sample code.  The examples and guidelines come directly from the Android developers documentation and the Material 3 design guidelines【742897159616975†L865-L919】【423209597717712†L856-L919】.

## Dialogs

Dialogs are temporary surfaces that appear above the content to prompt the user for a decision or display critical information.  Compose includes two primary dialog composables:

* **`AlertDialog`** – a Material‑themed dialog that provides slots for a title, text, icon, confirm button and dismiss button.  You must supply an `onDismissRequest` lambda that is called when the user dismisses the dialog (for example by tapping outside it).  The API exposes parameters for `title`, `text`, `icon`, `confirmButton` and `dismissButton`【742897159616975†L865-L876】.
* **`Dialog`** – a bare container without any predefined styling or slots.  It simply overlays content; you must provide your own container (usually a `Card`) and specify the size, shape and content.  It accepts `onDismissRequest` and optional `properties` for custom behaviour【742897159616975†L966-L978】.

### Alert dialog example

The following composable uses `AlertDialog` to display a message with confirm and dismiss buttons.  The parent composable controls when the dialog is shown by toggling a boolean state.  The `onDismissRequest` and `confirmButton` lambdas update that state.

```kotlin
@Composable
fun AlertDialogExample(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector,
) {
    AlertDialog(
        icon = { Icon(icon, contentDescription = "Example Icon") },
        title = { Text(text = dialogTitle) },
        text = { Text(text = dialogText) },
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(onClick = { onConfirmation() }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = { onDismissRequest() }) { Text("Dismiss") }
        }
    )
}
```

You would call this composable from a parent `DialogExamples` composable that holds a `remember { mutableStateOf(false) }` flag.  When the flag is `true` the `AlertDialogExample` is shown; tapping either button calls the provided callbacks and closes the dialog【742897159616975†L927-L949】.

### Custom dialog

If you need a more complex layout than what `AlertDialog` supports, use `Dialog` and provide your own container.  The example below shows how to build a modal dialog with an image, text and two actions.  The `Card` provides the surface and shape, while `Column` and `Row` arrange the content【742897159616975†L1015-L1078】.

```kotlin
@Composable
fun DialogWithImage(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    painter: Painter,
    imageDescription: String,
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(375.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painter,
                    contentDescription = imageDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(160.dp)
                )
                Text(
                    text = "This is a dialog with buttons and an image.",
                    modifier = Modifier.padding(16.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(onClick = { onDismissRequest() }, modifier = Modifier.padding(8.dp)) {
                        Text("Dismiss")
                    }
                    TextButton(onClick = { onConfirmation() }, modifier = Modifier.padding(8.dp)) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}
```

When implementing custom dialogs, remember that you must explicitly set their size, shape and content; `Dialog` does not provide default styling【742897159616975†L966-L978】.

## Menus

Drop‑down menus present a list of actions or options anchored to another UI element.  Compose provides `DropdownMenu` and `DropdownMenuItem` in the Material 3 library.  A third component, `IconButton`, is often used as the trigger to show or hide the menu【423209597717712†L856-L919】.

### API surface

* `DropdownMenu(expanded, onDismissRequest, content)` – The main container.  Set `expanded` to control whether the menu is visible and provide an `onDismissRequest` lambda to hide it when the user taps outside.  The `content` block contains one or more `DropdownMenuItem` composables【423209597717712†L859-L870】.
* `DropdownMenuItem(text, onClick)` – Represents a selectable item in the menu.  The `text` parameter holds the label and `onClick` defines the action when the item is selected【423209597717712†L871-L874】.

### Basic drop‑down menu example

This example shows a minimal menu anchored to an `IconButton`.  The menu’s visibility is controlled by a `Boolean` state variable; clicking the button toggles this state【423209597717712†L876-L904】.

```kotlin
@Composable
fun MinimalDropdownMenu() {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(16.dp)) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Option 1") },
                onClick = { /* handle click */ }
            )
            DropdownMenuItem(
                text = { Text("Option 2") },
                onClick = { /* handle click */ }
            )
        }
    }
}
```

### Scrollable and detailed menus

`DropdownMenu` automatically becomes scrollable when there are too many items to fit onscreen.  The following variant demonstrates generating a list of 100 options; note that all items are created eagerly inside the composition【423209597717712†L926-L971】.

```kotlin
@Composable
fun LongBasicDropdownMenu() {
    var expanded by remember { mutableStateOf(false) }
    val menuItemData = List(100) { "Option ${it + 1}" }
    Box(modifier = Modifier.padding(16.dp)) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            menuItemData.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { /* handle */ })
            }
        }
    }
}
```

You can also add icons and group items with dividers.  The example below uses multiple `DropdownMenuItem` components with `leadingIcon` and `trailingIcon` slots, separated by `HorizontalDivider`【423209597717712†L981-L1047】.

```kotlin
@Composable
fun DropdownMenuWithDetails() {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // First section
            DropdownMenuItem(
                text = { Text("Profile") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                onClick = { }
            )
            DropdownMenuItem(
                text = { Text("Settings") },
                leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                onClick = { }
            )
            HorizontalDivider()
            // Second section
            DropdownMenuItem(
                text = { Text("Send Feedback") },
                leadingIcon = { Icon(Icons.Outlined.Feedback, contentDescription = null) },
                trailingIcon = { Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null) },
                onClick = { }
            )
            HorizontalDivider()
            // Third section
            DropdownMenuItem(
                text = { Text("About") },
                leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                onClick = { }
            )
            DropdownMenuItem(
                text = { Text("Help") },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Help, contentDescription = null) },
                trailingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
                onClick = { }
            )
        }
    }
}
```

## Checkboxes

Checkboxes allow users to select one or more items from a set.  Use them when the user can choose multiple options; if only one option can be selected, use a radio button instead【679367609368299†L846-L857】.  A checkbox consists of a box, a checkmark and an optional label【679367609368299†L860-L867】, and has three states: unselected, selected and indeterminate【679367609368299†L870-L875】.

### Minimal checkbox

The `Checkbox` composable has two essential parameters: `checked` and `onCheckedChange`.  `checked` controls whether the box is selected, and `onCheckedChange` is called when the user taps the checkbox.  The example below shows a simple checkbox with text and displays its state【679367609368299†L883-L913】.

```kotlin
@Composable
fun CheckboxMinimalExample() {
    var checked by remember { mutableStateOf(true) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Minimal checkbox")
        Checkbox(
            checked = checked,
            onCheckedChange = { checked = it }
        )
    }
    Text(if (checked) "Checkbox is checked" else "Checkbox is unchecked")
}
```

### Parent–child checkboxes

For lists of items where selecting the parent toggles all children, use a `TriStateCheckbox` for the parent.  The parent’s `state` parameter can be `On`, `Off` or `Indeterminate`; clicking it toggles all child checkboxes.  Each child `Checkbox` has its own `checked` state.  The parent state is computed from the child states【679367609368299†L942-L989】:

```kotlin
@Composable
fun CheckboxParentExample() {
    // States for three child checkboxes
    val childCheckedStates = remember { mutableStateListOf(false, false, false) }
    // Compute parent state: On if all children are checked, Off if none, Indeterminate otherwise
    val parentState = when {
        childCheckedStates.all { it } -> ToggleableState.On
        childCheckedStates.none { it } -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    }
    Column {
        // Parent row with label and TriStateCheckbox
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Select all")
            TriStateCheckbox(state = parentState, onClick = {
                val newState = parentState != ToggleableState.On
                childCheckedStates.indices.forEach { index ->
                    childCheckedStates[index] = newState
                }
            })
        }
        // Child checkboxes
        childCheckedStates.forEachIndexed { index, checked ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Option ${index + 1}")
                Checkbox(
                    checked = checked,
                    onCheckedChange = { isChecked -> childCheckedStates[index] = isChecked }
                )
            }
        }
    }
    if (childCheckedStates.all { it }) {
        Text("All options selected")
    }
}
```

When you need a checkbox that can display an indeterminate state, use `TriStateCheckbox` instead of `Checkbox`【679367609368299†L1001-L1023】.

## Switches

A switch is a binary toggle used to turn a setting or feature on or off.  It consists of a thumb (the circular knob) that slides along a track.  Users can tap or drag the thumb to toggle the state【485231568122358†L848-L860】.

### Basic implementation

The basic `Switch` composable includes `checked` and `onCheckedChange` parameters.  You can optionally provide `enabled` to disable the component, and `colors` to customise its appearance【485231568122358†L865-L876】.

```kotlin
@Composable
fun SwitchMinimalExample() {
    var checked by remember { mutableStateOf(true) }
    Switch(
        checked = checked,
        onCheckedChange = { checked = it }
    )
}
```

### Customising the switch

To add more personality, customise the thumb content or colours:

* **Custom thumb:** Provide a `thumbContent` lambda that returns a composable.  The example below shows a check icon when the switch is on【485231568122358†L909-L936】.

  ```kotlin
  @Composable
  fun SwitchWithIconExample() {
      var checked by remember { mutableStateOf(true) }
      Switch(
          checked = checked,
          onCheckedChange = { checked = it },
          thumbContent = if (checked) {
              { Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
          } else {
              null
          }
      )
  }
  ```

* **Custom colours:** Use the `colors` parameter to set different colours for the thumb and track based on the checked state.  The following example uses values from `MaterialTheme.colorScheme`【485231568122358†L949-L969】:

  ```kotlin
  @Composable
  fun SwitchWithCustomColors() {
      var checked by remember { mutableStateOf(true) }
      Switch(
          checked = checked,
          onCheckedChange = { checked = it },
          colors = SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.primary,
              checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
              uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
              uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer,
          )
      )
  }
  ```

## Radio buttons

Radio buttons allow users to select exactly one option from a list.  You should wrap each `RadioButton` and its label in a `Row` and group the rows inside a `Column`.  For proper accessibility, call `Modifier.selectableGroup()` on the `Column` and `Modifier.selectable()` on each `Row`【772023406081861†L875-L907】.

### API surface

* `RadioButton(selected, onClick)` – the radio button itself.  When `onClick` is set to `null`, the button is non‑interactive and selection is handled by the `Row`’s `selectable` modifier.
* `Modifier.selectableGroup()` – identifies a group of selectable items for accessibility.  Must be applied to the container `Column`【772023406081861†L884-L927】.
* `Modifier.selectable()` – applied to each `Row` to handle selection; use `role = Role.RadioButton` to inform accessibility services【772023406081861†L890-L936】.

### Example: Single selection list

```kotlin
@Composable
fun RadioButtonSingleSelection(modifier: Modifier = Modifier) {
    val radioOptions = listOf("Calls", "Missed", "Friends")
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0]) }
    Column(modifier.selectableGroup()) {
        radioOptions.forEach { text ->
            Row(
                modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .selectable(
                        selected = (text == selectedOption),
                        onClick = { onOptionSelected(text) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (text == selectedOption),
                    onClick = null // handled by the Row
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
```

Using `onClick = null` on the `RadioButton` itself ensures that the selection is handled by the enclosing `Row`, which improves accessibility for screen readers【772023406081861†L929-L940】.

## Date pickers

Material 3 introduces updated date picker components: `DatePicker`, `DatePickerDialog` and `DateRangePicker`.  These composables are currently experimental【938715940730462†L864-L873】.  All date picker variants take a `state` parameter—either `DatePickerState` or `DateRangePickerState`—which exposes the selected date or date range【938715940730462†L875-L880】.

### Docked date picker

You can embed a docked date picker within a form by showing it in a `Popup` anchored to an input field.  In the example below, clicking a calendar icon toggles `showDatePicker` and displays a `DatePicker` beneath an `OutlinedTextField`【938715940730462†L887-L939】.  The selected date is formatted using a helper function and displayed in the text field.

```kotlin
@Composable
fun DatePickerDocked() {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val selectedDate = datePickerState.selectedDateMillis?.let { convertMillisToDate(it) } ?: ""
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedDate,
            onValueChange = { },
            label = { Text("DOB") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = !showDatePicker }) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select date")
                }
            },
            modifier = Modifier.fillMaxWidth().height(64.dp)
        )
        if (showDatePicker) {
            Popup(onDismissRequest = { showDatePicker = false }, alignment = Alignment.TopStart) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 64.dp)
                        .shadow(elevation = 4.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    DatePicker(state = datePickerState, showModeToggle = false)
                }
            }
        }
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}
```

Key points:

* Clicking the calendar icon toggles the `showDatePicker` state, controlling the visibility of the `DatePicker`【938715940730462†L887-L939】.
* A `Popup` overlays the picker without affecting the surrounding layout and positions it below the text field using `offset`【938715940730462†L920-L931】.
* The selected date is retrieved from `DatePickerState.selectedDateMillis` and formatted for display【938715940730462†L895-L900】.

### Modal date picker

For dialogs, wrap a `DatePicker` inside a `DatePickerDialog`.  Provide confirm and dismiss buttons to handle the selection.  The sample below passes the selected date back through a callback【938715940730462†L1020-L1048】.

```kotlin
@Composable
fun DatePickerModal(onDateSelected: (Long?) -> Unit, onDismiss: () -> Unit) {
    val datePickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
```

The `state` parameter shares selection information with the parent composable.  When the user confirms, pass `selectedDateMillis` to the `onDateSelected` callback and dismiss the dialog【938715940730462†L1020-L1048】.

### Date range picker

`DateRangePicker` works similarly to `DatePicker` but allows selecting a start and end date.  It uses `DateRangePickerState` instead of `DatePickerState`, and you typically return a `Pair<Long?, Long?>` to indicate the range【938715940730462†L1118-L1123】.  The `DatePickerDialog` accepts a `DateRangePicker` as its content, and you pass both selected values to the parent when the user confirms.

---

These additional components provide the building blocks for complex user interfaces.  Combined with the theming, layout and navigation guidance in the other files, they enable you to build polished, production‑quality UIs using Material 3 in Jetpack Compose.