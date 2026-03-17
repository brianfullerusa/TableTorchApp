//
//  ScreenshotTests.swift
//  TableTorchUITests
//
//  Automated screenshot capture for App Store localization.
//  Each test method launches the app with specific launch arguments
//  to configure palette, color index, and visual effects, then
//  captures a named screenshot as an XCTAttachment.
//
//  Tests are split into two groups:
//  - Localized tests: run for every locale (splash, main, settings)
//  - English-only tests: color/ember screenshots with no localized text
//
//  The shell orchestrator runs English-only tests once and copies them
//  into every locale's output folder.
//

import XCTest

// MARK: - Localized Screenshots (run for every locale)

final class ScreenshotTests: XCTestCase {

    private var app: XCUIApplication!

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        app = XCUIApplication()
    }

    // MARK: - Helpers

    private func launch(
        splash: Bool = false,
        palette: String? = nil,
        colorIndex: Int? = nil,
        embers: Bool = false
    ) {
        var args = ["-uiScreenshotMode"]
        if splash {
            args.append("-uiScreenshotSplash")
        }
        if let palette {
            args += ["-uiPalette", palette]
        }
        if let colorIndex {
            args += ["-uiColorIndex", "\(colorIndex)"]
        }
        if embers {
            args.append("-uiEmberParticles")
        }
        app.launchArguments = args
        app.launch()
    }

    private func takeScreenshot(named name: String) {
        let screenshot = app.windows.firstMatch.screenshot()
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func openSettingsSheet() {
        let settingsButton = app.buttons["settingsButton"]
        XCTAssertTrue(
            settingsButton.waitForExistence(timeout: 5),
            "Settings button not found"
        )
        settingsButton.tap()

        let navBar = app.navigationBars.firstMatch
        XCTAssertTrue(
            navBar.waitForExistence(timeout: 5),
            "Settings sheet did not appear"
        )
        sleep(1)
    }

    private func swipeSheetToLargeDetent() {
        let window = app.windows.firstMatch
        let start = window.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        let end = window.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.1))
        start.press(forDuration: 0.1, thenDragTo: end)
        sleep(1)
    }

    private func swipeSheetContentUp() {
        let window = app.windows.firstMatch
        let start = window.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.7))
        let end = window.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.2))
        start.press(forDuration: 0.1, thenDragTo: end)
        sleep(1)
    }

    // MARK: - 1. Splash Screen

    func testSplashScreen() {
        launch(splash: true)
        sleep(2)
        takeScreenshot(named: "01_splash")
    }

    // MARK: - 2. Main Screen (Low Light, 4th color)

    func testMainScreen() {
        launch(palette: "lowLight", colorIndex: 3)
        sleep(1)
        takeScreenshot(named: "02_main_lowlight_color4")
    }

    // MARK: - 3. Settings Half-Open (Medium Detent)

    func testSettingsMedium() {
        launch(palette: "lowLight", colorIndex: 3)
        sleep(1)
        openSettingsSheet()
        takeScreenshot(named: "03_settings_medium")
    }

    // MARK: - 4. Settings Fully Open (Large Detent, top)

    func testSettingsLarge() {
        launch(palette: "lowLight", colorIndex: 3)
        sleep(1)
        openSettingsSheet()
        swipeSheetToLargeDetent()
        takeScreenshot(named: "04_settings_large")
    }

    // MARK: - 5. Settings Fully Open (Large Detent, scrolled down)

    func testSettingsLargeScrolled() {
        launch(palette: "lowLight", colorIndex: 3)
        sleep(1)
        openSettingsSheet()
        swipeSheetToLargeDetent()
        swipeSheetContentUp()
        takeScreenshot(named: "05_settings_large_scrolled")
    }
}

// MARK: - English-Only Screenshots (color/ember, no localized text)

final class ColorScreenshotTests: XCTestCase {

    private var app: XCUIApplication!

    override func setUp() {
        super.setUp()
        continueAfterFailure = false
        app = XCUIApplication()
    }

    private func launch(palette: String, colorIndex: Int, embers: Bool = false) {
        var args = ["-uiScreenshotMode", "-uiPalette", palette, "-uiColorIndex", "\(colorIndex)"]
        if embers {
            args.append("-uiEmberParticles")
        }
        app.launchArguments = args
        app.launch()
    }

    private func takeScreenshot(named name: String) {
        let screenshot = app.windows.firstMatch.screenshot()
        let attachment = XCTAttachment(screenshot: screenshot)
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    // MARK: - Low Light Palette - Each Color

    func testLowLightColors() {
        for colorIndex in 0..<6 {
            launch(palette: "lowLight", colorIndex: colorIndex)
            sleep(1)
            takeScreenshot(named: "06_lowlight_color\(colorIndex)")
        }
    }

    // MARK: - Party Palette - Each Color

    func testPartyColors() {
        for colorIndex in 0..<6 {
            launch(palette: "party", colorIndex: colorIndex)
            sleep(1)
            takeScreenshot(named: "07_party_color\(colorIndex)")
        }
    }

    // MARK: - Party Palette with Ember Particles

    func testPartyEmberColors() {
        for colorIndex in 0..<6 {
            launch(palette: "party", colorIndex: colorIndex, embers: true)
            sleep(8)
            takeScreenshot(named: "08_party_ember_color\(colorIndex)")
        }
    }
}
