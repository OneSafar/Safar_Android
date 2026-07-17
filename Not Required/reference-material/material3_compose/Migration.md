# Migration from Material 2 to Material 3 in Compose

Material Design 3 (often abbreviated **M3**) is the next evolution of Material Design. It introduces updated theming, components and personalization features such as dynamic color and is cohesive with the new visual style introduced with Material You on Android 12 and higher【253712462661658†L865-L912】. The Material 2 library (abbreviated **M2**) is still available in Compose, but you should not rely on both design systems long term; use a phased approach to migrate your app to Material 3【253712462661658†L865-L912】. This document summarises the differences between the M2 and M3 libraries and provides guidelines for migrating existing Compose apps.

## Approaches to migration

Migration should be performed gradually. The official guidelines recommend migrating the **design system** first (themes, colours, typography and shapes) and then updating composables and modules one at a time. The steps are【253712462661658†L865-L912】:

1. **Add the Material 3 dependency** (`androidx.compose.material3:material3`). Initially you can keep your existing Material 2 dependency to avoid breaking changes【253712462661658†L916-L925】.
2. **Define Material 3 themes** (colour scheme, typography and shapes) at the root of your app. Leave the old Material 2 theme wrappers around screens until each screen is migrated【253712462661658†L865-L912】.
3. **Migrate screens and components gradually** by replacing M2 composables with their M3 equivalents. Many components are drop‑in replacements (e.g., `BottomNavigation` becomes `NavigationBar`), but others require rethinking (see tables below).
4. **Remove Material 2 themes** once all screens have been migrated to M3. Avoid mixing M2 and M3 `MaterialTheme`s in the same composable hierarchy【253712462661658†L865-L912】.
5. **Remove the Material 2 dependency** once your app no longer uses any of its APIs【253712462661658†L865-L912】.

### When to migrate

M3 introduces features such as dynamic colour, expressive typography and updated component styles. Migrating early allows your app to align with new Android design guidance and system UI. However, M2 remains supported, so you can plan your migration around feature releases or design overhauls. If your app supports Android versions below 12, dynamic colour will gracefully fall back to static palettes.

## Dependencies and packaging

- **Material 2**: `androidx.compose.material:material`【253712462661658†L916-L925】.
- **Material 3**: `androidx.compose.material3:material3`【253712462661658†L916-L925】.

It is possible to include both dependencies during migration. Ensure you prefix imports correctly (`import androidx.compose.material3.*` for M3) to avoid accidentally using M2 composables.

## Theming differences

### `MaterialTheme` parameters

In M2, `MaterialTheme` accepts `colors`, `typography` and `shapes` parameters. The M3 theme uses `colorScheme`, `typography` and `shapes` instead【253712462661658†L953-L979】. When migrating:

- Replace `colors` with `colorScheme`. Colours are now grouped into **tonal roles** (primary, secondary, tertiary, error, etc.) rather than pairs of base colour and on‑colour【253712462661658†L984-L1043】.
- Continue to define your own `typography` and `shapes`, but note the differences described below.

### Colour system

The M3 colour system is based on tonal palettes rather than discrete variants. There is no direct one‑to‑one mapping from every M2 colour (e.g., `primaryVariant`, `secondaryVariant`) to M3. The recommended approach is to use the **Material Theme Builder** to generate a colour scheme from your brand colours or to adopt dynamic colour on Android 12+ devices【253712462661658†L984-L1043】. When manually mapping colours:

- `primary` in M2 maps to `primary` in M3.
- `primaryVariant` maps to `secondary`【253712462661658†L984-L1043】.
- `secondary` maps to `tertiary`【253712462661658†L984-L1043】.
- `secondaryVariant` maps to `tertiaryContainer`【253712462661658†L984-L1043】.
- `error` maps to `error`.

M3 does not include an `isLight` property on `ColorScheme`. If you need to infer whether you’re using a light or dark palette, store a boolean in a `CompositionLocal` and pass it through your theme【253712462661658†L1052-L1104】.

Dynamic colour is supported through the functions `dynamicLightColorScheme(context)` and `dynamicDarkColorScheme(context)`【253712462661658†L1118-L1127】, which extract colours from the user’s wallpaper on Android 12+ devices. Provide static fallback palettes for earlier versions.

### Typography differences

M3 introduces a new type scale with names like **displayLarge**, **headlineMedium** and **bodySmall**. The following table shows how common M2 styles map to M3【253712462661658†L1155-L1174】:

| M2 text style | M3 equivalent | Notes |
| --- | --- | --- |
| `h1`, `h2`, `h3` | `displayLarge`, `displayMedium`, `displaySmall` | Display styles are very large and are seldom used in apps. |
| `h4`, `h5`, `h6` | `headlineLarge`, `headlineMedium`, `headlineSmall` | Use for section headings. |
| `subtitle1`, `subtitle2` | `titleLarge`, `titleMedium` | Medium emphasis titles. |
| `body1`, `body2` | `bodyLarge`, `bodyMedium` | Default body text. |
| `button` | `labelLarge` | Use for button text. |
| `caption` | `labelSmall` | Use for captions or overlines. |

Additionally, M3 typography no longer includes a `defaultFontFamily` property. If you previously set a default font family in M2, you should specify it explicitly for each `TextStyle` in your `Typography` instance【253712462661658†L1155-L1174】.

### Shape differences

The M3 shape system introduces two new sizes—**extraSmall** and **extraLarge**—and uses consistent rounded corner sizes across components. The mapping from M2 shapes to M3 is【253712462661658†L1204-L1215】:

| M2 shape | M3 equivalent |
| --- | --- |
| `small` | `extraSmall` |
| `medium` | `small` |
| `large` | `medium` |
| — | `large`, `extraLarge` (new sizes) |

M3 removes the `none` and `full` special shapes; instead use `RectangleShape` to represent a shape with no corners and `CircleShape` for fully round shapes【253712462661658†L1204-L1215】.

## Component and layout differences

Material 3 introduces new components and removes or renames others. Use the following guidelines when updating your UI:

| M2 component/layout | M3 equivalent | Notes |
| --- | --- | --- |
| `BottomNavigation` | `NavigationBar`【253712462661658†L1230-L1258】 | M3 provides `NavigationBar` and `NavigationBarItem` for bottom navigation【924910518083618†L860-L874】. |
| `TopAppBar`, `TopAppBarDefaults` | `SmallTopAppBar`, `MediumTopAppBar`, `LargeTopAppBar`, `CenterAlignedTopAppBar`【253712462661658†L1230-L1258】 | M3 splits the top app bar into size variants. |
| `Drawer` / `BottomDrawer` | `ModalNavigationDrawer` or `PermanentNavigationDrawer`【253712462661658†L1230-L1258】 | There is no `BottomDrawer` in M3; use `ModalBottomSheet` instead. |
| `Chip` | `AssistChip`, `FilterChip`, `InputChip`, `SuggestionChip`【253712462661658†L1247-L1258】 | M3 breaks chips into specific types with distinct semantics【897895251418885†L879-L899】. |
| `Button`, `OutlinedButton`, etc. | `Button`, `FilledTonalButton`, `OutlinedButton`, `ElevatedButton`, `TextButton` | M3 still offers button variants but emphasises new tonal roles【248704915920315†L888-L897】. |
| `Scaffold` with `topBar`, `bottomBar` and drawers | `Scaffold` with `containerColor`, `contentWindowInsets` and `snackbarHost`; drawers moved out to `ModalNavigationDrawer`【253712462661658†L1340-L1473】 | M3 `Scaffold` no longer manages navigation drawers. |
| `BackdropScaffold` | **No direct equivalent**【253712462661658†L1230-L1258】 | Replace with `Scaffold` or `BottomSheetScaffold` and custom gestures. |
| `FloatingActionButton` (FAB) | `FloatingActionButton`, `ExtendedFloatingActionButton` | The API remains but uses M3 colours. |

Some M2 components have no replacement in M3 (e.g. `BackdropScaffold`). In such cases you should rebuild the UI using lower‑level layout primitives (`Scaffold`, `BottomSheetScaffold`, `LazyRow`, etc.).

## Scaffold differences

The `Scaffold` composable has changed significantly between M2 and M3:

- **Background vs container colour**: M2 uses `backgroundColor` to colour the scaffold; M3 replaces this with `containerColor`【253712462661658†L1340-L1473】. The default `containerColor` is the colour scheme’s surface colour.
- **State parameters**: The `ScaffoldState` parameter has been removed. Instead, pass a `SnackbarHost` to the `snackbarHost` parameter and manage its state via `SnackbarHostState`【253712462661658†L1340-L1473】.
- **Navigation drawers**: M3 `Scaffold` no longer includes `drawerContent` or `drawerGesturesEnabled` parameters; use `ModalNavigationDrawer` or `PermanentNavigationDrawer` outside the `Scaffold` to implement navigation drawers【253712462661658†L1340-L1473】.
- **Content window insets**: M3 allows passing `contentWindowInsets` to specify how the scaffold handles system bars, accommodating edge‑to‑edge layouts.

For example:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(snackbarHostState: SnackbarHostState) {
    ModalNavigationDrawer(
        drawerContent = { /* Drawer items */ },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = { SmallTopAppBar(title = { Text("Title") }) },
            bottomBar = { NavigationBar { /* items */ } }
        ) { innerPadding ->
            // Screen content, pass padding to preserve insets
        }
    }
}
```

## Experimental APIs

Many M3 APIs are marked with `@ExperimentalMaterial3Api`. You must annotate your composables with `@OptIn(ExperimentalMaterial3Api::class)` to use them. Take care to monitor the Jetpack release notes for API changes.

## Conclusion

Migrating to Material 3 requires careful planning and incremental work. Begin by adopting the new dependency and themes, then replace M2 components with their M3 equivalents while adhering to updated colour, typography and shape systems. Refer to the official migration guide and design guidelines for specific details and use `MaterialTheme` consistently throughout your app to obtain the full benefits of Material You【253712462661658†L865-L912】.