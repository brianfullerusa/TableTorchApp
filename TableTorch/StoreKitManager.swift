//  StoreKitManager.swift
//  TableTorch
//
//  Handles StoreKit 2 product loading, purchase, and restore flows.

import Foundation
import StoreKit

@MainActor
final class StoreKitManager: ObservableObject {
    @Published private(set) var fullUnlockProduct: Product?
    @Published private(set) var lastErrorDescription: String?

    private let productId = PurchaseConfiguration.fullUnlockProductId
    private var updatesTask: Task<Void, Never>?

    init() {
        // Start listening for transaction updates to avoid missing successful purchases.
        updatesTask = Task { [weak self] in
            await self?.listenForTransactions()
        }
    }

    func loadProducts() async {
        do {
            let products = try await Product.products(for: [productId])
            fullUnlockProduct = products.first
        } catch {
            lastErrorDescription = "Failed to load products: \(error.localizedDescription)"
        }
    }

    func purchaseFullUnlock() async -> Bool {
        let product: Product

        if let loadedProduct = fullUnlockProduct {
            product = loadedProduct
        } else {
            guard let fetchedProduct = await loadAndReturnProduct() else {
                lastErrorDescription = "Product unavailable."
                return false
            }
            product = fetchedProduct
        }

        do {
            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                if case .verified(let transaction) = verification {
                    await transaction.finish()
                    return true
                }
                lastErrorDescription = "Purchase verification failed."
                return false
            case .userCancelled, .pending:
                return false
            @unknown default:
                return false
            }
        } catch {
            lastErrorDescription = error.localizedDescription
            return false
        }
    }

    func restorePurchases() async -> Bool {
        var restored = false

        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result,
               transaction.productID == productId {
                restored = true
            }
        }

        if !restored {
            lastErrorDescription = "No previous purchases found."
        }

        return restored
    }

    /// Listens for StoreKit transaction updates and finishes verified transactions.
    private func listenForTransactions() async {
        for await result in Transaction.updates {
            switch result {
            case .verified(let transaction):
                // If this is our product, finish it to complete the purchase flow.
                if transaction.productID == productId {
                    await transaction.finish()
                } else {
                    // Always finish verified transactions you don't handle to keep the queue clean.
                    await transaction.finish()
                }
            case .unverified(_, let error):
                // Record the error for diagnostics.
                await MainActor.run {
                    self.lastErrorDescription = "Unverified transaction: \(error.localizedDescription)"
                }
            }
        }
    }

    // MARK: - Private

    private func loadAndReturnProduct() async -> Product? {
        do {
            let products = try await Product.products(for: [productId])
            let product = products.first
            fullUnlockProduct = product
            return product
        } catch {
            lastErrorDescription = error.localizedDescription
            return nil
        }
    }

    deinit {
        updatesTask?.cancel()
    }
}
