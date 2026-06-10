# Motion and elevation

Material 3 introduces subtle yet meaningful changes to how elevation and motion are represented.  Understanding these concepts helps you create responsive, tactile interfaces.

## Tonal vs shadow elevation

Material 3 represents elevation using **tonal overlays** in addition to traditional shadows.  Tonal overlays adjust the surface colour to a higher tone from the colour palette, giving depth without relying solely on shadows【641042291835853†L1293-L1316】.  Shadows are still used, but they are combined with tonal elevation to create a more harmonious appearance.

Use the `Surface` composable’s `tonalElevation` and `shadowElevation` parameters to customise both aspects:

```kotlin
Surface(
    tonalElevation = 8.dp,
    shadowElevation = 4.dp,
    modifier = Modifier.padding(16.dp),
    shape = MaterialTheme.shapes.medium
) {
    // content
}
```

Increase `tonalElevation` to move a container visually forward (darker overlay in light themes or lighter overlay in dark themes).  Adjust `shadowElevation` to control the drop shadow.  In dark themes the tonal overlay is essential for distinguishing surfaces.

## Ripple and touch feedback

Material 3 updates the ripple effect with a subtle sparkle.  Compose uses the platform `RippleDrawable` under the hood, so all Material 3 components get this new effect automatically on Android 12 and above【641042291835853†L1487-L1493】.  You don’t need to configure anything to enable it.

## Overscroll

Overscroll in Compose now uses a **stretch effect** that pulls the content at the edges of scrollable containers【641042291835853†L1496-L1501】.  This effect is enabled by default in `LazyColumn`, `LazyRow` and `LazyVerticalGrid` and works across API levels when you use foundation 1.1.0 or newer.  If you need the old glow effect, you can override the overscroll configuration on your container.

```kotlin
val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
// apply nested scroll connection to enable collapse/expand animations
Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = { /* top bar */ },
    content = { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            items(100) { index ->
                Text("Item $index")
            }
        }
    }
)
```

## Animations and motion

Compose provides a rich set of animation APIs for motion: `updateTransition`, `animate*AsState`, `rememberInfiniteTransition` and more.  While not specific to Material 3, animations help convey changes in state and surface elevation.  Material 3 Expressive introduces motion theming that aligns animations with the design system; explore `material3-motion` libraries and design guidelines for details.  Use animations judiciously to reinforce hierarchy and user intent.

Motion and elevation are subtle tools—when combined with consistent theming, they provide depth and tactility without distracting users.