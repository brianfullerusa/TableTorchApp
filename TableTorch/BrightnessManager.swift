//
//  BrightnessManager.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//


import SwiftUI

class BrightnessManager: ObservableObject {
    private var systemBrightness: CGFloat = UIScreen.main.brightness

    @Published var currentBrightness: CGFloat = UIScreen.main.brightness {
        didSet {
            UIScreen.main.brightness = currentBrightness
        }
    }

    func saveSystemBrightness() {
        systemBrightness = UIScreen.main.brightness
    }

    func restoreSystemBrightness() {
        UIScreen.main.brightness = systemBrightness
    }

    /// Adjust brightness based on how far the screen is tilted away from flat
    ///  tilt ≈ 0 => device flat => 100% brightness
    ///  tilt ≈ π/2 => device standing on edge => 10% brightness
    func updateBrightness(for tiltAngle: Double) {
        let clamped = min(max(tiltAngle, 0), Double.pi / 2)
        let fraction = clamped / (Double.pi / 2)  // Range 0..1
        let newBrightness = CGFloat(1.0 - 0.9 * fraction)  // from 1.0 down to 0.1
        currentBrightness = newBrightness
    }

    func updateBrightnessBasedOnPitch(_ pitch: Double) {
        updateBrightness(for: abs(pitch))
    }
}

