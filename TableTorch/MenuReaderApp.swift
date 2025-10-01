//
//  MenuReaderApp.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//


import SwiftUI
import UIKit

@main
struct TableTorchApp: App {
    @StateObject private var brightnessManager = BrightnessManager()

    // Control splash visibility
    @State private var showSplash = true

    var body: some Scene {
        WindowGroup {
            ZStack {
                // Main content
                ContentView()
                    .environmentObject(brightnessManager)
                    .onAppear {
                        // Begin managing brightness at launch
                        brightnessManager.beginManagingBrightness()
                    }
                    // When app goes out of foreground
                    .onReceive(NotificationCenter.default.publisher(for: UIScene.willDeactivateNotification)) { _ in
                        brightnessManager.endManagingBrightness()
                    }

                // Overlay the splash screen if needed
                if showSplash {
                    SplashView()
                        .transition(.opacity)
                        .allowsHitTesting(false)
                }
            }
            .onAppear {
                // Hide splash shortly after launch to match iOS guidance
                DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                    withAnimation {
                        showSplash = false
                    }
                }
            }
        }
    }
}
