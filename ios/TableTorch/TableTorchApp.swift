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

    private static let isScreenshotMode = ProcessInfo.processInfo.arguments.contains("-uiScreenshotMode")
    private static let isScreenshotSplash = ProcessInfo.processInfo.arguments.contains("-uiScreenshotSplash")

    init() {
        if Self.isScreenshotMode {
            UIView.setAnimationsEnabled(false)
        }
    }

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
                        // In screenshot-splash mode, don't auto-dismiss
                        guard !Self.isScreenshotSplash else { return }
                        withAnimation(AnimationConstants.smoothTransition) {
                            showSplash = false
                        }
                    })
                    .transition(.opacity)
                    .allowsHitTesting(false)
                }
            }
            .onAppear {
                if Self.isScreenshotMode && !Self.isScreenshotSplash {
                    showSplash = false
                }
            }
        }
    }
}
