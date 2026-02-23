//
//  ColorIndicatorView.swift
//  TableTorch
//
//  Dot indicator showing current and adjacent colors during color cycling
//

import SwiftUI

struct ColorIndicatorView: View {
    let colors: [Color]
    let selectedIndex: Int
    let isVisible: Bool

    @State private var opacity: Double = 0.0

    private let dotSize: CGFloat = 10
    private let selectedDotSize: CGFloat = 14
    private let spacing: CGFloat = 8

    var body: some View {
        VStack {
            Spacer()

            HStack(spacing: spacing) {
                ForEach(Array(colors.enumerated()), id: \.offset) { index, color in
                    let isSelected = index == selectedIndex
                    let isAdjacent = abs(index - selectedIndex) == 1 ||
                                     (selectedIndex == 0 && index == colors.count - 1) ||
                                     (selectedIndex == colors.count - 1 && index == 0)

                    Circle()
                        .fill(color)
                        .frame(
                            width: isSelected ? selectedDotSize : dotSize,
                            height: isSelected ? selectedDotSize : dotSize
                        )
                        .overlay(
                            Circle()
                                .stroke(Color.white.opacity(0.5), lineWidth: isSelected ? 2 : 0)
                        )
                        .opacity(isSelected ? 1.0 : (isAdjacent ? 0.6 : 0.3))
                        .scaleEffect(isSelected ? 1.0 : 0.9)
                        .animation(AnimationConstants.quickResponse, value: selectedIndex)
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(
                Capsule()
                    .fill(.ultraThinMaterial)
                    .opacity(0.8)
            )
            .opacity(opacity)
            .padding(.bottom, 50)
        }
        .onChange(of: isVisible) { newValue in
            if newValue {
                withAnimation(AnimationConstants.quickResponse) {
                    opacity = 1.0
                }
            } else {
                // Delay fade out
                DispatchQueue.main.asyncAfter(deadline: .now() + AnimationConstants.indicatorFadeDelay) {
                    withAnimation(AnimationConstants.smoothTransition) {
                        opacity = 0.0
                    }
                }
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

#Preview {
    ZStack {
        Color.black
        ColorIndicatorView(
            colors: [.white, .orange, .red, .blue, .green, .purple],
            selectedIndex: 2,
            isVisible: true
        )
    }
    .ignoresSafeArea()
}
