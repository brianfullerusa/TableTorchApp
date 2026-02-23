//
//  EditColorsView.swift
//  TableTorch
//
//  Full-screen color editing for all torch colors
//

import SwiftUI

struct EditColorsView: View {
    @Binding var colors: [Color]
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                ForEach(Array(colors.enumerated()), id: \.offset) { index, color in
                    EditColorRow(
                        color: $colors[index],
                        index: index
                    )
                }

                // Restore presets
                VStack(spacing: 12) {
                    Text("Restore Preset")
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(.white.opacity(0.6))
                        .frame(maxWidth: .infinity, alignment: .leading)

                    HStack(spacing: 12) {
                        Button {
                            withAnimation(AnimationConstants.smoothTransition) {
                                colors = AppSettings.defaultColors
                            }
                            HapticEngine.shared.toggleChanged()
                        } label: {
                            presetLabel(
                                icon: "paintpalette",
                                title: "Original",
                                colors: AppSettings.defaultColors
                            )
                        }

                        Button {
                            withAnimation(AnimationConstants.smoothTransition) {
                                colors = AppSettings.lowLightColors
                            }
                            HapticEngine.shared.toggleChanged()
                        } label: {
                            presetLabel(
                                icon: "moon.stars",
                                title: "Low Light",
                                colors: AppSettings.lowLightColors
                            )
                        }
                    }
                }
                .padding(.top, 12)
            }
            .padding()
        }
        .background(
            ZStack {
                Color.black.opacity(0.3)
            }
            .ignoresSafeArea()
        )
        .navigationTitle("Edit Colors")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func presetLabel(icon: String, title: String, colors: [Color]) -> some View {
        VStack(spacing: 8) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.caption)
                Text(title)
                    .font(.subheadline.weight(.medium))
            }
            .foregroundColor(.white)

            // Color preview dots
            HStack(spacing: 3) {
                ForEach(0..<min(colors.count, 6), id: \.self) { i in
                    Circle()
                        .fill(colors[i])
                        .frame(width: 10, height: 10)
                        .overlay(
                            Circle()
                                .stroke(Color.white.opacity(0.3), lineWidth: 0.5)
                        )
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .padding(.horizontal, 12)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(.ultraThinMaterial)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(Color.white.opacity(0.15), lineWidth: 1)
                )
        )
    }
}

// MARK: - Edit Color Row

private struct EditColorRow: View {
    @Binding var color: Color
    let index: Int

    var body: some View {
        HStack(spacing: 16) {
            // Color swatch preview
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(color)
                .frame(width: 56, height: 56)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(Color.white.opacity(0.3), lineWidth: 1)
                )
                .shadow(color: color.opacity(0.5), radius: 6)

            // Label
            VStack(alignment: .leading, spacing: 2) {
                Text("Torch \(index + 1)")
                    .font(.headline)
                    .foregroundColor(.white)

                Text(color.hexString)
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.5))
                    .monospacedDigit()
            }

            Spacer()

            // Color picker
            ColorPicker("", selection: $color, supportsOpacity: false)
                .labelsHidden()
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(.ultraThinMaterial)
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(color.opacity(0.08))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(Color.white.opacity(0.1), lineWidth: 1)
                )
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Torch \(index + 1) color")
        .accessibilityHint("Tap to change color")
    }
}

// MARK: - Color Hex String

private extension Color {
    var hexString: String {
        let uiColor = UIColor(self)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        uiColor.getRed(&r, green: &g, blue: &b, alpha: &a)
        return String(
            format: "#%02X%02X%02X",
            Int(r * 255),
            Int(g * 255),
            Int(b * 255)
        )
    }
}

#Preview {
    NavigationStack {
        EditColorsView(colors: .constant(AppSettings.defaultColors))
    }
}
