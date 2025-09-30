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
    @StateObject private var settings = AppSettings()
    @StateObject private var motionManager = MotionManager()
    @State private var selectedIndex: Int = 0 //store an index for the selected color

    var body: some View {
        NavigationView {
            ZStack {
                Color.black
                    .edgesIgnoringSafeArea(.all)

                VStack(spacing: 0) {
                    // Show the color from the array at 'selectedIndex'
                    Rectangle()
                        .foregroundColor(settings.selectedColors[safe: selectedIndex] ?? .white)
                        .cornerRadius(10)
                        .padding()

                    Spacer(minLength: 0)

                    VStack(spacing: 10) {
                        BrightnessSliderView(brightness: $brightnessManager.currentBrightness)
                            .frame(height: 30)

                        HStack(spacing: 10) {
                            // Pass the binding for selectedIndex
                            ColorButtonsView(
                                selectedIndex: $selectedIndex,
                                buttonColors: settings.selectedColors
                            )

                            NavigationLink(destination: SettingsView(settings: settings)) {
                                Image(systemName: "gearshape")
                                    .resizable()
                                    .scaledToFit()
                                    .foregroundColor(.white)
                                    .frame(width: 30, height: 30)
                            }
                        }
                        .frame(height: 40)
                    }
                    .padding(.bottom)
                }
                // Header
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .principal) {
                        Text("Table Torch")
                            .font(.custom("Copperplate", size: 24))
                            .foregroundColor(Color(red: 1.0, green: 0.65, blue: 0.0))
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
        .navigationViewStyle(StackNavigationViewStyle())
        .onAppear {
            // Save system brightness
            brightnessManager.saveSystemBrightness()

            // Use default brightness if set
            if settings.useDefaultBrightnessOnAppear {
                brightnessManager.currentBrightness = settings.defaultBrightness
            }

            // Load the last selected index from settings
            self.selectedIndex = settings.lastSelectedColorIndex

            // Start motion if angle-based brightness is active
            if settings.isAngleBasedBrightnessActive {
                motionManager.startUpdates()
            }
            
            // Load the auto sleep settings
            UIApplication.shared.isIdleTimerDisabled = settings.preventScreenLock
        }
        // If user toggles angle-based brightness in settings
        .onChange(of: settings.isAngleBasedBrightnessActive) { newValue in
            if newValue {
                motionManager.startUpdates()
            } else {
                motionManager.stopUpdates()
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
            brightnessManager.restoreSystemBrightness()
            motionManager.stopUpdates()
        }
        // Whenever selectedIndex changes, store it in settings
        .onChange(of: selectedIndex) { newValue in
            settings.lastSelectedColorIndex = newValue
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
