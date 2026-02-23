//
//  PaletteListView.swift
//  TableTorch
//
//  Full palette management list screen
//

import SwiftUI

struct PaletteListView: View {
    @ObservedObject var settings: AppSettings
    @State private var paletteToDelete: ColorPalette?
    @State private var paletteToRename: ColorPalette?
    @State private var renameText = ""
    @State private var showDeleteAlert = false
    @State private var showRenameAlert = false

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Built-In section
                VStack(alignment: .leading, spacing: 12) {
                    Text("Built-In")
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(.white.opacity(0.6))
                        .frame(maxWidth: .infinity, alignment: .leading)

                    ForEach(ColorPalette.builtInPresets) { palette in
                        PaletteRow(
                            palette: palette,
                            isActive: settings.activePaletteId == palette.id
                                && !settings.hasUnsavedColorChanges
                        ) {
                            settings.loadPalette(palette)
                        }
                    }
                }

                // Custom section
                VStack(alignment: .leading, spacing: 12) {
                    Text("Custom")
                        .font(.subheadline.weight(.medium))
                        .foregroundColor(.white.opacity(0.6))
                        .frame(maxWidth: .infinity, alignment: .leading)

                    if settings.customPalettes.isEmpty {
                        emptyState
                    } else {
                        ForEach(settings.customPalettes) { palette in
                            PaletteRow(
                                palette: palette,
                                isActive: settings.activePaletteId == palette.id
                                    && !settings.hasUnsavedColorChanges
                            ) {
                                settings.loadPalette(palette)
                            }
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    paletteToDelete = palette
                                    showDeleteAlert = true
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                            .contextMenu {
                                Button {
                                    paletteToRename = palette
                                    renameText = palette.name
                                    showRenameAlert = true
                                } label: {
                                    Label("Rename", systemImage: "pencil")
                                }

                                Button {
                                    settings.duplicatePalette(id: palette.id)
                                    HapticEngine.shared.toggleChanged()
                                } label: {
                                    Label("Duplicate", systemImage: "doc.on.doc")
                                }

                                Divider()

                                Button(role: .destructive) {
                                    paletteToDelete = palette
                                    showDeleteAlert = true
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                        }
                    }
                }
            }
            .padding()
        }
        .background(
            Color.black.opacity(0.3)
                .ignoresSafeArea()
        )
        .navigationTitle("Palettes")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Delete Palette?", isPresented: $showDeleteAlert) {
            Button("Delete", role: .destructive) {
                if let palette = paletteToDelete {
                    withAnimation(AnimationConstants.quickResponse) {
                        settings.deletePalette(id: palette.id)
                    }
                    HapticEngine.shared.toggleChanged()
                }
            }
            Button("Cancel", role: .cancel) { }
        } message: {
            if let palette = paletteToDelete {
                Text("Are you sure you want to delete \"\(palette.name)\"? This cannot be undone.")
            }
        }
        .alert("Rename Palette", isPresented: $showRenameAlert) {
            TextField("Palette name", text: $renameText)
            Button("Save") {
                if let palette = paletteToRename, !renameText.isEmpty {
                    settings.renamePalette(id: palette.id, newName: renameText)
                }
            }
            .disabled(renameText.isEmpty)
            Button("Cancel", role: .cancel) { }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 8) {
            Text("No custom palettes yet")
                .font(.subheadline)
                .foregroundColor(.white.opacity(0.5))
            Text("Save your current colors from Edit Colors")
                .font(.caption)
                .foregroundColor(.white.opacity(0.35))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
    }
}

// MARK: - Palette Row

private struct PaletteRow: View {
    let palette: ColorPalette
    let isActive: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 12) {
                // Icon badge
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(palette.colors.first?.color.opacity(0.3) ?? Color.orange.opacity(0.3))
                    .frame(width: 36, height: 36)
                    .overlay(
                        Image(systemName: palette.icon)
                            .font(.system(size: 16))
                            .foregroundColor(palette.colors.first?.color ?? .orange)
                    )

                // Name and status
                VStack(alignment: .leading, spacing: 2) {
                    Text(palette.name)
                        .font(.headline)
                        .foregroundColor(.white)
                    if isActive {
                        Text("Active")
                            .font(.caption)
                            .foregroundColor(.orange)
                    }
                }

                Spacer()

                // Color dots
                HStack(spacing: 3) {
                    ForEach(0..<min(palette.colors.count, 6), id: \.self) { i in
                        Circle()
                            .fill(palette.colors[i].color)
                            .frame(width: 10, height: 10)
                            .overlay(
                                Circle()
                                    .stroke(Color.white.opacity(0.3), lineWidth: 0.5)
                            )
                    }
                }

                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.3))
            }
            .padding(12)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(.ultraThinMaterial)
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(isActive ? Color.orange.opacity(0.08) : Color.clear)
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .stroke(
                                isActive ? Color.orange.opacity(0.3) : Color.white.opacity(0.1),
                                lineWidth: 1
                            )
                    )
            )
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(palette.name) palette\(isActive ? ", active" : "")")
        .accessibilityHint("Tap to load this palette")
        .accessibilityAddTraits(isActive ? .isSelected : [])
    }
}

// Extend View to add swipeActions when not in a List
private extension View {
    @ViewBuilder
    func swipeActions(edge: HorizontalEdge, @ViewBuilder content: () -> some View) -> some View {
        self // Swipe actions require List; we use contextMenu instead for ScrollView
    }
}

#Preview {
    NavigationStack {
        PaletteListView(settings: AppSettings())
    }
}
