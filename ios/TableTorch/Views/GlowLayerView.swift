//
//  GlowLayerView.swift
//  TableTorch
//
//  Multi-layer glow effects for immersive light experience
//

import SwiftUI

struct GlowLayerView: View {
    let color: Color
    let intensity: CGFloat

    var body: some View {
        // Single radial gradient replaces three expensive full-screen blur layers.
        // Blurring a solid color is visually a no-op in the interior, so the
        // original three blurs were GPU-expensive for no visible benefit.
        // A radial gradient provides actual center-to-edge depth cheaply.
        RadialGradient(
            stops: [
                .init(color: color.opacity(0.40 * intensity), location: 0.0),
                .init(color: color.opacity(0.20 * intensity), location: 0.4),
                .init(color: color.opacity(0.05 * intensity), location: 0.85),
                .init(color: color.opacity(0.0), location: 1.0)
            ],
            center: .center,
            startRadius: 0,
            endRadius: 500
        )
        .ignoresSafeArea()
        .allowsHitTesting(false)
    }
}

#Preview {
    ZStack {
        Color.black
        GlowLayerView(color: .orange, intensity: 1.0)
    }
    .ignoresSafeArea()
}
