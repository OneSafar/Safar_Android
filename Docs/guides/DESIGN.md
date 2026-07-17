---
name: Architectural Minimalist
colors:
  surface: '#101416'
  surface-dim: '#101416'
  surface-bright: '#363a3c'
  surface-container-lowest: '#0b0f10'
  surface-container-low: '#181c1e'
  surface-container: '#1c2022'
  surface-container-high: '#262b2c'
  surface-container-highest: '#313537'
  on-surface: '#e0e3e5'
  on-surface-variant: '#c4c7c7'
  inverse-surface: '#e0e3e5'
  inverse-on-surface: '#2d3133'
  outline: '#8e9192'
  outline-variant: '#444748'
  surface-tint: '#c8c6c5'
  primary: '#c8c6c5'
  on-primary: '#313030'
  primary-container: '#121212'
  on-primary-container: '#7e7d7d'
  inverse-primary: '#5f5e5e'
  secondary: '#bcc7dd'
  on-secondary: '#263142'
  secondary-container: '#3c475a'
  on-secondary-container: '#aab6cc'
  tertiary: '#bac8da'
  on-tertiary: '#243240'
  tertiary-container: '#051320'
  on-tertiary-container: '#717f8f'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#e5e2e1'
  primary-fixed-dim: '#c8c6c5'
  on-primary-fixed: '#1c1b1b'
  on-primary-fixed-variant: '#474646'
  secondary-fixed: '#d8e3fa'
  secondary-fixed-dim: '#bcc7dd'
  on-secondary-fixed: '#111c2c'
  on-secondary-fixed-variant: '#3c475a'
  tertiary-fixed: '#d6e4f7'
  tertiary-fixed-dim: '#bac8da'
  on-tertiary-fixed: '#0f1d2a'
  on-tertiary-fixed-variant: '#3b4857'
  background: '#101416'
  on-background: '#e0e3e5'
  surface-variant: '#313537'
typography:
  headline-lg:
    fontFamily: Hanken Grotesk
    fontSize: 48px
    fontWeight: '600'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Hanken Grotesk
    fontSize: 32px
    fontWeight: '500'
    lineHeight: '1.2'
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Hanken Grotesk
    fontSize: 24px
    fontWeight: '500'
    lineHeight: '1.2'
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
  accent-serif:
    fontFamily: EB Garamond
    fontSize: 20px
    fontWeight: '400'
    lineHeight: '1.4'
  label-sm:
    fontFamily: Hanken Grotesk
    fontSize: 12px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: 0.08em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 8px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 64px
  container-max: 1200px
---

## Brand & Style

This design system embodies a grounded, architectural approach to luxury. It moves away from the ethereal glow of typical "midnight" themes in favor of a structural, distraction-free environment. The aesthetic is defined by high-quality sans-serif typography that communicates precision, supported by elegant serif accents that provide a sense of heritage and depth. 

The mood is professional and quiet, targeting a high-end audience that values clarity and intentionality. By utilizing a restrained color palette and focusing on the relationship between negative space and thin-line geometry, the interface feels less like a digital screen and more like a curated editorial experience.

## Colors

The palette is anchored in deep charcoal and slate, creating a sophisticated tonal range that avoids the harshness of pure black. 

- **Primary:** A grounding charcoal (#121212) used for the base canvas to establish a solid, non-reflective foundation.
- **Secondary:** A muted slate (#4A5568) used for structural elements, containers, and secondary interactive states.
- **Neutral:** A range of cool grays and off-whites (#F7FAFC) for text and iconography to ensure maximum legibility without high-glare contrast.
- **Accents:** Use slate-tints for subtle borders and dividers, maintaining a monochromatic and focused atmosphere.

## Typography

The typographic hierarchy is intentionally inverted to create a modern-premium feel. Large, bold sans-serifs (Hanken Grotesk) serve as the primary structural anchors for headlines and navigation. Elegant serifs (EB Garamond) are used sparingly for sub-headlines, pull-quotes, or descriptive labels to introduce a layer of sophisticated warmth.

On mobile devices, `headline-lg` should scale down to `32px` to maintain the architectural grid integrity. Letter spacing is slightly tightened for large headlines and significantly tracked out for labels to enhance the premium, "designed" quality of the text blocks.

## Layout & Spacing

The layout follows a strict fixed-grid philosophy on desktop, utilizing a 12-column system with generous margins to focus the user's eye on the center of the experience. On mobile, the system shifts to a 4-column fluid grid.

Spacing is used as a functional tool to separate concerns. High-density information is avoided; instead, components are given ample "breathing room" (utilizing multiples of 8px). Section transitions should use the 64px margin to reinforce a sense of deliberate pace and premium architectural scale.

## Elevation & Depth

This design system avoids the trend of heavy glassmorphism and glowing filters. Depth is instead achieved through:

1.  **Low-Contrast Outlines:** Surfaces are defined by 1px solid borders in a slightly lighter slate than the background.
2.  **Tonal Tiering:** Elevation is communicated through subtle shifts in the charcoal value. Higher-level components (like modals or active cards) use a slightly lighter background hex than the base canvas.
3.  **Refined Shadows:** Where necessary, use very soft, large-radius ambient shadows with 0% spread and low opacity (15-20%) to create a "floated" effect rather than a "glowing" one. 
4.  **Crispness:** Every element must have sharp, defined edges to maintain the architectural feel.

## Shapes

The shape language is disciplined and geometric. A `Soft` roundedness (0.25rem) is the standard for buttons and inputs, providing just enough approachability without compromising the professional, rigid structure of the architectural style. 

- Use **Sharp (0px)** corners for large section containers and vertical dividers to emphasize the grid.
- Use **Soft (4px)** for interactive elements like buttons.
- Use **Rounded-lg (8px)** exclusively for cards and secondary floating panels to distinguish them from the primary background structure.

## Components

- **Buttons:** Use "Ghost" or "Outline" styles for primary actions. A 1px border in a light slate with sans-serif uppercase text. Hover states should involve a subtle background tint shift rather than a color change.
- **Input Fields:** Minimalist underlines or 1px borders. Use the Serif font for placeholder text to create a literary, bespoke feel.
- **Cards:** No background fill; use a 1px border and subtle ambient shadow. Headlines within cards should be Hanken Grotesk Medium.
- **Chips/Labels:** Use the Serif italic variant (`accent-serif`) for categories to distinguish them from functional UI labels.
- **Progress Indicators:** Thin, hairline strokes. Avoid gradients; use solid slate and charcoal tones to represent state.
- **Icons:** Thin-stroke, linear icons. Avoid filled or "chunky" iconography to maintain the lightweight, architectural aesthetic.