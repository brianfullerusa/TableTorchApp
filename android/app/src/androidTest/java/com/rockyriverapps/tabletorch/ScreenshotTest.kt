package com.rockyriverapps.tabletorch

import android.graphics.Bitmap
import android.os.Environment
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Automated screenshot capture for Google Play Store listing.
 * Mirrors the iOS ScreenshotTests to produce equivalent screenshots.
 *
 * Target device: Samsung Galaxy S25 (6.2", 1080x2340)
 *
 * Screenshots are saved to:
 *   /sdcard/Pictures/TableTorchScreenshots/
 *
 * To pull screenshots after running:
 *   adb pull /sdcard/Pictures/TableTorchScreenshots/ ./screenshots/android/en/6.2in/
 *
 * Run with:
 *   ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.rockyriverapps.tabletorch.ScreenshotTest
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ========================================================================
    // Main Screen Screenshots
    // ========================================================================

    /**
     * Main screen with Low Light palette, 4th color (golden amber).
     * Equivalent to iOS: 02_main_lowlight_color4
     */
    @Test
    fun screenshot_02_main_lowlight_color3() {
        waitForAppReady()
        switchToPalette("Low Light")
        selectColorInBar(3)
        waitForSettle()
        takeScreenshot("02_main_lowlight_color3")
    }

    // ========================================================================
    // Settings Screenshots
    // ========================================================================

    /**
     * Settings sheet fully open.
     * Equivalent to iOS: 04_settings_large
     */
    @Test
    fun screenshot_03_settings() {
        waitForAppReady()
        switchToPalette("Low Light")
        selectColorInBar(3)
        waitForSettle()
        openSettings()
        takeScreenshot("03_settings")
    }

    /**
     * Settings sheet scrolled down to show visual effects section.
     * Equivalent to iOS: 05_settings_large_scrolled
     */
    @Test
    fun screenshot_04_settings_scrolled() {
        waitForAppReady()
        switchToPalette("Low Light")
        selectColorInBar(3)
        waitForSettle()
        openSettings()
        scrollSettingsDown()
        takeScreenshot("04_settings_scrolled")
    }

    // ========================================================================
    // Low Light Palette - Each Color
    // ========================================================================

    /**
     * All 6 Low Light palette colors on the main screen.
     * Equivalent to iOS: 06_lowlight_color0 through 06_lowlight_color5
     */
    @Test
    fun screenshot_06_lowlight_colors() {
        waitForAppReady()
        switchToPalette("Low Light")

        for (colorIndex in 0 until 6) {
            selectColorInBar(colorIndex)
            waitForSettle()
            takeScreenshot("06_lowlight_color$colorIndex")
        }
    }

    // ========================================================================
    // Party Palette - Each Color
    // ========================================================================

    /**
     * All 6 Party palette colors on the main screen.
     * Equivalent to iOS: 07_party_color0 through 07_party_color5
     */
    @Test
    fun screenshot_07_party_colors() {
        waitForAppReady()
        switchToPalette("Party")

        for (colorIndex in 0 until 6) {
            selectColorInBar(colorIndex)
            waitForSettle()
            takeScreenshot("07_party_color$colorIndex")
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private fun waitForAppReady() {
        composeTestRule.waitForIdle()
        Thread.sleep(1500)
    }

    private fun waitForSettle() {
        composeTestRule.waitForIdle()
        Thread.sleep(800)
    }

    /**
     * Opens the settings sheet by tapping the settings icon.
     * The settings icon is either inside the FloatingColorBar (contentDescription = "Settings")
     * or a standalone button when the color bar is hidden.
     */
    private fun openSettings() {
        composeTestRule.onNodeWithContentDescription("Settings")
            .performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(800)
    }

    /**
     * Closes the settings sheet by tapping the "Done" button.
     */
    private fun closeSettings() {
        composeTestRule.onNodeWithText("Done")
            .performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(500)
    }

    /**
     * Switches to a palette by opening settings, tapping the palette chip,
     * and closing settings.
     */
    private fun switchToPalette(paletteName: String) {
        openSettings()

        // Scroll to make palette chips visible and tap the palette
        composeTestRule.onNodeWithText(paletteName)
            .performScrollTo()
        composeTestRule.waitForIdle()
        Thread.sleep(300)

        composeTestRule.onNodeWithText(paletteName)
            .performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        closeSettings()
    }

    /**
     * Selects a color in the floating color bar by tapping the corresponding flame icon.
     * Color bar buttons have contentDescription like "Color 1", "Color 2", etc.
     */
    @OptIn(ExperimentalTestApi::class)
    private fun selectColorInBar(index: Int) {
        val colorNumber = index + 1

        // Try to find and tap the color button in the floating bar.
        // The contentDescription is "Color N" or "Color N, selected".
        // Use hasContentDescription with substring=true to match either.
        val selectedDesc = "Color $colorNumber, selected"
        val unselectedDesc = "Color $colorNumber"

        // Check if already selected
        val alreadySelected = try {
            composeTestRule.onNodeWithContentDescription(selectedDesc)
                .assertExists()
            true
        } catch (_: AssertionError) {
            false
        }

        if (!alreadySelected) {
            composeTestRule.onNodeWithContentDescription(unselectedDesc)
                .performClick()
            composeTestRule.waitForIdle()
        }
    }

    /**
     * Scrolls the settings sheet content down to reveal lower sections.
     */
    private fun scrollSettingsDown() {
        // Scroll to a node near the bottom of settings to trigger scroll
        try {
            composeTestRule.onNodeWithText("Breathing Animation")
                .performScrollTo()
        } catch (_: AssertionError) {
            // Fallback: try to swipe up on the sheet content
        }
        composeTestRule.waitForIdle()
        Thread.sleep(500)
    }

    /**
     * Takes a screenshot using UiAutomation and saves it to external storage.
     */
    private fun takeScreenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiAutomation = instrumentation.uiAutomation
        val bitmap = uiAutomation.takeScreenshot()

        if (bitmap != null) {
            saveBitmap(bitmap, name)
            bitmap.recycle()
        }
    }

    @Suppress("DEPRECATION")
    private fun saveBitmap(bitmap: Bitmap, name: String) {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "TableTorchScreenshots"
        )
        dir.mkdirs()

        val file = File(dir, "$name.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
