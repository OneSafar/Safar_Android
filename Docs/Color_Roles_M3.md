# Material Design 3: Color Roles & Theming Guide

Color roles act as the "connective tissue" between UI elements and their specific colors. They map to Material Components, guarantee accessibility, and are implemented via design tokens.

## Core Principles & Rules

* **Accessibility First:** The system is built on accessible color pairings providing a minimum **3:1 contrast ratio**.
* **Tokenization:** Roles are implemented as reusable design tokens in code.
* **Layering Rule (DO):** Always apply colors in intended pairs (e.g., `Primary` paired with `On Primary`) to ensure visual results stay legible even when users change system contrast levels.
* **Layering Rule (DON'T):** Never improperly mix mappings (e.g., putting `On Surface` text over a `Secondary Container` fill), as this breaks accessibility in dynamic color modes.

---

## Naming Conventions (General Concepts)

Understanding these keywords makes it easy to know where a color belongs:
* **Surface:** Used for backgrounds and large, low-emphasis areas.
* **Primary, Secondary, Tertiary:** Accent colors for emphasizing/de-emphasizing foreground elements.
* **Container:** A fill color for foreground elements (like buttons). *Do not use for text or icons.*
* **On [Role]:** Indicates a color for text or icons *on top* of its paired parent color (e.g., `On Primary` sits on top of `Primary`).
* **Variant:** A lower-emphasis alternative to its non-variant pair.

---

## Accent Color Roles

Used based on importance and needed emphasis.

### 1. Primary
For the most prominent components (e.g., Floating Action Buttons (FAB), high-emphasis buttons, active states).
* **Primary:** High-emphasis fills.
* **On Primary:** Text/icons against Primary.
* **Primary Container:** Standout fill color.
* **On Primary Container:** Text/icons against Primary Container.

### 2. Secondary
For less prominent components (e.g., filter chips, unselected navigation icons).
* **Secondary / On Secondary**
* **Secondary Container / On Secondary Container**

### 3. Tertiary
For contrasting accents that balance primary/secondary colors or highlight specific elements like input fields.
* **Tertiary / On Tertiary**
* **Tertiary Container / On Tertiary Container**

---

## State & Structural Roles

### Error
Communicates urgent error states (e.g., invalid text fields). *Note: Error colors are static by default and do not shift in dynamic schemes, though they do adapt to light/dark modes.*
* **Error / On Error**
* **Error Container / On Error Container**

### Surface
Neutral backgrounds and component containers (cards, dialogs, sheets).
* **Surface:** Default background.
* **On Surface / On Surface Variant:** Text/icons against surfaces.
* **Surface Containers:** Ranging from `Lowest` to `Highest` emphasis. Useful for creating visual hierarchy (e.g., `Surface` for the app body, `Surface Container` for the navigation area).

### Inverse Colors
Applied selectively to reverse the surrounding UI for high contrast.
* **Inverse Surface:** Background fills contrasting against the normal surface (e.g., Snackbars).
* **Inverse On Surface:** Text/icons on Inverse Surface.
* **Inverse Primary:** Actionable elements on Inverse Surface.

### Outline
* **Outline:** Important boundaries needing 3:1 contrast (e.g., text field borders).
* **Outline Variant:** Decorative elements (e.g., dividers) or borders for targets that already have sufficient internal contrast.
    * *Rule:* Do not use `Outline` for dividers or multi-element cards; use `Outline Variant`.

---

## Add-on Color Roles (Advanced Light/Dark Theming)

Use these only if your specific product requires precise control over Light/Dark theme behavior.

### Fixed Accent Colors
Colors that maintain the **exact same tone** in both Light and Dark themes (unlike container colors which shift).
* **Primary Fixed, Secondary Fixed, Tertiary Fixed**
* **[Role] Fixed Dim:** A stronger, deeper tone with the same fixed behavior.
* **On Fixed / On Fixed Variant:** Text/icons sitting on top of fixed colors.
* *Rule:* Because they do not shift, avoid using them where contrast adaptation between light/dark themes is necessary.

### Bright and Dim Surface Roles
Instead of inverting completely (light to dark), these maintain their *relative brightness* across themes.
* **Surface Bright:** The brightest surface color in *both* Light and Dark themes.
* **Surface Dim:** The dimmest surface color in *both* Light and Dark themes.