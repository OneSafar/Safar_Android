# Cards

Cards are Material 3 containers used to group related information and actions.  Jetpack Compose provides several card composables that correspond to Material Design guidelines: `Card`, `ElevatedCard` and `OutlinedCard`.  These composables expose parameters for colours, elevation, shape and content.

## Basic usage

The simplest way to create a card is to use the `Card` composable.  By default it uses the surface variant colour and a medium shape from `MaterialTheme.shapes`.  You can add any content inside a card:

```kotlin
@Composable
fun FilledCardExample() {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Title", style = MaterialTheme.typography.titleMedium)
            Text("Body text goes here.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
```

## Elevated card

An **elevated card** provides a shadow to distinguish it from the background.  Use the `ElevatedCard` composable and customise its `elevation` parameter via `CardDefaults.cardElevation()`【688772239458035†L930-L966】.  An example:

```kotlin
@Composable
fun ElevatedCardExample() {
    ElevatedCard(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.size(width = 240.dp, height = 100.dp)
    ) {
        Text(
            text = "Elevated",
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}
```

## Outlined card

An **outlined card** shows a border instead of a fill.  Use the `OutlinedCard` composable and specify a `BorderStroke` for the border.  The following example shows how to create an outlined card with a custom border and colours【688772239458035†L970-L999】:

```kotlin
@Composable
fun OutlinedCardExample() {
    OutlinedCard(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.Black),
        modifier = Modifier.size(width = 240.dp, height = 100.dp)
    ) {
        Text(
            text = "Outlined",
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}
```

## Best practices

* **Elevations and tonal overlays:** Use `ElevatedCard` to create hierarchy within a surface.  On dark themes, tonal overlays help differentiate surfaces【641042291835853†L1293-L1316】.
* **Size and shape:** Cards don’t provide their own scrolling or dismiss behaviour.  Integrate cards into other composables like `SwipeToDismiss` or `verticalScroll` if you need these behaviours【688772239458035†L1001-L1007】.
* **Interactable content:** Cards can contain buttons, images, text and other interactive elements.  Use `Modifier.clickable` on the card or place interactive elements inside the card to capture user actions.

Cards are a versatile way to structure UI; choose the variant that matches the desired emphasis and context.