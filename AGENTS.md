# Repository Guidelines

## Project Structure & Module Organization
The TableTorch SwiftUI app lives in the `TableTorch/` directory. `MenuReaderApp.swift` wires the entry point, while views such as `ContentView.swift`, `SettingsView.swift`, `BrightnessSliderView.swift`, `ColorButtonsView.swift`, and `SplashView.swift` compose the UI. Device-facing services sit alongside them: `BrightnessManager.swift` manages screen brightness, and `MotionManager.swift` streams device pitch for angle-aware dimming. Visual assets are under `TableTorch/Assets.xcassets`, and SwiftUI previews live in `TableTorch/Preview Content`. Open `TableTorch.xcodeproj` to inspect targets or adjust build settings.

## Build, Test, and Development Commands
- `open TableTorch.xcodeproj` launches Xcode; use this for day-to-day editing and simulator runs.
- `xcodebuild -scheme TableTorch -destination 'platform=iOS Simulator,name=iPhone 15' build` validates the app from the command line. Swap the destination to match your simulator or device UDID.
- `xcodebuild test -scheme TableTorch -destination 'platform=iOS Simulator,name=iPhone 15'` prepares the testing pipeline. It currently reports “no tests”, but keep the command handy for CI once XCTest targets exist.

## Coding Style & Naming Conventions
Follow standard Swift 5 style: four-space indentation, braces on the same line, and `UpperCamelCase` types with `lowerCamelCase` properties. Prefer SwiftUI modifiers chained in semantic blocks, grouped by layout, styling, then behavior. Keep stateful types `final` unless subclassing is required, and store shared data in `ObservableObject` implementations similar to `AppSettings`. Localized strings should be added to `TableTorch/Localizable.xcstrings` using descriptive keys.

## Testing Guidelines
There is no `TableTorchTests` target yet; create one before adding regressions. Co-locate test fixtures with the production module, and follow `FeatureNameTests.testBehaviorDescription` naming. Run `xcodebuild test` locally before pushing. Aim for unit coverage on managers (`BrightnessManager`, `MotionManager`) and view model logic; rely on SwiftUI preview snapshots only for ad-hoc UI checks.

## Commit & Pull Request Guidelines
Write imperative, single-line commit subjects (e.g., `Add motion-driven brightness clamp`). Include context in the body when behavior changes or new assets are introduced. Pull requests should link tracking issues, outline testing performed (simulator models, iOS version), and add screenshots whenever UI changes. Tag reviewers familiar with brightness or motion flows when touching those modules, and confirm localization keys for user-facing copy.

## Device & Configuration Notes
Angle-based brightness and idle-timer behavior depend on Motion & Fitness permissions and real device testing. When adjusting brightness logic, verify that `UIApplication.shared.isIdleTimerDisabled` toggles as expected on hardware, and reset system brightness after manual testing to avoid draining devices.
