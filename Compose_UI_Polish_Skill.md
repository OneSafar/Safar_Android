# Skill: Jetpack Compose Visual Polish for SAFAR

Use this skill when editing Android Jetpack Compose UI screens in the SAFAR app.

## Design intent

SAFAR should feel:
- calm
- premium
- focused
- spiritual but not cluttered
- modern Android-native

Prefer dark, atmospheric visuals, but keep text and primary actions highly readable.

## UI rules

1. One dominant focal point per screen.
   - If the hero card is visually strong, keep bottom navigation/tool cards calmer.
   - If a selected card glows, do not also make it too large.

2. Use glow and blur sparingly.
   - Active glow alpha should usually stay between `0.20f` and `0.30f`.
   - Inactive glow alpha should usually stay between `0.04f` and `0.08f`.
   - Avoid stacking multiple strong blur layers.

3. Selected card scale should usually be subtle.
   - Prefer `1.06f` to `1.12f`.
   - Avoid `1.20f` unless there is lots of spacing around the component.

4. Maintain comfortable spacing.
   - Horizontal screen padding for dense bottom rows should usually be `18.dp` to `24.dp`.
   - Spacing between cards should usually be at least `8.dp`.
   - Avoid visual elements touching the screen edge after scale animations.

5. Typography hierarchy:
   - Tiny uppercase module label: `10.sp` to `11.sp`, letter spacing around `2.sp`.
   - Hero headline: `26.sp` to `30.sp`, controlled line height.
   - Body copy: `13.sp` to `14.sp`, line height around `20.sp` to `22.sp`.

6. Button polish:
   - Main CTA should be highly visible.
   - Avoid overly tall or overly wide buttons unless the screen is sparse.
   - Use consistent corner radius and gradient direction.

7. Code quality:
   - Remove unused pager/import logic if using `Crossfade` only.
   - Keep composables small and readable.
   - Prefer named constants for repeated animation values if the file grows.
   - Do not change navigation routes while doing visual polish.

8. Accessibility:
   - Interactive images need meaningful `contentDescription`.
   - Decorative background images should use `contentDescription = null`.
   - Keep tap targets at least 48.dp.
   - Add button semantics where practical.

## Before finishing

Check:
- light/dark theme assumptions
- small screen layout
- long labels in localized strings
- all four module backgrounds
- active state for each tool card
- build warnings and unused imports
