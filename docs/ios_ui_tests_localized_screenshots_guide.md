The most practical way is:

1. write **UI tests** with XCTest that navigate to the exact screens you want,
2. run those tests once per localization, and
3. either:
   - use **fastlane snapshot** to automatically save screenshots into language-specific folders, or
   - use plain XCTest screenshots as test attachments, then export them from the test results.

For your specific goal, **fastlane snapshot is usually the best fit** because it is built to generate localized iOS screenshots across languages and devices, and it supports multiple languages and launch arguments out of the box.

## 1) Create a UI test target

In Xcode, add a **UI Testing Bundle** if you do not already have one. Apple’s UI automation tooling is based on XCTest/XCUIAutomation, and Xcode can even record interactions to help generate starter test code.

A basic UI test looks like this:

```swift
import XCTest

final class AppScreenshotsUITests: XCTestCase {
    let app = XCUIApplication()

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testHomeAndSettingsScreens() throws {
        app.launch()

        // Wait for first screen to exist
        XCTAssertTrue(app.navigationBars["Home"].waitForExistence(timeout: 5))

        takeScreenshot(named: "01_Home")

        app.buttons["Settings"].tap()
        XCTAssertTrue(app.navigationBars["Settings"].waitForExistence(timeout: 5))

        takeScreenshot(named: "02_Settings")
    }

    private func takeScreenshot(named name: String) {
        let screenshot = XCUIScreen.main.screenshot()
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
```

Apple supports capturing screenshots from UI tests with `XCUIScreen` / `XCUIScreenshot`, and attaching them to tests using `XCTAttachment`.

## 2) Launch the app in a specific localization

To test each language, launch the app with localization arguments. In UI tests, `XCUIApplication` supports `launchArguments`, and the common pattern is to pass `AppleLanguages` and `AppleLocale` before launch.

Example:

```swift
func launchApp(language: String, locale: String) {
    app.launchArguments += [
        "-AppleLanguages", "(\(language))",
        "-AppleLocale", locale
    ]
    app.launch()
}
```

Then create one test per localization, or parameterize via a helper:

```swift
func testScreens_en_US() throws {
    launchApp(language: "en", locale: "en_US")
    XCTAssertTrue(app.navigationBars["Home"].waitForExistence(timeout: 5))
    takeScreenshot(named: "01_Home_en")
}

func testScreens_es_ES() throws {
    launchApp(language: "es", locale: "es_ES")
    XCTAssertTrue(app.navigationBars["Inicio"].waitForExistence(timeout: 5))
    takeScreenshot(named: "01_Home_es")
}
```

## 3) Better than duplicating tests: use a Test Plan

A strong setup is:

- create one UI test method per screen flow
- create an **Xcode Test Plan**
- add one configuration per localization:
  - English: `-AppleLanguages (en) -AppleLocale en_US`
  - Spanish: `-AppleLanguages (es) -AppleLocale es_ES`
  - French: `-AppleLanguages (fr) -AppleLocale fr_FR`

That way the same test runs once for each locale without copying code.

## 4) Saving screenshots into folders by localization

### Best option: fastlane snapshot

`fastlane snapshot` is specifically for generating **localized iOS screenshots** on multiple devices and languages, and saving them in an organized structure for App Store use.

Typical setup:

### `Snapfile`

```ruby
languages([
  "en-US",
  "es-ES",
  "fr-FR"
])

devices([
  "iPhone 16 Pro"
])

scheme("YourApp")
output_directory("./fastlane/screenshots")
clear_previous_screenshots(true)
```

### UI test using Snapshot helper

After running `fastlane snapshot init`, fastlane gives you a `SnapshotHelper.swift` helper file. Then your UI test becomes:

```swift
import XCTest

final class SnapshotUITests: XCTestCase {
    let app = XCUIApplication()

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        setupSnapshot(app)
        app.launch()
    }

    func testTakeScreenshots() {
        snapshot("01_Home")

        app.buttons["Settings"].tap()
        snapshot("02_Settings")

        app.buttons["Profile"].tap()
        snapshot("03_Profile")
    }
}
```

When snapshot runs, it organizes output by language and device inside the screenshots directory. That is the cleanest match for “save screenshots in folders by localization.”

## 5) If you want to stay pure XCTest only

Plain XCTest screenshots are stored as **test attachments** in the test results bundle, not automatically as nice language folders on disk.

So with pure XCTest you would usually:

- run the UI tests per locale
- keep attachments with `.keepAlways`
- export them afterward from the `.xcresult` bundle, or inspect them in Xcode’s test report

This works, but it is more manual than fastlane.

## 6) Recommended project structure

I would structure it like this:

- `AppScreenshotsUITests.swift`
  - one flow per screen family
- `TestPlan.xctestplan`
  - one configuration per localization
- `fastlane/Snapfile`
  - list of languages and devices
- output:
  - `fastlane/screenshots/en-US/...`
  - `fastlane/screenshots/es-ES/...`
  - `fastlane/screenshots/fr-FR/...`

## 7) Tips so the screenshots stay stable

A few things make screenshot tests much more reliable:

- Seed predictable data so every locale shows the same content.
- Add accessibility identifiers to important buttons and screens.
- Wait for elements with `waitForExistence(timeout:)` instead of using `sleep`.
- Turn off popups, onboarding, and random animations when launched in screenshot mode.
- Pass a custom launch argument like `-uiScreenshotMode YES` and let the app load mock or demo data.

Example:

```swift
app.launchArguments += [
    "-uiScreenshotMode", "YES",
    "-AppleLanguages", "(es)",
    "-AppleLocale", "es_ES"
]
```

Then in your app:

```swift
let isScreenshotMode = ProcessInfo.processInfo.arguments.contains("-uiScreenshotMode")
```

## 8) Recommendation

For your goal, use:

- **XCTest UI tests** to drive the app
- **Xcode Test Plan** for locale configurations
- **fastlane snapshot** to save the screenshots in localization-specific folders

That gives you the least custom plumbing and the cleanest output structure. Apple’s XCTest APIs handle the navigation and capture, while fastlane handles the localization and device matrix and folder organization.

If you want, I can also turn this into a more complete starter package with:

- an `AppScreenshotsUITests.swift`
- a sample `Snapfile`
- and a sample `.xctestplan` layout
