//
//  BrightnessIndicatorView.swift
//  TableTorch
//
//  Thin vertical edge indicator with sun icon and percentage shown during brightness adjustment
//

import SwiftUI

struct BrightnessIndicatorView: View {
    let brightness: CGFloat
    let isVisible: Bool
    var torchColor: Color = .black
    var alwaysVisible: Bool = false

    @State private var opacity: Double = 0.0
    @State private var fadeTask: Task<Void, Never>?
    private let dimmedOpacity: Double = 0.3

    /// Foreground color that contrasts with the current torch color.
    private var foregroundColor: Color {
        torchColor.adaptiveForeground
    }

    /// Shadow color opposite to the foreground for extra separation.
    private var shadowColor: Color {
        torchColor.isLight ? .white : .black
    }

    var body: some View {
        GeometryReader { geometry in
            HStack {
                Spacer()

                // Indicator column on right edge
                VStack(spacing: 8) {
                    // Sun icon at top
                    sunIcon
                        .font(.system(size: 16, weight: .medium))
                        .foregroundColor(foregroundColor)
                        .shadow(color: shadowColor.opacity(0.3), radius: 2)

                    // Percentage text
                    Text("\(Int(brightness * 100))%")
                        .font(.system(size: 11, weight: .medium, design: .rounded))
                        .foregroundColor(foregroundColor.opacity(0.9))
                        .monospacedDigit()
                        .shadow(color: shadowColor.opacity(0.3), radius: 2)

                    // Vertical indicator bar
                    ZStack(alignment: .bottom) {
                        // Track background
                        RoundedRectangle(cornerRadius: 2)
                            .fill(foregroundColor.opacity(0.2))
                            .frame(width: 4)

                        // Fill indicator
                        RoundedRectangle(cornerRadius: 2)
                            .fill(foregroundColor)
                            .frame(width: 4, height: indicatorHeight(for: geometry))
                    }
                    .frame(height: geometry.size.height * 0.5)
                }
                .padding(.trailing, 12)
                .padding(.top, geometry.safeAreaInsets.top + 8)
                .opacity(opacity)
            }
            .frame(maxHeight: .infinity, alignment: .top)
        }
        .onAppear {
            opacity = alwaysVisible ? dimmedOpacity : 0.0
        }
        .onChange(of: alwaysVisible) { _, newValue in
            withAnimation(AnimationConstants.smoothTransition) {
                opacity = newValue ? dimmedOpacity : 0.0
            }
        }
        .onChange(of: isVisible) { _, newValue in
            fadeTask?.cancel()
            if newValue {
                withAnimation(AnimationConstants.quickResponse) {
                    opacity = 1.0
                }
            } else {
                fadeTask = Task { @MainActor in
                    try? await Task.sleep(nanoseconds: UInt64(AnimationConstants.indicatorFadeDelay * 1_000_000_000))
                    guard !Task.isCancelled else { return }
                    withAnimation(AnimationConstants.smoothTransition) {
                        opacity = alwaysVisible ? dimmedOpacity : 0.0
                    }
                }
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }

    /// Sun icon that changes based on brightness level
    @ViewBuilder
    private var sunIcon: some View {
        if brightness < 0.25 {
            Image(systemName: "moon.fill")
        } else if brightness < 0.50 {
            Image(systemName: "sun.min.fill")
        } else if brightness < 0.75 {
            Image(systemName: "sun.max.fill")
        } else {
            Image(systemName: "sun.max.fill")
                .symbolRenderingMode(.hierarchical)
        }
    }

    private func indicatorHeight(for geometry: GeometryProxy) -> CGFloat {
        geometry.size.height * 0.5 * brightness
    }
}

#Preview("Dark background") {
    ZStack {
        Color.red.opacity(0.3)
        BrightnessIndicatorView(brightness: 0.75, isVisible: true, torchColor: .red)
    }
    .ignoresSafeArea()
}

#Preview("Light background") {
    ZStack {
        Color.white
        BrightnessIndicatorView(brightness: 0.75, isVisible: true, torchColor: .white)
    }
    .ignoresSafeArea()
}
