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
    let breathingDepth: CGFloat
    let cycleDuration: Double

    @State private var intensity: CGFloat = 1.0
    @State private var breathingTimer: Timer?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var isBreathingActive: Bool {
        enableBreathing && !reduceMotion
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
            if isBreathingActive { startBreathing() }
        }
        .onDisappear { stopBreathing() }
        .onChange(of: enableBreathing) { _, newValue in
            if newValue && !reduceMotion {
                startBreathing()
            } else {
                stopBreathing()
                intensity = 1.0
            }
        }
        .onChange(of: reduceMotion) { _, newValue in
            if newValue {
                stopBreathing()
                intensity = 1.0
            } else if enableBreathing {
                startBreathing()
            }
        }
        .onChange(of: breathingDepth) { _, _ in
            if isBreathingActive { startBreathing() }
        }
        .onChange(of: cycleDuration) { _, _ in
            if isBreathingActive { startBreathing() }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("Light canvas"))
    }

    private func startBreathing() {
        stopBreathing()
        let startTime = CACurrentMediaTime()
        let depth = breathingDepth
        let duration = cycleDuration
        breathingTimer = Timer.scheduledTimer(withTimeInterval: 1.0 / 30.0, repeats: true) { _ in
            Task { @MainActor in
                let elapsed = CACurrentMediaTime() - startTime
                let phase = elapsed.truncatingRemainder(dividingBy: duration) / duration
                let halfDepth = depth / 2.0
                intensity = 1.0 - halfDepth + sin(phase * .pi * 2) * halfDepth
            }
        }
    }

    private func stopBreathing() {
        breathingTimer?.invalidate()
        breathingTimer = nil
    }
}

#Preview("Breathing Animation") {
    LuminousCanvasView(color: .orange, enableBreathing: true, breathingDepth: 0.12, cycleDuration: 4.0)
        .preferredColorScheme(.dark)
}

#Preview("Static Light") {
    LuminousCanvasView(color: .white, enableBreathing: false, breathingDepth: 0.12, cycleDuration: 4.0)
        .preferredColorScheme(.dark)
}
