# Titli

Titli is the stationary tutorial entry in existing top-bar action slots. Shared
DrawerScaffold toolbars, Home/Ekagra custom chrome, and Material feature/back
headers use `TitliGuideButton`; no extra bar, reserved row, or inset is added.

`TitliIcon` caches Kotlin vector paths. Wings/body are black, with a deep-pink
outline halo and pink glowing spots. The About Titli portrait levitates. During
practice, a companion sits beside the measured spotlight and gently bobs using
graphicsLayer translation. Its speech bubble contains the step title, short instruction, progress and tutorial
controls. A 4dp spacing grid aligns the step count and practice caption on the
left with equally sized Info/Help/Close actions on the right. The full-width
21sp heading and 13sp instruction retain the roughly 1.618 size ratio. Skip stays
left and Back/Next stay grouped right; controls wrap when larger fonts need space.
Guidance floats over the full-size feature canvas, without shifting its
navigation or shrinking its timer. Placement tries below, above, and either side of the
target and stays inside the stage. The last valid target and speech position are
retained while the next target registers and measures. Placement is solved against
the final target, and a persistent Animatable moves directly from the current
position over 350ms, including interrupted Back/Next transitions. The first
placement and disabled-motion mode snap without a flight from the origin. Levitation stops when system animations are disabled; the decorative companion is hidden from TalkBack. Toolbar icons stay still.

The first tap shows an English/Hindi/Hinglish introduction. Let's begin starts the
active feature's core guide; Not now dismisses it. A dedicated DataStore key
records acknowledgement, independently of retired butterfly flags. Subsequent
taps open the active feature's lesson list. Ekagra and Goals register their
existing isolated tutorials, retaining busy/eligibility checks. Unimplemented
screens offer the available feature guides without reviving disabled tours.

The host adds no layout. Practice windows hide live host actions in preview
headers and use a Help action to return to the lesson list; Titli appears beside
the spotlight. During a lesson, Help, close and the practice label live inside the
speech bubble; the lesson picker retains its header.
Tests use uniquely namespaced introduction keys, so they never dismiss the real
user's introduction. Pixel assertions check black wings and pink highlights in
both themes; tests also exercise first-use/replay routing, spotlight placement on
Back/Next, reduced motion, levitation, and removal of the old dot indicator.
