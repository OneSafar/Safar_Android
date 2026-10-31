# Sidebar popup window fix

Sidebar category menus now use a native Compose Popup, anchored to the tapped row, instead of the library dropdown panel path. The position provider clamps against the activity window with 16dp gutters and system insets. Expanded rows overlap at 42% of row width, allowing the panel to extend beyond the drawer edge; compact rails open beside the rail. RTL mirrors the placement. Menus retain scrollable items, selection state, premium lock indication, outside/Back dismissal and existing navigation handlers.

Only SoftSidebar.kt, the new SidebarMenuPositionProvider.kt and its test file were changed for this fix. No Home files or global popup wrappers were edited. Whitespace/source checks completed. Tests were added but not run; no Kotlin compilation, builds or device inspection were performed, as requested. Device confirmation of the final placement remains with the user.
