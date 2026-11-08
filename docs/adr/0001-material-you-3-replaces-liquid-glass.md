# ADR-0001: Material You 3 replaces liquid-glass/Haze

## Status
Accepted — 2026-11-08

## Context
The app shipped with a custom "liquid glass" design system: animated drifting gradient
blobs (`LiquidBackdrop`), Haze backdrop blur on every card, iridescent cyan→violet→pink
accents, and ~500 lines of custom `Glass.kt` modifiers. It was visually distinctive but
expensive (per-frame blur, GPU fill-rate) and hard to maintain — every screen hand-rolled
colors via `CompositionLocal`s.

## Decision
Replace the visual language with stock **Material You 3**:
- `dynamicLightColorScheme`/`dynamicDarkColorScheme` (API 31+, we target 37) derive the
  palette from the user's wallpaper.
- `MaterialTheme` with custom `Shapes` (8/12/16/24/32dp radii) + expanded `Typography`.
- Stock M3 components: `NavigationBar`, `TopAppBar`, `FloatingActionButton`, `FilterChip`,
  `Surface`, `Button`, `TextField`.
- Delete `dev.chrisbanes.haze` dependency and all blur/backdrop code.

## Consequences
- **Perf win**: no blur, no infinite animations, no haze bitmaps → lower GPU/CPU, smaller APK.
- **Maintainability**: theme is now `MaterialTheme.colorScheme.*`; no custom color locals.
- **Trade-off**: loses the unique liquid-glass identity; the app now looks like a standard
  Android app. Accepted because the goal was UX correctness + perf, not brand differentiation.
- `GlassCard`/`GlassButton`/etc. names kept as thin wrappers over M3 components to minimize
  call-site churn; they can be renamed later.

## Alternatives considered
- Keep liquid glass + optimize blur (rejected: still custom, still expensive).
- iOS-style clean (rejected: fights Android conventions, shadows cost GPU).
- Minimal editorial (rejected: too big a departure for an email client).
