//
//  MenuReaderApp.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//


import SwiftUI

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
                        // Save the system brightness at launch
                        brightnessManager.saveSystemBrightness()
                    }
                    // When app goes out of foreground
                    .onReceive(NotificationCenter.default.publisher(for: UIScene.willDeactivateNotification)) { _ in
                        brightnessManager.restoreSystemBrightness()
                    }

                // Overlay the splash screen if needed
                if showSplash {
                    SplashView()
                        .transition(.opacity)
                }
            }
            .onAppear {
                // Hide splash after 2 seconds
                DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                    withAnimation {
                        showSplash = false
                    }
                }
            }
        }
    }
}
