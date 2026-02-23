//
//  ColorCycleGesture.swift
//  TableTorch
//
//  Horizontal swipe gesture for cycling through colors
//

import SwiftUI

struct ColorCycleGestureModifier: ViewModifier {
    @Binding var selectedIndex: Int
    @Binding var isCycling: Bool
    let colorCount: Int
    let onColorChanged: () -> Void

    @State private var startIndex: Int = 0
    @State private var accumulatedTranslation: CGFloat = 0.0

    private let threshold = AnimationConstants.Gesture.colorCycleThreshold

    func body(content: Content) -> some View {
        content
            .gesture(
                DragGesture(minimumDistance: 20)
                    .onChanged { value in
                        guard colorCount > 1 else { return }

                        // Only respond to horizontal swipes
                        let isHorizontal = abs(value.translation.width) > abs(value.translation.height)
                        guard isHorizontal else { return }

                        if !isCycling {
                            startIndex = selectedIndex
                            accumulatedTranslation = 0.0
                            isCycling = true
                            HapticEngine.shared.prepare()
                        }

                        let translation = value.translation.width
                        let delta = translation - accumulatedTranslation

                        // Check if we've crossed the threshold for a color change
                        if abs(delta) >= threshold {
                            let direction = delta > 0 ? -1 : 1  // Swipe right = previous, left = next
                            let newIndex = (selectedIndex + direction + colorCount) % colorCount

                            if newIndex != selectedIndex {
                                selectedIndex = newIndex
                                accumulatedTranslation = translation
                                onColorChanged()
                                HapticEngine.shared.colorChanged()
                            }
                        }
                    }
                    .onEnded { _ in
                        isCycling = false
                        accumulatedTranslation = 0.0
                    }
            )
    }
}

extension View {
    func colorCycleGesture(
        selectedIndex: Binding<Int>,
        isCycling: Binding<Bool>,
        colorCount: Int,
        onColorChanged: @escaping () -> Void = {}
    ) -> some View {
        modifier(ColorCycleGestureModifier(
            selectedIndex: selectedIndex,
            isCycling: isCycling,
            colorCount: colorCount,
            onColorChanged: onColorChanged
        ))
    }
}
