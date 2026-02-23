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
    @State private var showSplash = true

    var body: some Scene {
        WindowGroup {
            ZStack {
                // Main content
                ContentView()
                    .environmentObject(brightnessManager)

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
