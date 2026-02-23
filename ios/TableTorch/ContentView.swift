//
//  ContentView.swift
//  TableTorch
//
//  Redesigned full-screen light canvas with gesture-based controls
//

import SwiftUI
import CoreMotion

struct ContentView: View {
    @EnvironmentObject var brightnessManager: BrightnessManager
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var settings = AppSettings()
    @StateObject private var motionManager = MotionManager()

    // State for color and brightness
    @State private var selectedIndex: Int = 0
    @State private var brightnessDraft: CGFloat = UIScreen.main.brightness

    // Gesture states
    @State private var isAdjustingBrightness: Bool = false
    @State private var showSettingsSheet: Bool = false

    // Double tap state
    @State private var previousBrightness: CGFloat = 1.0
    @State private var isMaxBrightness: Bool = true

    var body: some View {
        mainContent
            .statusBarHidden(true)
            .persistentSystemOverlays(.hidden)
            .preferredColorScheme(.dark)
            .sheet(isPresented: $showSettingsSheet) {
                SettingsSheetView(
                    settings: settings,
                    brightness: $brightnessDraft,
                    selectedIndex: $selectedIndex
                )
            }
            .onAppear(perform: handleOnAppear)
            .onChange(of: settings.isAngleBasedBrightnessActive, perform: handleAngleBasedChange)
            .onChange(of: settings.preventScreenLock) { newValue in
                UIApplication.shared.isIdleTimerDisabled = newValue
            }
            .onReceive(motionManager.$brightnessTiltAngle) { tilt in
                if settings.isAngleBasedBrightnessActive {
                    brightnessManager.updateBrightness(for: tilt)
                }
            }
            .onDisappear(perform: handleOnDisappear)
            .onChange(of: selectedIndex) { newValue in
                settings.lastSelectedColorIndex = newValue
            }
            .onChange(of: scenePhase, perform: handleScenePhaseChange)
            .onChange(of: brightnessManager.currentBrightness, perform: updateBrightnessDraftIfNeeded)
            .onChange(of: brightnessDraft, perform: propagateBrightnessChange)
            .environmentObject(settings)
    }

    // MARK: - Main Content

    private var mainContent: some View {
        GeometryReader { _ in
            ZStack {
                canvasLayer
                particleLayer
                brightnessIndicatorLayer
                bottomControls
            }
            .applyGestures(
                brightnessDraft: $brightnessDraft,
                isAdjustingBrightness: $isAdjustingBrightness,
                isAngleBasedBrightnessActive: settings.isAngleBasedBrightnessActive,
                toggleMaxBrightness: toggleMaxBrightness
            )
        }
    }

    // MARK: - Layers

    private var canvasLayer: some View {
        LuminousCanvasView(
            color: currentColor,
            enableBreathing: settings.enableBreathingAnimation
        )
        .ignoresSafeArea()
    }

    private var particleLayer: some View {
        EmberParticleView(
            color: currentColor,
            isEnabled: settings.enableEmberParticles
        )
        .ignoresSafeArea()
    }

    private var brightnessIndicatorLayer: some View {
        BrightnessIndicatorView(
            brightness: brightnessDraft,
            isVisible: isAdjustingBrightness,
            torchColor: currentColor,
            alwaysVisible: settings.alwaysShowBrightnessIndicator
        )
    }

    private var bottomControls: some View {
        VStack {
            Spacer()
            if settings.showQuickColorBar {
                FloatingColorBarView(
                    colors: settings.selectedColors,
                    selectedIndex: $selectedIndex,
                    onSettingsTapped: { showSettingsSheet = true }
                )
                .opacity(isAdjustingBrightness ? 0 : 1)
                .animation(AnimationConstants.smoothTransition, value: isAdjustingBrightness)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .padding(.bottom, 16)
            } else {
                settingsButton
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(AnimationConstants.smoothTransition, value: settings.showQuickColorBar)
    }

    private var settingsButton: some View {
        HStack {
            Spacer()
            Button {
                showSettingsSheet = true
            } label: {
                Image(systemName: "gearshape.fill")
                    .font(.system(size: 20))
                    .foregroundStyle(.white.opacity(0.5))
                    .padding(12)
                    .contentShape(Circle())
            }
            .accessibilityLabel("Settings")
            .padding(.trailing, 16)
            .padding(.bottom, 16)
        }
    }
}

// MARK: - Gesture Application

private extension View {
    func applyGestures(
        brightnessDraft: Binding<CGFloat>,
        isAdjustingBrightness: Binding<Bool>,
        isAngleBasedBrightnessActive: Bool,
        toggleMaxBrightness: @escaping () -> Void
    ) -> some View {
        self
            .brightnessGesture(
                brightness: brightnessDraft,
                isAdjusting: isAdjustingBrightness,
                isEnabled: !isAngleBasedBrightnessActive,
                onThresholdCrossed: { _ in }
            )
            .simultaneousGesture(
                TapGesture(count: 2)
                    .onEnded {
                        toggleMaxBrightness()
                    }
            )
    }
}

// MARK: - Helper Extensions

extension RandomAccessCollection {
    subscript(safe index: Index) -> Element? {
        return indices.contains(index) ? self[index] : nil
    }
}

// MARK: - Private Methods

private extension ContentView {
    var currentColor: Color {
        settings.selectedColors[safe: selectedIndex] ?? .white
    }

    func handleOnAppear() {
        brightnessManager.beginManagingBrightness()
        HapticEngine.shared.prepare()

        if settings.useDefaultBrightnessOnAppear {
            brightnessManager.currentBrightness = settings.defaultBrightness
        }

        selectedIndex = settings.lastSelectedColorIndex
        startMotionUpdatesIfNeeded()
        UIApplication.shared.isIdleTimerDisabled = settings.preventScreenLock
        brightnessDraft = brightnessManager.currentBrightness
    }

    func handleOnDisappear() {
        brightnessManager.endManagingBrightness()
        stopMotionUpdates()
        settings.flushPendingSaves()
    }

    func handleAngleBasedChange(_ newValue: Bool) {
        if newValue {
            startMotionUpdatesIfNeeded()
        } else {
            stopMotionUpdates()
        }
    }

    func handleScenePhaseChange(_ phase: ScenePhase) {
        switch phase {
        case .active:
            brightnessManager.beginManagingBrightness()
            startMotionUpdatesIfNeeded()
            HapticEngine.shared.prepare()
        case .inactive, .background:
            brightnessManager.endManagingBrightness()
            stopMotionUpdates()
            settings.flushPendingSaves()
        @unknown default:
            break
        }
    }

    func startMotionUpdatesIfNeeded() {
        if settings.isAngleBasedBrightnessActive {
            motionManager.startUpdates()
        }
    }

    func stopMotionUpdates() {
        motionManager.stopUpdates()
    }

    func updateBrightnessDraftIfNeeded(with newValue: CGFloat) {
        if abs(newValue - brightnessDraft) > 0.001 {
            brightnessDraft = newValue
        }
    }

    func propagateBrightnessChange(ifNeeded newValue: CGFloat) {
        guard abs(brightnessManager.currentBrightness - newValue) > 0.001 else { return }
        Task { @MainActor in
            brightnessManager.currentBrightness = newValue
        }
    }

    func toggleMaxBrightness() {
        guard !settings.isAngleBasedBrightnessActive else { return }

        HapticEngine.shared.brightnessThreshold()

        if isMaxBrightness || brightnessDraft >= 0.99 {
            previousBrightness = brightnessDraft
            withAnimation(AnimationConstants.quickResponse) {
                brightnessDraft = 0.3
            }
            isMaxBrightness = false
        } else {
            withAnimation(AnimationConstants.quickResponse) {
                brightnessDraft = previousBrightness >= 0.99 ? 1.0 : previousBrightness
            }
            isMaxBrightness = true
        }
    }
}

#Preview {
    ContentView()
        .environmentObject(BrightnessManager())
}
