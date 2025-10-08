//
//  AppSettings.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//

import SwiftUI

class AppSettings: ObservableObject {
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
            selectedColorsValue = [
                Color(red: 255/255, green: 255/255, blue: 255/255), // white
                Color(red: 255/255, green: 200/255, blue: 150/255), // soft white
                Color(red: 152/255, green: 255/255, blue: 152/255), // mint green
                Color(red: 70/255, green: 130/255, blue: 180/255), // steel blue
                Color(red: 255/255, green: 0/255, blue: 0/255), // red
                Color(red: 128/255, green: 0/255, blue: 0/255) // dark red
            ]
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
