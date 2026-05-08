# Coding Agent Prompt: Refine SAFAR HomeScreen UI

You are working on an Android Jetpack Compose screen: `HomeScreen.kt` in package `com.safar.app.ui.home`.

Goal: polish the existing HomeScreen design without changing the product concept, routes, module list, background images, or navigation behavior. Keep the current glassmorphism + dark spiritual/productivity visual identity, but reduce visual noise, improve spacing, and make the UI feel more premium.

## Files to inspect first

Attach/read these before editing:
1. `HomeScreen.kt`
2. `SafarDrawerScaffold.kt`
3. `Routes.kt`
4. app theme files: colors, typography, font definitions
5. `strings.xml`
6. asset files used by the Home slides:
   - `img_ekagara.jpeg`
   - `img_nishtha.jpeg`
   - `img_mehefil.jpeg`
   - `img_dhyan.jpeg`
7. remote tool card images, or any local replacements if available:
   - `focus-timer.webp`
   - `nishtha-silhouette.webp`
   - `mehfil-silhouette.webp`
   - `meditation-silhouette.webp`

## Required changes

### 1. Make the selected tool card less aggressive

In `ToolImageCard`, change the active scale from `1.2f` to `1.10f`.

```kotlin
targetValue = if (isActive) 1.10f else 1f
```

Change active glow alpha from `0.4f` to `0.28f`, and inactive glow from `0.1f` to `0.06f`.

```kotlin
targetValue = if (isActive) 0.28f else 0.06f
```

Change active vertical spacing from `14.dp` to `10.dp`. Keep inactive spacing around `6.dp` or `7.dp`.

```kotlin
targetValue = if (isActive) 10.dp else 7.dp
```

Keep the spring animation, but reduce bounce slightly:

```kotlin
dampingRatio = Spring.DampingRatioNoBouncy,
stiffness = Spring.StiffnessMediumLow
```

Expected result: the selected card should still be clearly active, but it should not dominate the entire bottom area or look like it is touching the screen edge.

### 2. Improve bottom tool row spacing

In the bottom tool row, change horizontal padding from `12.dp` to `18.dp` or `20.dp`, and spacing from `6.dp` to `8.dp`.

```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
)
```

Change each card width from `fillMaxWidth(0.86f)` to `fillMaxWidth(0.82f)`.

```kotlin
modifier = Modifier.fillMaxWidth(0.82f)
```

Expected result: cards should breathe better and not feel cramped on small devices.

### 3. Reduce visual heaviness of the glass hero card

In the glass card section:
- reduce blur from `35.dp` to `24.dp`
- reduce shadow blur from `20.dp` to `12.dp`
- reduce the black shadow alpha from `0.45f` to `0.25f`
- reduce the animated gloss alpha from `0.2f` to `0.10f`

Suggested edits:

```kotlin
.blur(24.dp, BlurredEdgeTreatment.Unbounded)
```

```kotlin
.blur(12.dp, BlurredEdgeTreatment.Unbounded)
.background(Color.Black.copy(alpha = 0.25f))
```

```kotlin
Color.White.copy(alpha = 0.10f)
```

Expected result: keep the premium glass style, but make it less game-like and less visually noisy.

### 4. Refine typography inside the hero card

Update the module label:
```kotlin
fontSize = 10.sp
letterSpacing = 2.sp
color = Color.White.copy(alpha = 0.72f)
```

Update the headline:
```kotlin
fontSize = 27.sp
lineHeight = 32.sp
fontWeight = FontWeight.SemiBold
```

Update the body:
```kotlin
fontSize = 13.sp
lineHeight = 20.sp
color = Color.White.copy(alpha = 0.86f)
```

Keep `LoraFontFamily` for the headline.

Expected result: the card should feel calmer and more premium.

### 5. Adjust the CTA button polish

Keep the green gradient, but make the button slightly less heavy:
- height: `46.dp` instead of `48.dp`
- horizontal padding: `44.dp` instead of `40.dp`
- font size: `11.sp`
- letter spacing: `1.1.sp`

```kotlin
.padding(horizontal = 44.dp)
.height(46.dp)
```

Expected result: still clear, but slightly more refined.

### 6. Improve top-bar polish if `SafarDrawerScaffold` allows it

Inspect `SafarDrawerScaffold.kt`. If the top bar is implemented there:
- keep hamburger left aligned
- use title font size around `20.sp`
- use `FontWeight.Bold` or `SemiBold`
- align icon and title vertically
- use top padding/status bar padding correctly
- keep color white, but avoid excessive shadow/stroke

Do not rename the app or change navigation.

### 7. Fix carousel auto-advance state

Currently `currentPage` changes in a timer, but the actual `pagerState` is not being animated to that page. Either:
- remove unused `pagerState` entirely if the screen is intended to crossfade only, or
- animate the pager state properly with `pagerState.animateScrollToPage(nextPage)`

Preferred: remove unused pager imports/state unless another part of the app uses swipe. The current code uses `Crossfade`, not `HorizontalPager`, so avoid keeping unused pager state.

Expected result: cleaner code and no misleading pager logic.

### 8. Accessibility and touch targets

Add meaningful `contentDescription` for tool cards; background images can remain decorative with `null`.

Ensure the clickable area for each tool remains at least 48.dp.

CTA should have a semantic role if easy to add:

```kotlin
.semantics { role = Role.Button }
```

Only import semantics if used.

### 9. Do not change

Do not change:
- routes
- module names
- slide copy unless there is a typo
- dark theme concept
- drawer behavior
- login/auth behavior
- notification permission flow
- data store logic

## Acceptance criteria

After the change:
1. The screen should look calmer and more premium.
2. The selected card should be obvious but not oversized.
3. The bottom row should have better horizontal breathing room.
4. The glass card should remain readable over all four backgrounds.
5. No navigation route should break.
6. No unused pager state/imports should remain.
7. Build should pass with no new Compose warnings.
