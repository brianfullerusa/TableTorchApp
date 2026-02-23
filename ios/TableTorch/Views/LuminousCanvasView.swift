//
//  LuminousCanvasView.swift
//  TableTorch
//
//  Full-screen light surface with breathing animation and glow effects
//

import SwiftUI

struct LuminousCanvasView: View {
    let color: Color
    let enableBreathing: Bool

    @State private var breathingPhase: CGFloat = 0.0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// Breathing intensity variance (30%)
    private let breathingVariance: CGFloat = 0.30

    /// Computed intensity based on breathing animation
    private var intensity: CGFloat {
        guard enableBreathing && !reduceMotion else { return 1.0 }
        // Sinusoidal breathing: varies between 0.97 and 1.03
        let breathingOffset = sin(breathingPhase * .pi * 2) * breathingVariance
        return 1.0 + breathingOffset
    }

    var body: some View {
        ZStack {
            // Base color fill - edge to edge
            color
                .ignoresSafeArea()
                .opacity(intensity)

            // Glow layers for depth
            GlowLayerView(color: color, intensity: intensity)
        }
        .onAppear {
            startBreathingAnimation()
        }
        .onChange(of: enableBreathing) { newValue in
            if newValue && !reduceMotion {
                startBreathingAnimation()
            } else {
                withAnimation(.easeOut(duration: 0.3)) {
                    breathingPhase = 0.0
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Light canvas")
    }

    private func startBreathingAnimation() {
        guard enableBreathing && !reduceMotion else { return }

        // Continuous breathing animation
        withAnimation(
            .linear(duration: AnimationConstants.breathingDuration)
            .repeatForever(autoreverses: false)
        ) {
            breathingPhase = 1.0
        }
    }
}

#Preview("Breathing Animation") {
    LuminousCanvasView(color: .orange, enableBreathing: true)
        .preferredColorScheme(.dark)
}

#Preview("Static Light") {
    LuminousCanvasView(color: .white, enableBreathing: false)
        .preferredColorScheme(.dark)
}
