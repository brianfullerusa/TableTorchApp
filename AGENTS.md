# Repository Guidelines

## Project Structure & Module Organization

This is a multi-platform project with iOS and Android applications sharing common assets.

```
TableTorch/
├── ios/                      # iOS/Xcode project
├── android/                  # Android Studio project
├── shared/                   # Shared assets and documentation
│   ├── assets/               # Images, icons for both platforms
│   ├── api-specs/            # API documentation
│   └── docs/                 # Project plans and documentation
└── AGENTS.md
```

### iOS App
The TableTorch SwiftUI app lives in the `ios/TableTorch/` directory. `MenuReaderApp.swift` wires the entry point, while views such as `ContentView.swift`, `SettingsView.swift`, `BrightnessSliderView.swift`, `ColorButtonsView.swift`, and `SplashView.swift` compose the UI. Device-facing services sit alongside them: `BrightnessManager.swift` manages screen brightness, and `MotionManager.swift` streams device pitch for angle-aware dimming. Visual assets are under `ios/TableTorch/Assets.xcassets`, and SwiftUI previews live in `ios/TableTorch/Preview Content`. Open `ios/TableTorch.xcodeproj` to inspect targets or adjust build settings.

### Android App
The Android app lives in the `android/` directory. Open this folder in Android Studio to build and run the app.

### Shared Resources
The `shared/` directory contains assets and documentation used by both platforms:
- `shared/assets/` - Common images and icons (copy to each platform's asset catalog as needed)
- `shared/api-specs/` - API specifications and contracts
- `shared/docs/` - Project plans, design documents, and cross-platform documentation

## Build, Test, and Development Commands

### iOS
- `open ios/TableTorch.xcodeproj` launches Xcode; use this for day-to-day editing and simulator runs.
- `xcodebuild -project ios/TableTorch.xcodeproj -scheme TableTorch -destination 'platform=iOS Simulator,name=iPhone 15' build` validates the app from the command line. Swap the destination to match your simulator or device UDID.
- `xcodebuild test -project ios/TableTorch.xcodeproj -scheme TableTorch -destination 'platform=iOS Simulator,name=iPhone 15'` prepares the testing pipeline. It currently reports "no tests", but keep the command handy for CI once XCTest targets exist.

### Android
- Open the `android/` folder in Android Studio for development.
- `./android/gradlew assembleDebug` builds the debug APK from the command line.
- `./android/gradlew test` runs unit tests.

## Coding Style & Naming Conventions

### iOS (Swift)
Follow standard Swift 5 style: four-space indentation, braces on the same line, and `UpperCamelCase` types with `lowerCamelCase` properties. Prefer SwiftUI modifiers chained in semantic blocks, grouped by layout, styling, then behavior. Keep stateful types `final` unless subclassing is required, and store shared data in `ObservableObject` implementations similar to `AppSettings`. Localized strings should be added to `ios/TableTorch/Localizable.xcstrings` using descriptive keys.

### Android (Kotlin)
Follow standard Kotlin conventions: four-space indentation, `UpperCamelCase` for classes, `lowerCamelCase` for functions and properties. Use Jetpack Compose for UI where possible. String resources go in `android/app/src/main/res/values/strings.xml`.

## Testing Guidelines

### iOS
There is no `TableTorchTests` target yet; create one before adding regressions. Co-locate test fixtures with the production module, and follow `FeatureNameTests.testBehaviorDescription` naming. Run `xcodebuild test` locally before pushing. Aim for unit coverage on managers (`BrightnessManager`, `MotionManager`) and view model logic; rely on SwiftUI preview snapshots only for ad-hoc UI checks.

### Android
Use JUnit for unit tests and place them in `android/app/src/test/`. Instrumented tests go in `android/app/src/androidTest/`. Run `./android/gradlew test` for unit tests and `./android/gradlew connectedAndroidTest` for instrumented tests.

## Commit & Pull Request Guidelines
Write imperative, single-line commit subjects (e.g., `Add motion-driven brightness clamp`). Include context in the body when behavior changes or new assets are introduced. Pull requests should link tracking issues, outline testing performed (simulator models, iOS/Android versions), and add screenshots whenever UI changes. Tag reviewers familiar with brightness or motion flows when touching those modules, and confirm localization keys for user-facing copy. Prefix commits with `[iOS]` or `[Android]` when changes are platform-specific.

## Device & Configuration Notes

### iOS
Angle-based brightness and idle-timer behavior depend on Motion & Fitness permissions and real device testing. When adjusting brightness logic, verify that `UIApplication.shared.isIdleTimerDisabled` toggles as expected on hardware, and reset system brightness after manual testing to avoid draining devices.

### Android
Test on multiple screen densities (mdpi, hdpi, xhdpi, xxhdpi). Verify runtime permissions for sensors. Use Android Emulator for quick iteration, but always confirm behavior on physical devices before release.
