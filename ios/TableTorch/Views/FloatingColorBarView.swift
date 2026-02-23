//
//  FloatingColorBarView.swift
//  TableTorch
//
//  Floating pill-shaped color bar for quick one-tap color switching
//

import SwiftUI

struct FloatingColorBarView: View {
    let colors: [Color]
    @Binding var selectedIndex: Int
    let onSettingsTapped: () -> Void

    private let touchTarget: CGFloat = 44

    var body: some View {
        HStack(spacing: 2) {
            // Color flame tokens
            ForEach(Array(colors.enumerated()), id: \.offset) { index, color in
                FlameColorToken(
                    color: color,
                    index: index,
                    isSelected: index == selectedIndex
                ) {
                    selectedIndex = index
                    HapticEngine.shared.colorChanged()
                }
            }

            // Divider
            Capsule()
                .fill(Color.white.opacity(0.2))
                .frame(width: 1, height: 24)
                .padding(.horizontal, 4)

            // Gear button
            Button {
                onSettingsTapped()
            } label: {
                Image(systemName: "gearshape.fill")
                    .font(.system(size: 16))
                    .foregroundStyle(.white.opacity(0.6))
                    .frame(width: touchTarget, height: touchTarget)
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Settings")
        }
        .padding(.horizontal, 6)
        .padding(.vertical, 5)
        .background(
            Capsule()
                .fill(.ultraThinMaterial)
                .overlay(
                    Capsule()
                        .fill(Color.black.opacity(0.15))
                )
                .overlay(
                    Capsule()
                        .stroke(Color.white.opacity(0.12), lineWidth: 0.5)
                )
                .shadow(color: .black.opacity(0.35), radius: 14, y: 5)
        )
        .animation(AnimationConstants.quickResponse, value: selectedIndex)
    }
}

// MARK: - Flame Color Token

private struct FlameColorToken: View {
    let color: Color
    let index: Int
    let isSelected: Bool
    let onTap: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast
    @State private var isBreathing = false
    @State private var isPressed = false

    private let flameSize: CGFloat = 22
    private let touchTarget: CGFloat = 44
    private let backingDiameter: CGFloat = 38

    private var increaseContrast: Bool {
        colorSchemeContrast == .increased
    }

    /// Compute a darkened, desaturated variant of the flame color for selected
    /// backing. Preserves hue relationship but clamps brightness to at most 0.25
    /// so even white/peach produce a genuinely dark surface.
    private var darkenedColor: Color {
        let uiColor = UIColor(color)
        var h: CGFloat = 0, s: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        uiColor.getHue(&h, saturation: &s, brightness: &b, alpha: &a)
        return Color(
            hue: Double(h),
            saturation: Double(s) * 0.6,
            brightness: min(Double(b) * 0.25, 0.25),
            opacity: 0.85
        )
    }

    var body: some View {
        Button(action: onTap) {
            ZStack {
                // --- Raised circular backing ---
                // Outer soft glow for selected state (drawn behind the circle)
                if isSelected {
                    Circle()
                        .fill(color.opacity(0.35))
                        .frame(width: backingDiameter + 6, height: backingDiameter + 6)
                        .blur(radius: 8)
                }

                // Backing circle: opaque dark inset that decouples the token
                // from whatever the bar background samples behind it.
                //
                // - Unselected: neutral dark at 0.28 opacity. Reads as a subtle
                //   inset well, dark enough to contrast every flame color
                //   (white, peach, green, blue, red, dark red).
                // - Selected: darkened variant of the flame's own hue, creating
                //   a cohesive but high-contrast color relationship.
                // - Increase Contrast: bump opacity for accessibility.
                Circle()
                    .fill(
                        isSelected
                            ? darkenedColor
                            : Color.black.opacity(increaseContrast ? 0.38 : 0.28)
                    )
                    .frame(width: backingDiameter, height: backingDiameter)
                    // Subtle top-left highlight: maintains the raised "button" feel
                    // at reduced intensity since the backing is now darker.
                    .overlay(
                        Circle()
                            .fill(
                                LinearGradient(
                                    colors: [
                                        Color.white.opacity(isSelected ? 0.12 : 0.08),
                                        Color.clear
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                    )
                    // Bevel stroke: defines the circle edge against the bar.
                    // Slightly stronger than before to compensate for the darker fill.
                    .overlay(
                        Circle()
                            .stroke(
                                LinearGradient(
                                    colors: [
                                        Color.white.opacity(isSelected ? 0.25 : 0.15),
                                        Color.black.opacity(0.15)
                                    ],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                ),
                                lineWidth: increaseContrast ? 1.5 : 1.0
                            )
                    )
                    // Selected ring: luminous border in the flame's color.
                    // Against the dark backing this now pops clearly.
                    .overlay(
                        Circle()
                            .stroke(
                                isSelected ? color.opacity(0.7) : Color.clear,
                                lineWidth: 1.5
                            )
                    )
                    .scaleEffect(isPressed ? 0.92 : 1.0)

                // --- Flame icon with dark outline for universal contrast ---
                ZStack {
                    // Dark silhouette layer: rendered at 1.08x scale to produce
                    // a ~1pt dark halo around the colored flame. This defines
                    // the shape boundary regardless of fill color, similar to
                    // how Apple's .palette rendering mode separates symbol layers.
                    Image(systemName: "flame.fill")
                        .font(.system(size: flameSize, weight: .medium))
                        .foregroundStyle(Color.black.opacity(increaseContrast ? 0.65 : 0.50))
                        .scaleEffect(1.08)

                    // Primary colored flame, drawn on top of the silhouette.
                    Image(systemName: "flame.fill")
                        .font(.system(size: flameSize, weight: .medium))
                        .foregroundStyle(color)
                }
                .scaleEffect(isSelected ? (isBreathing ? 1.12 : 1.05) : 0.92)
                .opacity(isSelected ? 1.0 : 0.80)
            }
            .frame(width: touchTarget, height: touchTarget)
            .contentShape(Circle())
        }
        .buttonStyle(FlameTokenButtonStyle(isPressed: $isPressed))
        .accessibilityLabel("Color \(index + 1), \(color.accessibleName)")
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
        .accessibilityHint(isSelected ? "Currently selected" : "Double tap to select")
        .onChange(of: isSelected) { selected in
            if selected && !reduceMotion {
                withAnimation(
                    .easeInOut(duration: AnimationConstants.breathingDuration / 2)
                    .repeatForever(autoreverses: true)
                ) {
                    isBreathing = true
                }
            } else {
                withAnimation(.linear(duration: 0.1)) {
                    isBreathing = false
                }
            }
        }
        .onAppear {
            if isSelected && !reduceMotion {
                withAnimation(
                    .easeInOut(duration: AnimationConstants.breathingDuration / 2)
                    .repeatForever(autoreverses: true)
                ) {
                    isBreathing = true
                }
            }
        }
    }
}

// MARK: - Button Style (press feedback without overriding .plain behavior)

/// Custom button style that communicates press state back to the token
/// so the backing circle can scale down on touch, reinforcing tappability.
private struct FlameTokenButtonStyle: ButtonStyle {
    @Binding var isPressed: Bool

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .onChange(of: configuration.isPressed) { pressed in
                withAnimation(AnimationConstants.quickResponse) {
                    isPressed = pressed
                }
            }
    }
}

#Preview {
    ZStack {
        Color.black.ignoresSafeArea()
        VStack {
            Spacer()
            FloatingColorBarView(
                colors: AppSettings.defaultColors,
                selectedIndex: .constant(1),
                onSettingsTapped: {}
            )
            .padding(.bottom, 30)
        }
    }
}
