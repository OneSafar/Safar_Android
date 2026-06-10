# Buttons

Buttons are one of the most recognisable components in Material Design.  In Material 3 you can choose from several button variants to match the hierarchy and emphasis of your app’s actions.  This file describes the key button types in Jetpack Compose and provides code examples from the Android Developer documentation.

## Basic button API

All button types in Compose share a common set of parameters:

| Parameter | Description |
| --- | --- |
| `onClick` | Lambda invoked when the user taps the button. |
| `enabled` | Controls whether the button responds to user input. |
| `modifier` | Applies layout, style or behaviour modifiers. |
| `shape` | Overrides the corner radius; defaults to `MaterialTheme.shapes.small`. |
| `colors` | A `ButtonColors` object defining container and content colours. |
| `contentPadding` | Padding inside the button. |

When you need a custom button that isn’t covered by the prebuilt variants, use the basic `Button` composable and supply your own parameters.

## Filled button

The **filled button** is the default button style in Material 3.  It uses the `Button` composable and is filled with the primary colour by default.  This variant is suited for high‑emphasis actions【248704915920315†L888-L897】.

```kotlin
@Composable
fun FilledButtonExample(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text("Filled")
    }
}
```

## Filled tonal button

A **tonal button** uses the `FilledTonalButton` composable.  It is filled with a tonal variant of the primary colour and is appropriate for secondary actions【248704915920315†L910-L921】.

```kotlin
@Composable
fun FilledTonalButtonExample(onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) {
        Text("Tonal")
    }
}
```

## Outlined button

An **outlined button** is transparent and has an outline stroke.  Use the `OutlinedButton` composable for medium‑emphasis actions that require less prominence【248704915920315†L929-L944】.

```kotlin
@Composable
fun OutlinedButtonExample(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Text("Outlined")
    }
}
```

## Elevated button

An **elevated button** includes a shadow in addition to a filled background.  Use `ElevatedButton` for high‑emphasis actions that need to stand out from the surface【248704915920315†L950-L969】.

```kotlin
@Composable
fun ElevatedButtonExample(onClick: () -> Unit) {
    ElevatedButton(onClick = onClick) {
        Text("Elevated")
    }
}
```

## Text button

A **text button** appears as unfilled text until pressed.  Use it for low‑emphasis actions, such as secondary actions in a dialog【248704915920315†L972-L989】.

```kotlin
@Composable
fun TextButtonExample(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text("Text Button")
    }
}
```

## Floating action button (FAB)

Floating action buttons are circular or extended buttons that float above your UI and represent the primary action.  Material 3 provides `FloatingActionButton`, `ExtendedFloatingActionButton` and a number of default colours and elevations.

An **extended FAB** combines an icon and label.  Use it for prominent actions with descriptive labels, such as adding a new item【641042291835853†L1341-L1354】:

```kotlin
@Composable
fun ExtendedFabExample(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = {
            Icon(imageVector = Icons.Default.Edit, contentDescription = null)
        },
        text = {
            Text("Add item")
        }
    )
}
```

For a simple circular FAB you can use `FloatingActionButton`:

```kotlin
@Composable
fun FabExample(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null)
    }
}
```

## Customising buttons

All button composables accept optional parameters for colours, shapes, elevation and content padding.  Use `ButtonDefaults.buttonColors()` to provide your own `containerColor` and `contentColor`, and `ButtonDefaults.elevatedButtonElevation()` for custom elevations.  Avoid overriding built‑in semantics such as disabled state colours and focus indicators so that your buttons remain accessible.