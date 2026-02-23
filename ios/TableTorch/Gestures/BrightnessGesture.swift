//
//  BrightnessGesture.swift
//  TableTorch
//
//  Vertical swipe gesture for brightness control
//

import SwiftUI

struct BrightnessGestureModifier: ViewModifier {
    @Binding var brightness: CGFloat
    @Binding var isAdjusting: Bool
    let isEnabled: Bool
    let onThresholdCrossed: (CGFloat) -> Void

    @State private var startBrightness: CGFloat = 0.0
    @State private var lastThreshold: CGFloat? = nil

    private let thresholds = AnimationConstants.Gesture.brightnessThresholds

    func body(content: Content) -> some View {
        content
            .gesture(
                DragGesture(minimumDistance: 10)
                    .onChanged { value in
                        guard isEnabled else { return }

                        if !isAdjusting {
                            startBrightness = brightness
                            isAdjusting = true
                            HapticEngine.shared.prepare()
                        }

                        // Map vertical translation to brightness
                        // Swipe up = increase, swipe down = decrease
                        let screenHeight = UIScreen.main.bounds.height
                        let delta = -value.translation.height / screenHeight
                        let newBrightness = max(0, min(1, startBrightness + delta))
                        brightness = newBrightness

                        // Check for threshold crossings
                        checkThreshold(newBrightness)
                    }
                    .onEnded { _ in
                        isAdjusting = false
                        lastThreshold = nil
                    }
            )
    }

    private func checkThreshold(_ value: CGFloat) {
        // Find the nearest threshold
        for threshold in thresholds {
            let distance = abs(value - threshold)
            if distance < 0.02 { // Within 2% of threshold
                if lastThreshold != threshold {
                    lastThreshold = threshold
                    onThresholdCrossed(threshold)
                    HapticEngine.shared.brightnessThreshold()
                }
                break
            }
        }
    }
}

extension View {
    func brightnessGesture(
        brightness: Binding<CGFloat>,
        isAdjusting: Binding<Bool>,
        isEnabled: Bool = true,
        onThresholdCrossed: @escaping (CGFloat) -> Void = { _ in }
    ) -> some View {
        modifier(BrightnessGestureModifier(
            brightness: brightness,
            isAdjusting: isAdjusting,
            isEnabled: isEnabled,
            onThresholdCrossed: onThresholdCrossed
        ))
    }
}
