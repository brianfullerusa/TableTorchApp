//
//  TableTorchApp.swift
//  TableTorch
//
//  Main app entry point with animated splash transition
//

import SwiftUI
import UIKit

@main
struct TableTorchApp: App {
    @StateObject private var brightnessManager = BrightnessManager()
    @StateObject private var settings = AppSettings()
    @State private var showSplash = true

    var body: some Scene {
        WindowGroup {
            ZStack {
                // Main content
                ContentView()
                    .environmentObject(brightnessManager)
                    .environmentObject(settings)

                // Overlay the splash screen if needed
                if showSplash {
                    SplashView(onComplete: {
                        withAnimation(AnimationConstants.smoothTransition) {
                            showSplash = false
                        }
                    })
                    .transition(.opacity)
                    .allowsHitTesting(false)
                }
            }
        }
    }
}
