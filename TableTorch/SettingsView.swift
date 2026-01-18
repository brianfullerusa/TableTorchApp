//
//  SettingsView.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//


import SwiftUI

struct SettingsView: View {
    @ObservedObject var settings: AppSettings
    @EnvironmentObject var entitlements: EntitlementManager
    @EnvironmentObject var storeKit: StoreKitManager

    @State private var isProcessingPurchase = false
    @State private var purchaseError: String?

    // For the two-column layout of torch colors
    private let gridColumns = [
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8)
    ]
    
    var body: some View {
        ZStack {
            Color.black
                .edgesIgnoringSafeArea(.all)

            Form {
                // 1) Button Colors
                Section(header:
                    Text("Torch Colors")
                        .font(.title2)
                        .fontWeight(.semibold)
                        .foregroundColor(.orange)
                ) {
                    LazyVGrid(columns: gridColumns, spacing: 8) {
                        ForEach(0..<settings.selectedColors.count, id: \.self) { index in
                            // Show Torch # + flame icon
                            //FlameColorPicker(label: "Torch \(index + 1)", color: $settings.selectedColors[index])
                            FlameColorPicker(label: "\(index + 1)", color: $settings.selectedColors[index])
                        }
                    }
                    
                    
                    // Add a "Restore Defaults" button here
                    Button("Restore Default Colors") {
                        settings.selectedColors = AppSettings.defaultColors
                    }
                    .foregroundColor(.blue)
                }
                .listRowBackground(Color.black)

                // Brightness + toggle for "Use Default Brightness on Launch"
                Section(header:
                    Text("Brightness Settings")
                    .font(.title2)
                    .fontWeight(.semibold)
                        .foregroundColor(.orange)
                ) {
                    HStack {
                        Slider(value: $settings.defaultBrightness, in: 0...1, step: 0.01)
                        Text("\(Int(settings.defaultBrightness * 100))%")
                            .foregroundColor(.white)
                    }
                    .listRowBackground(Color.black)

                    Toggle("Use Default Brightness on Launch", isOn: $settings.useDefaultBrightnessOnAppear)
                        .toggleStyle(SwitchToggleStyle(tint: .blue))
                        .listRowBackground(Color.black)
                    
                    Toggle("Prevent Screen Lock", isOn: $settings.preventScreenLock)
                        .toggleStyle(SwitchToggleStyle(tint: .blue))
                    
                    Text("When enabled, Table Torch will keep the screen on and prevent auto-lock.")
                        .foregroundColor(.white)
                        .font(.footnote)
                }
                .listRowBackground(Color.black)
               
                // 3) Angle-Based Brightness
                Section(header:
                    Text("Tilt Brightness Control")
                        .font(.title2)
                        .fontWeight(.semibold)
                        .foregroundColor(.orange)
                ) {
                    Toggle("Enable Tilt Brightness Control", isOn: $settings.isAngleBasedBrightnessActive)
                        .toggleStyle(SwitchToggleStyle(tint: .blue))
                    Text("Tilt phone:\nVertical=30% brightness,\nFlat=100% brightness")
                        .foregroundColor(.white)
                        .font(.footnote)
                }
                .listRowBackground(Color.black)

                Section(header:
                    Text("Access & Purchases")
                        .font(.title2)
                        .fontWeight(.semibold)
                        .foregroundColor(.orange)
                ) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text(accessStatusLabel)
                            .foregroundColor(.white)

                        if let remainingDays = entitlements.trialDaysRemaining(), entitlements.accessLevel == .trialActive {
                            Text("Trial days left: \(remainingDays)")
                                .foregroundColor(.white.opacity(0.8))
                                .font(.subheadline)
                        }
                    }
                    .listRowBackground(Color.black)

                    if entitlements.accessLevel == .locked || entitlements.accessLevel == .trialActive {
                        Button(action: { Task { await attemptPurchase() } }) {
                            Text("Unlock for \(priceLabel)")
                                .fontWeight(.semibold)
                        }
                        .disabled(isProcessingPurchase)
                        .foregroundColor(.blue)
                    }

                    Button(action: { Task { await attemptRestore() } }) {
                        Text("Restore Purchases")
                    }
                    .disabled(isProcessingPurchase)
                    .foregroundColor(.blue)

                    if let purchaseError {
                        Text(purchaseError)
                            .foregroundColor(.red)
                            .font(.footnote)
                    }
                }
                .listRowBackground(Color.black)
            }
            .scrollContentBackground(.hidden) // iOS 16+ to remove default form background
            .navigationBarTitle("Settings", displayMode: .inline)
            .foregroundColor(.white)
            .task {
                if storeKit.fullUnlockProduct == nil {
                    await storeKit.loadProducts()
                }
            }
        }
    }
}

private extension SettingsView {
    var priceLabel: String {
        storeKit.fullUnlockProduct?.displayPrice ?? PurchaseConfiguration.fallbackDisplayPrice
    }

    var accessStatusLabel: String {
        switch entitlements.accessLevel {
        case .unlockedGrandfathered:
            return "Access: Grandfathered (thank you for your early support!)"
        case .unlockedPaid:
            return "Access: Unlocked via purchase"
        case .trialActive:
            return "Access: Trial active"
        case .locked:
            return "Access: Locked"
        }
    }

    func attemptPurchase() async {
        isProcessingPurchase = true
        purchaseError = nil
        let success = await storeKit.purchaseFullUnlock()
        if success {
            await entitlements.refreshEntitlements()
        } else if let message = storeKit.lastErrorDescription {
            purchaseError = message
        }
        isProcessingPurchase = false
    }

    func attemptRestore() async {
        isProcessingPurchase = true
        purchaseError = nil
        let restored = await storeKit.restorePurchases()
        if restored {
            await entitlements.refreshEntitlements()
        } else if let message = storeKit.lastErrorDescription {
            purchaseError = message
        }
        isProcessingPurchase = false
    }
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        SettingsView(settings: AppSettings())
            .environmentObject(EntitlementManager())
            .environmentObject(StoreKitManager())
            //.environment(\.locale, .init(identifier: "hrv"))
    }
}
