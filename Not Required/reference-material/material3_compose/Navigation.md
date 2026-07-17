# Navigation components

Material 3 provides several navigation components to help users move between destinations in your app.  Jetpack Compose supports **navigation bars** for compact screens, **navigation rails** for medium screens and **navigation drawers** for larger screens.  This file presents the core APIs and examples for navigation bars and rails; drawers build on the same principles but provide a persistent panel.

## Navigation bar

Use a **navigation bar** when your app has up to five primary destinations and runs on a phone or other compact device.  Compose offers the `NavigationBar` and `NavigationBarItem` composables.  Each item requires a `selected` state and an `onClick` handler; optionally supply an `icon` and `label`【924910518083618†L860-L874】.

### Example: Bottom navigation bar

The following snippet demonstrates a bottom navigation bar with three destinations.  It uses a `NavHostController` from the `navigation-compose` library to perform navigation and a `rememberSaveable` state holder to track the selected item【924910518083618†L887-L924】:

```kotlin
enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    SONGS("songs", "Songs", Icons.Default.MusicNote),
    ALBUM("album", "Album", Icons.Default.Album),
    PLAYLIST("playlist", "Playlist", Icons.Default.List)
}

@Composable
fun NavigationBarExample() {
    val navController = rememberNavController()
    val startDestination = Destination.SONGS
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }
    Scaffold(
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                Destination.entries.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedDestination == index,
                        onClick = {
                            navController.navigate(route = destination.route)
                            selectedDestination = index
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { contentPadding ->
        AppNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(contentPadding)
        )
    }
}
```

Key points about the API【924910518083618†L929-L944】:

- `NavigationBar` hosts multiple `NavigationBarItem` children.
- `rememberNavController()` creates a `NavHostController` used to navigate between destinations.
- `selectedDestination` holds the index of the currently selected item.  Update this state in each item’s `onClick` handler to highlight the selected destination.
- Use a `NavHost` (here represented by `AppNavHost`) to display the corresponding screen.

## Navigation rail

A **navigation rail** is suited for tablets, foldables or phones in landscape.  It appears on the left edge and provides ergonomic navigation for medium‑sized screens.  The API mirrors that of the navigation bar; use `NavigationRail` and `NavigationRailItem` and supply `selected` and `onClick` parameters.

```kotlin
@Composable
fun NavigationRailExample() {
    val navController = rememberNavController()
    val startDestination = Destination.SONGS
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }
    Scaffold(
        content = { innerPadding ->
            Row {
                NavigationRail(modifier = Modifier.fillMaxHeight()) {
                    Destination.entries.forEachIndexed { index, destination ->
                        NavigationRailItem(
                            selected = selectedDestination == index,
                            onClick = {
                                navController.navigate(destination.route)
                                selectedDestination = index
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
                // content area
                AppNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.padding(innerPadding).weight(1f)
                )
            }
        }
    )
}
```

## Navigation drawer

For large screens (tablets or desktop) you can use **navigation drawers** to present navigation options and contextual actions.  Compose provides `PermanentNavigationDrawer` and `ModalNavigationDrawer` composables.  A permanent drawer stays visible alongside the content, while a modal drawer overlays the content and can be swiped out.  The API is similar to the rail; supply a `drawerContent` lambda containing navigation items and wrap your content in the drawer.  For example:

```kotlin
@Composable
fun PermanentDrawerExample() {
    val navController = rememberNavController()
    var selected by remember { mutableStateOf(Destination.SONGS) }
    PermanentNavigationDrawer(
        drawerContent = {
            NavigationDrawerContent(
                destinations = Destination.entries,
                selectedDestination = selected,
                onDestinationSelected = { dest ->
                    navController.navigate(dest.route)
                    selected = dest
                }
            )
        }
    ) {
        AppNavHost(navController = navController, startDestination = selected)
    }
}
```

Implement your own `NavigationDrawerContent` composable that lists items with optional icons and highlights the selected item.  For modal drawers use `ModalNavigationDrawer` instead of `PermanentNavigationDrawer`.

## Best practices

* **Choose the right component for the screen size.** Use navigation bars on compact screens, rails on medium devices, and drawers on large devices.
* **Keep the number of destinations manageable.** More than five destinations can overwhelm users; group related screens under categories or use secondary navigation patterns.
* **Highlight the current destination.** Always update the `selected` state so users know where they are in the app.  Provide clear labels and icons.
* **Compose Navigation library.** Use the `navigation-compose` library with `NavHost` and `NavController` to implement actual navigation logic behind your navigation components.