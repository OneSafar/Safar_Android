# Accessibility considerations

Inclusive design is a core principle of Material 3.  The system’s colour palettes, typography and components are engineered to meet accessibility standards out of the box, but you should still evaluate your customisations to ensure they remain accessible.

## Colour accessibility

Material 3’s colour system uses tonal palettes that maintain contrast across light and dark themes.  Dynamic colour generates palettes from the user’s wallpaper while respecting accessible contrast levels【641042291835853†L1514-L1529】.  When customising colours, follow these guidelines:

* Use appropriate **on‑colours**.  For example, pair `containerColor = MaterialTheme.colorScheme.primary` with `contentColor = MaterialTheme.colorScheme.onPrimary` to ensure text and icons contrast sufficiently【641042291835853†L1540-L1547】.
* Avoid mismatching roles.  Using `tertiaryContainer` for a button’s background and `primaryContainer` for its content yields poor contrast【641042291835853†L1548-L1555】.
* Use Material Theme Builder or contrast checking tools (e.g., WCAG contrast checkers) to verify your palette meets at least AA standards.

## Typography accessibility

Material 3’s simplified type scale ensures consistent sizes across devices.  Use relative units like `sp` so text scales with user accessibility settings.  Provide sufficient line height and avoid truncation where possible.

* Use `TextOverflow.Ellipsis` only when necessary and limit `maxLines` to maintain readability【88287122145971†L1003-L1044】.
* Consider dynamic type sizes for large screens and adapt your UI accordingly【641042291835853†L1566-L1570】.

## Layout and large screens

Design for a range of devices, including tablets, foldables and desktops.  Material guidelines recommend using navigation rails or drawers for large screens and providing more spacious layouts【641042291835853†L1572-L1579】.  Test your UI on different screen sizes and orientations to ensure that content remains accessible and interactive elements are reachable.

## Touch targets and gestures

Ensure touch targets meet the recommended minimum size (48 dp square) and have sufficient spacing.  Provide visual feedback (ripples) and accessible labels for icons.  Avoid relying solely on gestures for critical actions; always provide an alternative, visible control.

## Semantic labels and screen readers

Compose automatically exposes accessibility semantics for most Material components.  For custom elements, add `Modifier.semantics` or use `contentDescription` on icons so that screen readers can describe your UI accurately.  Group related elements with `semantics { heading() }` or `mergeDescendants = true` when appropriate.

By adhering to these guidelines, you can build Material 3 apps that are usable by a wide range of users with varying abilities.