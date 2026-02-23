//
//  AnimationConstants.swift
//  TableTorch
//
//  Standard animation curves and durations for consistent UI motion
//

import SwiftUI

enum AnimationConstants {
    // MARK: - Spring Animations

    /// Quick, responsive spring for immediate feedback
    static let quickResponse: Animation = .spring(response: 0.3, dampingFraction: 0.8)

    /// Smooth transition for color changes and overlays
    static let smoothTransition: Animation = .spring(response: 0.4, dampingFraction: 0.85)

    /// Gentle floating motion for ambient effects
    static let gentleFloat: Animation = .spring(response: 0.5, dampingFraction: 0.9)

    /// Bouncy spring for playful interactions
    static let bouncy: Animation = .spring(response: 0.35, dampingFraction: 0.7)

    // MARK: - Timing Animations

    /// Standard ease-in-out for simple transitions
    static let standard: Animation = .easeInOut(duration: 0.3)

    /// Slow ease for dramatic reveals
    static let slowReveal: Animation = .easeOut(duration: 0.5)

    // MARK: - Durations

    /// Duration for breathing animation cycle
    static let breathingDuration: Double = 4.0

    /// Duration for indicator fade-out
    static let indicatorFadeDelay: Double = 1.2

    /// Duration for overlay auto-dismiss
    static let overlayAutoDismiss: Double = 5.0

    /// Staggered reveal delay per element
    static let staggerDelay: Double = 0.05

    /// Splash screen phase durations (cinematic ignition sequence)
    enum Splash {
        // Phase 1: Darkness with anticipation
        static let darknessHold: Double = 0.4

        // Phase 2: Spark ignition (tiny bright point appears)
        static let sparkDelay: Double = 0.4       // time before spark appears
        static let sparkFlashDuration: Double = 0.15
        static let sparkGrowDuration: Double = 0.6

        // Phase 3: Flame emergence (spark grows into full flame)
        static let flameGrowDelay: Double = 1.0   // from onAppear
        static let flameGrowDuration: Double = 0.8
        static let flameSettleDuration: Double = 0.4

        // Phase 4: Title illumination
        static let titleDelay: Double = 2.0       // from onAppear
        static let titleFadeDuration: Double = 0.6

        // Phase 5: Hold then complete
        static let totalDuration: Double = 3.2

        // Continuous flame animation parameters
        static let flickerCycleDuration: Double = 2.4
        static let innerFlamePhaseOffset: Double = 0.4

        // Ambient glow
        static let glowBuildDelay: Double = 0.6
        static let glowBuildDuration: Double = 1.2

        // Spark particle parameters
        static let sparkCount: Int = 6
        static let sparkLifetime: Double = 0.8

        // Flame image dimensions
        static let flameWidth: CGFloat = 120
        static let flameHeight: CGFloat = 160
    }

    // MARK: - Ember Particle Constants

    enum Particles {
        static let spawnRate: Double = 4.0  // particles per second
        static let minLifetime: Double = 2.0
        static let maxLifetime: Double = 4.0
        static let minSize: CGFloat = 2.0
        static let maxSize: CGFloat = 6.0
        static let driftSpeed: CGFloat = 30.0
        static let wobbleAmplitude: CGFloat = 15.0
    }

    // MARK: - Glow Layer Constants

    enum Glow {
        static let primaryBlur: CGFloat = 60.0
        static let primaryOpacity: Double = 0.40
        static let secondaryBlur: CGFloat = 160.0
        static let secondaryOpacity: Double = 0.15
        static let ambientBlur: CGFloat = 240.0
        static let ambientOpacity: Double = 0.05
    }

    // MARK: - Control Overlay Constants

    enum Overlay {
        static let dimOpacity: Double = 0.70
        static let colorRingRadius: CGFloat = 80.0
        static let colorOrbSize: CGFloat = 56.0
        static let touchTargetSize: CGFloat = 52.0
    }

    // MARK: - Gesture Constants

    enum Gesture {
        static let colorCycleThreshold: CGFloat = 50.0
        static let longPressDuration: Double = 0.5
        static let brightnessThresholds: [CGFloat] = [0.0, 0.25, 0.50, 0.75, 1.0]
    }
}
