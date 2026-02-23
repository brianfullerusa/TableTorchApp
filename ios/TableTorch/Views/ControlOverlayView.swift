//
//  ControlOverlayView.swift
//  TableTorch
//
//  Main overlay container revealed by long press
//

import SwiftUI

struct ControlOverlayView: View {
    let colors: [Color]
    @Binding var selectedIndex: Int
    @Binding var brightness: CGFloat
    @Binding var isPresented: Bool
    let isBrightnessGestureEnabled: Bool
    let onSettingsTapped: () -> Void
    let onBrightnessChanged: (CGFloat) -> Void

    @State private var autoDismissTask: Task<Void, Never>?
    @State private var dimOpacity: Double = 0.0
    @State private var contentVisible: Bool = false

    var body: some View {
        ZStack {
            // Dimmed background
            Color.black
                .opacity(AnimationConstants.Overlay.dimOpacity * dimOpacity)
                .ignoresSafeArea()
                .onTapGesture {
                    dismiss()
                }

            VStack {
                Spacer()

                // Color ring in center area
                ColorRingView(
                    colors: colors,
                    selectedIndex: $selectedIndex,
                    onSettingsTapped: {
                        dismiss()
                        onSettingsTapped()
                    }
                )
                .opacity(contentVisible ? 1.0 : 0.0)
                .scaleEffect(contentVisible ? 1.0 : 0.8)

                Spacer()

                // Brightness arc at bottom
                LuminosityArcView(
                    brightness: $brightness,
                    isEnabled: isBrightnessGestureEnabled,
                    onBrightnessChanged: { newValue in
                        resetAutoDismissTimer()
                        onBrightnessChanged(newValue)
                    }
                )
                .opacity(contentVisible ? 1.0 : 0.0)
                .padding(.bottom, 40)
            }
        }
        .onAppear {
            HapticEngine.shared.overlayRevealed()
            show()
            startAutoDismissTimer()
        }
        .onDisappear {
            autoDismissTask?.cancel()
        }
        .onChange(of: selectedIndex) { _ in
            resetAutoDismissTimer()
        }
        .accessibilityAddTraits(.isModal)
    }

    private func show() {
        withAnimation(AnimationConstants.smoothTransition) {
            dimOpacity = 1.0
        }
        withAnimation(AnimationConstants.smoothTransition.delay(0.1)) {
            contentVisible = true
        }
    }

    private func dismiss() {
        autoDismissTask?.cancel()

        withAnimation(AnimationConstants.quickResponse) {
            contentVisible = false
            dimOpacity = 0.0
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            isPresented = false
        }
    }

    private func startAutoDismissTimer() {
        autoDismissTask?.cancel()
        autoDismissTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: UInt64(AnimationConstants.overlayAutoDismiss * 1_000_000_000))
            guard !Task.isCancelled else { return }
            dismiss()
        }
    }

    private func resetAutoDismissTimer() {
        startAutoDismissTimer()
    }
}

#Preview {
    ZStack {
        Color.orange
            .ignoresSafeArea()

        ControlOverlayView(
            colors: [.white, .orange, .red, .blue, .green, .purple],
            selectedIndex: .constant(1),
            brightness: .constant(0.75),
            isPresented: .constant(true),
            isBrightnessGestureEnabled: true,
            onSettingsTapped: {},
            onBrightnessChanged: { _ in }
        )
    }
}
