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
    @StateObject private var entitlements = EntitlementManager()
    @StateObject private var storeKitManager = StoreKitManager()

    // Control splash visibility
    @State private var showSplash = true

    var body: some Scene {
        WindowGroup {
            ZStack {
                // Main content (paywall or full app)
                RootView()
                    .environmentObject(brightnessManager)
                    .environmentObject(entitlements)
                    .environmentObject(storeKitManager)

                // Overlay the splash screen if needed
                if showSplash {
                    SplashView()
                        .transition(.opacity)
                        .allowsHitTesting(false)
                }
            }
            .onAppear {
                // Hide splash shortly after launch to match iOS guidance
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
                    withAnimation {
                        showSplash = false
                    }
                }
            }
        }
    }
}
