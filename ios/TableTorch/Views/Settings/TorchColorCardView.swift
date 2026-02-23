//
//  TorchColorCardView.swift
//  TableTorch
//
//  Color customization card for settings
//

import SwiftUI

struct TorchColorCardView: View {
    @Binding var colors: [Color]
    @Binding var selectedIndex: Int

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Section header
            Text("Torch Colors")
                .font(.headline)
                .foregroundColor(.orange)

            // Color grid
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(Array(colors.enumerated()), id: \.offset) { index, color in
                    ColorPickerCell(
                        color: color,
                        index: index,
                        isSelected: selectedIndex == index,
                        onSelected: {
                            withAnimation(AnimationConstants.quickResponse) {
                                selectedIndex = index
                            }
                        }
                    )
                }
            }

            // Edit colors link
            NavigationLink {
                EditColorsView(colors: $colors)
            } label: {
                HStack {
                    Image(systemName: "paintpalette")
                    Text("Edit Colors")
                }
                .font(.subheadline)
                .foregroundColor(.orange)
            }
            .padding(.top, 8)
        }
        .glassCard(tintColor: .orange)
    }
}

private struct ColorPickerCell: View {
    let color: Color
    let index: Int
    let isSelected: Bool
    let onSelected: () -> Void

    var body: some View {
        VStack(spacing: 6) {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(color)
                .frame(height: 50)
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(isSelected ? Color.white : Color.white.opacity(0.3),
                                lineWidth: isSelected ? 2.5 : 1)
                )
                .shadow(color: color.opacity(isSelected ? 0.7 : 0.4), radius: isSelected ? 8 : 6)
                .onTapGesture {
                    onSelected()
                }

            HStack(spacing: 4) {
                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.caption2)
                        .foregroundColor(.orange)
                }
                Text("Torch \(index + 1)")
                    .font(.caption)
                    .foregroundColor(isSelected ? .white : .white.opacity(0.8))
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Torch \(index + 1) color\(isSelected ? ", selected" : "")")
        .accessibilityHint("Tap to select")
    }
}

#Preview {
    ZStack {
        Color.black
            .ignoresSafeArea()

        TorchColorCardView(
            colors: .constant(AppSettings.defaultColors),
            selectedIndex: .constant(1)
        )
        .padding()
    }
}
