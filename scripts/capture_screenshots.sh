#!/usr/bin/env bash
#
# capture_screenshots.sh
#
# Automated App Store screenshot capture for TableTorch iOS.
# Uses xcodebuild build-for-testing / test-without-building to run
# XCUITests across multiple simulators and locales, then extracts
# screenshot attachments from .xcresult bundles.
#
# Color/ember screenshots (no localized text) run only for English
# and are copied into every other locale's folder automatically.
#
# Usage:
#   ./scripts/capture_screenshots.sh              # full matrix
#   ./scripts/capture_screenshots.sh --quick       # en on iPhone 16 Pro Max only
#   ./scripts/capture_screenshots.sh --locale en   # single locale, all devices
#   ./scripts/capture_screenshots.sh --device "iPhone 16 Pro Max" # single device, all locales
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
IOS_DIR="$PROJECT_DIR/ios"
OUTPUT_DIR="$PROJECT_DIR/screenshots"
RESULTS_DIR="$PROJECT_DIR/.screenshot_results"

SCHEME="TableTorch"
PROJECT="$IOS_DIR/TableTorch.xcodeproj"
DERIVED_DATA="$PROJECT_DIR/.screenshot_derived_data"

# Maximum parallel simulators
MAX_PARALLEL="${MAX_PARALLEL:-4}"

# --- Device Matrix ---
# Format: "Simulator Name|folder_name"
# To add older devices (iPhone 8, SE, etc.), install their iOS runtimes
# via Xcode > Settings > Platforms, then uncomment the lines below.
declare -a DEVICES=(
    "iPhone 16 Pro Max|6.9in"
    "iPhone 16 Plus|6.7in"
    "iPhone 16|6.1in"
    "iPhone 16e|4.7in"
    # "iPhone 11 Pro Max|6.5in"   # requires iOS 17.x runtime
    # "iPhone 15|6.1in"           # requires iOS 17.x runtime
    # "iPhone 8 Plus|5.5in"       # requires iOS 16.x runtime
    # "iPhone SE (3rd generation)|4.7in"  # requires iOS 17.x runtime
)

# --- Locale Matrix ---
declare -a LOCALES=(
    en ar bg ca cs da de el en-AU en-GB
    es es-419 es-US et fi fr he hi hr hu
    id it ja ko lt lv ms nb nl pl
    pt-BR pt-PT ro ru sk sl sv th tr uk
    vi zh-Hans zh-Hant
)

# --- Test class names ---
# Localized tests: splash, main screen, settings (run for every locale)
LOCALIZED_TESTS="TableTorchUITests/ScreenshotTests"
# Color-only tests: no localized text (run only for English, then copied)
COLOR_TESTS="TableTorchUITests/ColorScreenshotTests"

# --- Parse arguments ---
QUICK_MODE=false
SINGLE_LOCALE=""
SINGLE_DEVICE=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --quick)
            QUICK_MODE=true
            shift
            ;;
        --locale)
            SINGLE_LOCALE="$2"
            shift 2
            ;;
        --device)
            SINGLE_DEVICE="$2"
            shift 2
            ;;
        *)
            echo "Unknown argument: $1"
            exit 1
            ;;
    esac
done

if $QUICK_MODE; then
    DEVICES=("iPhone 16 Pro Max|6.9in")
    LOCALES=(en)
fi

if [[ -n "$SINGLE_LOCALE" ]]; then
    LOCALES=("$SINGLE_LOCALE")
fi

# --- Helper: parse locale into language and region ---
parse_locale() {
    local locale="$1"
    local lang region

    case "$locale" in
        zh-Hans) lang="zh-Hans"; region="" ;;
        zh-Hant) lang="zh-Hant"; region="" ;;
        pt-BR)   lang="pt"; region="BR" ;;
        pt-PT)   lang="pt"; region="PT" ;;
        en-AU)   lang="en"; region="AU" ;;
        en-GB)   lang="en"; region="GB" ;;
        es-419)  lang="es"; region="419" ;;
        es-US)   lang="es"; region="US" ;;
        *)       lang="$locale"; region="" ;;
    esac

    echo "$lang|$region"
}

# --- iOS Simulator OS version ---
# Auto-detect the OS version for the first device, or set manually:
#   export SIM_OS="18.6"
if [[ -z "${SIM_OS:-}" ]]; then
    first_device="${DEVICES[0]%%|*}"
    SIM_OS=$(xcrun simctl list devices available -j \
        | python3 -c "
import json, sys
data = json.load(sys.stdin)
name = '$first_device'
for runtime, devs in data.get('devices', {}).items():
    for d in devs:
        if d['name'] == name and d['isAvailable']:
            # runtime like 'com.apple.CoreSimulator.SimRuntime.iOS-18-6'
            ver = runtime.rsplit('.', 1)[-1].replace('iOS-','').replace('-','.')
            print(ver)
            sys.exit(0)
print('latest')
" 2>/dev/null || echo "latest")
    echo "Auto-detected simulator OS: $SIM_OS"
fi

# --- Step 1: Build for testing ---
echo "=== Building for testing ==="
xcodebuild build-for-testing \
    -project "$PROJECT" \
    -scheme "$SCHEME" \
    -derivedDataPath "$DERIVED_DATA" \
    -destination "platform=iOS Simulator,name=${DEVICES[0]%%|*},OS=$SIM_OS" \
    -quiet \
    2>&1 | tail -5

echo "=== Build complete ==="

# --- Run a test suite for a device/locale ---
run_test() {
    local device_name="$1"
    local folder_name="$2"
    local locale="$3"
    local test_class="$4"
    local label="$5"

    local parsed
    parsed=$(parse_locale "$locale")
    local lang="${parsed%%|*}"
    local region="${parsed##*|}"

    local result_path="$RESULTS_DIR/${locale}_${folder_name}_${label}.xcresult"
    local output_path="$OUTPUT_DIR/$locale/$folder_name"

    echo "  [TEST] $locale / $folder_name - $label ($device_name)"

    # Remove old result bundle
    rm -rf "$result_path"

    local lang_args=(-testLanguage "$lang")
    if [[ -n "$region" ]]; then
        lang_args+=(-testRegion "$region")
    fi

    if xcodebuild test-without-building \
        -project "$PROJECT" \
        -scheme "$SCHEME" \
        -derivedDataPath "$DERIVED_DATA" \
        -destination "platform=iOS Simulator,name=$device_name,OS=$SIM_OS" \
        -resultBundlePath "$result_path" \
        -only-testing:"$test_class" \
        "${lang_args[@]}" \
        -quiet \
        2>&1 | tail -3; then
        echo "  [DONE] $locale / $folder_name - $label"
    else
        echo "  [FAIL] $locale / $folder_name - $label (extracting anyway)"
    fi

    # Extract screenshots from result bundle
    extract_screenshots "$result_path" "$output_path"
}

# --- Extract screenshots from .xcresult ---
extract_screenshots() {
    local result_path="$1"
    local output_path="$2"

    if [[ ! -d "$result_path" ]]; then
        echo "  [WARN] No result bundle at $result_path"
        return 1
    fi

    mkdir -p "$output_path"

    # Export attachments via xcresulttool and rename using manifest
    python3 "$SCRIPT_DIR/extract_screenshots.py" "$result_path" "$output_path" || {
        echo "  [WARN] Extraction failed for $result_path (will retry on next run)"
        return 0
    }
}

# --- Copy English color screenshots to other locale folders ---
copy_color_screenshots() {
    local folder_name="$1"
    local en_dir="$OUTPUT_DIR/en/$folder_name"

    if [[ ! -d "$en_dir" ]]; then
        echo "  [WARN] No English screenshots at $en_dir to copy"
        return 0
    fi

    for locale in "${LOCALES[@]}"; do
        [[ "$locale" == "en" ]] && continue
        local dest="$OUTPUT_DIR/$locale/$folder_name"
        mkdir -p "$dest"
        # Copy color/ember screenshots (06_, 07_, 08_ prefixes)
        for png in "$en_dir"/0[678]_*.png; do
            [[ -f "$png" ]] || continue
            cp -n "$png" "$dest/" 2>/dev/null || true
        done
    done
    echo "  [COPY] Color screenshots from en/$folder_name -> ${#LOCALES[@]} locales"
}

# --- Main loop ---
mkdir -p "$RESULTS_DIR"
mkdir -p "$OUTPUT_DIR"

for device_entry in "${DEVICES[@]}"; do
    device_name="${device_entry%%|*}"
    folder_name="${device_entry##*|}"

    if [[ -n "$SINGLE_DEVICE" ]] && [[ "$device_name" != "$SINGLE_DEVICE" ]]; then
        continue
    fi

    echo ""
    echo "=== Device: $device_name ($folder_name) ==="

    # --- Phase 1: English color/ember screenshots (no localized text) ---
    echo ""
    echo "--- Phase 1: Color screenshots (English only) ---"
    run_test "$device_name" "$folder_name" "en" "$COLOR_TESTS" "colors"

    # --- Phase 2: Localized screenshots for every locale ---
    echo ""
    echo "--- Phase 2: Localized screenshots (${#LOCALES[@]} locales) ---"
    locale_num=0
    for locale in "${LOCALES[@]}"; do
        locale_num=$((locale_num + 1))
        echo "[$locale_num/${#LOCALES[@]}]"
        run_test "$device_name" "$folder_name" "$locale" "$LOCALIZED_TESTS" "localized"
    done

    # --- Phase 3: Copy English color screenshots to all other locales ---
    echo ""
    echo "--- Phase 3: Copying color screenshots to all locales ---"
    copy_color_screenshots "$folder_name"
done

# --- Summary ---
echo ""
echo "=== Screenshot Capture Complete ==="
total_screenshots=$(find "$OUTPUT_DIR" -name "*.png" 2>/dev/null | wc -l | tr -d ' ')
total_folders=$(find "$OUTPUT_DIR" -mindepth 2 -maxdepth 2 -type d 2>/dev/null | wc -l | tr -d ' ')
echo "Total screenshots: $total_screenshots"
echo "Total folders:     $total_folders"
echo "Output directory:  $OUTPUT_DIR"
