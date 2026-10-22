# Shared adaptive layouts

Keep Android's `LocalDensity` and font scaling. Do not shrink fonts or replace
the device density to make a layout fit. Use the existing Composables UI theme,
buttons, icons and text; no additional design library is needed.

- `SafarAdaptiveRow`: groups of actions, subject cards, and text/action pairs.
  Measures the content's preferred width, including font scale and translated
  labels. Uses a row when everything fits, otherwise full-width stacked items.
  Use `equalWidth = false` for details plus a trailing action. Do not put weighted
  modifiers on direct children. Lazy lists and subcomposed components do not
  support intrinsic measurement and must not be direct children.
- `SafarScrollableTabRow`: tab strips that should keep their label size and allow
  horizontal scrolling instead of squeezing labels. Use Composables UI controls
  inside it.
- `SafarContentWithBottomBar`: gives a bottom control its measured space, then
  assigns the remaining height to the body. Apply the supplied modifier to the
  body and make long body content scroll. Do not overlay controls on content with
  a guessed fixed spacer.

Let text-bearing controls grow in height. Keep minimum touch targets and normal
padding. Put adaptive groups in scrollable screen or dialog content. Wrapping a
single label can still be necessary at extreme font sizes; preserve its text.

Currently adopted in Profile subscription/actions, shared Planner and Toppers
Batch dialog footers, Toppers Batch subject cards, and Ekagra mode/action/bottom
control layouts. Other bespoke screen rows need to opt into these components;
there is deliberately no global modifier that changes arbitrary layouts.

`SafarAdaptiveLayoutTest` covers real Profile labels/clicks and bottom-bar
separation at 320/360/600 dp and font scales 1.0/1.5/2.0 in light and dark themes.
