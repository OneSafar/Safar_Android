# App bars

App bars provide contextual information and actions at the top or bottom of a screen.  Material 3 offers several **top app bar** variants (small, centre‑aligned, medium and large) and a **bottom app bar**.  Jetpack Compose provides dedicated composables for each style and exposes a `scrollBehavior` parameter so that the bar can react to scrolling.

## Top app bars

Top app bars display navigation elements and actions at the top of the screen.  Use them to provide quick access to the most important actions on the current screen.

### Small top app bar

The **small top app bar** uses the `TopAppBar` composable.  It occupies the standard app bar height and typically holds a title and optional icons.  The example below shows a simple top bar inside a `Scaffold`【88287122145971†L951-L974】:

```kotlin
@Composable
fun SmallTopAppBarExample() {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = { Text("Small Top App Bar") }
            )
        }
    ) { innerPadding ->
        ScrollContent(innerPadding)
    }
}
```

### Centre‑aligned top app bar

When a screen has a single primary action or you want to emphasise the title, use a **centre‑aligned top app bar**.  Compose provides the `CenterAlignedTopAppBar` composable.  Pass a `scrollBehavior` from `TopAppBarDefaults.pinnedScrollBehavior()` to collapse the bar on scroll【88287122145971†L993-L1035】:

```kotlin
@Composable
fun CenterAlignedTopAppBarExample() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text(
                        "Centered Top App Bar",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* handle navigation */ }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* handle action */ }) {
                        Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        ScrollContent(innerPadding)
    }
}
```

### Medium top app bar

Medium bars place the title beneath the icons and expand/collapse with scroll.  Use the `MediumTopAppBar` composable and pass an `enterAlwaysScrollBehavior()` scroll behaviour【88287122145971†L1050-L1096】:

```kotlin
@Composable
fun MediumTopAppBarExample() {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text(
                        "Medium Top App Bar",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* nav */ }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* action */ }) {
                        Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        ScrollContent(innerPadding)
    }
}
```

### Large top app bar

Large top app bars occupy more vertical space and provide room for additional content.  Use `LargeTopAppBar` and pass an `exitUntilCollapsedScrollBehavior()` so the bar collapses when the user scrolls but expands once the scroll reaches the top【88287122145971†L1123-L1168】:

```kotlin
@Composable
fun LargeTopAppBarExample() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Text(
                        "Large Top App Bar",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* back */ }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* menu */ }) {
                        Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        ScrollContent(innerPadding)
    }
}
```

## Bottom app bar

A **bottom app bar** anchors primary actions and navigation at the bottom of the screen.  The `BottomAppBar` composable accepts an `actions` parameter for a row of icons and a `floatingActionButton` parameter.  The following example shows a bottom bar with four icons and a floating action button【88287122145971†L1181-L1227】:

```kotlin
@Composable
fun BottomAppBarExample() {
    Scaffold(
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { /* action1 */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Check")
                    }
                    IconButton(onClick = { /* action2 */ }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { /* action3 */ }) {
                        Icon(Icons.Filled.Mic, contentDescription = "Voice")
                    }
                    IconButton(onClick = { /* action4 */ }) {
                        Icon(Icons.Filled.Image, contentDescription = "Image")
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { /* fab action */ },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                    }
                }
            )
        }
    ) { innerPadding ->
        Text(
            modifier = Modifier.padding(innerPadding),
            text = "Example of a scaffold with a bottom app bar."
        )
    }
}
```

### Best practices

- Use top app bars to host the screen title and a small set of common actions.  Choose the variant (small, centre‑aligned, medium or large) based on how much emphasis you want on the title and how the bar should behave during scrolling.
- Provide navigation affordances such as a back button or hamburger menu on the left, and place less‑frequent actions on the right.
- Use bottom app bars sparingly; they are best suited for screens with multiple primary actions or when the UI benefits from a floating action button anchored to the bottom.

App bars are a central part of your UI’s structure.  Selecting the appropriate variant helps users navigate and understand the hierarchy of your app.