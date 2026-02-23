//
//  SettingsSheetView.swift
//  TableTorch
//
//  Glass morphism settings sheet
//

import SwiftUI

struct SettingsSheetView: View {
    @ObservedObject var settings: AppSettings
    @Binding var brightness: CGFloat
    @Binding var selectedIndex: Int
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Colors section
                    TorchColorCardView(
                        settings: settings,
                        selectedIndex: $selectedIndex
                    )

                    // Brightness section
                    brightnessSection

                    // Behavior section
                    behaviorSection

                    // Visual Effects section
                    visualEffectsSection
                }
                .padding()
            }
            .background(
                ZStack {
                    // Background color tinted with current torch color
                    settings.selectedColors[safe: settings.lastSelectedColorIndex]?.opacity(0.2) ?? Color.clear
                    Color.black.opacity(0.3)
                }
                .ignoresSafeArea()
            )
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                    .foregroundColor(.orange)
                }
            }
            .toolbarBackground(.ultraThinMaterial, for: .navigationBar)
            .toolbarBackground(.visible, for: .navigationBar)
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        .modifier(PresentationBackgroundModifier())
    }

    private var brightnessSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Brightness")
                .font(.headline)
                .foregroundColor(.orange)

            // Default brightness slider
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("Default Brightness")
                        .foregroundColor(.white)
                    Spacer()
                    Text("\(Int(settings.defaultBrightness * 100))%")
                        .foregroundColor(.white.opacity(0.7))
                        .monospacedDigit()
                }

                Slider(value: $settings.defaultBrightness, in: 0...1, step: 0.01)
                    .tint(.orange)
            }

            EmberToggleView(
                title: "Use Default on Launch",
                isOn: $settings.useDefaultBrightnessOnAppear,
                subtitle: "Apply default brightness when app opens"
            )

            EmberToggleView(
                title: "Tilt Brightness Control",
                isOn: $settings.isAngleBasedBrightnessActive,
                subtitle: "Adjust brightness by tilting device"
            )

            EmberToggleView(
                title: "Always Show Brightness",
                isOn: $settings.alwaysShowBrightnessIndicator,
                subtitle: "Keep brightness indicator visible on screen"
            )
        }
        .glassCard(tintColor: .orange)
    }

    private var behaviorSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Behavior")
                .font(.headline)
                .foregroundColor(.orange)

            EmberToggleView(
                title: "Prevent Screen Lock",
                isOn: $settings.preventScreenLock,
                subtitle: "Keep screen on while using torch"
            )

            EmberToggleView(
                title: "Quick Color Bar",
                isOn: $settings.showQuickColorBar,
                subtitle: "Show floating color bar on main screen"
            )
        }
        .glassCard(tintColor: .orange)
    }

    private var visualEffectsSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Visual Effects")
                .font(.headline)
                .foregroundColor(.orange)

            EmberToggleView(
                title: "Breathing Animation",
                isOn: Binding(
                    get: { settings.enableBreathingAnimation && !reduceMotion },
                    set: { settings.enableBreathingAnimation = $0 }
                ),
                subtitle: reduceMotion ? "Disabled (Reduce Motion is on)" : "Subtle pulsing light effect"
            )

            EmberToggleView(
                title: "Ember Particles",
                isOn: Binding(
                    get: { settings.enableEmberParticles && !reduceMotion },
                    set: { settings.enableEmberParticles = $0 }
                ),
                subtitle: reduceMotion ? "Disabled (Reduce Motion is on)" : "Floating particles on warm colors"
            )

            if settings.enableEmberParticles && !reduceMotion {
                particleShapePicker
            }
        }
        .glassCard(tintColor: .orange)
        .animation(AnimationConstants.smoothTransition, value: settings.enableEmberParticles)
    }

    private var particleShapePicker: some View {
        HStack(spacing: 8) {
            ForEach(ParticleShape.allCases) { shape in
                Button {
                    settings.particleShape = shape
                    HapticEngine.shared.selectionChanged()
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: shape.pickerSymbolName)
                            .font(.system(size: 18))
                        Text(shape.displayName)
                            .font(.caption2)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(settings.particleShape == shape
                                  ? Color.orange.opacity(0.3)
                                  : Color.white.opacity(0.08))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(settings.particleShape == shape
                                    ? Color.orange
                                    : Color.clear, lineWidth: 1.5)
                    )
                }
                .foregroundColor(settings.particleShape == shape ? .orange : .white.opacity(0.7))
            }
        }
        .transition(.opacity.combined(with: .move(edge: .top)))
    }
}

/// Modifier to apply presentation background with iOS version check
private struct PresentationBackgroundModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 16.4, *) {
            content.presentationBackground(.ultraThinMaterial)
        } else {
            content
        }
    }
}

#Preview {
    Color.orange
        .ignoresSafeArea()
        .sheet(isPresented: .constant(true)) {
            SettingsSheetView(
                settings: AppSettings(),
                brightness: .constant(0.75),
                selectedIndex: .constant(1)
            )
        }
}
