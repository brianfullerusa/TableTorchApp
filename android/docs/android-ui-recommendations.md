# Android UI Recommendations for TableTorch

**Generated:** January 23, 2026
**Status:** Complete
**Purpose:** Track UI improvements for a clean, intuitive interface optimized for low-light use

---

## Overview

This document consolidates recommendations from 5 Android UI expert reviews covering:
- Color picker and selection UI
- Main screen layout and structure
- Theme, colors, and typography
- Reusable UI components
- Accessibility and user experience

TableTorch is a screen-light app that illuminates menus or books in low-light environments (instead of using a flashlight). The UI should prioritize maximum light output, ease of use in darkness, and accessibility compliance.

---

## Task Tracking Legend

- [ ] Not started
- [x] Completed
- [~] In progress

---

## Critical Priority (Accessibility Violations)

These must be addressed to meet WCAG and Android accessibility guidelines.

### 1. Increase color grid touch targets
- [x] **Status:** Completed
- **Description:** Reduce grid from 8 to 6 columns to achieve 48dp minimum touch targets. Current cells are approximately 35dp.
- **Files:** `FlameColorPicker.kt`
- **Changes made:**
  - Changed `GridCells.Fixed(8)` to `GridCells.Fixed(6)`
  - Added 8.dp spacing between grid items
  - Added `.sizeIn(minWidth = 48.dp, minHeight = 48.dp)` to each cell
  - Added `colorNames` list with descriptive names for accessibility
  - Added `contentDescription` to each color cell

### 2. Add accessibility semantics to custom sliders
- [x] **Status:** Completed
- **Description:** Add `Role.Slider`, `contentDescription`, and `stateDescription` to HueSlider, SaturationValuePanel, and ColorChannelSlider. Screen readers cannot interact with these controls.
- **Files:** `FlameColorPicker.kt`
- **Changes made:**
  - SaturationValuePanel: Added semantics with role, contentDescription, and stateDescription showing saturation/brightness percentages
  - HueSlider: Added semantics showing hue in degrees
  - ColorChannelSlider: Added semantics showing channel label and value

### 3. Add content descriptions to brightness sliders
- [x] **Status:** Completed
- **Description:** Include semantic announcements for current value (e.g., "Brightness 75 percent").
- **Files:** `BrightnessSlider.kt`, `SettingsScreen.kt`
- **Changes made:**
  - Added `Modifier.semantics` with contentDescription and stateDescription to both sliders
  - Brightness percentage calculated and announced dynamically

### 4. Make settings toggle rows fully accessible
- [x] **Status:** Completed
- **Description:** Make entire row clickable with `Role.Switch`, merge semantics, and add state descriptions.
- **Files:** `SettingsToggle.kt`
- **Changes made:**
  - Added `.clickable(role = Role.Switch)` to entire row
  - Added `semantics(mergeDescendants = true)` with stateDescription "On"/"Off"
  - Switch's `onCheckedChange` set to null (row handles clicks)

---

## High Priority (Theme & Consistency)

These ensure maintainable, consistent styling across the app.

### 5. Replace hardcoded colors with MaterialTheme
- [x] **Status:** Completed
- **Description:** Replace direct `Color.White`, `TorchOrange` references with `MaterialTheme.colorScheme` throughout.
- **Files:** `MainScreen.kt`, `SettingsScreen.kt`, `FlameColorPicker.kt`, `SettingsToggle.kt`
- **Changes made:**
  - `Color.White` → `MaterialTheme.colorScheme.onBackground`
  - `TorchOrange` → `MaterialTheme.colorScheme.primary`
  - `Color.White.copy(alpha = 0.7f)` → `MaterialTheme.colorScheme.onSurfaceVariant`
  - Removed hardcoded TorchOrange imports from multiple files

### 6. Complete the Typography scale
- [x] **Status:** Completed
- **Description:** Define all 15 Material 3 text styles. Currently only 5 are defined, causing fallback to Roboto instead of Cinzel branding.
- **Files:** `Type.kt`
- **Changes made:**
  - Added all 10 missing styles: displayMedium, displaySmall, headlineLarge, headlineMedium, headlineSmall, titleMedium, titleSmall, bodySmall, labelLarge, labelMedium
  - CinzelFont used for display and headline styles (branding)
  - FontFamily.Default used for body and label styles (readability)

### 7. Add lineHeight to all typography styles
- [x] **Status:** Completed
- **Description:** `displayLarge` and `titleLarge` are missing lineHeight, affecting accessibility and vertical rhythm.
- **Files:** `Type.kt`
- **Changes made:**
  - All 15 typography styles now have proper lineHeight values
  - Follows Material 3 typography guidelines

### 8. Complete Material 3 color scheme
- [x] **Status:** Completed
- **Description:** Add distinct `primaryContainer`, `secondary`, `tertiary`, `error`, and `outline` colors. Currently primary/secondary/tertiary are all TorchOrange.
- **Files:** `Color.kt`, `Theme.kt`
- **Changes made:**
  - Added TorchOrangeContainer, TorchSecondary, TorchSecondaryContainer, TorchError, TorchErrorContainer, TorchOutline, TorchOutlineVariant
  - Updated dark color scheme with complete Material 3 roles
  - Secondary now uses TorchSecondary instead of TorchOrange

---

## High Priority (Layout & UX)

These significantly improve the core user experience.

### 9. Remove TopAppBar from main screen
- [x] **Status:** Completed
- **Description:** Maximize screen light output by eliminating the title bar. Brand identity is already established on splash screen.
- **Files:** `MainScreen.kt`
- **Changes made:**
  - Removed Scaffold and TopAppBar completely
  - Replaced with simpler Box layout
  - Color display area now maximizes available space

### 10. Consolidate bottom controls into unified panel
- [x] **Status:** Completed
- **Description:** Group brightness slider, color buttons, and settings into a cohesive bottom control panel with subtle surface background.
- **Files:** `MainScreen.kt`
- **Changes made:**
  - Created unified bottom panel using Surface with `TorchSurface.copy(alpha = 0.8f)`
  - Rounded top corners: `RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)`
  - All controls grouped with consistent internal padding
  - Panel aligned to BottomCenter

### 11. Add before/after color preview in picker
- [x] **Status:** Completed
- **Description:** Split the preview into "Current" and "New" halves so users can compare their selection against the original color.
- **Files:** `FlameColorPicker.kt`, `strings.xml`, `strings.xml (es)`
- **Changes made:**
  - Side-by-side preview showing "Current" (original) and "New" (selected) colors
  - Added labels below each half
  - Added string resources for localization

---

## Medium Priority (Visual Feedback)

These improve user confidence and interaction quality.

### 12. Add selection indicator to color grid
- [x] **Status:** Completed
- **Description:** Add checkmark overlay to selected color swatch (adaptive black/white based on luminance). Border alone isn't visible on similar-colored swatches.
- **Files:** `FlameColorPicker.kt`
- **Changes made:**
  - Added `Icons.Filled.Check` overlay on selected swatch
  - Adaptive checkmark color based on luminance calculation
  - White checkmark on dark colors, black on light colors

### 13. Add visual feedback for tilt-controlled brightness
- [x] **Status:** Completed
- **Description:** Show "Tilt Active" indicator with icon when angle-based brightness is enabled and slider is disabled.
- **Files:** `BrightnessSlider.kt`
- **Changes made:**
  - Added "Tilt Active" indicator with PhoneAndroid icon
  - Shows explanatory text when slider is disabled
  - Styled subtly with 0.7f alpha

### 14. Add click feedback (ripple) to FlameColorPicker row
- [x] **Status:** Completed
- **Description:** Currently uses `.clickable{}` without indication. Use `Surface` with `onClick` for proper ripple effect.
- **Files:** `FlameColorPicker.kt`
- **Changes made:**
  - Added `rememberRipple(bounded = true)` indication
  - Added `MutableInteractionSource`

### 15. Improve unselected color button visibility
- [x] **Status:** Completed
- **Description:** Increase opacity from 50% to 70% and add subtle border for better low-light visibility.
- **Files:** `ColorButton.kt`
- **Changes made:**
  - Increased unselected opacity from 0.5f to 0.7f
  - Added subtle circular border (1.dp, Color.White.copy(alpha = 0.3f), CircleShape)

### 16. Add haptic feedback for touch interactions
- [x] **Status:** Completed
- **Description:** Add `HapticFeedbackConstants.CONTEXT_CLICK` to color selection, slider adjustments, and button presses for non-visual confirmation in dark environments.
- **Files:** `ColorButton.kt`, `BrightnessSlider.kt`
- **Changes made:**
  - ColorButton: Haptic feedback on every click
  - BrightnessSlider: Haptic feedback for significant changes (10% threshold)

---

## Medium Priority (Component Standardization)

These improve code maintainability and consistency.

### 17. Create TorchSliderDefaults for consistent slider styling
- [x] **Status:** Completed
- **Description:** Consolidate duplicate slider color definitions into shared defaults object.
- **Files:** New file: `ui/theme/TorchSliderDefaults.kt`, updated `BrightnessSlider.kt`, `SettingsScreen.kt`
- **Changes made:**
  - Created `TorchSliderDefaults` object with `colors()` composable function
  - Updated BrightnessSlider and SettingsScreen to use shared defaults

### 18. Define spacing tokens (Dimens.kt)
- [x] **Status:** Completed
- **Description:** Create consistent spacing scale and icon size tokens. Hardcoded values currently vary inconsistently (16dp, 12dp, 8dp used without pattern).
- **Files:** New file: `ui/theme/Dimens.kt`
- **Changes made:**
  - Created `TableTorchDimens` object with SpacingXs through SpacingXl
  - Added TouchTargetMin, CornerRadiusMd, CornerRadiusLg

### 19. Standardize icon sizes
- [x] **Status:** Completed
- **Description:** Define `TorchIconSize.Small` (24dp), `Medium` (32dp), `Large` (40dp). Currently sizes vary: 28dp, 32dp, 40dp across components.
- **Files:** `ui/theme/Dimens.kt`
- **Changes made:**
  - Added `TorchIconSize` object with Small, Medium, Large constants

### 20. Extract SettingsToggle to reusable component
- [x] **Status:** Completed
- **Description:** Move from private function in SettingsScreen to `/ui/components/` for reuse across potential future screens.
- **Files:** New file: `ui/components/SettingsToggle.kt`, updated `SettingsScreen.kt`
- **Changes made:**
  - Created public SettingsToggle component
  - Added optional `leadingIcon` and `leadingIconContentDescription` parameters
  - Updated SettingsScreen to import from new location

---

## Low Priority (Polish & Enhancement)

These add final polish to the user experience.

### 21. Replace shadow with color-matched glow
- [x] **Status:** Completed
- **Description:** The 18dp elevation shadow looks odd on dark colors. Replace with subtle glow border matching current color.
- **Files:** `MainScreen.kt`
- **Changes made:**
  - Replaced `.shadow()` with `.border(2.dp, currentColor.copy(alpha = 0.4f), RoundedCornerShape(24.dp))`

### 22. Add navigation transitions
- [x] **Status:** Completed
- **Description:** Add slide-in/slide-out animations between MainScreen and SettingsScreen for premium feel.
- **Files:** `NavGraph.kt`
- **Changes made:**
  - Added enterTransition, exitTransition, popEnterTransition, popExitTransition
  - Slide-in/slide-out with fadeIn/fadeOut

### 23. Improve settings section visual hierarchy
- [x] **Status:** Completed
- **Description:** Add dividers between sections and increase padding. Sections currently blend together.
- **Files:** `SettingsScreen.kt`
- **Changes made:**
  - Added HorizontalDivider between sections
  - Increased section header padding from 8.dp to 12.dp

### 24. Increase button spacing in ColorButtonsRow
- [x] **Status:** Completed
- **Description:** Change from 4dp to 8dp spacing for easier selection in low-light conditions.
- **Files:** `ColorButton.kt`
- **Changes made:**
  - Changed `Arrangement.spacedBy(4.dp)` to `Arrangement.spacedBy(8.dp)`

### 25. Add hex input field to color picker
- [x] **Status:** Completed
- **Description:** Allow power users to type hex codes directly instead of read-only display.
- **Files:** `FlameColorPicker.kt`
- **Changes made:**
  - Replaced read-only hex display with editable OutlinedTextField
  - Added hex input validation (0-9, A-F only, max 6 characters)
  - RGB sliders update when valid hex is entered
  - Error state shown for invalid/incomplete hex

### 26. Improve dialog button contrast
- [x] **Status:** Completed
- **Description:** Change cancel button from `Color.White.copy(alpha = 0.7f)` to `MaterialTheme.colorScheme.onSurfaceVariant` for proper 4.5:1 contrast ratio.
- **Files:** `FlameColorPicker.kt`
- **Changes made:**
  - Cancel button now uses `MaterialTheme.colorScheme.onSurfaceVariant`
  - All low-alpha white text replaced with theme colors

### 27. Add tap-to-skip on splash screen
- [x] **Status:** Completed
- **Description:** Allow users to skip the 1.4s splash by tapping, improving access in frequent low-light usage.
- **Files:** `SplashScreen.kt`, `strings.xml`
- **Changes made:**
  - Added `.clickable { onTimeout() }` to main container
  - Added contentDescription for accessibility
  - No visual indication (clean appearance)

### 28. Create contrast helper function
- [x] **Status:** Completed
- **Description:** Add `Color.contrastingTextColor()` extension to determine optimal text color based on luminance for overlays on torch colors.
- **Files:** `Color.kt`
- **Changes made:**
  - Added extension function using luminance calculation
  - Returns Color.Black for light colors, Color.White for dark colors

### 29. Add light theme option
- [x] **Status:** Completed
- **Description:** Define light color scheme even if dark is default, to prevent unpredictable "force dark mode" behavior on some devices.
- **Files:** `Color.kt`, `Theme.kt`
- **Changes made:**
  - Added light theme color variants (TorchOrangeLight, TorchBackgroundLight, etc.)
  - Created LightTableTorchColorScheme
  - Updated TableTorchTheme to accept `darkTheme: Boolean = true` parameter
  - Status bar and navigation bar adapt to theme

---

## Summary by Category

| Category | Task Numbers | Count |
|----------|--------------|-------|
| Accessibility | 1, 2, 3, 4, 13, 16, 27 | 7 |
| Theme System | 5, 6, 7, 8, 17, 18, 19, 28, 29 | 9 |
| Layout/UX | 9, 10, 11, 21, 22, 23, 24 | 7 |
| Visual Feedback | 12, 14, 15 | 3 |
| Components | 17, 18, 19, 20, 25, 26 | 6 |

---

## Progress Tracking

| Priority | Total | Completed | Remaining |
|----------|-------|-----------|-----------|
| Critical | 4 | 4 | 0 |
| High (Theme) | 4 | 4 | 0 |
| High (Layout) | 3 | 3 | 0 |
| Medium (Feedback) | 5 | 5 | 0 |
| Medium (Components) | 4 | 4 | 0 |
| Low | 9 | 9 | 0 |
| **Total** | **29** | **29** | **0** |

---

## New Files Created

| File | Purpose |
|------|---------|
| `ui/theme/TorchSliderDefaults.kt` | Shared slider color configuration |
| `ui/theme/Dimens.kt` | Spacing tokens and icon sizes |
| `ui/components/SettingsToggle.kt` | Reusable toggle row component |

---

## Notes

- All 29 recommendations have been implemented
- The app now has a clean, maximized light display with consolidated bottom controls
- Full accessibility compliance with WCAG guidelines
- Complete Material 3 theme system with light/dark support
- Consistent component styling through shared defaults and tokens
- Enhanced low-light usability with improved visibility and haptic feedback
