# Material 3 Jetpack Compose Design System Guide

Material 3 (also called **M3** or *Material Design 3*) is the latest evolution of Google’s design system.  
It builds on *Material You* and introduces new color palettes, typography scales, shapes and components that adapt to a wide range of screen sizes and form factors.  
Jetpack Compose, Android’s modern declarative UI toolkit, provides a first‑class implementation of Material 3, including ready‑made composables for all of the new components, plus APIs for customizing colour schemes, typography and shapes.  

This repository contains a **collection of markdown files** that document how to build a production‑quality Android app using the Material 3 implementation in Jetpack Compose.  
The guide includes:

- How to set up the Material 3 dependency in your project and migrate from Material 2.
- Details of the **MaterialTheme** system: colour schemes, typography and shapes, including dynamic colour and dark mode support.
- Composable examples for the full range of Material 3 UI elements (buttons, cards, app bars, chips, navigation components and more), with explanations of when to use each variant.
- Best practices for elevation, emphasis and accessibility, along with snippets illustrating recommended usage.
- Tips for building adaptive layouts with Compose and designing for large screens.

Wherever possible the information in these files comes from the official Material 3 documentation and Android Developer guides.  
For example, the “Material Design 3 in Compose” guide explains that Compose implements Material You and Material 3 Expressive, and that adding the `androidx.compose.material3:material3` dependency is the first step to using M3 components【641042291835853†L846-L873】.  
Other pages show how to theme your app with a custom `MaterialTheme`【641042291835853†L889-L909】, how the new type scale groups styles into display, headline, title, body and label categories【641042291835853†L1076-L1099】 and how elevation now uses tonal overlays as well as shadows【641042291835853†L1293-L1316】.  

Each file focuses on a particular aspect of the design system.  See the index below for a high‑level overview of the contents.

## File index

| File | Purpose |
| --- | --- |
| `Theming.md` | Introduces the Material 3 theme system (colour, typography and shapes) and shows how to implement dynamic colour and dark mode. |
| `Buttons.md` | Covers all types of Material 3 buttons, including filled, tonal, outlined, elevated, text and extended floating action buttons. |
| `Cards.md` | Shows how to use the `Card`, `ElevatedCard` and `OutlinedCard` composables for containers. |
| `AppBars.md` | Demonstrates top app bar variants (small, centre‑aligned, medium and large) and bottom app bars. |
| `Chips.md` | Explains assist, filter, input and suggestion chips and shows sample implementations. |
| `Navigation.md` | Describes navigation bars, rails and drawers with examples of switching between destinations. |
| `Layouts.md` | Discusses scaffolds, surfaces, rows, columns and lazy lists, and describes tonal elevation and emphasis. |
| `Motion.md` | Summarises elevation and motion guidance, including tonal vs shadow elevation and system ripple/overscroll changes. |
| `Accessibility.md` | Provides guidelines for accessible colour contrast, typography and large‑screen support. |
| `Migration.md` | Outlines steps and considerations when migrating from Material 2 to Material 3 in Jetpack Compose. |
| `AdditionalComponents.md` | Contains notes on other useful composables (dialogs, menus, sliders, etc.) and utilities. |

To browse a specific topic, open the corresponding markdown file.