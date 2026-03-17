//
//  ColorRingView.swift
//  TableTorch
//
//  Circular arrangement of color selection orbs
//

import SwiftUI

struct ColorRingView: View {
    let colors: [Color]
    @Binding var selectedIndex: Int
    let onSettingsTapped: () -> Void

    @State private var animatedAppearance: Bool = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private let radius: CGFloat = AnimationConstants.Overlay.colorRingRadius

    var body: some View {
        ZStack {
            // Color orbs arranged in circle
            ForEach(Array(colors.enumerated()), id: \.offset) { index, color in
                let angle = angleForIndex(index, total: colors.count)
                let position = positionForAngle(angle, radius: radius)
                let delay = Double(index) * AnimationConstants.staggerDelay

                AccessibleColorToken(
                    color: color,
                    index: index,
                    isSelected: selectedIndex == index,
                    colorName: color.accessibleName,
                    onTap: {
                        withAnimation(AnimationConstants.quickResponse) {
                            selectedIndex = index
                        }
                    }
                )
                .offset(x: position.x, y: position.y)
                .scaleEffect(animatedAppearance ? 1.0 : 0.1)
                .opacity(animatedAppearance ? 1.0 : 0.0)
                .animation(
                    reduceMotion ? .none : AnimationConstants.bouncy.delay(delay),
                    value: animatedAppearance
                )
            }

            // Center button - shows current color with settings icon overlay
            Button(action: onSettingsTapped) {
                ZStack {
                    Circle()
                        .fill(colors[safe: selectedIndex] ?? .white)
                        .frame(width: 44, height: 44)
                        .shadow(color: (colors[safe: selectedIndex] ?? .white).opacity(0.5), radius: 8)

                    Image(systemName: "gearshape")
                        .font(.system(size: 18, weight: .medium))
                        .foregroundColor(.white)
                        .shadow(color: .black.opacity(0.5), radius: 2)
                }
            }
            .buttonStyle(.plain)
            .scaleEffect(animatedAppearance ? 1.0 : 0.5)
            .opacity(animatedAppearance ? 1.0 : 0.0)
            .animation(
                reduceMotion ? .none : AnimationConstants.smoothTransition.delay(0.1),
                value: animatedAppearance
            )
            .accessibilityLabel(Text("Settings"))
            .accessibilityHint(Text("Double tap to open settings"))
        }
        .onAppear {
            animatedAppearance = true
        }
        .onDisappear {
            animatedAppearance = false
        }
    }

    private func angleForIndex(_ index: Int, total: Int) -> Double {
        // Start from top (-90 degrees) and go clockwise
        let startAngle = -Double.pi / 2
        let angleStep = (2 * Double.pi) / Double(total)
        return startAngle + (Double(index) * angleStep)
    }

    private func positionForAngle(_ angle: Double, radius: CGFloat) -> CGPoint {
        CGPoint(
            x: CGFloat(cos(angle)) * radius,
            y: CGFloat(sin(angle)) * radius
        )
    }
}

#Preview {
    ZStack {
        Color.black.opacity(0.7)
        ColorRingView(
            colors: [.white, .orange, .red, .blue, .green, .purple],
            selectedIndex: .constant(1),
            onSettingsTapped: {}
        )
    }
    .ignoresSafeArea()
}
