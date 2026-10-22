# Titli directory and supplied butterfly

The global tutorial directory now follows the approved Soft Geometry palette: blush/plum canvas, white rounded feature cards, peach icon tiles, a circular Close action, plum Other action and mint queries/replies action. The header has a compact Titli identity row and a separate full-width greeting, with scrollable feature links and fixed support actions. Light/dark palette selection, localization, feature routing, support routing and existing test tags are preserved.

`AnimatedTitliButterfly.kt` adapts the user's `animated_butterfly_composable_vector_resource.kt` into the app package. It preserves the supplied black silhouettes, pink wing patches, spots, body and antenna paths. Paths are cached, the viewport is padded to contain tilted wings, and animation state is read during drawing. Compose's animation clock replaces the standalone frame loop; the existing app motion policy provides a still fallback when animations are disabled. Input remains owned by the parent buttons instead of the supplied tap-burst gesture, so the art cannot intercept navigation or guide controls. No demo screen, new dependency, media asset or system-setting write is introduced.

The shared `TitliIcon` uses this vector across the app. Toolbar icons remain still; the menu portrait, introduction portrait and spotlight companion animate their wings. The existing guide positioning and levitation are preserved.

Checks run in the isolated source-validation checkout; no APK/AAB is assembled and no emulator/device run is performed. Device visual/accessibility review remains manual.

Final source validation passed: 20 relevant JVM tests, debug/release Kotlin compilation, instrumentation-test source compilation and release lint (zero errors; 905 warnings, 8 hints). All 928 source/resource/test files matched the isolated validation checkout. All seven supplied vector path constants match the original file. `git diff --check` passed. Log: `Outputs/logs/titli-butterfly-menu-2026-10-20/verified-checks.log`. Instrumentation tests were compiled, not executed. Short windows scroll the entire menu so header and support actions remain reachable.
