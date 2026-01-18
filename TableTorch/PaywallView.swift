//  PaywallView.swift
//  TableTorch
//
//  Presented when the trial ends or the user is otherwise locked.

import SwiftUI

struct PaywallView: View {
    @EnvironmentObject var entitlements: EntitlementManager
    @EnvironmentObject var storeKit: StoreKitManager

    @State private var isProcessing = false
    @State private var errorMessage: String?

    private var priceText: String {
        storeKit.fullUnlockProduct?.displayPrice ?? PurchaseConfiguration.fallbackDisplayPrice
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            VStack(spacing: 20) {
                VStack(spacing: 8) {
                    Text("Trial Ended")
                        .font(.largeTitle.bold())
                        .foregroundColor(.white)

                    Text("Your 3-day trial is over. Unlock Table Torch to keep full access to every feature.")
                        .multilineTextAlignment(.center)
                        .foregroundColor(.white)
                        .padding(.horizontal)
                }

                if let remaining = entitlements.trialDaysRemaining(), remaining > 0 {
                    Text("Trial time left: \(remaining) day\(remaining == 1 ? "" : "s")")
                        .foregroundColor(.white.opacity(0.8))
                }

                VStack(spacing: 12) {
                    Button {
                        Task { await attemptPurchase() }
                    } label: {
                        HStack {
                            Spacer()
                            Text("Unlock for \(priceText)")
                                .fontWeight(.semibold)
                            Spacer()
                        }
                        .padding()
                        .background(Color.orange)
                        .foregroundColor(.black)
                        .cornerRadius(12)
                    }
                    .disabled(isProcessing)

                    Button("Restore Purchases") {
                        Task { await attemptRestore() }
                    }
                    .disabled(isProcessing)
                    .foregroundColor(.white)
                }

                if isProcessing {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .tint(.white)
                }

                if let errorMessage {
                    Text(errorMessage)
                        .foregroundColor(.red)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }

                Spacer()

                Text("Existing paid users are automatically unlocked.")
                    .foregroundColor(.white.opacity(0.7))
                    .font(.footnote)
            }
            .padding()
        }
        .task {
            if storeKit.fullUnlockProduct == nil {
                await storeKit.loadProducts()
            }
        }
    }

    private func attemptPurchase() async {
        isProcessing = true
        errorMessage = nil
        let success = await storeKit.purchaseFullUnlock()
        if success {
            await entitlements.refreshEntitlements()
        } else if let message = storeKit.lastErrorDescription {
            errorMessage = message
        }
        isProcessing = false
    }

    private func attemptRestore() async {
        isProcessing = true
        errorMessage = nil
        let restored = await storeKit.restorePurchases()
        if restored {
            await entitlements.refreshEntitlements()
        } else if let message = storeKit.lastErrorDescription {
            errorMessage = message
        }
        isProcessing = false
    }
}
