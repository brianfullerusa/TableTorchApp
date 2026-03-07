//
//  PaletteChipView.swift
//  TableTorch
//
//  Compact capsule chip showing palette colors and name
//

import SwiftUI

struct PaletteChipView: View {
    let palette: ColorPalette
    let isActive: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 4) {
                HStack(spacing: 2) {
                    ForEach(0..<min(palette.colors.count, 6), id: \.self) { i in
                        Circle()
                            .fill(palette.colors[i].color)
                            .frame(width: 3, height: 3)
                    }
                }

                HStack(spacing: 3) {
                    if palette.isBuiltIn {
                        Image(systemName: palette.icon)
                            .font(.system(size: 7))
                    }
                    Text(palette.name)
                        .font(.caption2)
                        .lineLimit(1)
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(
                Capsule()
                    .fill(isActive ? Color.orange.opacity(0.1) : Color.white.opacity(0.05))
                    .overlay(
                        Capsule()
                            .stroke(isActive ? Color.orange : Color.white.opacity(0.15), lineWidth: 1)
                    )
            )
            .foregroundColor(isActive ? .orange : .white.opacity(0.8))
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(Text("\(palette.name) palette\(isActive ? String(localized: ", active") : "")"))
        .accessibilityHint(Text("Tap to load this palette"))
        .accessibilityAddTraits(isActive ? .isSelected : [])
    }
}

struct ModifiedChipView: View {
    var body: some View {
        VStack(spacing: 4) {
            HStack(spacing: 2) {
                ForEach(0..<6, id: \.self) { _ in
                    Circle()
                        .fill(Color.white.opacity(0.3))
                        .frame(width: 3, height: 3)
                }
            }

            Text("Modified")
                .font(.caption2)
                .lineLimit(1)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(
            Capsule()
                .fill(Color.orange.opacity(0.05))
                .overlay(
                    Capsule()
                        .strokeBorder(style: StrokeStyle(lineWidth: 1, dash: [4, 3]))
                        .foregroundColor(.orange.opacity(0.6))
                )
        )
        .foregroundColor(.orange.opacity(0.8))
        .accessibilityLabel(Text("Modified colors, not saved to any palette"))
    }
}

#Preview {
    ZStack {
        Color.black.ignoresSafeArea()
        HStack(spacing: 8) {
            ModifiedChipView()
            PaletteChipView(
                palette: .bright,
                isActive: true,
                onTap: {}
            )
            PaletteChipView(
                palette: .lowLight,
                isActive: false,
                onTap: {}
            )
        }
        .padding()
    }
}
