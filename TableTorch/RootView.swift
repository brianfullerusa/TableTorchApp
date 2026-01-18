//  RootView.swift
//  TableTorch
//
//  Routes between the main experience and the paywall based on entitlements.

import SwiftUI

struct RootView: View {
    @EnvironmentObject var entitlements: EntitlementManager
    @EnvironmentObject var storeKit: StoreKitManager

    @State private var hasStartedInitialLoad = false

    var body: some View {
        Group {
            if !entitlements.hasLoadedEntitlements {
                VStack(spacing: 12) {
                    ProgressView()
                    Text("Checking your access…")
                        .foregroundColor(.white)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color.black.ignoresSafeArea())
            } else {
                switch entitlements.accessLevel {
                case .locked:
                    PaywallView()
                case .trialActive, .unlockedPaid, .unlockedGrandfathered:
                    ContentView()
                }
            }
        }
        .task {
            guard !hasStartedInitialLoad else { return }
            hasStartedInitialLoad = true
            await storeKit.loadProducts()
            await entitlements.refreshEntitlements()
        }
    }
}
