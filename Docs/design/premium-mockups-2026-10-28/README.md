# Safar Premium — three UI directions

28 October 2026. Concept images generated using the built-in image generation tool. No app code changes.

1. **Soft Geometry** — closest to the approved tutorial. Blush background, geometric hero, benefit grid and side-by-side plans.
2. **Plum Membership** — recommended direction. A rich plum membership header, tutorial-like benefit rows and clear stacked plan options.
3. **Study Journey** — an illustrated planner and Plan / Revise / Refocus narrative, with a renewal timeline and prominent selected plan.

These show the active-member extension state from the supplied screenshot: 3 months ₹99 total, 6 months ₹119 total, Dhyan Live included. Existing expiry 1 Jan 2037; selected extension to 1 Jul 2037. For first-time buyers, implementation should change membership status, headline and action to match their actual access state.

The Soft Geometry reference is `../titli-mockups/03-soft-geometry.png`; the source palette is `app/src/main/java/com/safarparmar/app/ui/tutorial/TutorialVisualStyle.kt`. Exact prompts and content constraints are in `prompts.md`.

These are image concepts, not implemented or tested app screens. Generated status-bar details are illustrative; native system bars should be used in implementation. Final layout should retain a pinned checkout action while benefits/content scroll, preserve accurate server-provided dates/prices, and avoid repeating extension explanations. No conversion results or performance improvements are claimed.
