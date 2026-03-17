//
//  AppSettings.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//

import SwiftUI

@MainActor
final class AppSettings: ObservableObject {
    nonisolated static let defaultColors: [Color] = [
        AppSettings.color(red: 255, green: 255, blue: 255),   // white
        AppSettings.color(red: 255, green: 200, blue: 150),   // soft white
        AppSettings.color(red: 152, green: 255, blue: 152),   // mint green
        AppSettings.color(red: 70, green: 130, blue: 180),    // steel blue
        AppSettings.color(red: 255, green: 0, blue: 0),       // red
        AppSettings.color(red: 128, green: 0, blue: 0)        // dark red
    ]

    nonisolated static let lowLightColors: [Color] = [
        AppSettings.color(red: 180, green: 0, blue: 0),       // deep red — night vision
        AppSettings.color(red: 200, green: 50, blue: 0),      // warm red — dark reading
        AppSettings.color(red: 255, green: 147, blue: 41),    // amber — warm night
        AppSettings.color(red: 255, green: 180, blue: 107),   // warm white — candlelight
        AppSettings.color(red: 255, green: 200, blue: 150),   // soft white — reading light
        AppSettings.color(red: 255, green: 255, blue: 255)    // white — bright reading
    ]

    nonisolated static let partyColors: [Color] = [
        AppSettings.color(red: 255, green: 0,   blue: 255),   // hot magenta
        AppSettings.color(red: 255, green: 25,  blue: 200),   // neon pink
        AppSettings.color(red: 191, green: 0,   blue: 255),   // electric purple
        AppSettings.color(red: 127, green: 0,   blue: 255),   // deep violet
        AppSettings.color(red: 0,   green: 100, blue: 255),   // electric blue
        AppSettings.color(red: 186, green: 85,  blue: 211)    // deep orchid
    ]

    private var pendingSaveTask: Task<Void, Never>?
    private let saveDebounceDelay: UInt64 = 200_000_000  // 0.2 seconds
    private var isInitializing = true
    private var dirtyColors = false
    private var dirtyPalettes = false

    @Published var defaultBrightness: CGFloat {
        didSet { saveSettings() }
    }
    @Published var useDefaultBrightnessOnAppear: Bool {
        didSet { saveSettings() }
    }
    @Published var selectedColors: [Color] {
        didSet { dirtyColors = true; saveSettings() }
    }
    @Published var isAngleBasedBrightnessActive: Bool {
        didSet { saveSettings() }
    }
    // Instead of storing the last selected Color, store the index
    @Published var lastSelectedColorIndex: Int {
        didSet { saveSettings() }
    }
    // allow user to turn on prevent screen lock for the app
    @Published var preventScreenLock: Bool {
        didSet { saveSettings() }
    }

    // New properties for redesign
    @Published var hasCompletedOnboarding: Bool {
        didSet { saveSettings() }
    }
    @Published var enableBreathingAnimation: Bool {
        didSet { saveSettings() }
    }
    @Published var breathingDepth: Double {
        didSet { saveSettings() }
    }
    @Published var breathingCycleDuration: Double {
        didSet { saveSettings() }
    }
    @Published var enableEmberParticles: Bool {
        didSet { saveSettings() }
    }
    @Published var showQuickColorBar: Bool {
        didSet { saveSettings() }
    }
    @Published var alwaysShowBrightnessIndicator: Bool {
        didSet { saveSettings() }
    }
    @Published var particleShape: ParticleShape {
        didSet { saveSettings() }
    }

    // Palette management
    @Published var customPalettes: [ColorPalette] = [] {
        didSet {
            dirtyPalettes = true
            saveSettings()
            updateAllPalettes()
        }
    }
    @Published var activePaletteId: UUID? {
        didSet { saveSettings() }
    }

    @Published private(set) var allPalettes: [ColorPalette] = ColorPalette.builtInPresets

    private func updateAllPalettes() {
        allPalettes = ColorPalette.builtInPresets + customPalettes
    }

    var activePalette: ColorPalette? {
        guard let id = activePaletteId else { return nil }
        return allPalettes.first { $0.id == id }
    }

    var hasUnsavedColorChanges: Bool {
        guard let palette = activePalette else { return true }
        return !palette.matches(colors: selectedColors)
    }

    func savePalette(name: String) {
        let palette = ColorPalette(
            id: UUID(),
            name: name,
            colors: selectedColors.map { CodableColor(color: $0) },
            isBuiltIn: false
        )
        customPalettes.append(palette)
        activePaletteId = palette.id
    }

    func updateActivePalette() {
        guard let id = activePaletteId,
              let index = customPalettes.firstIndex(where: { $0.id == id }) else { return }
        customPalettes[index].colors = selectedColors.map { CodableColor(color: $0) }
    }

    func loadPalette(_ palette: ColorPalette) {
        withAnimation(AnimationConstants.smoothTransition) {
            selectedColors = palette.swiftUIColors
        }
        activePaletteId = palette.id
        HapticEngine.shared.colorChanged()
    }

    /// Load a palette without haptic feedback or animation (used in screenshot mode).
    func loadPaletteWithoutHaptic(_ palette: ColorPalette) {
        selectedColors = palette.swiftUIColors
        activePaletteId = palette.id
    }

    func deletePalette(id: UUID) {
        customPalettes.removeAll { $0.id == id }
        if activePaletteId == id {
            activePaletteId = nil
        }
    }

    func renamePalette(id: UUID, newName: String) {
        guard let index = customPalettes.firstIndex(where: { $0.id == id }) else { return }
        customPalettes[index].name = newName
    }

    func duplicatePalette(id: UUID) {
        guard let palette = customPalettes.first(where: { $0.id == id }) else { return }
        let copy = ColorPalette(
            id: UUID(),
            name: "\(palette.name) Copy",
            colors: palette.colors,
            isBuiltIn: false
        )
        customPalettes.append(copy)
    }

    func nextDefaultPaletteName() -> String {
        let baseName = "My Palette"
        let existingNames = Set(customPalettes.map(\.name))
        if !existingNames.contains(baseName) { return baseName }
        var counter = 2
        while existingNames.contains("\(baseName) \(counter)") {
            counter += 1
        }
        return "\(baseName) \(counter)"
    }

    /// Returns the palette matching current colors, if any
    func matchingPalette() -> ColorPalette? {
        allPalettes.first { $0.matches(colors: selectedColors) }
    }

    init() {
        let defaults = UserDefaults.standard

        // Register defaults
        defaults.register(defaults: [
            "defaultBrightness": 1.0,
            "useDefaultBrightnessOnAppear": true,
            "preventScreenLock": true,
            "isAngleBasedBrightnessActive": false,
            "hasCompletedOnboarding": false,
            "enableBreathingAnimation": false,
            "breathingDepth": 0.12,
            "breathingCycleDuration": 4.0,
            "enableEmberParticles": false,
            "showQuickColorBar": true,
            "alwaysShowBrightnessIndicator": true
        ])

        // Compute values locally first (avoid touching self before full init)
        let defaultBrightnessValue = CGFloat(defaults.double(forKey: "defaultBrightness"))
        let useDefaultBrightnessOnAppearValue = defaults.bool(forKey: "useDefaultBrightnessOnAppear")
        let preventScreenLockValue = defaults.bool(forKey: "preventScreenLock")

        let selectedColorsValue: [Color]
        if let data = defaults.data(forKey: "selectedColors"),
           let decoded = try? JSONDecoder().decode([CodableColor].self, from: data) {
            selectedColorsValue = decoded.map { $0.color }
        } else {
            selectedColorsValue = AppSettings.lowLightColors
        }

        let isAngleBasedBrightnessActiveValue = defaults.bool(forKey: "isAngleBasedBrightnessActive")

        let lastSelectedColorIndexValue: Int
        if defaults.object(forKey: "lastSelectedColorIndex") == nil {
            // First launch: default to the second preset when available
            lastSelectedColorIndexValue = selectedColorsValue.indices.contains(3) ? 3 : 0
        } else {
            let storedIndex = defaults.integer(forKey: "lastSelectedColorIndex")
            lastSelectedColorIndexValue = selectedColorsValue.indices.contains(storedIndex) ? storedIndex : 0
        }

        // New redesign settings
        let hasCompletedOnboardingValue = defaults.bool(forKey: "hasCompletedOnboarding")
        let enableBreathingAnimationValue = defaults.bool(forKey: "enableBreathingAnimation")
        let breathingDepthValue = defaults.double(forKey: "breathingDepth")
        let breathingCycleDurationValue = defaults.double(forKey: "breathingCycleDuration")
        let enableEmberParticlesValue = defaults.bool(forKey: "enableEmberParticles")
        let showQuickColorBarValue = defaults.bool(forKey: "showQuickColorBar")
        let alwaysShowBrightnessIndicatorValue = defaults.bool(forKey: "alwaysShowBrightnessIndicator")

        let particleShapeValue: ParticleShape
        if let raw = defaults.string(forKey: "particleShape"),
           let shape = ParticleShape(rawValue: raw) {
            particleShapeValue = shape
        } else {
            particleShapeValue = .embers
        }

        // Load custom palettes
        let customPalettesValue: [ColorPalette]
        if let data = defaults.data(forKey: "customPalettes"),
           let decoded = try? JSONDecoder().decode([ColorPalette].self, from: data) {
            customPalettesValue = decoded
        } else {
            customPalettesValue = []
        }

        // Load active palette ID
        let activePaletteIdValue: UUID?
        if let idString = defaults.string(forKey: "activePaletteId"),
           let uuid = UUID(uuidString: idString) {
            activePaletteIdValue = uuid
        } else {
            // Migration: detect which built-in preset matches current colors
            if ColorPalette.lowLight.matches(colors: selectedColorsValue) {
                activePaletteIdValue = ColorPalette.lowLight.id
            } else if ColorPalette.bright.matches(colors: selectedColorsValue) {
                activePaletteIdValue = ColorPalette.bright.id
            } else if ColorPalette.party.matches(colors: selectedColorsValue) {
                activePaletteIdValue = ColorPalette.party.id
            } else {
                activePaletteIdValue = nil
            }
        }

        // Now assign to stored properties (safe to use self)
        self.defaultBrightness = defaultBrightnessValue
        self.useDefaultBrightnessOnAppear = useDefaultBrightnessOnAppearValue
        self.selectedColors = selectedColorsValue
        self.isAngleBasedBrightnessActive = isAngleBasedBrightnessActiveValue
        self.lastSelectedColorIndex = lastSelectedColorIndexValue
        self.preventScreenLock = preventScreenLockValue
        self.hasCompletedOnboarding = hasCompletedOnboardingValue
        self.enableBreathingAnimation = enableBreathingAnimationValue
        self.breathingDepth = breathingDepthValue
        self.breathingCycleDuration = breathingCycleDurationValue
        self.enableEmberParticles = enableEmberParticlesValue
        self.showQuickColorBar = showQuickColorBarValue
        self.alwaysShowBrightnessIndicator = alwaysShowBrightnessIndicatorValue
        self.particleShape = particleShapeValue
        self.customPalettes = customPalettesValue
        self.activePaletteId = activePaletteIdValue

        // Screenshot mode: override settings from launch arguments.
        // Must happen here (not in App.init) because @StateObject creates
        // the instance lazily -- App.init would configure a throwaway copy.
        let args = ProcessInfo.processInfo.arguments
        if args.contains("-uiScreenshotMode") {
            self.alwaysShowBrightnessIndicator = true
            self.enableBreathingAnimation = false

            if let idx = args.firstIndex(of: "-uiPalette"), idx + 1 < args.count {
                switch args[idx + 1] {
                case "lowLight":
                    self.selectedColors = ColorPalette.lowLight.swiftUIColors
                    self.activePaletteId = ColorPalette.lowLight.id
                case "bright":
                    self.selectedColors = ColorPalette.bright.swiftUIColors
                    self.activePaletteId = ColorPalette.bright.id
                case "party":
                    self.selectedColors = ColorPalette.party.swiftUIColors
                    self.activePaletteId = ColorPalette.party.id
                default: break
                }
            }

            if let idx = args.firstIndex(of: "-uiColorIndex"), idx + 1 < args.count,
               let colorIndex = Int(args[idx + 1]),
               self.selectedColors.indices.contains(colorIndex) {
                self.lastSelectedColorIndex = colorIndex
            }

            if args.contains("-uiEmberParticles") {
                self.enableEmberParticles = true
            } else {
                self.enableEmberParticles = false
            }
        }

        self.isInitializing = false
        updateAllPalettes()
    }

    private func saveSettings() {
        guard !isInitializing else { return }
        scheduleSave()
    }

    func flushPendingSaves() {
        pendingSaveTask?.cancel()
        pendingSaveTask = nil
        dirtyColors = true
        dirtyPalettes = true
        persistSettings()
    }

    private func scheduleSave() {
        pendingSaveTask?.cancel()
        let delay = saveDebounceDelay
        pendingSaveTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: delay)
            guard let self else { return }
            self.persistSettings()
        }
    }

    private func persistSettings() {
        pendingSaveTask = nil

        let defaults = UserDefaults.standard
        defaults.set(Double(defaultBrightness), forKey: "defaultBrightness")
        defaults.set(useDefaultBrightnessOnAppear, forKey: "useDefaultBrightnessOnAppear")
        defaults.set(preventScreenLock, forKey: "preventScreenLock")

        // Only re-encode JSON arrays when they've actually changed
        if dirtyColors {
            let codableColors = selectedColors.map { CodableColor(color: $0) }
            if let encoded = try? JSONEncoder().encode(codableColors) {
                defaults.set(encoded, forKey: "selectedColors")
            }
            dirtyColors = false
        }

        defaults.set(isAngleBasedBrightnessActive, forKey: "isAngleBasedBrightnessActive")
        defaults.set(lastSelectedColorIndex, forKey: "lastSelectedColorIndex")
        defaults.set(hasCompletedOnboarding, forKey: "hasCompletedOnboarding")
        defaults.set(enableBreathingAnimation, forKey: "enableBreathingAnimation")
        defaults.set(breathingDepth, forKey: "breathingDepth")
        defaults.set(breathingCycleDuration, forKey: "breathingCycleDuration")
        defaults.set(enableEmberParticles, forKey: "enableEmberParticles")
        defaults.set(showQuickColorBar, forKey: "showQuickColorBar")
        defaults.set(alwaysShowBrightnessIndicator, forKey: "alwaysShowBrightnessIndicator")
        defaults.set(particleShape.rawValue, forKey: "particleShape")

        if dirtyPalettes {
            if let encoded = try? JSONEncoder().encode(customPalettes) {
                defaults.set(encoded, forKey: "customPalettes")
            }
            dirtyPalettes = false
        }

        if let id = activePaletteId {
            defaults.set(id.uuidString, forKey: "activePaletteId")
        } else {
            defaults.removeObject(forKey: "activePaletteId")
        }
    }

    private nonisolated static func color(red: Double, green: Double, blue: Double) -> Color {
        Color(.sRGB, red: red / 255, green: green / 255, blue: blue / 255, opacity: 1.0)
    }
}

// CodableColor struct
struct CodableColor: Codable, Equatable {
    let red: CGFloat
    let green: CGFloat
    let blue: CGFloat
    let alpha: CGFloat

    static func == (lhs: CodableColor, rhs: CodableColor) -> Bool {
        abs(lhs.red - rhs.red) < 0.01 &&
        abs(lhs.green - rhs.green) < 0.01 &&
        abs(lhs.blue - rhs.blue) < 0.01 &&
        abs(lhs.alpha - rhs.alpha) < 0.01
    }

    init(color: Color) {
        let uiColor = UIColor(color)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        uiColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        self.red = r
        self.green = g
        self.blue = b
        self.alpha = a
    }

    var color: Color {
        Color(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }
}

