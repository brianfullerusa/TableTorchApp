package com.rockyriverapps.tabletorch

import android.graphics.Bitmap
import android.os.Environment
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Instrumented test that launches the app, selects each particle shape,
 * and captures screenshots at 2-second and 5-second intervals.
 *
 * Screenshots are saved to:
 *   /sdcard/Pictures/ParticleScreenshots/
 *
 * To pull screenshots after running:
 *   adb pull /sdcard/Pictures/ParticleScreenshots/ ./particle_screenshots/
 */
@RunWith(AndroidJUnit4::class)
class ParticleScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val particleShapes = listOf("Embers", "Hearts", "Stars", "Snowflakes", "Music Notes")

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun captureAllParticleShapeScreenshots() {
        // Wait for app to fully launch and settle
        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        particleShapes.forEach { shapeName ->
            // --- Open settings ---
            composeTestRule.onNodeWithContentDescription("Settings")
                .performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(500)

            // Scroll down to "Ember Particles" toggle and enable it
            composeTestRule.onNodeWithText("Ember Particles")
                .performScrollTo()
            composeTestRule.waitForIdle()
            Thread.sleep(300)

            // Check if particles are already enabled by looking for the shape picker
            val shapePickerExists = try {
                composeTestRule.onNodeWithText("Particle Shape")
                    .assertExists()
                true
            } catch (_: AssertionError) {
                false
            }

            // If particles are not enabled, tap the toggle to enable them
            if (!shapePickerExists) {
                composeTestRule.onNodeWithText("Ember Particles")
                    .performClick()
                composeTestRule.waitForIdle()
                Thread.sleep(500)
            }

            // Scroll to make sure shape picker is visible
            composeTestRule.onNodeWithText("Particle Shape")
                .performScrollTo()
            composeTestRule.waitForIdle()
            Thread.sleep(300)

            // Select the particle shape
            composeTestRule.onNodeWithText(shapeName)
                .performScrollTo()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText(shapeName)
                .performClick()
            composeTestRule.waitForIdle()
            Thread.sleep(300)

            // Close settings by tapping "Done"
            composeTestRule.onNodeWithText("Done")
                .performClick()
            composeTestRule.waitForIdle()

            // Wait 2 seconds, then take screenshot
            Thread.sleep(2000)
            takeScreenshot("particle_${shapeName.replace(" ", "_")}_2s")

            // Wait another 3 seconds (5 total), then take screenshot
            Thread.sleep(3000)
            takeScreenshot("particle_${shapeName.replace(" ", "_")}_5s")
        }
    }

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
            "ParticleScreenshots"
        )
        dir.mkdirs()

        val file = File(dir, "$name.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
