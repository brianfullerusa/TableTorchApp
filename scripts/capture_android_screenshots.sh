#!/usr/bin/env bash
#
# capture_android_screenshots.sh
#
# Automated Play Store screenshot capture for TableTorch Android.
# Runs instrumented tests on a connected device/emulator and pulls
# screenshot PNGs to the local screenshots directory.
#
# Target device: Samsung Galaxy S25 (6.2", 1080x2340)
#
# Prerequisites:
#   - Connected device or running emulator (adb devices must show a device)
#   - App must be installable on the device
#
# Usage:
#   ./scripts/capture_android_screenshots.sh           # full screenshot suite
#   ./scripts/capture_android_screenshots.sh --test <method>  # single test method
#
# Examples:
#   ./scripts/capture_android_screenshots.sh
#   ./scripts/capture_android_screenshots.sh --test screenshot_02_main_lowlight_color3
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ANDROID_DIR="$PROJECT_DIR/android"
OUTPUT_DIR="$PROJECT_DIR/screenshots/android/en/6.2in"

# --- Resolve Android SDK tools ---
ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"

if [[ ! -x "$ADB" ]]; then
    echo "ERROR: adb not found at $ADB"
    echo "Set ANDROID_HOME or install Android SDK platform-tools."
    exit 1
fi

# Device screenshot directory (must match ScreenshotTest.kt)
DEVICE_SCREENSHOT_DIR="/sdcard/Pictures/TableTorchScreenshots"

# Test class
TEST_CLASS="com.rockyriverapps.tabletorch.ScreenshotTest"

# --- Parse arguments ---
SINGLE_TEST=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --test)
            SINGLE_TEST="$2"
            shift 2
            ;;
        *)
            echo "Unknown argument: $1"
            echo "Usage: $0 [--test <test_method_name>]"
            exit 1
            ;;
    esac
done

# --- Verify ADB device ---
echo "=== Checking for connected device ==="
if ! "$ADB" devices | grep -q "device$"; then
    echo "ERROR: No connected Android device found."
    echo "Connect a device or start an emulator, then try again."
    exit 1
fi

DEVICE_MODEL=$("$ADB" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || echo "unknown")
DEVICE_RESOLUTION=$("$ADB" shell wm size 2>/dev/null | grep -oE '[0-9]+x[0-9]+' | tail -1 || echo "unknown")
echo "Device: $DEVICE_MODEL ($DEVICE_RESOLUTION)"

# --- Clean previous screenshots on device ---
echo ""
echo "=== Cleaning previous screenshots on device ==="
"$ADB" shell "rm -rf $DEVICE_SCREENSHOT_DIR" 2>/dev/null || true
"$ADB" shell "mkdir -p $DEVICE_SCREENSHOT_DIR" 2>/dev/null || true

# --- Build and run tests ---
echo ""
echo "=== Running screenshot tests ==="

cd "$ANDROID_DIR"

if [[ -n "$SINGLE_TEST" ]]; then
    echo "Running single test: $SINGLE_TEST"
    TEST_ARGS="-Pandroid.testInstrumentationRunnerArguments.class=${TEST_CLASS}#${SINGLE_TEST}"
else
    echo "Running all screenshot tests"
    TEST_ARGS="-Pandroid.testInstrumentationRunnerArguments.class=${TEST_CLASS}"
fi

# Run the instrumented tests
# Use --info for more verbose output, --stacktrace for debugging
if ./gradlew connectedAndroidTest $TEST_ARGS 2>&1 | tee /dev/stderr | tail -5; then
    echo ""
    echo "=== Tests completed ==="
else
    echo ""
    echo "=== Tests completed (some may have failed) ==="
fi

# --- Pull screenshots from device ---
echo ""
echo "=== Pulling screenshots from device ==="

mkdir -p "$OUTPUT_DIR"

# Check if screenshots were created on device
SCREENSHOT_COUNT=$("$ADB" shell "ls $DEVICE_SCREENSHOT_DIR/*.png 2>/dev/null | wc -l" | tr -d '\r ')
if [[ "$SCREENSHOT_COUNT" -eq 0 ]]; then
    echo "WARNING: No screenshots found on device at $DEVICE_SCREENSHOT_DIR"
    echo "The tests may have failed to capture screenshots."
    exit 1
fi

# Pull all screenshots
"$ADB" pull "$DEVICE_SCREENSHOT_DIR/." "$OUTPUT_DIR/"

# --- Summary ---
echo ""
echo "=== Screenshot Capture Complete ==="
LOCAL_COUNT=$(find "$OUTPUT_DIR" -name "*.png" 2>/dev/null | wc -l | tr -d ' ')
echo "Screenshots captured: $LOCAL_COUNT"
echo "Output directory:     $OUTPUT_DIR"
echo ""
echo "Screenshots:"
ls -la "$OUTPUT_DIR"/*.png 2>/dev/null || echo "  (none found)"
