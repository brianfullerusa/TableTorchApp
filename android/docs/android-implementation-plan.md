# TableTorch Android Implementation Plan

## App Overview

**TableTorch** is a screen-based torch/flashlight app that turns the device display into a customizable light source. Key features:
- 6 customizable flame colors
- Brightness control via slider
- Tilt-based automatic brightness (flat = bright, vertical = dim)
- Screen lock prevention
- Persistent settings

---

## Phase 1: Project Setup & Core Architecture

### 1.1 Package Structure
```
com.rockyriverapps.tabletorch/
├── MainActivity.kt
├── ui/
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── screens/
│   │   ├── SplashScreen.kt
│   │   ├── MainScreen.kt
│   │   └── SettingsScreen.kt
│   └── components/
│       ├── ColorButton.kt
│       ├── BrightnessSlider.kt
│       └── FlameColorPicker.kt
├── data/
│   ├── AppSettings.kt
│   └── PreferencesManager.kt
├── sensors/
│   ├── BrightnessManager.kt
│   └── TiltSensorManager.kt
└── navigation/
    └── NavGraph.kt
```

### 1.2 Dependencies (build.gradle.kts)
```kotlin
dependencies {
    // Compose
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.+")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.+")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.+")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.0.+")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.+")
}
```

### 1.3 Permissions (AndroidManifest.xml)
```xml
<uses-permission android:name="android.permission.WRITE_SETTINGS" />
<uses-feature android:name="android.hardware.sensor.accelerometer" android:required="false" />

<application
    android:theme="@style/Theme.TableTorch"
    ...>
    <activity
        android:name=".MainActivity"
        android:screenOrientation="portrait"
        android:exported="true">
```

**Note:** `WRITE_SETTINGS` requires special permission handling - must redirect user to system settings to grant.

---

## Phase 2: Data Layer

### 2.1 AppSettings Data Class
```kotlin
data class AppSettings(
    val defaultBrightness: Float = 0.85f,
    val useDefaultBrightnessOnLaunch: Boolean = true,
    val selectedColors: List<Int> = defaultColors,
    val isAngleBasedBrightnessActive: Boolean = false,
    val lastSelectedColorIndex: Int = 1,
    val preventScreenLock: Boolean = true
) {
    companion object {
        val defaultColors = listOf(
            0xFFFFFFFF.toInt(),  // White
            0xFFFFC896.toInt(),  // Soft White (default)
            0xFF98FF98.toInt(),  // Mint Green
            0xFF4682B4.toInt(),  // Steel Blue
            0xFFFF0000.toInt(),  // Red
            0xFF800000.toInt()   // Dark Red
        )
    }
}
```

### 2.2 PreferencesManager (DataStore)
```kotlin
class PreferencesManager(private val context: Context) {
    private val dataStore = context.dataStore

    val settingsFlow: Flow<AppSettings>

    suspend fun updateSettings(update: (AppSettings) -> AppSettings)
    suspend fun updateColor(index: Int, color: Int)
    suspend fun restoreDefaultColors()
}
```

**Key difference from iOS:** Use Kotlin Flow instead of @Published for reactive updates.

---

## Phase 3: Sensor Layer

### 3.1 BrightnessManager
```kotlin
class BrightnessManager(private val context: Context) {
    private var originalBrightness: Float = -1f
    var currentBrightness: MutableStateFlow<Float>

    fun beginManaging()      // Store system brightness
    fun endManaging()        // Restore original brightness
    fun setBrightness(value: Float)
    fun updateForTiltAngle(angleRadians: Double)
}
```

**Android-specific:**
```kotlin
// Set screen brightness
val window = (context as Activity).window
val params = window.attributes
params.screenBrightness = brightnessValue  // 0.0f to 1.0f
window.attributes = params

// For system-wide brightness (requires WRITE_SETTINGS):
Settings.System.putInt(
    contentResolver,
    Settings.System.SCREEN_BRIGHTNESS,
    (brightnessValue * 255).toInt()
)
```

### 3.2 TiltSensorManager
```kotlin
class TiltSensorManager(context: Context) : SensorEventListener {
    private val sensorManager: SensorManager
    private val accelerometer: Sensor?

    val tiltAngle: StateFlow<Double>  // 0 = flat, π/2 = vertical

    fun startListening()
    fun stopListening()

    override fun onSensorChanged(event: SensorEvent) {
        // Calculate tilt from gravity vector
        val z = event.values[2]
        val angle = acos(z / 9.81).coerceIn(0.0, PI / 2)
        _tiltAngle.value = angle
    }
}
```

**Sensor config:**
- Use `Sensor.TYPE_ACCELEROMETER` or `TYPE_GRAVITY`
- Sample rate: `SensorManager.SENSOR_DELAY_GAME` (~60Hz)

---

## Phase 4: UI Layer

### 4.1 Theme (Match iOS Dark Theme)
```kotlin
// Color.kt
val TorchOrange = Color(0xFFFFA500)      // Title bar accent
val TorchBackground = Color(0xFF000000)  // Black background
val TorchSurface = Color(0xFF1C1C1E)     // Dark gray for cards

// Theme.kt
private val DarkColorScheme = darkColorScheme(
    primary = TorchOrange,
    background = TorchBackground,
    surface = TorchSurface
)
```

### 4.2 SplashScreen
```kotlin
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1400)  // 1.4 seconds like iOS
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.flame_primary),
                modifier = Modifier.size(160.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "TABLE TORCH",
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
        }
    }
}
```

### 4.3 MainScreen Layout
```kotlin
@Composable
fun MainScreen(
    settings: AppSettings,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    onColorSelect: (Int) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = CenterVertically) {
                        Image(painterResource(R.drawable.flame_primary), ...)
                        Text("Table Torch", fontFamily = copperplateFamily)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
        ) {
            // Color display area (92% height)
            Box(
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(settings.selectedColors[settings.lastSelectedColorIndex]))
                    .shadow(18.dp, RoundedCornerShape(24.dp))
            )

            // Brightness slider
            BrightnessSlider(
                value = brightness,
                onValueChange = onBrightnessChange,
                enabled = !settings.isAngleBasedBrightnessActive
            )

            // Color buttons + Settings
            Row {
                ColorButtonsRow(
                    colors = settings.selectedColors,
                    selectedIndex = settings.lastSelectedColorIndex,
                    onSelect = onColorSelect
                )
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, "Settings")
                }
            }
        }
    }
}
```

### 4.4 ColorButton Component
```kotlin
@Composable
fun ColorButton(
    color: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Image(
            painter = painterResource(
                if (isSelected) R.drawable.flame_filled
                else R.drawable.flame_outline
            ),
            colorFilter = ColorFilter.tint(Color(color)),
            alpha = if (isSelected) 1f else 0.5f
        )
    }
}
```

### 4.5 SettingsScreen
```kotlin
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onColorChange: (Int, Int) -> Unit,
    onRestoreDefaults: () -> Unit
) {
    LazyColumn {
        // Section: Torch Colors (2-column grid)
        item {
            SectionHeader("Torch Colors")
            LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                itemsIndexed(settings.selectedColors) { index, color ->
                    FlameColorPicker(
                        label = "Torch ${index + 1}",
                        color = color,
                        onColorChange = { onColorChange(index, it) }
                    )
                }
            }
            TextButton(onClick = onRestoreDefaults) {
                Text("Restore Default Colors")
            }
        }

        // Section: Brightness Settings
        item {
            SectionHeader("Brightness Settings")
            SliderRow(
                label = "Default Brightness",
                value = settings.defaultBrightness,
                onValueChange = { /* update */ }
            )
            SwitchRow(
                label = "Use Default Brightness on Launch",
                checked = settings.useDefaultBrightnessOnLaunch,
                onCheckedChange = { /* update */ }
            )
            SwitchRow(
                label = "Prevent Screen Lock",
                checked = settings.preventScreenLock,
                onCheckedChange = { /* update */ }
            )
        }

        // Section: Tilt Control
        item {
            SectionHeader("Tilt Brightness Control")
            SwitchRow(
                label = "Enable Tilt Brightness Control",
                checked = settings.isAngleBasedBrightnessActive,
                onCheckedChange = { /* update */ }
            )
            Text(
                "Tilt phone: Vertical=30% brightness, Flat=100% brightness",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
```

---

## Phase 5: Navigation

### 5.1 NavGraph
```kotlin
@Composable
fun TableTorchNavGraph() {
    val navController = rememberNavController()
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onTimeout = { showSplash = false })
    } else {
        NavHost(navController, startDestination = "main") {
            composable("main") {
                MainScreen(
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
```

---

## Phase 6: MainActivity Integration

```kotlin
class MainActivity : ComponentActivity() {
    private lateinit var brightnessManager: BrightnessManager
    private lateinit var tiltManager: TiltSensorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        brightnessManager = BrightnessManager(this)
        tiltManager = TiltSensorManager(this)

        setContent {
            TableTorchTheme {
                val settings by preferencesManager.settingsFlow.collectAsState()
                val brightness by brightnessManager.currentBrightness.collectAsState()
                val tiltAngle by tiltManager.tiltAngle.collectAsState()

                // Update brightness from tilt if enabled
                LaunchedEffect(tiltAngle, settings.isAngleBasedBrightnessActive) {
                    if (settings.isAngleBasedBrightnessActive) {
                        brightnessManager.updateForTiltAngle(tiltAngle)
                    }
                }

                TableTorchNavGraph(...)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        brightnessManager.beginManaging()
        tiltManager.startListening()

        if (settings.preventScreenLock) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onPause() {
        super.onPause()
        brightnessManager.endManaging()
        tiltManager.stopListening()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
```

---

## Phase 7: Assets Required

### 7.1 Drawable Resources
Copy/convert from iOS `Assets.xcassets`:
- `flame_primary.png` (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
- `flame_secondary.png` (same sizes)
- `flame_filled.xml` (vector for selected state)
- `flame_outline.xml` (vector for unselected state)

### 7.2 App Icon
- `ic_launcher.png` - Standard launcher icons (all densities)
- `ic_launcher_round.png` - Round launcher icons
- Use existing TableTorch icon from iOS

---

## Implementation Order

### Week 1: Foundation
1. [ ] Set up package structure
2. [ ] Implement PreferencesManager with DataStore
3. [ ] Create AppSettings data class
4. [ ] Set up dark theme

### Week 2: Core Managers
5. [ ] Implement BrightnessManager
6. [ ] Implement TiltSensorManager
7. [ ] Handle WRITE_SETTINGS permission flow

### Week 3: UI - Main Screen
8. [ ] Create SplashScreen
9. [ ] Build MainScreen layout
10. [ ] Implement ColorButton component
11. [ ] Implement BrightnessSlider

### Week 4: UI - Settings & Polish
12. [ ] Build SettingsScreen
13. [ ] Implement FlameColorPicker (color dialog)
14. [ ] Set up navigation
15. [ ] Wire up lifecycle handling

### Week 5: Testing & Refinement
16. [ ] Test on physical devices (tilt sensor)
17. [ ] Verify brightness control works
18. [ ] Test settings persistence
19. [ ] Polish animations and transitions

---

## Key Differences from iOS

| Feature | iOS | Android |
|---------|-----|---------|
| Brightness control | `UIScreen.main.brightness` | `window.attributes.screenBrightness` |
| Keep screen on | `isIdleTimerDisabled` | `FLAG_KEEP_SCREEN_ON` |
| Motion sensor | `CMMotionManager` | `SensorManager` + `TYPE_ACCELEROMETER` |
| Settings storage | `UserDefaults` | `DataStore` |
| Reactive state | `@Published` + Combine | `StateFlow` + Coroutines |
| Color picker | Native `ColorPicker` | Custom dialog or third-party lib |

---

## Potential Challenges

1. **WRITE_SETTINGS permission** - Requires user to manually grant in system settings. Need graceful fallback if denied.

2. **Color Picker** - No native Compose color picker. Options:
   - Use third-party library (e.g., `compose-color-picker`)
   - Build custom HSV picker
   - Use simple color palette dialog

3. **Font matching** - iOS uses Copperplate. Android alternative:
   - Include Copperplate as custom font, or
   - Use similar serif font (Playfair Display, Cinzel)

4. **Sensor accuracy** - Accelerometer behavior varies by device. Test on multiple phones.

---

## Testing Checklist

- [ ] Splash screen displays for 1.4s then fades
- [ ] Color buttons change display color
- [ ] Brightness slider adjusts screen brightness
- [ ] Tilt control adjusts brightness (30% vertical, 100% flat)
- [ ] Settings persist across app restarts
- [ ] Screen stays on when prevent lock is enabled
- [ ] Custom colors save correctly
- [ ] Restore defaults works
- [ ] App restores last selected color on launch
- [ ] Brightness restores to system value on app exit
