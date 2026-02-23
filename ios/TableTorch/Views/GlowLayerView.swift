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
        ZStack {
            // Primary glow - closest, most visible
            color
                .blur(radius: AnimationConstants.Glow.primaryBlur)
                .opacity(AnimationConstants.Glow.primaryOpacity * intensity)

            // Secondary glow - medium distance
            color
                .blur(radius: AnimationConstants.Glow.secondaryBlur)
                .opacity(AnimationConstants.Glow.secondaryOpacity * intensity)

            // Ambient glow - furthest, subtle
            color
                .blur(radius: AnimationConstants.Glow.ambientBlur)
                .opacity(AnimationConstants.Glow.ambientOpacity * intensity)
        }
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
