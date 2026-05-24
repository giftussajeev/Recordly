---
name: Recordly Adaptive
colors:
  surface: '#f9f9fc'
  surface-dim: '#dadadc'
  surface-bright: '#f9f9fc'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f3f6'
  surface-container: '#eeeef0'
  surface-container-high: '#e8e8ea'
  surface-container-highest: '#e2e2e5'
  on-surface: '#1a1c1e'
  on-surface-variant: '#424753'
  inverse-surface: '#2f3133'
  inverse-on-surface: '#f0f0f3'
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
  tertiary: '#533a73'
  on-tertiary: '#ffffff'
  tertiary-container: '#6c528c'
  on-tertiary-container: '#e6ceff'
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
  tertiary-fixed: '#eedbff'
  tertiary-fixed-dim: '#d8bafc'
  on-tertiary-fixed: '#270d45'
  on-tertiary-fixed-variant: '#543b73'
  background: '#f9f9fc'
  on-background: '#1a1c1e'
  surface-variant: '#e2e2e5'
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
  headline-lg-mobile:
    fontFamily: Roboto Flex
    fontSize: 28px
    fontWeight: '400'
    lineHeight: 36px
  title-lg:
    fontFamily: Roboto Flex
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
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
  label-md:
    fontFamily: Roboto Flex
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
  metadata-sm:
    fontFamily: Roboto Flex
    fontSize: 11px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0.4px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 16px
  margin-mobile: 16px
  margin-desktop: 24px
---

## Brand & Style

The design system is engineered for high-performance data management and media organization, targeting professionals who require both aesthetic polish and functional density. The brand personality is **Precise, Technical, and Sophisticated**. 

The design style leans into **Corporate Modern** with a strong emphasis on **Tonal Layering**. It leverages the adaptability of variable typography and a refined color strategy to transition seamlessly between high-productivity environments (Light/Dark) and battery-efficient, immersive contexts (Absolute Dark). The goal is to evoke a sense of "quiet power"—a UI that stays out of the way until needed, then provides surgical precision.

## Colors

The palette is built on a tripartite mode system to support diverse lighting conditions and hardware specs:

1.  **Light Mode:** Optimized for readability in bright environments. Background uses a subtle off-white (#FDFBFF) to reduce eye strain, with Primary #005AC1 providing a professional anchor.
2.  **Dark Mode:** A standard Material 3 charcoal palette (#1A1C1E) for balanced contrast and reduced glare.
3.  **Absolute Dark (AMOLED):** Designed for OLED displays. The background is strictly **#000000**. Containers and surfaces use a slightly elevated charcoal (#1A1C1E) or deep navy-grey to maintain depth. To ensure eye comfort, white text is capped at 87% opacity (High Emphasis) rather than pure #FFFFFF.

Secondary and Tertiary colors are muted to ensure they don't distract from the primary data streams, used primarily for status indicators and categorical tagging.

## Typography

This design system exclusively utilizes **Roboto Flex** to capitalize on its variable axes. 

*   **Hierarchy:** Headlines use a wider width axis and standard weight for an architectural feel. Body text uses standard proportions for maximum legibility.
*   **Metadata Density:** For high-density data, use the `metadata-sm` role. To maintain readability, the optical size (opsz) axis should be pinned to 12pt even for smaller rendered sizes, and the weight axis should be slightly increased (450-500) to prevent strokes from disappearing on dark backgrounds.
*   **Contrast:** Primary content uses High-Emphasis text colors. Metadata uses Medium-Emphasis (60% opacity) to create a clear visual gap between the subject and its attributes.

## Layout & Spacing

The layout follows a **Fluid Grid** model based on an 8px root scale. 

*   **Desktop:** 12-column grid with 24px margins. Content density is prioritized, allowing for sidebars that house complex filtering and metadata controls.
*   **Mobile:** 4-column grid with 16px margins. 
*   **Refinement:** Spacing between related metadata items (e.g., a timestamp and a file size) should use the `xs` (4px) unit to group them visually. Larger sections or distinct cards use `lg` (24px) for clear breathing room.

## Elevation & Depth

Depth is communicated through **Tonal Layers** rather than heavy drop shadows, adhering to Material 3 principles:

*   **Surface Tiers:** Higher elevation is represented by lighter tonal overlays in Dark/Absolute Dark modes. For example, a card at Level 1 has a 5% primary color tint; Level 2 has an 8% tint.
*   **Absolute Dark Specifics:** On #000000 backgrounds, containers use a subtle "Elevation 1" surface (#1A1C1E) to remain visible. Avoid shadows on #000000 as they are invisible; use thin, low-contrast inner strokes (1px, 10% white) to define boundaries if necessary.
*   **Interactive State:** On hover or press, the surface tint increases in intensity rather than moving "closer" to the user with a larger shadow.

## Shapes

The shape language is sophisticated and modern, utilizing varied corner radii to distinguish between containers and interactive elements:

*   **Large Containers/Cards:** Use a radius of **28px** to provide a premium, smooth appearance that frames high-density data gently.
*   **Buttons:** Fully pill-shaped (100px) to clearly denote action.
*   **Inputs & Small UI:** A **12px** radius provides enough softness to match the system while remaining space-efficient for high-density forms.

## Components

*   **Premium Cards:** Cards should have no border in Light/Dark mode, relying on tonal shifts. In Absolute Dark, use a 1px border of #2C2E33 to define the edge against pure black.
*   **Buttons:** 
    *   *Primary:* Solid fill with white text.
    *   *Secondary:* Tonal fill (Primary at 15% opacity).
    *   *Tertiary:* Text-only with an icon.
*   **Metadata Chips:** Small, low-contrast containers with `label-md` typography. Use `rounded-sm` (4px) for chips to contrast against the large curves of cards.
*   **Input Fields:** Outlined style with a 1px stroke. The stroke weight increases to 2px and takes the primary color only on focus.
*   **Lists:** High-density lists should utilize "Dividers" only when text density is extreme; otherwise, use whitespace (`sm` or 8px) to separate rows.
*   **Status Indicators:** Use small, high-chroma dots (8px) paired with `metadata-sm` text to communicate state (e.g., "Recording," "Syncing") without dominating the visual field.