//
//  EditColorsView.swift
//  TableTorch
//
//  Full-screen color editing for all torch colors
//

import SwiftUI

struct EditColorsView: View {
    @ObservedObject var settings: AppSettings
    @Environment(\.dismiss) private var dismiss
    @State private var showNameAlert = false
    @State private var newPaletteName = ""

    private var activePalette: ColorPalette? {
        settings.activePalette
    }

    private var isModifiedFromCustom: Bool {
        guard let palette = activePalette, !palette.isBuiltIn else { return false }
        return settings.hasUnsavedColorChanges
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                ForEach(Array(settings.selectedColors.enumerated()), id: \.offset) { index, _ in
                    EditColorRow(
                        color: $settings.selectedColors[index],
                        index: index
                    )
                }

                // Save as Palette section
                paletteSection
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
        .alert("Name Your Palette", isPresented: $showNameAlert) {
            TextField("Palette name", text: $newPaletteName)
            Button("Save") {
                guard !newPaletteName.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                settings.savePalette(name: newPaletteName.trimmingCharacters(in: .whitespaces))
                HapticEngine.shared.toggleChanged()
            }
            Button("Cancel", role: .cancel) { }
        }
    }

    @ViewBuilder
    private var paletteSection: some View {
        VStack(spacing: 12) {
            if let palette = activePalette, !settings.hasUnsavedColorChanges {
                // Colors match the active palette
                HStack(spacing: 6) {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(.orange)
                    Text("Currently: \(palette.name)")
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(.orange)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
            } else if isModifiedFromCustom, let palette = activePalette {
                // Modified from a custom palette
                VStack(spacing: 10) {
                    Text("Currently: \(palette.name) (modified)")
                        .font(.subheadline)
                        .foregroundColor(.orange.opacity(0.7))

                    Button {
                        settings.updateActivePalette()
                        HapticEngine.shared.toggleChanged()
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "arrow.triangle.2.circlepath")
                            Text("Update \"\(palette.name)\"")
                        }
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(
                            Capsule()
                                .fill(Color.orange)
                        )
                    }

                    Button {
                        newPaletteName = settings.nextDefaultPaletteName()
                        showNameAlert = true
                    } label: {
                        Text("Save as New Palette")
                            .font(.subheadline)
                            .foregroundColor(.orange)
                    }
                }
            } else {
                // No active palette or modified from built-in
                Button {
                    newPaletteName = settings.nextDefaultPaletteName()
                    showNameAlert = true
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "plus.square.on.square")
                        Text("Save as New Palette")
                    }
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(.orange)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(
                        Capsule()
                            .stroke(Color.orange, lineWidth: 1)
                    )
                }
            }
        }
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
        EditColorsView(settings: AppSettings())
    }
}
