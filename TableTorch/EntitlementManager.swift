//  EntitlementManager.swift
//  TableTorch
//
//  Determines whether the user is locked, on trial, grandfathered, or fully unlocked via IAP.

import Foundation
import StoreKit

@MainActor
final class EntitlementManager: ObservableObject {
    enum AccessLevel: Equatable {
        case locked
        case trialActive
        case unlockedPaid
        case unlockedGrandfathered
    }

    @Published private(set) var accessLevel: AccessLevel = .locked
    @Published private(set) var hasLoadedEntitlements = false
    @Published private(set) var lastErrorDescription: String?

    private let newBusinessModelVersion = "2.0"
    private let trialDurationDays: Int = 3
    private let trialStartDateKey = "trialStartDate"

    func refreshEntitlements() async {
        lastErrorDescription = nil

        if await isGrandfatheredUser() {
            accessLevel = .unlockedGrandfathered
            hasLoadedEntitlements = true
            return
        }

        if await hasNonConsumableUnlock() {
            accessLevel = .unlockedPaid
            hasLoadedEntitlements = true
            return
        }

        if isTrialActive() {
            accessLevel = .trialActive
        } else {
            accessLevel = .locked
        }

        hasLoadedEntitlements = true
    }

    func trialDaysRemaining() -> Int? {
        guard let startDate = UserDefaults.standard.object(forKey: trialStartDateKey) as? Date else {
            return nil
        }

        let calendar = Calendar.current
        guard let trialEndDate = calendar.date(byAdding: .day, value: trialDurationDays, to: startDate) else {
            return nil
        }

        if Date() >= trialEndDate {
            return 0
        }

        return calendar.dateComponents([.day], from: Date(), to: trialEndDate).day
    }

    // MARK: - Private

    private func isGrandfatheredUser() async -> Bool {
        do {
            let result = try await AppTransaction.shared
            guard case .verified(let appTransaction) = result else {
                return false
            }

            let originalVersion = appTransaction.originalAppVersion
            // Helpful logging to validate real-world values after release.
            print("AppTransaction originalAppVersion=\(originalVersion), threshold=\(newBusinessModelVersion)")

            return compareVersion(originalVersion, isLessThan: newBusinessModelVersion)
        } catch {
            lastErrorDescription = "Grandfather check failed: \(error.localizedDescription)"
            return false
        }
    }

    private func hasNonConsumableUnlock() async -> Bool {
        for await result in Transaction.currentEntitlements {
            if case .verified(let transaction) = result,
               transaction.productID == PurchaseConfiguration.fullUnlockProductId {
                return true
            }
        }
        return false
    }

    private func isTrialActive() -> Bool {
        let defaults = UserDefaults.standard

        if let start = defaults.object(forKey: trialStartDateKey) as? Date {
            guard let end = Calendar.current.date(byAdding: .day, value: trialDurationDays, to: start) else {
                return false
            }
            return Date() < end
        } else {
            // First time launching on the new model: begin the trial.
            defaults.set(Date(), forKey: trialStartDateKey)
            return true
        }
    }

    private func compareVersion(_ lhs: String, isLessThan rhs: String) -> Bool {
        let lhsParts = lhs.split(separator: ".").compactMap { Int($0) }
        let rhsParts = rhs.split(separator: ".").compactMap { Int($0) }
        let count = max(lhsParts.count, rhsParts.count)

        for index in 0..<count {
            let left = index < lhsParts.count ? lhsParts[index] : 0
            let right = index < rhsParts.count ? rhsParts[index] : 0
            if left < right { return true }
            if left > right { return false }
        }

        return false
    }
}
