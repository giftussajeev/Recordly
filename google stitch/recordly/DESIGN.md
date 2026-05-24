---
name: Recordly
colors:
  surface: '#fdf8f8'
  surface-dim: '#ddd9d9'
  surface-bright: '#fdf8f8'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f7f3f2'
  surface-container: '#f1edec'
  surface-container-high: '#ebe7e7'
  surface-container-highest: '#e6e1e1'
  on-surface: '#1c1b1b'
  on-surface-variant: '#424753'
  inverse-surface: '#313030'
  inverse-on-surface: '#f4f0ef'
  outline: '#727784'
  outline-variant: '#c2c6d5'
  surface-tint: '#015ac1'
  primary: '#004394'
  on-primary: '#ffffff'
  primary-container: '#005ac1'
  on-primary-container: '#c8d8ff'
  inverse-primary: '#adc6ff'
  secondary: '#535f70'
  on-secondary: '#ffffff'
  secondary-container: '#d7e3f8'
  on-secondary-container: '#596576'
  tertiary: '#513f5e'
  on-tertiary: '#ffffff'
  tertiary-container: '#6a5677'
  on-tertiary-container: '#e8cef5'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc6ff'
  on-primary-fixed: '#001a41'
  on-primary-fixed-variant: '#004494'
  secondary-fixed: '#d7e3f8'
  secondary-fixed-dim: '#bbc7db'
  on-secondary-fixed: '#101c2b'
  on-secondary-fixed-variant: '#3c4858'
  tertiary-fixed: '#f3daff'
  tertiary-fixed-dim: '#d6bee4'
  on-tertiary-fixed: '#251431'
  on-tertiary-fixed-variant: '#523f5f'
  background: '#fdf8f8'
  on-background: '#1c1b1b'
  surface-variant: '#e6e1e1'
typography:
  display-lg:
    fontFamily: Roboto Flex
    fontSize: 57px
    fontWeight: '400'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Roboto Flex
    fontSize: 32px
    fontWeight: '400'
    lineHeight: 40px
    letterSpacing: 0px
  headline-lg-mobile:
    fontFamily: Roboto Flex
    fontSize: 28px
    fontWeight: '400'
    lineHeight: 36px
    letterSpacing: 0px
  title-lg:
    fontFamily: Roboto Flex
    fontSize: 22px
    fontWeight: '400'
    lineHeight: 28px
    letterSpacing: 0px
  title-md:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: 0.15px
  body-lg:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  label-lg:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Roboto Flex
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  margin-mobile: 16px
  margin-tablet: 24px
  gutter: 16px
  baseline: 4px
  container-padding: 24px
---

## Brand & Style
The design system for this screen recording application is built upon the core tenets of Material Design 3 (Material You), emphasizing a professional, trustworthy, and human-centric utility experience. The aesthetic is defined by "Adaptive Precision"—balancing the technical nature of screen capturing with a warm, approachable interface that feels integrated into the Android ecosystem.

The style leverages **Corporate Modern** principles with a focus on:
- **Systematic Reliability:** Every interaction follows predictable M3 patterns to ensure the user feels in control of their privacy and data.
- **Airy Clarity:** High use of whitespace and "Surface" containers to reduce cognitive load during complex recording setups.
- **Human Touch:** Softened geometry and intentional motion that mirrors the fluidity of the Android OS.

## Colors
The palette utilizes a sophisticated tonal palette approach. The primary **#005AC1 (Royal Blue)** provides a professional anchor, symbolizing stability and high-fidelity output.

- **Light Mode:** Uses a warm, off-white background (`#FEF7FF`) to prevent eye strain and create an inviting canvas. Surfaces use `Surface Container Low` for subtle grouping.
- **Dark Mode:** Adheres to M3 specifications using a deep charcoal (`#1C1B1F`) rather than pure black. This allows for soft elevation overlays and maintains legible contrast for night-time usage.
- **Dynamic Application:** The system is designed to support Android's monat engine, where the primary blue can be swapped for user-selected wallpaper colors while maintaining the defined luminance ratios.

## Typography
The system uses **Roboto Flex** for its unparalleled adaptability and legibility across high-density displays. The type scale is strictly hierarchical:

- **Headlines:** `Headline Large` is reserved for page titles (e.g., "Library" or "Tools") to provide a clear sense of place.
- **Titles:** `Title Medium` is the primary choice for card headers and list items, providing a bold but compact identifier for recorded files.
- **Body & Labels:** `Body Large` is used for primary descriptions, while `Label Large` is utilized for button text and chip identifiers to ensure high scanability.
- **Alignment:** All text follows a 4dp baseline grid to maintain vertical rhythm.

## Layout & Spacing
This design system employs a **Fluid Grid** model based on the M3 8dp grid system (with 4dp increments for fine-tuning). 

- **Mobile:** 4-column layout with 16px side margins. 
- **Tablet:** 12-column layout with 24px side margins, often utilizing a navigation rail instead of a bottom bar.
- **Structure:** Content is organized into semantic containers. Vertical spacing between logical sections (e.g., between "Recent Recordings" and "Quick Tools") should be 24px, while internal element spacing (e.g., icon to text) should be 8px or 12px.

## Elevation & Depth
Depth is communicated through **Tonal Layers** and subtle ambient shadows, moving away from heavy drop shadows of previous generations.

- **Level 0 (Surface):** The base background.
- **Level 1 (Card/Container):** Uses a +5% Primary color tint overlay in light mode or +5% surface tint in dark mode. No shadow.
- **Level 2 (Active/Raised):** Used for elements that need to pop against Level 1, utilizing a soft, diffused shadow (1dp offset, 3dp blur, 8% opacity).
- **Glassmorphism:** Reserved exclusively for the Top App Bar when scrolled, utilizing a backdrop blur (20px) and a semi-transparent surface color to maintain context of the content underneath.

## Shapes
The shape language is highly characteristic of Material 3, featuring exaggerated rounded corners to feel organic and safe.

- **Cards:** Use a standard **28dp** (`rounded-xl`) corner radius.
- **Buttons:** Use fully circular (pill-shaped) corners for high emphasis.
- **Small Components:** Chips and text fields use an **8dp** (`rounded-md`) radius.
- **Bottom Sheets:** Feature 28dp top-corner rounding to emphasize their "sheet" metaphor as they slide over the UI.

## Components
Consistent component implementation is vital for the utility-first nature of a recorder.

- **Floating Action Button (FAB):** The primary "Start Recording" action. Use a Large FAB (96x96dp) with a 28dp radius, placed in the bottom right.
- **Bottom Navigation Bar:** Use the standard M3 container (80dp height) with active state indicators (tonal pills behind icons).
- **Cards:** Utilize `Elevated` or `Tonal` variants. In the "Library" view, cards should have a 28dp radius and include a subtle 1px border (`Outline` variant) if the background contrast is low.
- **Chips:** Use `Tonal Chips` for filter categories (e.g., "Screen", "Audio", "Game") and `Outlined Chips` for secondary settings within a recording setup.
- **Modal Bottom Sheets:** Used for all "Settings" and "Share" actions. Ensure a 28dp top corner radius and a visible drag handle.
- **Input Fields:** Filled text fields with a 1px bottom stroke and 8dp top corner rounding, ensuring the active state uses the primary #005AC1 color.
- **Progress Indicators:** Use the Linear Progress Indicator for upload/save states, applying a rounded cap to the track and indicator.