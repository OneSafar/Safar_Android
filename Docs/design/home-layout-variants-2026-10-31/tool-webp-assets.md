# Exact mockup tool images

Only Your tools changed: the eight icon tiles were cropped directly from the approved Plum Feature mockup, saved as PNG masters in extracted-assets/, and converted to lossless WebP in app/src/main/res/drawable-nodpi/home_mockup_*.webp. manifest.json records the source and exact crop coordinates.

PlumPhoneHome.kt renders the complete tiles with Image and no color tint or replacement background. Existing localized labels, feature routes, tap targets, tool order and large-text layout remain native and unchanged. Banner and hero are unchanged by this asset integration.

Verified all eight WebP files decode to the exact RGB pixels of their PNG masters, all eight resources are referenced, and the tool renderer uses untinted Image. No compilation, build, installation or device inspection performed in this change.
