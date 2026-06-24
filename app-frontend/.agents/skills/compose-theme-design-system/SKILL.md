---
name: compose-theme-design-system
description: "Trigger: Compose theme, styles, colors, design system, MaterialTheme. Keep UI styling tokenized and theme-driven."
license: Apache-2.0
metadata:
  author: ddgutierrezc
  version: "1.0"
---

## Activation Contract

Use when adding or changing Compose styling, theme tokens, colors, typography, shapes, or visual status semantics.

## Hard Rules

- Wrap shared UI with `AppTheme` at the app root.
- Keep Material color schemes in theme files, not scattered across screens.
- Prefer `MaterialTheme.colorScheme` for Material roles and semantic app tokens over hardcoded colors in feature screens.
- Add domain/status colors through an extended theme (`CompositionLocal`) or `ColorScheme` extension properties, not raw hex at call sites.
- Keep theme code in `composeApp/src/commonMain`; add platform-specific styling only when a platform API requires it.

## Decision Gates

| Need                         | Pattern                                          |
| ---------------------------- | ------------------------------------------------ |
| Global light/dark palette    | `lightColorScheme` / `darkColorScheme` in theme  |
| Screen foreground/background | `MaterialTheme.colorScheme.*`                    |
| Business/status meaning      | extended token such as `AppTheme.status.success` |
| New reusable component style | theme token or component parameter               |
| One-off visual state         | local modifier only if not a design token        |

## Execution Steps

1. Check existing tokens before adding a new color or style.
2. Add Material palette values to `ColorScheme`; add custom product/status tokens to an extended theme or extension property.
3. Use tokens through `MaterialTheme` or the extended app theme in screens.
4. Avoid duplicating style decisions across screens.
5. Report any new design token and where it is consumed.

## Output Contract

Report theme files touched, tokens added/used, hardcoded colors removed or justified, and preview/test impact.

## References

- `composeApp/src/commonMain/kotlin/io/github/kevinah95/spacex/theme/Theme.kt`
- `composeApp/src/commonMain/kotlin/io/github/kevinah95/spacex/theme/Color.kt`
- `assets/theme-tokens-example.kt`
- `assets/themed-screen-example.kt`
