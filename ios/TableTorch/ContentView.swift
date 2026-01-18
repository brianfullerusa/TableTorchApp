//
//  ContentView.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//

import SwiftUI
import CoreMotion

struct ContentView: View {
    @EnvironmentObject var brightnessManager: BrightnessManager
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var settings = AppSettings()
    @StateObject private var motionManager = MotionManager()
    @State private var selectedIndex: Int = 0 //store an index for the selected color
    @State private var brightnessDraft: CGFloat = UIScreen.main.brightness

    var body: some View {
        NavigationStack {
            ZStack {
                Color.black
                    .edgesIgnoringSafeArea(.all)

                GeometryReader { proxy in
                    let totalHeight = proxy.size.height
                    let bottomPadding = max(proxy.safeAreaInsets.bottom, 8)
                    let topSpacer = max(totalHeight * 0.01, 8)
                    let sliderHeight: CGFloat = 30
                    let buttonHeight: CGFloat = 44
                    let controlSpacing: CGFloat = 12
                    let controlsAllowance = topSpacer + controlSpacing + sliderHeight + buttonHeight + bottomPadding
                    let proposedDisplayHeight = totalHeight * 0.92
                    let availableHeight = max(totalHeight - controlsAllowance, CGFloat(0))
                    let desiredHeight = max(proposedDisplayHeight, CGFloat(320))
                    let displayHeight: CGFloat = {
                        guard availableHeight >= CGFloat(320) else { return availableHeight }
                        return min(desiredHeight, availableHeight)
                    }()

                    VStack(spacing: 0) {
                        Rectangle()
                            .fill(currentColor)
                        .frame(height: displayHeight)
                        .frame(maxWidth: .infinity)
                        .cornerRadius(24)
                        .padding(.horizontal, 12)
                        .padding(.top, 12)
                        .shadow(color: Color.black.opacity(0.35), radius: 18, x: 0, y: 12)

                        Spacer(minLength: topSpacer)

                        VStack(spacing: controlSpacing) {
                            BrightnessSliderView(
                                brightness: $brightnessDraft,
                                isEnabled: !settings.isAngleBasedBrightnessActive
                            )
                                .frame(height: sliderHeight)

                            HStack(spacing: 16) {
                                ColorButtonsView(
                                    selectedIndex: $selectedIndex,
                                    buttonColors: settings.selectedColors
                                )

                                NavigationLink(destination: SettingsView(settings: settings)) {
                                    Image(systemName: "gearshape")
                                        .resizable()
                                        .scaledToFit()
                                        .foregroundColor(.white)
                                        .frame(width: 32, height: 32)
                                        .padding(10)
                                        .background(Color.white.opacity(0.08))
                                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                                }
                            }
                            .frame(height: buttonHeight)
                        }
                        .padding(.horizontal, 24)
                        .padding(.bottom, bottomPadding)
                    }
                    .frame(width: proxy.size.width, height: proxy.size.height, alignment: .top)
                }
                // Header
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .principal) {
                        HStack(spacing: 8) {
                            Image("FlameLayerPrimary")
                                .resizable()
                                .scaledToFit()
                                .frame(width: 28, height: 28)
                                .accessibilityHidden(true)

                            Text("Table Torch")
                                .font(.custom("Copperplate", size: 24))
                                .foregroundColor(Color(red: 1.0, green: 0.65, blue: 0.0))
                        }
                    }
                }
                .toolbarBackground(
                    Color(red: 0.0, green: 0.0, blue: 0.0),
                    for: .navigationBar
                )
                .toolbarBackground(.visible, for: .navigationBar)
                .toolbarColorScheme(.light, for: .navigationBar)
            }
        }
        .onAppear {
            brightnessManager.beginManagingBrightness()

            // Use default brightness if set
            if settings.useDefaultBrightnessOnAppear {
                brightnessManager.currentBrightness = settings.defaultBrightness
            }

            // Load the last selected index from settings
            self.selectedIndex = settings.lastSelectedColorIndex

            // Start motion if angle-based brightness is active
            startMotionUpdatesIfNeeded()
            
            // Load the auto sleep settings
            UIApplication.shared.isIdleTimerDisabled = settings.preventScreenLock
            brightnessDraft = brightnessManager.currentBrightness
        }
        // If user toggles angle-based brightness in settings
        .onChange(of: settings.isAngleBasedBrightnessActive) { newValue in
            if newValue {
                startMotionUpdatesIfNeeded()
            } else {
                stopMotionUpdates()
            }
        }
        .onChange(of: settings.preventScreenLock) { newValue in
                    UIApplication.shared.isIdleTimerDisabled = newValue
        }
        // Update brightness if angle-based is on
        .onReceive(motionManager.$brightnessTiltAngle) { tilt in
            if settings.isAngleBasedBrightnessActive {
                brightnessManager.updateBrightness(for: tilt)
            }
        }
        // Restore brightness & stop motion on disappear
        .onDisappear {
            brightnessManager.endManagingBrightness()
            stopMotionUpdates()
            settings.flushPendingSaves()
        }
        // Whenever selectedIndex changes, store it in settings
        .onChange(of: selectedIndex) { newValue in
            settings.lastSelectedColorIndex = newValue
        }
        .onChange(of: scenePhase) { phase in
            switch phase {
            case .active:
                brightnessManager.beginManagingBrightness()
                startMotionUpdatesIfNeeded()
            case .inactive, .background:
                brightnessManager.endManagingBrightness()
                stopMotionUpdates()
                settings.flushPendingSaves()
            @unknown default:
                break
            }
        }
        .onChange(of: brightnessManager.currentBrightness) { newValue in
            updateBrightnessDraftIfNeeded(with: newValue)
        }
        .onChange(of: brightnessDraft) { newValue in
            propagateBrightnessChange(ifNeeded: newValue)
        }
        .environmentObject(settings)
    }
}

// An optional safe subscript so we don't crash if index is out of range
extension RandomAccessCollection {
    subscript(safe index: Index) -> Element? {
        return indices.contains(index) ? self[index] : nil
    }
}

private extension ContentView {
    var currentColor: Color {
        settings.selectedColors[safe: selectedIndex] ?? .white
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
        if abs(newValue - brightnessDraft) > brightnessSyncThreshold {
            brightnessDraft = newValue
        }
    }

    func propagateBrightnessChange(ifNeeded newValue: CGFloat) {
        guard abs(brightnessManager.currentBrightness - newValue) > brightnessSyncThreshold else { return }
        Task { @MainActor in
            brightnessManager.currentBrightness = newValue
        }
    }

    var brightnessSyncThreshold: CGFloat { 0.001 }
}
