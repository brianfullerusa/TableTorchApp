//  PurchaseConfiguration.swift
//  TableTorch
//
//  Central configuration for in-app purchase identifiers and display values.

import Foundation

enum PurchaseConfiguration {
    // Update this product identifier to match App Store Connect.
    static let fullUnlockProductId = "com.tabletorch.full_unlock"

    // Fallback display price while StoreKit product metadata is loading.
    static let fallbackDisplayPrice = "$0.99"
}
