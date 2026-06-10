# Layouts and surfaces

Material 3 is more than individual widgets; it provides guidelines for structuring screens and handling elevation.  Jetpack Compose offers containers such as `Scaffold`, `Surface`, `Row`, `Column` and lazy lists that integrate with the Material 3 theme.

## Scaffold

`Scaffold` is the foundational layout for many Compose screens.  It provides slots for common parts of a screen, such as `topBar`, `bottomBar`, `floatingActionButton`, `snackbarHost` and `drawerContent`.  Components such as app bars, navigation bars and floating action buttons integrate smoothly within a `Scaffold`.

```kotlin
@Composable
fun MyScreen() {
    Scaffold(
        topBar = { SmallTopAppBar(/* ... */) },
        bottomBar = { NavigationBarExample() },
        floatingActionButton = { FloatingActionButton(onClick = { /* primary action */ }) { Icon(Icons.Filled.Add, null) } },
    ) { innerPadding ->
        // Main content area; apply inner padding to avoid overlapping bars
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(100) { index -> Text("Item $index", modifier = Modifier.padding(16.dp)) }
        }
    }
}
```

## Surface

`Surface` is a lower‑level container that provides a material‐coloured background, elevation and shape.  In Material 3, `Surface` includes parameters for both **tonal elevation** and **shadow elevation**【641042291835853†L1293-L1316】.  Tonal elevation applies a colour overlay based on the primary colour; it is particularly important in dark themes where shadows alone are insufficient.  Shadow elevation adds a shadow behind the surface.

```kotlin
Surface(
    tonalElevation = 4.dp,
    shadowElevation = 2.dp,
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface
) {
    // content inside the surface
    Text("Elevated surface", modifier = Modifier.padding(16.dp))
}
```

Use `Surface` when you need explicit control over elevation, colour or shape.  Many high‑level components such as `Card` and `Button` are built on top of `Surface`.

## Rows and columns

Compose arranges child components using `Row` (horizontal) and `Column` (vertical) composables.  These support `Arrangement` and `Alignment` parameters for fine‑grained control.  Use `Modifier.weight()` to distribute available space among children.

```kotlin
Row(modifier = Modifier.fillMaxWidth()) {
    Text("Left", modifier = Modifier.weight(1f))
    Text("Right", modifier = Modifier.weight(1f))
}
```

## Lazy lists

`LazyColumn`, `LazyRow` and `LazyVerticalGrid` efficiently display large lists by composing only visible items.  Material 3 modifies scrolling behaviour with a **stretch overscroll effect** on Android 12 and above; Compose foundation 1.1.0 and later enable this by default【641042291835853†L1496-L1501】.  You can customise overscroll by applying `Modifier.scrollable()` or `Modifier.overscrollEffect()`.

```kotlin
LazyColumn {
    items(50) { index ->
        ListItem(/* your item content */)
        Divider()
    }
}
```

To implement swipe‑to‑dismiss or reordering, integrate `SwipeToDismiss`, `LazyItemScope.animateItemPlacement()` or similar utilities.

## Emphasis and spacing

Material 3 emphasises content using a combination of colour, elevation and typography.  Use **surface** colours (surface, surface‑variant, background) with corresponding `onSurface` colours to differentiate hierarchy【641042291835853†L1265-L1275】.  For spacing, Compose encourages 8 dp multiples to ensure rhythm and alignment across your UI.  Use `Spacer(modifier = Modifier.height(8.dp))` or `padding(16.dp)` to apply consistent spacing.

Proper layout structure and elevation control are key to building polished, adaptive UIs that scale from phones to tablets and beyond.