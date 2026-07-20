# Liquid Glass — Jetpack Compose Design System Template

A drop-in "liquid glass" UI kit for Jetpack Compose: a deep aurora-blurred
backdrop with translucent frosted panels floating on top, in the spirit of
modern glass-material design languages. Five files, all in one package —
copy them into your app and start swapping your existing UI for the
`Glass*` equivalents.

## Files

| File                  | Contents |
|-----------------------|----------|
| `GlassTheme.kt`       | Color palette (`GlassPalette`), dark color scheme, shape scale, `LiquidGlassTheme` wrapper |
| `GlassEffects.kt`     | `Modifier.liquidGlass(...)` — the core frosted-panel look — and `LiquidGlassBackdrop` — the animated blurred background |
| `GlassComponents.kt`  | `GlassLabel`, `GlassButton`, `GlassIconButton`, `GlassChip`, `GlassBadge`, `GlassSwitch`, `GlassTextField`, `GlassCard`, `GlassListItem`, `GlassDivider` |
| `GlassScaffold.kt`    | `GlassTopBar`, `GlassBottomNavBar`, `GlassFAB`, `GlassBottomSheet`, `GlassOverlay`, `GlassDialogCard` |
| `GlassDemoScreen.kt`  | `LiquidGlassDemoScreen` — a full example screen wiring every component together, plus an example `Activity` |

## Setup

1. Copy all five `.kt` files into a package in your app (e.g.
   `app/src/main/java/com/yourapp/ui/glass/`) and update the `package`
   line at the top of each file to match.
2. Make sure these dependencies are in your module's `build.gradle`:

   ```kotlin
   dependencies {
       implementation(platform("androidx.compose:compose-bom:2024.09.00")) // or newer
       implementation("androidx.compose.material3:material3")
       implementation("androidx.compose.material:material-icons-extended")
       implementation("androidx.activity:activity-compose:1.9.0")
   }
   ```

   (`material-icons-extended` is only needed for the icons used in
   `GlassDemoScreen.kt` — `Palette`, `Groups`, `RocketLaunch`, etc. Feel
   free to swap in whatever icons you already depend on.)
3. Wrap your app's root content in the theme:

   ```kotlin
   setContent {
       LiquidGlassTheme {
           YourAppRoot()
       }
   }
   ```
4. Put `LiquidGlassBackdrop()` as the first child of a root `Box` on any
   screen where you want the drifting aurora background, then stack your
   glass panels on top of it — see `LiquidGlassDemoScreen` for the pattern.

## Applying it to your own screens

Swap your existing widgets for the glass equivalents one at a time:

```kotlin
Box(Modifier.fillMaxSize()) {
    LiquidGlassBackdrop(Modifier.fillMaxSize())

    Column(Modifier.fillMaxSize()) {
        GlassTopBar(title = "Your Screen")

        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item { GlassCard { /* any content */ } }
            items(yourData) { row -> GlassListItem(title = row.name) }
        }
    }

    GlassFAB(icon = Icons.Rounded.Add, onClick = { /* ... */ },
        modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp))
}
```

Bottom navigation:

```kotlin
val items = listOf(
    GlassNavItem("Home", Icons.Rounded.Home),
    GlassNavItem("Search", Icons.Rounded.Search),
    GlassNavItem("Profile", Icons.Rounded.Person),
)
GlassBottomNavBar(items = items, selectedIndex = tab, onSelect = { tab = it })
```

Any surface — a card, a button, an image container — can get the glass
treatment directly:

```kotlin
Box(Modifier.liquidGlass(surfaceTint = GlassPalette.Cyan, tintAlpha = 0.3f)) { ... }
```

## Notes on the blur

`LiquidGlassBackdrop` uses a real `RenderEffect` blur on Android 12+ (API
31+). On older versions it falls back to plain soft-edged gradients — no
blur, but still a deliberate, layered look, so the design degrades
gracefully rather than breaking. If your `minSdk` is below 31 that's fine;
just know the "frosted" softness will be a bit crisper on older devices.

## Customizing the look

- Swap the four accent colors in `GlassPalette` (`Violet`, `Cyan`, `Coral`,
  `Amber`) for your brand colors — everything else derives from them.
- Tune `tintAlpha` per-component: ~0.08–0.14 reads as glass, higher values
  (0.3+) read as a solid tinted surface — useful for primary buttons/FABs
  that need to stay legible.
- Corner radii live in `LiquidGlassShapes` and as `shape` parameters on
  individual components — the whole system leans on generous rounding, so
  keep new components consistent with that scale (14–36dp).
