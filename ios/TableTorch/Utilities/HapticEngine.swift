//
//  HapticEngine.swift
//  TableTorch
//
//  Centralized haptic feedback manager for the redesigned UI
//

import UIKit

@MainActor
final class HapticEngine {
    static let shared = HapticEngine()

    private let lightImpact = UIImpactFeedbackGenerator(style: .light)
    private let mediumImpact = UIImpactFeedbackGenerator(style: .medium)
    private let softImpact = UIImpactFeedbackGenerator(style: .soft)
    private let selectionFeedback = UISelectionFeedbackGenerator()

    private init() {}

    /// Pre-warm haptic generators for immediate response
    func prepare() {
        lightImpact.prepare()
        mediumImpact.prepare()
        softImpact.prepare()
        selectionFeedback.prepare()
    }

    /// Medium impact for color changes
    func colorChanged() {
        mediumImpact.impactOccurred()
    }

    /// Light impact for brightness thresholds (0%, 25%, 50%, 75%, 100%)
    func brightnessThreshold() {
        lightImpact.impactOccurred()
    }

    /// Soft impact when overlay is revealed
    func overlayRevealed() {
        softImpact.impactOccurred()
    }

    /// Light impact for toggle state changes
    func toggleChanged() {
        lightImpact.impactOccurred()
    }

    /// Selection feedback for fine-grained adjustments
    func selectionChanged() {
        selectionFeedback.selectionChanged()
    }

    /// Light impact with specific intensity
    func lightImpactWithIntensity(_ intensity: CGFloat) {
        lightImpact.impactOccurred(intensity: intensity)
    }

    /// Medium impact with specific intensity
    func mediumImpactWithIntensity(_ intensity: CGFloat) {
        mediumImpact.impactOccurred(intensity: intensity)
    }
}
