//
//  AppSettings.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//

import SwiftUI

@MainActor
final class AppSettings: ObservableObject {
    static let defaultColors: [Color] = [
        AppSettings.color(red: 255, green: 255, blue: 255),   // white
        AppSettings.color(red: 255, green: 200, blue: 150),   // soft white
        AppSettings.color(red: 152, green: 255, blue: 152),   // mint green
        AppSettings.color(red: 70, green: 130, blue: 180),    // steel blue
        AppSettings.color(red: 255, green: 0, blue: 0),       // red
        AppSettings.color(red: 128, green: 0, blue: 0)        // dark red
    ]

    private var pendingSaveTask: Task<Void, Never>?
    private let saveDebounceDelay: UInt64 = 200_000_000  // 0.2 seconds

    @Published var defaultBrightness: CGFloat {
        didSet { saveSettings() }
    }
    @Published var useDefaultBrightnessOnAppear: Bool {
        didSet { saveSettings() }
    }
    @Published var selectedColors: [Color] {
        didSet { saveSettings() }
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

    init() {
        let defaults = UserDefaults.standard

        // Register defaults
        defaults.register(defaults: [
            "defaultBrightness": 1.0,
            "useDefaultBrightnessOnAppear": true,
            "preventScreenLock": true,
            "isAngleBasedBrightnessActive": true
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
            selectedColorsValue = AppSettings.defaultColors
        }

        let isAngleBasedBrightnessActiveValue = defaults.bool(forKey: "isAngleBasedBrightnessActive")

        let lastSelectedColorIndexValue: Int
        if defaults.object(forKey: "lastSelectedColorIndex") == nil {
            // First launch: default to the second preset when available
            lastSelectedColorIndexValue = selectedColorsValue.indices.contains(1) ? 1 : 0
        } else {
            let storedIndex = defaults.integer(forKey: "lastSelectedColorIndex")
            lastSelectedColorIndexValue = selectedColorsValue.indices.contains(storedIndex) ? storedIndex : 0
        }

        // Now assign to stored properties (safe to use self)
        self.defaultBrightness = defaultBrightnessValue
        self.useDefaultBrightnessOnAppear = useDefaultBrightnessOnAppearValue
        self.selectedColors = selectedColorsValue
        self.isAngleBasedBrightnessActive = isAngleBasedBrightnessActiveValue
        self.lastSelectedColorIndex = lastSelectedColorIndexValue
        self.preventScreenLock = preventScreenLockValue
    }

    private func saveSettings() {
        scheduleSave()
    }

    func flushPendingSaves() {
        pendingSaveTask?.cancel()
        pendingSaveTask = nil
        persistSettings()
    }

    private func scheduleSave() {
        pendingSaveTask?.cancel()
        pendingSaveTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: saveDebounceDelay)
            guard let self else { return }
            await self.persistSettings()
        }
    }

    private func persistSettings() {
        pendingSaveTask = nil

        UserDefaults.standard.set(Double(defaultBrightness), forKey: "defaultBrightness")
        UserDefaults.standard.set(useDefaultBrightnessOnAppear, forKey: "useDefaultBrightnessOnAppear")
        UserDefaults.standard.set(preventScreenLock, forKey: "preventScreenLock")
        
        let codableColors = selectedColors.map { CodableColor(color: $0) }
        if let encoded = try? JSONEncoder().encode(codableColors) {
            UserDefaults.standard.set(encoded, forKey: "selectedColors")
        }

        UserDefaults.standard.set(isAngleBasedBrightnessActive, forKey: "isAngleBasedBrightnessActive")

        // Save the selected index
        UserDefaults.standard.set(lastSelectedColorIndex, forKey: "lastSelectedColorIndex")
    }

    private static func color(red: Double, green: Double, blue: Double) -> Color {
        Color(.sRGB, red: red / 255, green: green / 255, blue: blue / 255, opacity: 1.0)
    }
}

// CodableColor struct
struct CodableColor: Codable {
    let red: CGFloat
    let green: CGFloat
    let blue: CGFloat
    let alpha: CGFloat

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
