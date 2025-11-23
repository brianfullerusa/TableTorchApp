//
//  BrightnessManager.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//


import SwiftUI

@MainActor
class BrightnessManager: ObservableObject {
    private var storedSystemBrightness: CGFloat?
    private var isManagingBrightness = false

    @Published var currentBrightness: CGFloat = UIScreen.main.brightness {
        didSet {
            guard isManagingBrightness else { return }
            UIScreen.main.brightness = currentBrightness
        }
    }

    func beginManagingBrightness() {
        guard !isManagingBrightness else { return }
        storedSystemBrightness = UIScreen.main.brightness
        isManagingBrightness = true
        UIScreen.main.brightness = currentBrightness
    }

    func endManagingBrightness() {
        guard isManagingBrightness else { return }
        isManagingBrightness = false

        guard let baseline = storedSystemBrightness else { return }
        UIScreen.main.brightness = baseline
        storedSystemBrightness = nil
    }

    /// Adjust brightness based on how far the screen is tilted away from flat
    ///  tilt ≈ 0 => device flat => 100% brightness
    ///  tilt ≈ π/2 => device standing on edge => 30% brightness
    func updateBrightness(for tiltAngle: Double) {
        guard isManagingBrightness else { return }
        let clamped = min(max(tiltAngle, 0), Double.pi / 2)
        let fraction = clamped / (Double.pi / 2)  // Range 0..1
        let newBrightness = CGFloat(1.0 - 0.7 * fraction)  // from 1.0 down to 0.3
        currentBrightness = newBrightness
    }

    func updateBrightnessBasedOnPitch(_ pitch: Double) {
        updateBrightness(for: abs(pitch))
    }
}
