# Home background carousel — 4 November 2026

The module artwork now renders as a full-viewport layer behind phone Home, including its header, hero, and lower content, with the v5-style light/dark readability fade. The compact module text and dots share the same page state. The rectangular image frame and its rounded clipping are removed; the purple Your tools grid, promotional banners, and Go to Dashboard action retain their layout.

Page advancement runs every four seconds while Home is resumed. It no longer depends on whether system animations are enabled. The USB device currently reports animator_duration_scale=0: this previously disabled the timer as well as the animation. The timer now continues, while transition animation still honors that setting. Manual dots update both background artwork and descriptions. No raster assets changed or were generated.

Validation: production debug app and test APKs assembled successfully. All seven tests passed on the USB Moto G45, including the six existing responsive/navigation scenarios and a full-background bounds/automatic-advance test with animations explicitly disabled.
