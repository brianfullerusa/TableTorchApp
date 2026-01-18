# Trial-to-Full Conversion Plan

## Goals
- Let new downloads run TableTorch free for ~48 hours, then require a purchase to unlock all features permanently.
- Preserve unlimited access for everyone who previously bought the paid download, even after the app switches to free + in-app purchase (IAP).
- Keep the codebase maintainable: reuse existing `AppSettings`, `BrightnessManager`, etc., and avoid regressions in brightness/motion behavior.

## Viable Monetization Patterns
The App Store offers a few paths to transition from up-front paid to trial-first. Each affects engineering scope, review requirements, and customer experience.

1. **Option A – Non-consumable IAP ("Full Access") with app-managed trial**
   - **Flow**: Convert the app to 免费, ship a new `non-consumable` IAP (e.g., `com.tabtorch.fullaccess`). Everyone gets full functionality for 48 hours after install/update; after the countdown, the paywall appears until the IAP is purchased or restored.
   - **Pros**: Matches "pay once" expectation; no recurring charge; clearest path for existing owners (they already effectively own the product). Apple allows manual trials for non-consumables as long as functionality clearly states "trial".
   - **Cons**: Trial enforcement must be implemented client-side (risk of clock tampering); cannot leverage App Store free trial UI; requires careful messaging to comply with App Review.

2. **Option B – Auto-renewable subscription with introductory free trial**
   - **Flow**: Replace up-front price with a low-cost monthly or yearly subscription that offers a 2-day free trial via StoreKit metadata.
   - **Pros**: Intro offers are enforced by the App Store (no client clock juggling); built-in receipts for entitlement status.
   - **Cons**: Ongoing subscription may feel wrong for a utility torch; existing paid users must be grandfathered with a lifetime entitlement (still need receipt parsing); more legal copy (subscriptions require Terms).

3. **Option C – Non-renewing subscription (consumable trial pass)**
   - **Flow**: Sell a non-renewing "lifetime" access product and distribute promo codes/vouchers for legacy users.
   - **Pros**: Works offline; App Review-friendly.
   - **Cons**: Manual support burden; no automatic recognition of historical purchases; not ideal here.

**Recommendation**: Ship Option A. It keeps the existing "buy once" business model, adds a lightweight trial, and minimizes product uncertainty. The rest of the plan assumes Option A, but the same architecture can host Option B later (swap entitlement source).

## High-level Milestones
1. **Analytics & requirements** – Confirm which features are paywalled, finalize copy, and document the "2-day" requirement.
2. **App Store Connect setup** – Flip the app to free, register `Full Access` IAP, price tiers, review metadata.
3. **StoreKit integration** – Implement StoreKit 2 purchase + restore flows, entitlement caching, and receipt validation helper.
4. **Trial experience** – Add `TrialManager` to start/stop the countdown, surface banners/modals, and persist state securely.
5. **Legacy purchaser migration** – Detect historical owners automatically via receipt analysis, set a `grandfathered` flag, and keep a manual restore path.
6. **UX polish & localization** – Extend existing SwiftUI views for trial countdown, purchase screen, and settings toggles.
7. **QA + release** – Cover paths on simulator (fresh install, reinstall, restore) and a real device with historical receipt.

## Detailed Implementation Plan

### 1. Requirements + Architecture (1 sprint)
- Inventory which screens/features need gating (likely everything except onboarding + settings preview).
- Decide the trial length (48 hours) and grace policies (e.g., continue running session if timer hits zero mid-use or immediately lock?).
- Update product policy docs, privacy policy, and marketing copy to mention the trial.
- Draft UX flows (wireframes) for: welcome screen with countdown, paywall modal, settings section showing entitlement status.

### 2. App Store Connect Work (in parallel)
- Change the app price to `Free` in App Store Connect, matching upcoming binary version.
- Create a new `Non-Consumable` IAP named "Full Access" with product ID `com.tabtorch.fullaccess` (or similar) and provide localized descriptions.
- Upload the same screenshots but add text for the trial; include compliance answers (no cross-border data).
- Prepare `Promo Codes` only for support (not for migration, which is automated).
- Capture the last purely paid version (e.g., `1.3.0`) to compare against receipts.

### 3. StoreKit 2 Integration (2 sprints)
- Add `StoreKit` capability to the target if missing.
- Create `PurchaseManager` (ObservableObject) that fetches product info via `Product.products(for:)`, handles `try await product.purchase()`, and listens for `Transaction.updates` to keep entitlements fresh.
- Save entitlements in `AppSettings`, e.g., `@Published var hasUnlockedFullAccess`.
- Implement `Restore Purchases` button calling `AppStore.sync()` to refresh the receipt.
- Persist the latest verified transaction ID and expiry (if you later add subscriptions) in the keychain to guard against reinstall resets.

### 4. Trial Manager (concurrent with StoreKit)
- Introduce `TrialManager` struct/class responsible for starting/stopping the countdown:
  - On first launch (no existing entitlement), store `trialStartDate` in the keychain or `UserDefaults` with `NSUbiquitousKeyValueStore` sync for multi-device.
  - Compute `trialEndDate = start + 48h` and expose publishers like `trialRemaining`.
  - Listen to significant time changes (`NSNotification.Name.NSSystemClockDidChange`, `UIApplication.significantTimeChangeNotification`) and re-validate to mitigate manual clock rewinds; optionally ping Apple receipt timestamps to detect tampering.
  - Provide derived state: `.active`, `.expired`, `.neverStarted`.
- Hook `TrialManager` into `AppSettings` so that `ContentView` can check `.isFeatureUnlocked = hasUnlockedFullAccess || trialActive || isGrandfathered`.

### 5. Legacy Purchaser Detection
- On first launch after installing version `X` (the free update), run a `LegacyEntitlementChecker`:
  - Request/refresh the App Store receipt via `try await AppStore.sync()` if missing.
  - Parse `original_application_version` and compare against the last paid build number (e.g., `CFBundleVersion` ≤ `100`). Apple sets this to the version the user first purchased, so if it predates the free conversion, mark them as `grandfathered`.
  - As a fallback, look for a transaction of product type `.nonRenewable` or `original_purchase_date` < conversion date.
  - Persist `isGrandfathered = true` in secure storage; never unset unless user signs out.
  - Keep `Restore Purchases` visible so legacy users who delete/reinstall can trigger the same check.
- Communicate clearly: "Thanks for being an early supporter—your purchase unlocks TableTorch forever." No trial gating for these users.

### 6. Paywall & UX Updates
- **Entry point**: On app launch, show `SplashView` → `ContentView`. Inject `AppSettings` (with trial + entitlement state) so UI knows when to show gating.
- **Banner**: Add a subtle countdown banner in `ContentView` showing `Trial ends in Xh Ym`.
- **Modal paywall**: After trial expiry and while `hasUnlockedFullAccess == false`, present a modal with feature highlights, price, purchase + restore buttons, and link to Terms/Privacy.
- **SettingsView**: Add a section listing entitlement status, purchase button, restore, Manage Subscription (future-proof), and copy about the trial. Gate brightness sliders/motion toggles when locked.
- **Accessibility/localization**: Add strings to `Localizable.xcstrings`; ensure Dynamic Type and VoiceOver read the countdown.

### 7. Telemetry, Edge Cases, and Tests
- Unit-test `TrialManager` (start/end calculations, persistence, tamper detection) and `PurchaseManager` (mock StoreKit transactions using `StoreKitTest` configuration files).
- Add UI tests for the paywall gating path once a test target exists.
- Use the StoreKit Testing environment in Xcode 15+ to exercise trial expiry, purchase, and restore flows.
- Confirm `UIApplication.shared.isIdleTimerDisabled` logic still runs when access is unlocked (trial or purchase), and remains disabled when locked.
- QA matrix:
  - Fresh install → start trial → let expire → purchase.
  - Fresh install → purchase immediately (trial should stop or mark as unlocked).
  - Legacy owner updating from paid version → verify immediate full access.
  - Device clock forward/back tests (should not re-open trial).
  - Offline mode when trial expires (should still block until purchase). StoreKit caches purchases; ensure paywall copy handles offline state gracefully.

### 8. Release & Post-launch
- Update marketing copy/screenshots to mention the 2-day trial.
- Submit binary with new business model notes for App Review; mention that legacy owners stay unlocked via receipt check.
- Monitor analytics (retention, conversion). If 48h proves too short/long, adjust duration via remote config (even just `UserDefaults` default) but keep App Review updated.
- Provide support macros for "How to restore purchases".

## Outstanding Info Needed
- Confirm the last paid build number + version (used for receipt comparison).
- Decide if any features should remain free forever (e.g., simple white light) to reduce negative reviews.
- Determine whether to block the app entirely after expiry or degrade gracefully (reduced brightness cap?).
- Whether legal review is needed for new Terms/Privacy text due to monetization shift.
- If analytics is desired for trial usage, choose tooling (e.g., Firebase, TelemetryDeck) and ensure consent prompts cover it.

## Timeline Estimate
- Week 1: Requirements, copy, App Store Connect change request.
- Week 2–3: StoreKit + Trial implementation, basic UI.
- Week 4: Legacy migration testing, localization, QA, and App Review submission.

Keeping all of these steps documented ensures both the trial experience and the legacy entitlement path are clear before touching production code.
