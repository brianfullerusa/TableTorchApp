//
//  GlassCardView.swift
//  TableTorch
//
//  Glass morphism card component for settings
//

import SwiftUI

struct GlassCardModifier: ViewModifier {
    let tintColor: Color
    let cornerRadius: CGFloat

    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    private var increaseContrast: Bool {
        colorSchemeContrast == .increased
    }

    func body(content: Content) -> some View {
        content
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(.ultraThinMaterial)
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                            .fill(tintColor.opacity(0.1))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                            .stroke(
                                increaseContrast ? Color.white.opacity(0.5) : Color.white.opacity(0.1),
                                lineWidth: increaseContrast ? 2 : 1
                            )
                    )
                    .shadow(color: .black.opacity(0.2), radius: 10, y: 5)
            )
    }
}

extension View {
    func glassCard(tintColor: Color = .clear, cornerRadius: CGFloat = 16) -> some View {
        modifier(GlassCardModifier(tintColor: tintColor, cornerRadius: cornerRadius))
    }
}

#Preview {
    ZStack {
        Color.orange
            .ignoresSafeArea()

        VStack(spacing: 16) {
            Text("Glass Card Content")
                .foregroundColor(.white)
        }
        .glassCard(tintColor: .orange)
        .padding()
    }
}
