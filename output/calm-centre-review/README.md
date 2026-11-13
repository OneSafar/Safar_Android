# Calm Centre implementation review

Implemented inside SAFAR's existing Calm Centre route, keeping its drawer, app header,
typography, theme controls and rounded Composables UI controls. No dependencies added.

The new flow asks about mood, intention and available time. A deterministic recommendation
explains its suggestion; users can also choose any of the other practices. Questions and
session progress survive activity recreation through saved Compose state. Answers are local
to this screen; they are not sent to an AI service or stored as a health profile.

Four original native Canvas compositions reuse the visual vocabulary of the existing
materials: an apricot breathing blossom, rose companions, sage balancing stones and a moon
with drifting clouds. They do not load or copy the existing Headspace Lottie artwork.
The generated concept is a design reference, not a screenshot of the application.

The session clock drives the artwork, breathing instructions, countdown and progress.
Breathing uses four seconds in and six seconds out, with a continuous cosine curve and no
holds. Pause freezes the artwork and timer. Lifecycle pause immediately stops the session.
Reduced-motion settings retain functional timing and prompts with still illustrations.
Frame state is read in drawing; prose updates once per second. The other practices sequence
their prompts across the selected duration. These are visual and text-guided practices,
with no audio playback or claimed streaks.

## Verification

- `:app:assembleProdDebug` and `:app:assembleProdDebugAndroidTest`: passed.
- Six `CalmPracticeTest` unit tests: passed (recommendations, phase boundaries, continuity,
  five-sense sequencing and countdown rounding).
- Three `CalmCentreJourneyTest` instrumented tests on USB moto g45 5G: passed.
- Device tests cover required answers, recommendation, breath phase changes, pause/resume,
  completion, returning to recommendations, dark theme, alternatives, and reachable choices
  at 320 / 375 / 414 / 768 dp using density overrides on the same physical device.
- Real navigation checked through sidebar → Well-being → Calm Centre.
- Manually verified Home → return pauses the live practice and displays Resume practice.
- Text contrast sampled on practice surfaces: light muted text ≥ 4.53:1; dark muted text
  ≥ 5.97:1; primary button text ≥ 8.8:1.
- Live device recording: `calm-centre-motion.mp4` (approximately 12 seconds).
- Device frame statistics during recording: 1,556 frames, 8 deadline-janky frames (0.51%);
  median 12 ms, 95th percentile 18 ms. Legacy jank metric was 15.10%. This is a debug build
  with screen recording active, not a universal frame-rate guarantee.

The existing `copyApksToOutputs` task has an unrelated implicit dependency validation error
when app and test APKs are assembled together. Validation excluded that copy task using
`-x :app:copyApksToOutputs`; both actual APKs built and were installed on the device.

## Artifacts

- `design-concept.png`: built-in ImageGen design reference.
- `device-check-in-final.png`: real app check-in with SAFAR header.
- `device-recommendation-final.png`: real app recommendation.
- `device-breathing-final.png`: real app breathing session.
- `kindness-dark.png`, `grounding-dark.png`: dark-theme device test captures.
- `layout-320.png`, `layout-768.png`: density-override layout checks, scrolled to last choice.
- `calm-centre-motion.mp4`: live recording from the USB-connected phone.
- `Safar-calm-centre-debug.apk`: installed debug build.
- `device-journey-tests.txt`, `build-validation.txt`, `device-frame-stats.txt`: verification.

## Image generation

Built-in ImageGen was used with the existing Dhyan lavender-stage image as a brand reference.
The final prompt was:

> Use case: ui-mockup. Create a polished high fidelity design concept image with THREE adjacent flat Android app screen designs, no device frames, on an ivory canvas. Use the provided image only as a SAFAR brand reference: warm ivory background, dark plum Hanken Grotesk-like typography, very rounded pastel cards, generous whitespace, existing top bar hamburger and centered page title, restrained plum primary buttons. This is a new Calm Centre feature within that SAME app, not a rebrand. Screen one title Calm Centre, small three-segment progress indicator, heading 'How are you arriving today?', supportive copy 'There is no right answer. Start where you are.', centered small original abstract soft peach sun with an off-center curved smile and small petal leaves. Five rounded selection rows 'A little overwhelmed', 'Feeling low', 'Restless & distracted', 'Ready to unwind', 'Doing okay', one selected peach outline; plum button 'Continue'. Screen two heading 'A softer pace, just for you.', peach recommendation hero with original apricot asymmetrical nested petal artwork with soft arcs and tiny eyes, title 'Find your rhythm', subtitle 'Gentle breathing · 2 min', supportive reason text, plum button 'Begin practice'; below compact rose 'A little self-kindness' and sage 'Come back to here' alternate practice rows. Screen three title 'Find your rhythm', warm peach stage, elegant large central original breathing blossom formed from five curved peach and terracotta petals with a small peaceful face, delicate thin orbital guide, text 'Breathe in', subtext 'Let the shape open with you', timer '01:42', pill pause button. All artwork flat vector-like organic shapes inspired by calm breathing animation materials, NOT exact Headspace characters or logos. Exquisite spacing and refined shape curves. Native app realistic UI, readable type, minimal decorative clutter. Existing SAFAR visual identity retained, plum navigation and buttons throughout.
