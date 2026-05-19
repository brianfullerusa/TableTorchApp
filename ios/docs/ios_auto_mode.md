# TableTorch Auto Mode — Unified Design Plan

## Overview

Auto Mode uses the front-facing camera (which points at the menu when the screen illuminates it) to continuously sense ambient lighting and surface colors, then automatically selects the optimal screen color and brightness from the user's active palette. No camera preview is shown — the camera operates entirely in the background.

---

## 1. Core Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────┐
│  AutoModeManager (@EnvironmentObject, @MainActor)   │
│                                                     │
│  ┌──────────────────┐   ┌────────────────────────┐  │
│  │  CameraAnalyzer  │──>│  Stability Pipeline    │  │
│  │  (internal)      │   │  • Asymmetric EMA      │  │
│  │                  │   │  • Hysteresis band      │  │
│  │  AVCaptureSession│   │  • Rate limiter         │  │
│  │  EXIF extraction │   │                        │  │
│  │  CIAreaAverage   │   │  Kruithof curve scoring │  │
│  └──────────────────┘   │  3-regime brightness    │  │
│                         └───────┬────────────────┘  │
│                                 │                   │
│  @Published recommendedIndex: Int?                  │
│  @Published recommendedBrightness: CGFloat          │
│  @Published state: AutoModeState                    │
└────────────────────┬────────────────────────────────┘
                     │
                     ▼
┌──────────────────────────────────────────┐
│  ContentView (existing)                  │
│  • Reads recommendedIndex when auto on   │
│  • Writes to brightnessDraft             │
│  • Existing LuminousCanvasView renders   │
│  • FloatingColorBarView gains Auto token │
└──────────────────────────────────────────┘
```

### Design Decisions

- `CameraAnalyzer` is an internal implementation detail of `AutoModeManager` — never exposed to views.
- `AutoModeManager` is the single new `@EnvironmentObject`.
- `BrightnessManager` remains completely unchanged.

### New Files

| File | Purpose |
|---|---|
| `Managers/CameraAnalyzer.swift` | AVCaptureSession lifecycle, EXIF extraction, pixel analysis |
| `Managers/AutoModeManager.swift` | Decision engine, stability pipeline, publishes recommendations |
| `Models/LightingAnalysis.swift` | Value type for per-frame sensor data |
| `Views/AutoModeToken.swift` | "Auto" token for the FloatingColorBarView |

### Modified Files

| File | Changes |
|---|---|
| `MenuReaderApp.swift` | Add `@StateObject` for `AutoModeManager`, inject as `.environmentObject` |
| `ContentView.swift` | Add `.onReceive` for auto brightness/color, extend scene phase handling, disable gestures when auto active |
| `AppSettings.swift` | Add `isAutoModeEnabled: Bool`, `autoModeSensitivity: Double` |
| `FloatingColorBarView.swift` | Conditionally show Auto token when auto mode is active |
| `SettingsSheetView.swift` | Add Auto Mode settings section |
| `Info.plist` | Add `NSCameraUsageDescription` |

### Unchanged (Intentionally)

`BrightnessManager.swift`, `MotionManager.swift`, `LuminousCanvasView.swift`, all particle/glow views, `ColorPalette.swift`, `HapticEngine.swift`

---

## 2. Camera Capture Pipeline

### Configuration

- **Camera**: Front-facing wide-angle (`.builtInWideAngleCamera`, `.front`)
- **Preset**: `.low` (352x288) — minimal resolution since we only need aggregate statistics
- **Pixel format**: `kCVPixelFormatType_32BGRA` (native to CoreImage)
- **Frame rate**: Adaptive — 4fps startup burst (3 seconds), 2fps steady-state, duty-cycles down to 0.5fps when stable
- **Audio**: None — no `AVAudioSession` interaction, won't interrupt music playback

### Camera Setup

```swift
let discoverySession = AVCaptureDevice.DiscoverySession(
    deviceTypes: [.builtInWideAngleCamera],
    mediaType: .video,
    position: .front
)
guard let frontCamera = discoverySession.devices.first else { return }

let input = try AVCaptureDeviceInput(device: frontCamera)
session.addInput(input)

let videoOutput = AVCaptureVideoDataOutput()
videoOutput.videoSettings = [
    kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA
]
videoOutput.alwaysDiscardsLateVideoFrames = true
videoOutput.setSampleBufferDelegate(self, queue: processingQueue)
session.addOutput(videoOutput)

// Low frame rate for power efficiency
try frontCamera.lockForConfiguration()
frontCamera.activeVideoMinFrameDuration = CMTime(value: 1, timescale: 4)  // 4 fps max
frontCamera.activeVideoMaxFrameDuration = CMTime(value: 1, timescale: 2)  // 2 fps min
frontCamera.unlockForConfiguration()
```

### State Machine

```
idle → requestingPermission → denied
                            → running → throttled → running
                                      → suspended → running
                                      → error
```

- `suspended`: App backgrounded or session interrupted (phone call)
- `throttled`: Thermal pressure — reduced frame rate or paused
- `denied`: Camera permission refused — shows inline warning with Settings deep-link

### Primary Ambient Signal: EXIF Metadata

EXIF `BrightnessValue` is the primary ambient light signal. This is superior to pixel analysis because:

- The camera's auto-exposure compensates for scene brightness — raw pixel values are misleading (a dark room and bright room produce similar pixel averages after AE normalization)
- EXIF tells us what the camera *had to do* (ISO, exposure time, brightness APEX) to produce those pixels
- Virtually zero processing cost — just dictionary lookup on `CMSampleBuffer` attachments
- Typical APEX range: -2 (very dark restaurant) to +12 (bright sunlight)

```swift
func extractExposureMetadata(from sampleBuffer: CMSampleBuffer) -> ExposureInfo? {
    guard let metadata = CMCopyDictionaryOfAttachments(
        allocator: nil,
        target: sampleBuffer,
        attachmentMode: kCMAttachmentMode_ShouldPropagate
    ) as? [String: Any] else { return nil }

    guard let exif = metadata[kCGImagePropertyExifDictionary as String] as? [String: Any]
    else { return nil }

    let iso = exif[kCGImagePropertyExifISOSpeedRatings as String] as? [Int]
    let exposureDuration = exif[kCGImagePropertyExifExposureTime as String] as? Double
    let brightness = exif[kCGImagePropertyExifBrightnessValue as String] as? Double

    return ExposureInfo(
        iso: iso?.first ?? 100,
        exposureDuration: exposureDuration ?? (1.0 / 30.0),
        brightnessValue: brightness ?? 0.0
    )
}
```

### Secondary Signal: CIAreaAverage for Color

GPU-accelerated average color computation on center 60% crop via a 3x3 grid. Used for ambient color temperature estimation and menu surface analysis. `CIContext` is created once and reused across frames.

```swift
func extractDominantColors(from pixelBuffer: CVPixelBuffer, context: CIContext) -> [DominantColor] {
    let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
    let extent = ciImage.extent

    // Center crop: 60% of frame
    let cropRect = extent.insetBy(dx: extent.width * 0.2, dy: extent.height * 0.2)
    let cropped = ciImage.cropped(to: cropRect)

    // Divide into 3x3 grid and compute average for each cell
    var colors: [DominantColor] = []
    let cellWidth = cropRect.width / 3.0
    let cellHeight = cropRect.height / 3.0

    for row in 0..<3 {
        for col in 0..<3 {
            let cellRect = CGRect(
                x: cropRect.minX + CGFloat(col) * cellWidth,
                y: cropRect.minY + CGFloat(row) * cellHeight,
                width: cellWidth,
                height: cellHeight
            )
            if let avgColor = areaAverage(of: cropped, in: cellRect, context: context) {
                colors.append(avgColor)
            }
        }
    }

    return clusterColors(colors)
}
```

### Edge Case Handling

- **Camera occlusion**: If mean luminance < 0.02 and variance < 0.005 → freeze last good reading
- **Specular highlights** (glossy menus): If P90/P50 luminance ratio > 2.0 → use trimmed mean (10th-80th percentile)
- **Session interruptions**: Handle `AVCaptureSession.wasInterruptedNotification` (FaceTime, phone calls)

### Frame Processing Cadence

```swift
private var lastProcessedTime: CFTimeInterval = 0
private let minimumProcessingInterval: CFTimeInterval = 0.5  // 2 Hz

func captureOutput(
    _ output: AVCaptureOutput,
    didOutput sampleBuffer: CMSampleBuffer,
    from connection: AVCaptureConnection
) {
    let now = CACurrentMediaTime()
    guard now - lastProcessedTime >= minimumProcessingInterval else { return }
    lastProcessedTime = now

    guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

    let exposure = extractExposureMetadata(from: sampleBuffer)
    let dominantColors = extractDominantColors(from: pixelBuffer, context: reusableCIContext)
    let documentAnalysis = estimateDocumentRegion(from: dominantColors)

    let result = LightingAnalysis(
        ambientBrightness: exposure?.brightnessValue ?? 0,
        averageLuminance: computeAverageLuminance(from: pixelBuffer),
        documentAnalysis: documentAnalysis,
        timestamp: now,
        confidence: adjustConfidence(for: exposure)
    )

    Task { @MainActor in
        autoModeDelegate?.didUpdateAnalysis(result)
    }
}
```

---

## 3. Algorithms

### 3a. Ambient Light to Brightness (Three-Regime Model)

Based on human vision psychophysics (Stevens' Power Law, scotopic/photopic transition):

| Ambient Lux | Regime | Screen Brightness | Rationale |
|---|---|---|---|
| < 1 | Scotopic | 5% – 15% | Preserve dark adaptation |
| 1 – 50 | Mesopic | 15% – 60% | Power curve (exponent 0.4) |
| 50 – 500 | Photopic | 60% – 100% | Linear ramp, compete with ambient |
| > 500 | Bright | 100% | Max output |

```swift
func optimalBrightness(ambientLux: Float) -> CGFloat {
    // Scotopic: near total darkness
    if ambientLux < 1.0 {
        let t = ambientLux / 1.0
        return CGFloat(0.05 + 0.10 * t)
    }

    // Mesopic: typical dim restaurant
    if ambientLux < 50.0 {
        let t = (ambientLux - 1.0) / 49.0
        return CGFloat(0.15 + 0.45 * pow(t, 0.4))
    }

    // Photopic: well-lit environment
    if ambientLux < 500.0 {
        let t = (ambientLux - 50.0) / 450.0
        return CGFloat(0.60 + 0.40 * t)
    }

    return 1.0
}
```

Additional compensation: dark-colored menus (low reflectance detected via camera) get up to 40% brightness boost.

```swift
func reflectanceCompensation(menuLuminance: Float, screenBrightness: CGFloat) -> CGFloat {
    let expectedWhiteLuminance = Float(screenBrightness) * 0.8
    let estimatedReflectance = min(1.0, menuLuminance / max(expectedWhiteLuminance, 0.01))

    let compensation: Float
    if estimatedReflectance < 0.3 {
        compensation = 1.0 + 0.4 * (1.0 - estimatedReflectance / 0.3)
    } else if estimatedReflectance > 0.7 {
        compensation = 1.0 - 0.2 * (estimatedReflectance - 0.7) / 0.3
    } else {
        compensation = 1.0
    }

    return CGFloat(min(1.0, Float(screenBrightness) * compensation))
}
```

### 3b. Ambient Light to Color Selection (Kruithof Curve)

The Kruithof curve maps which color temperatures feel "pleasant" at different illuminance levels. Warm light in dark environments, cool light in bright ones.

| Ambient Lux | Target CCT | Palette Match (Low Light) |
|---|---|---|
| < 2 | ~1000K | Deep red (preserves night vision) |
| 2-5 | ~1400K | Warm red |
| 5-15 | ~2100K | Amber |
| 15-40 | ~2800K | Warm white (candlelight) |
| 40-100 | ~3500K | Soft white |
| 100+ | ~6500K | White |

The existing Low Light palette is almost perfectly designed for this — its colors form a monotonic CCT progression that maps cleanly to the Kruithof curve.

### Color Temperature Computation (McCamy's Formula)

For each color in the active palette, compute its Correlated Color Temperature:

```swift
struct ColorTemperatureMapper {
    static func correlatedColorTemperature(r: CGFloat, g: CGFloat, b: CGFloat) -> Float {
        // sRGB to XYZ
        let X = 0.4124564 * r + 0.3575761 * g + 0.1804375 * b
        let Y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
        let Z = 0.0193339 * r + 0.1191920 * g + 0.9503041 * b

        let sum = X + Y + Z
        guard sum > 0 else { return 6500 }

        let x = X / sum
        let y = Y / sum

        // McCamy's approximation
        let n = (x - 0.3320) / (0.1858 - y)
        let cct = 449.0 * pow(n, 3) + 3525.0 * pow(n, 2) + 6823.3 * n + 5520.33

        return Float(cct)
    }

    static func optimalPaletteIndex(
        ambientLux: Float,
        palette: [CodableColor]
    ) -> Int {
        let targetCCT: Float
        if ambientLux < 5 {
            targetCCT = 1800
        } else if ambientLux < 20 {
            let t = (ambientLux - 5) / 15
            targetCCT = 1800 + t * 1200
        } else if ambientLux < 100 {
            let t = (ambientLux - 20) / 80
            targetCCT = 3000 + t * 2000
        } else {
            targetCCT = 5000 + min(ambientLux / 500, 1.0) * 1500
        }

        var bestIndex = 0
        var bestDistance: Float = .infinity

        for (index, color) in palette.enumerated() {
            let cct = correlatedColorTemperature(r: color.red, g: color.green, b: color.blue)
            let distance = abs(cct - targetCCT)
            if distance < bestDistance {
                bestDistance = distance
                bestIndex = index
            }
        }

        return bestIndex
    }
}
```

### 3c. Feedback Loop Mitigation

**The core challenge**: The screen illuminates the menu, the camera reads the menu, the system changes the screen, which changes what the camera sees — potential oscillation.

**Solution: Screen-color subtraction.** Since we know exactly what color and brightness the screen is emitting, we analytically estimate and subtract the screen's reflected contribution from the camera reading before computing ambient light level.

```swift
func subtractScreenContribution(
    observed: (r: Float, g: Float, b: Float),
    screenColor: CodableColor,
    screenBrightness: CGFloat
) -> (r: Float, g: Float, b: Float) {
    let weight: Float = 0.3 * Float(screenBrightness)  // empirical coupling factor
    return (
        r: max(0, observed.r - Float(screenColor.red) * weight),
        g: max(0, observed.g - Float(screenColor.green) * weight),
        b: max(0, observed.b - Float(screenColor.blue) * weight)
    )
}
```

### 3d. Four-Layer Stability Stack

Prevents jitter and oscillation in the output recommendations:

```
Raw EXIF BrightnessValue
    │
    ▼
[1. Outlier Rejection] — Discard if delta from rolling median > 3 EV
    │
    ▼
[2. Asymmetric EMA] — Fast attack (α=0.20), slow release (α=0.08)
    │                   Brightening is quick; dimming is gentle
    │                   (respects dark adaptation physiology)
    ▼
[3. Hysteresis Band] — Don't change brightness until delta > 5%
    │                   Don't change color until CCT shift > 200K
    │                   (prevents micro-oscillation at regime boundaries)
    ▼
[4. Rate Limiter] — Max brightness change: 0.3/sec
    │                Max color change: 1 index per 4 seconds
    │                (prevents jarring transitions)
    ▼
Final recommendedIndex + recommendedBrightness
```

```swift
struct AsymmetricEMA {
    private var smoothedValue: Float = 0.5
    private let attackFactor: Float = 0.20
    private let releaseFactor: Float = 0.08

    mutating func update(with newValue: Float) -> Float {
        let factor = newValue > smoothedValue ? attackFactor : releaseFactor
        smoothedValue += factor * (newValue - smoothedValue)
        return smoothedValue
    }
}

struct HysteresisFilter {
    private var currentOutput: Float = 0.5

    private func threshold(atBrightness brightness: Float) -> Float {
        brightness < 0.15 ? 0.04 : 0.02
    }

    mutating func filter(target: Float) -> Float {
        if abs(target - currentOutput) > threshold(atBrightness: currentOutput) {
            currentOutput = target
        }
        return currentOutput
    }
}

struct RateLimiter {
    private var lastValue: Float = 0.5
    private var lastUpdateTime: CFTimeInterval = 0
    private let maxRatePerSecond: Float = 0.3

    mutating func limit(target: Float, currentTime: CFTimeInterval) -> Float {
        let dt = Float(currentTime - lastUpdateTime)
        guard dt > 0 else { return lastValue }

        let maxDelta = maxRatePerSecond * dt
        let delta = max(-maxDelta, min(maxDelta, target - lastValue))

        lastValue += delta
        lastUpdateTime = currentTime
        return lastValue
    }
}
```

---

## 4. Mode System & State Management

### Mutual Exclusivity

```
Manual Mode ←──→ Tilt Mode ←──→ Auto Mode
(gestures)      (CoreMotion)    (camera)
```

All three are mutually exclusive. Enabling one disables the others. Enforced in AppSettings:

- Enabling Auto → sets `isAngleBasedBrightnessActive = false`
- Enabling Tilt → sets `isAutoModeEnabled = false`

### Manual Override Flow

When auto mode is active and the user taps a color on the floating bar:

1. Auto **color** adaptation pauses (30-second timeout)
2. Tapped color displays immediately
3. Auto token on bar deselects; tapped token selects
4. Auto **brightness** adaptation continues
5. After 30s of no manual input, OR user taps Auto token → auto color resumes

```swift
enum ColorOverrideState {
    case autoControlled
    case manualOverride(color: Color, since: Date)
}
```

### New AppSettings Properties

```swift
@Published var isAutoModeEnabled: Bool {      // Persisted master toggle
    didSet { saveSettings() }
}
@Published var autoModeSensitivity: Double {  // 0.0 (sluggish) to 1.0 (responsive)
    didSet { saveSettings() }                 // Maps to hysteresis band width
}
```

---

## 5. UX Design

### FloatingColorBarView with Auto Token

```
  [✨ Auto] [ 🔴 ] [ 🟠 ] [ 🟡 ] [ ⚪ ] [ ⬜ ] [ ⚪ ] | [⚙️]
   ^^^^^^^^
   New token (wand.and.stars symbol, shows current auto-recommended color)
   Only appears when auto mode is enabled
```

### Visual Indicators

1. **Auto token** on the color bar — primary indicator, with subtle pulse animation
2. **"AUTO" label** on brightness indicator — secondary, small caption below percentage
3. **Settings toggle state** — tertiary

### Permission Flow

1. User enables "Auto Adjust" in Settings → camera permission requested at that moment (not at launch)
2. If granted → auto mode starts, auto token appears on bar
3. If denied → inline warning with "Tap to open Settings" deep-link, toggle resets to off

```swift
func requestCameraAccess() async -> Bool {
    let status = AVCaptureDevice.authorizationStatus(for: .video)
    switch status {
    case .authorized:
        return true
    case .notDetermined:
        return await AVCaptureDevice.requestAccess(for: .video)
    case .denied, .restricted:
        return false
    @unknown default:
        return false
    }
}
```

**Info.plist text**: "TableTorch uses the front camera to detect ambient lighting and automatically adjust screen color and brightness for optimal reading. No images are captured, stored, or transmitted."

### The Green Dot

The iOS camera indicator dot is unavoidable. Mitigation:

- First-time inline info card: "You may see a green indicator — this is normal. No photos are taken."
- The auto token on the color bar contextualizes the dot

### Settings Section

```swift
private var autoModeSection: some View {
    VStack(alignment: .leading, spacing: 16) {
        Text("Auto Mode")
            .font(.headline)
            .foregroundColor(.orange)

        EmberToggleView(
            title: "Auto Adjust",
            isOn: $settings.isAutoModeEnabled,
            subtitle: "Camera detects lighting and adjusts automatically"
        )

        if settings.isAutoModeEnabled {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("Responsiveness")
                        .foregroundColor(.white)
                    Spacer()
                    Text(sensitivityLabel)
                        .foregroundColor(.white.opacity(0.7))
                }
                Slider(value: $settings.autoModeSensitivity, in: 0...1, step: 0.1)
                    .tint(.orange)
            }

            cameraPermissionRow
        }
    }
    .glassCard(tintColor: .orange)
    .animation(AnimationConstants.smoothTransition, value: settings.isAutoModeEnabled)
}
```

### First Activation

When auto mode starts for the first time, a 1.5-second ease-in transition smoothly shifts from the current manual color to the auto-recommended color.

---

## 6. Power & Performance

| Strategy | Impact |
|---|---|
| 352x288 resolution | Minimal memory bandwidth |
| Adaptive frame rate (4→2→0.5 fps) | Major battery savings in steady state |
| EXIF metadata primary (no heavy pixel processing) | <0.5ms per frame |
| Duty cycling when stable (via luminance variance) | 30-40mW in steady state |
| Camera fully stopped on background/suspend | Zero draw when inactive |
| Thermal throttling: `.serious` → 1fps, `.critical` → stop | Prevents thermal shutdown |

**Estimated battery impact**: ~10-15% additional power draw over base app, dropping to ~5% in stable environments with duty cycling.

### Thermal Throttling

```swift
private func handleThermalChange() {
    switch ProcessInfo.processInfo.thermalState {
    case .nominal, .fair:
        // Normal operation
        break
    case .serious:
        setFrameRate(1)
    case .critical:
        stopCapture()
        delegate?.cameraAnalyzerDidThrottle(self)
    @unknown default:
        break
    }
}
```

### Adaptive Duty Cycling

```swift
private func adjustFrameRate(forStability stability: Float) {
    let targetFPS: Int32
    switch stability {
    case 0.9...1.0: targetFPS = 1   // Very stable: 1 FPS
    case 0.7..<0.9: targetFPS = 2   // Somewhat stable: 2 FPS
    default:        targetFPS = 4   // Changing: 4 FPS
    }
    // Apply to device...
}
```

---

## 7. iOS Version Strategy

| Feature | iOS 18 | iOS 26 |
|---|---|---|
| AVCaptureSession, EXIF metadata | Yes | Yes |
| CIAreaAverage, CIContext | Yes | Yes |
| Core capture pipeline | Full support | Full support |
| VNGenerateAttentionBasedSaliencyImageRequest (smart document ROI) | Available | Enhanced |

Build targeting iOS 18. Gate saliency-based document detection behind `#available(iOS 26, *)` as an enhancement (replaces the hardcoded center-60% crop with Vision-detected document region).

```swift
if #available(iOS 26, *) {
    // Use VNGenerateAttentionBasedSaliencyImageRequest for smart document ROI
} else {
    // Fallback to center-60% crop
}
```

---

## 8. Testing Strategy

| Layer | Approach |
|---|---|
| CameraAnalyzer pixel analysis | Pure function with synthetic `CVPixelBuffer` inputs |
| AutoModeManager decisions | `LightingAnalysisProvider` protocol → mock emits controlled sequences |
| Stability pipeline | Unit tests: verify EMA, hysteresis, rate limiter with deterministic inputs |
| UI integration | Launch argument `-mockAutoMode` provides mock cycling through dark→medium→bright |
| Manual testing | Test matrix: bright room, dim room, camera covered, colored ambient light, thermal stress |
| Performance | `os_signpost` instrumentation, Instruments Energy Log profiling |

### Protocol for Testability

```swift
protocol LightingAnalysisProvider {
    var analysisPublisher: AnyPublisher<LightingAnalysis, Never> { get }
}

class MockAnalysisProvider: LightingAnalysisProvider {
    let subject = PassthroughSubject<LightingAnalysis, Never>()
    var analysisPublisher: AnyPublisher<LightingAnalysis, Never> {
        subject.eraseToAnyPublisher()
    }
}
```

### Manual Test Matrix

| Scenario | Expected Result |
|---|---|
| Enable auto mode in bright room | Screen dims to moderate brightness, neutral color |
| Enable auto mode, move to dim room | Screen brightens, shifts to warmer color |
| Cover front camera while auto mode active | Holds last good reading, does not flicker |
| Auto mode on, tap manual color | Color switches immediately, auto token deselects |
| Auto mode on, tap auto token after manual override | Returns to auto color |
| Background app with auto mode on | Camera stops, no power drain |
| Return to app with auto mode on | Camera resumes within 0.5s, recommendations resume |
| Enable auto mode, then enable tilt brightness | Auto mode disables, tilt takes over |
| Thermal throttle to critical | Auto mode pauses gracefully, holds last state |
| Deny camera permission, then enable auto mode | Warning row appears, toggle resets to off |

---

## 9. Future Enhancements (v2+)

These were discussed and deliberately deferred from v1:

- **OKLCH color interpolation**: Generate smooth intermediate colors between palette entries using perceptually uniform color space (full sRGB-to-OKLCH conversion math is available)
- **Brightness modulation probing**: Active 3% brightness micro-dips to measure differential camera response for higher-accuracy ambient estimation in mesopic range
- **Continuous color mode**: User opts into fully generated colors rather than palette-based selection
- **First-run calibration**: Baseline EXIF range recording for the user's typical environment
- **VoiceOver announcements**: `UIAccessibility.post(notification:)` for significant auto transitions
- **Display P3 color space**: Ensure color science accounts for P3 gamut on modern iPhones
- **Ambient color cast compensation**: Partially inverse-correct for colored ambient light (neon signs, colored LEDs)

### OKLCH Color Space Reference (for v2)

Full sRGB to OKLCH conversion for perceptually uniform interpolation between palette colors:

```swift
struct OKLCHColor {
    var L: Float  // lightness 0-1
    var C: Float  // chroma 0-~0.4
    var H: Float  // hue in degrees 0-360
    var alpha: Float

    init(sRGB r: Float, _ g: Float, _ b: Float, alpha: Float = 1.0) {
        // sRGB to linear RGB (remove gamma)
        let lr = r <= 0.04045 ? r / 12.92 : pow((r + 0.055) / 1.055, 2.4)
        let lg = g <= 0.04045 ? g / 12.92 : pow((g + 0.055) / 1.055, 2.4)
        let lb = b <= 0.04045 ? b / 12.92 : pow((b + 0.055) / 1.055, 2.4)

        // Linear RGB to LMS
        let l = 0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb
        let m = 0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb
        let s = 0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb

        // Cube root
        let l_ = cbrt(l)
        let m_ = cbrt(m)
        let s_ = cbrt(s)

        // LMS to Oklab
        let okL = 0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_
        let okA = 1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_
        let okB = 0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_

        // Oklab to OKLCH
        self.L = okL
        self.C = sqrt(okA * okA + okB * okB)
        self.H = atan2(okB, okA) * 180.0 / .pi
        if self.H < 0 { self.H += 360 }
        self.alpha = alpha
    }

    func toCodableColor() -> CodableColor {
        let hRad = H * .pi / 180.0
        let okA = C * cos(hRad)
        let okB = C * sin(hRad)

        let l_ = L + 0.3963377774 * okA + 0.2158037573 * okB
        let m_ = L - 0.1055613458 * okA - 0.0638541728 * okB
        let s_ = L - 0.0894841775 * okA - 1.2914855480 * okB

        let l = l_ * l_ * l_
        let m = m_ * m_ * m_
        let s = s_ * s_ * s_

        var lr =  4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
        var lg = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
        var lb = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s

        lr = max(0, min(1, lr))
        lg = max(0, min(1, lg))
        lb = max(0, min(1, lb))

        let r = lr <= 0.0031308 ? lr * 12.92 : 1.055 * pow(lr, 1.0/2.4) - 0.055
        let g = lg <= 0.0031308 ? lg * 12.92 : 1.055 * pow(lg, 1.0/2.4) - 0.055
        let b = lb <= 0.0031308 ? lb * 12.92 : 1.055 * pow(lb, 1.0/2.4) - 0.055

        return CodableColor(
            red: CGFloat(max(0, min(1, r))),
            green: CGFloat(max(0, min(1, g))),
            blue: CGFloat(max(0, min(1, b))),
            alpha: CGFloat(alpha)
        )
    }

    static func interpolate(from a: OKLCHColor, to b: OKLCHColor, t: Float) -> OKLCHColor {
        let L = a.L + (b.L - a.L) * t
        let C = a.C + (b.C - a.C) * t

        var deltaH = b.H - a.H
        if deltaH > 180 { deltaH -= 360 }
        if deltaH < -180 { deltaH += 360 }
        var H = a.H + deltaH * t
        if H < 0 { H += 360 }
        if H >= 360 { H -= 360 }

        let alpha = a.alpha + (b.alpha - a.alpha) * t
        return OKLCHColor(L: L, C: C, H: H, alpha: alpha)
    }
}
```

---

## 10. Key Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Feedback loop oscillation | Screen-color subtraction + 4-layer stability stack |
| Green dot confuses/alarms users | Contextual messaging, auto token visual, Info.plist description |
| Battery drain from camera | Aggressive duty cycling, thermal monitoring, low resolution |
| Camera noise in very dark venues | EXIF-primary (not pixel-dependent), wider hysteresis in scotopic |
| User confusion about auto vs manual | Clear mutual exclusivity, obvious auto token, 30s override timeout |
| Glossy/reflective menus | Specular highlight detection via histogram analysis, trimmed means |
| Camera occlusion (finger over lens) | Luminance + variance detection, freeze last good reading |
| App Store privacy concerns | No images stored/transmitted, on-device only, clear privacy label |

---

## 11. Privacy & App Store Review

- **Camera usage justification**: Ambient light and color analysis only. No frames stored, no images leave the device, no face detection or biometric analysis.
- **NSCameraUsageDescription**: Clear, honest, specific about purpose.
- **No background camera usage**: Session suspended on `.background` scene phase, resumed on `.active`. No background camera entitlement needed.
- **No recording**: Never uses `AVCaptureMovieFileOutput` or `AVCapturePhotoOutput`. Only `AVCaptureVideoDataOutput` for real-time processing.
- **App Store privacy nutrition label**: Camera data falls under "Data Not Collected."
