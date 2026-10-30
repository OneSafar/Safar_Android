# Approved phone direction → proposed tablet layouts

Target: **Safar_Android_v4 first**. This deliverable is the requested tablet mockup stage before app implementation. No production code modified; V5 untouched.

## Landscape, approximately 1280 × 800 dp

Existing app navigation rail at the start edge. Center a workspace capped at about 1200 dp; 24–32 dp outer gutters and 24 dp pane gap. A roughly 320 dp identity/account column sits beside a flexible details editor. Avatar/camera, identity and plan badge lead the left column, followed by membership management and separate account actions. Right pane has the details heading and one grouped form; Save Profile aligns to the form's trailing edge with a capped width, rather than stretching across the entire tablet.

## Portrait, approximately 840 × 1120 dp

Navigation rail stays at the edge. Identity becomes a full-width horizontal card above the editor. Beneath the heading, a wider details column (~390 dp) sits next to a narrower membership/actions pane (~270 dp), with a 20 dp gutter. Save Profile stays in the supporting pane above logout/delete. At insufficient available content width or enlarged text, collapse to the approved phone's single-column flow instead of forcing cramped columns. Choose using actual available pane width after navigation and insets, not physical tablet detection alone.

## Implementation invariants

Reuse the existing V4 navigation rail and original Titli component; generated artwork/icons are illustrative approximations. Do not implement the mockup rail's shortened destinations as a navigation replacement. Preserve existing avatar picker/preview, read-only email/copy behavior, editable name/gender/exam/preparation stage, save loading/errors, plan eligibility/navigation, confirmation dialogs, support contact and footer. Use existing localized strings and real user data; mockup identities are placeholders.

Preserve blush/plum/peach/mint tokens and the selected compact phone design. Keep white text only on opaque plum; reserve/clamp artwork bounds so it never overlaps membership text. Field chevrons/pencils must open real editors or native existing selectors. Retain 48 dp touch targets, keyboard/focus navigation, independent appropriate scrolling with IME insets, and width caps so controls do not stretch excessively. Dark mode needs its matching semantic surfaces, not a faded overlay.

V4 currently has `TabletProfileContent` and `LocalSafarAdaptiveState`; adapt the existing ownership and state flow in place. No app-wide navigation migration is required for this profile-only redesign.

Generated with built-in image generation; exact prompts in prompts.md. These are visual concepts, not compiled or device-tested app screens.
