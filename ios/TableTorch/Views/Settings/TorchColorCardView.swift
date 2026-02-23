//
//  TorchColorCardView.swift
//  TableTorch
//
//  Color customization card for settings
//

import SwiftUI

struct TorchColorCardView: View {
    @ObservedObject var settings: AppSettings
    @Binding var selectedIndex: Int

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    private var showModifiedChip: Bool {
        settings.matchingPalette() == nil
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Section header
            Text("Torch Colors")
                .font(.headline)
                .foregroundColor(.orange)

            // Color grid
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(Array(settings.selectedColors.enumerated()), id: \.offset) { index, color in
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

            // Palette chips horizontal scroll
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    if showModifiedChip {
                        ModifiedChipView()
                    }

                    ForEach(settings.allPalettes) { palette in
                        PaletteChipView(
                            palette: palette,
                            isActive: palette.matches(colors: settings.selectedColors),
                            onTap: {
                                settings.loadPalette(palette)
                            }
                        )
                    }
                }
                .padding(.horizontal, 2)
            }
            .scrollIndicators(.hidden)

            // Edit Colors + Palettes links
            HStack(spacing: 24) {
                NavigationLink {
                    EditColorsView(settings: settings)
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "paintpalette")
                        Text("Edit Colors")
                    }
                    .font(.subheadline)
                    .foregroundColor(.orange)
                }

                NavigationLink {
                    PaletteListView(settings: settings)
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "tray.full")
                        Text("Palettes")
                    }
                    .font(.subheadline)
                    .foregroundColor(.orange)
                }
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

        NavigationStack {
            TorchColorCardView(
                settings: AppSettings(),
                selectedIndex: .constant(1)
            )
            .padding()
        }
    }
}
