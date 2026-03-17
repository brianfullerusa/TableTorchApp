//
//  SplashView.swift
//  TableTorch
//
//  Cinematic launch sequence: a torch ignited from darkness.
//  The flame emerges as a spark, grows organically, and continuously
//  flickers using layered animations on the two flame assets.
//

import SwiftUI

// MARK: - SplashView

struct SplashView: View {
    let onComplete: () -> Void

    // MARK: Phase state

    /// Master elapsed time drives the entire sequence.
    @State private var startDate: Date?

    // Phase triggers (set once by dispatched timers)
    @State private var sparkVisible = false
    @State private var sparkFlashed = false
    @State private var flameRevealed = false
    @State private var flameLive = false
    @State private var titleRevealed = false
    @State private var completed = false

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private typealias S = AnimationConstants.Splash

    // MARK: - Body

    private static let isScreenshotSplash = ProcessInfo.processInfo.arguments.contains("-uiScreenshotSplash")

    var body: some View {
        ZStack {
            // Layer 0: Pure black background
            Color.black
                .ignoresSafeArea()

            // Layer 1: Warm ambient glow radiating outward
            ambientGlowLayer

            // Layer 2: Flame + Title centered together
            VStack(spacing: 16) {
                flameAssembly

                titleView
            }
            // Offset the group slightly above true center for visual balance
            .offset(y: -20)
        }
        .onAppear {
            if Self.isScreenshotSplash || reduceMotion {
                showEverythingImmediately()
            } else {
                beginIgnitionSequence()
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Table Torch loading")
    }

    // MARK: - Ambient Glow Layer

    /// A multi-stop radial glow that builds as the flame grows,
    /// simulating warm light cast onto the surrounding darkness.
    private var ambientGlowLayer: some View {
        let glowOpacity: Double = flameRevealed ? 1.0 : (sparkVisible ? 0.3 : 0.0)

        return ZStack {
            // Inner warm glow (bright, tight)
            RadialGradient(
                colors: [
                    Color.orange.opacity(0.25),
                    Color.orange.opacity(0.08),
                    Color.clear
                ],
                center: .center,
                startRadius: 10,
                endRadius: 200
            )
            .scaleEffect(flameRevealed ? 1.6 : 0.5)
            .opacity(glowOpacity)

            // Outer diffuse glow (wide, subtle)
            RadialGradient(
                colors: [
                    Color(red: 1.0, green: 0.6, blue: 0.1).opacity(0.10),
                    Color(red: 1.0, green: 0.4, blue: 0.0).opacity(0.03),
                    Color.clear
                ],
                center: .center,
                startRadius: 30,
                endRadius: 400
            )
            .scaleEffect(flameRevealed ? 1.4 : 0.3)
            .opacity(glowOpacity * 0.7)
        }
        .ignoresSafeArea()
        .animation(.easeOut(duration: S.glowBuildDuration), value: sparkVisible)
        .animation(.easeOut(duration: S.glowBuildDuration), value: flameRevealed)
        .allowsHitTesting(false)
    }

    // MARK: - Flame Assembly

    /// Central visual: spark point that grows into layered flame images.
    private var flameAssembly: some View {
        ZStack {
            // Spark point: a tiny bright circle that appears first
            sparkPoint

            // Rising spark particles during ignition
            if sparkVisible && !completed {
                sparkParticlesLayer
            }

            // Primary flame layer (outer orange/yellow flame)
            flameLayerPrimary

            // Secondary flame layer (inner bright wisp)
            flameLayerSecondary
        }
        .frame(width: S.flameWidth * 1.5, height: S.flameHeight * 1.5)
        .accessibilityHidden(true)
    }

    // MARK: Spark Point

    /// A concentrated point of light -- the initial ignition source.
    /// Starts as a pinpoint, flashes bright, then persists as the
    /// flame's hot core until the full flame image takes over.
    private var sparkPoint: some View {
        let baseSize: CGFloat = sparkFlashed ? 16 : 3
        let flashScale: CGFloat = sparkFlashed ? 1.0 : 0.3
        let coreOpacity: Double = sparkVisible ? (flameRevealed ? 0.0 : 1.0) : 0.0

        return Circle()
            .fill(
                RadialGradient(
                    colors: [
                        .white,
                        Color(red: 1.0, green: 0.95, blue: 0.7),
                        Color(red: 1.0, green: 0.7, blue: 0.2),
                        .clear
                    ],
                    center: .center,
                    startRadius: 0,
                    endRadius: baseSize * 0.5
                )
            )
            .frame(width: baseSize, height: baseSize)
            .scaleEffect(flashScale)
            .opacity(coreOpacity)
            .blur(radius: sparkFlashed ? 2 : 0.5)
            // Position at the base-center of the flame area
            .offset(y: S.flameHeight * 0.25)
            .animation(.easeOut(duration: S.sparkFlashDuration), value: sparkFlashed)
            .animation(.easeOut(duration: S.sparkGrowDuration), value: sparkVisible)
            .animation(.easeOut(duration: S.flameGrowDuration * 0.5), value: flameRevealed)
    }

    // MARK: Spark Particles

    /// Small bright dots that shoot upward and outward during the
    /// spark-to-flame transition, like sparks from striking a match.
    private var sparkParticlesLayer: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
            let elapsed = timeline.date.timeIntervalSince(startDate ?? timeline.date)
            Canvas { context, size in
                drawSparkParticles(context: &context, size: size, elapsed: elapsed)
            }
        }
        .opacity(sparkVisible && !flameRevealed ? 1.0 : 0.0)
        .animation(.easeOut(duration: 0.4), value: flameRevealed)
    }

    /// Draw individual spark particles on the Canvas.
    private func drawSparkParticles(
        context: inout GraphicsContext,
        size: CGSize,
        elapsed: Double
    ) {
        let sparkStart = S.sparkDelay
        let sparkElapsed = elapsed - sparkStart
        guard sparkElapsed > 0 else { return }

        let centerX = size.width * 0.5
        let centerY = size.height * 0.65 // base of flame area

        for i in 0..<S.sparkCount {
            let seed = Double(i)
            let particleAge = sparkElapsed - (seed * 0.06)
            guard particleAge > 0 && particleAge < S.sparkLifetime else { continue }

            let progress = particleAge / S.sparkLifetime

            // Each spark flies outward at a unique angle
            let angle = (seed / Double(S.sparkCount)) * .pi * 2.0
                + sin(seed * 7.3) * 0.4  // slight randomization
            let speed: CGFloat = 40.0 + CGFloat(sin(seed * 3.7)) * 20.0
            let dx = cos(angle) * speed * CGFloat(particleAge)
            let dy = sin(angle) * speed * CGFloat(particleAge)
                - CGFloat(particleAge * particleAge) * 30 // gravity pulls up slightly

            let x = centerX + dx
            let y = centerY + dy

            let opacity = (1.0 - progress) * 0.9
            let sparkSize: CGFloat = (1.0 - progress) * 3.0 + 1.0

            let rect = CGRect(
                x: x - sparkSize * 0.5,
                y: y - sparkSize * 0.5,
                width: sparkSize,
                height: sparkSize
            )

            // Bright core
            context.opacity = opacity
            context.fill(
                Circle().path(in: rect),
                with: .color(Color(red: 1.0, green: 0.9, blue: 0.6))
            )

            // Soft glow around each spark
            let glowRect = rect.insetBy(dx: -sparkSize, dy: -sparkSize)
            context.opacity = opacity * 0.3
            context.fill(
                Circle().path(in: glowRect),
                with: .color(.orange)
            )
        }
    }

    // MARK: Primary Flame Layer

    /// The main flame image, grown from a seed point at its base to
    /// full size. The scale-from-bottom approach preserves the flame's
    /// natural tapered tip at every stage of the reveal, unlike a
    /// rectangular mask which would clip it to a flat top edge.
    /// Once fully grown, continuous flicker keeps the flame alive.
    private var flameLayerPrimary: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
            let elapsed = timeline.date.timeIntervalSince(startDate ?? timeline.date)
            let flicker = flameLive ? computeFlicker(elapsed: elapsed, offset: 0) : FlickerState.zero

            Image("FlameLayerPrimary")
                .resizable()
                .scaledToFit()
                .frame(width: S.flameWidth, height: S.flameHeight)
                // Growth: scale from tiny seed at base to full size
                .scaleEffect(
                    x: (flameRevealed ? 1.0 : 0.1) + flicker.scaleX,
                    y: (flameRevealed ? 1.0 : 0.05) + flicker.scaleY,
                    anchor: .bottom
                )
                .rotationEffect(.degrees(flicker.rotation), anchor: .bottom)
                .offset(x: flicker.offsetX, y: flicker.offsetY)
                .opacity(flameRevealed ? 1.0 : 0.0)
                .animation(
                    .easeOut(duration: S.flameGrowDuration),
                    value: flameRevealed
                )
        }
    }

    // MARK: Secondary Flame Layer

    /// The inner bright wisp, animated with a phase offset from the
    /// primary layer to create depth and parallax between the outer
    /// and inner flame forms.
    private var flameLayerSecondary: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
            let elapsed = timeline.date.timeIntervalSince(startDate ?? timeline.date)
            let flicker = flameLive ? computeFlicker(
                elapsed: elapsed,
                offset: S.innerFlamePhaseOffset
            ) : FlickerState.zero

            Image("FlameLayerSecondary")
                .resizable()
                .scaledToFit()
                .frame(width: S.flameWidth * 0.55, height: S.flameHeight * 0.65)
                .scaleEffect(
                    x: (flameRevealed ? 1.0 : 0.15) + flicker.scaleX * 1.3,
                    y: (flameRevealed ? 1.0 : 0.08) + flicker.scaleY * 0.8,
                    anchor: .bottom
                )
                .rotationEffect(.degrees(flicker.rotation * 1.2), anchor: .bottom)
                .offset(x: flicker.offsetX * 1.1, y: flicker.offsetY + 4)
                .opacity(flameRevealed ? 0.85 : 0.0)
                .blendMode(.screen)
                .animation(
                    .easeOut(duration: S.flameGrowDuration * 1.1),
                    value: flameRevealed
                )
        }
    }

    // MARK: - Title View

    /// The app name, revealed as though lit by the flame above.
    /// Uses a warm gradient overlay that mimics firelight illumination,
    /// fading from bright gold at the top to deeper amber at the bottom.
    private var titleView: some View {
        Text("Table Torch")
            .font(.system(size: 36, weight: .bold, design: .default))
            .foregroundColor(.clear)
            .overlay(
                LinearGradient(
                    colors: [
                        Color(red: 1.0, green: 0.95, blue: 0.85),
                        Color(red: 1.0, green: 0.8, blue: 0.5)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .mask(
                    Text("Table Torch")
                        .font(.system(size: 36, weight: .bold, design: .default))
                )
            )
            .opacity(titleRevealed ? 1.0 : 0.0)
            .scaleEffect(titleRevealed ? 1.0 : 0.95)
            .animation(
                .easeOut(duration: S.titleFadeDuration),
                value: titleRevealed
            )
            .accessibilityAddTraits(.isHeader)
    }

    // MARK: - Flicker Engine

    /// Describes the instantaneous deviation of a flame layer from its
    /// resting pose. Multiple sine waves at irrational frequency ratios
    /// prevent the motion from ever visibly looping.
    private struct FlickerState {
        var scaleX: CGFloat
        var scaleY: CGFloat
        var rotation: Double
        var offsetX: CGFloat
        var offsetY: CGFloat

        static let zero = FlickerState(
            scaleX: 0, scaleY: 0, rotation: 0, offsetX: 0, offsetY: 0
        )
    }

    /// Compute flicker values from elapsed time.
    ///
    /// The flame's motion is the sum of several sine waves at
    /// incommensurate frequencies (using irrational-ish ratios).
    /// This guarantees the motion never repeats within the splash
    /// duration and reads as organic, unpredictable flicker rather
    /// than a mechanical oscillation.
    private func computeFlicker(elapsed: Double, offset: Double) -> FlickerState {
        let t = elapsed + offset

        // Frequency set: chosen so no two are integer multiples
        let f1 = 1.0 / 1.7   // ~0.588 Hz
        let f2 = 1.0 / 2.3   // ~0.435 Hz
        let f3 = 1.0 / 0.9   // ~1.111 Hz
        let f4 = 1.0 / 3.1   // ~0.323 Hz
        let f5 = 1.0 / 1.3   // ~0.769 Hz

        // Horizontal sway: the flame tip drifts left and right
        let scaleX = CGFloat(
            sin(t * f1 * .pi * 2) * 0.015
            + sin(t * f3 * .pi * 2) * 0.008
        )

        // Vertical stretch: flame height varies subtly
        let scaleY = CGFloat(
            sin(t * f2 * .pi * 2) * 0.025
            + sin(t * f5 * .pi * 2) * 0.012
        )

        // Rotation: very slight tilt, anchored at base
        let rotation =
            sin(t * f1 * .pi * 2) * 1.2
            + sin(t * f4 * .pi * 2) * 0.7

        // Horizontal drift
        let offsetX = CGFloat(
            sin(t * f2 * .pi * 2) * 1.0
            + sin(t * f5 * .pi * 2) * 0.5
        )

        // Vertical bob (very subtle)
        let offsetY = CGFloat(
            sin(t * f3 * .pi * 2) * 0.8
        )

        return FlickerState(
            scaleX: scaleX,
            scaleY: scaleY,
            rotation: rotation,
            offsetX: offsetX,
            offsetY: offsetY
        )
    }

    // MARK: - Animation Sequencing

    /// The main ignition choreography.
    ///
    /// Timeline:
    ///   0.0s - 0.4s  : Darkness. Screen is pure black. Anticipation.
    ///   0.4s          : Spark appears -- a tiny white-hot point at flame base.
    ///   0.55s         : Spark flashes brighter, particles burst outward.
    ///   0.6s - 1.0s   : Ambient glow begins building around the spark.
    ///   1.0s          : Flame images scale up from seed at base (tip preserved).
    ///   1.0s - 1.8s   : Flame grows to full height; secondary layer follows.
    ///   1.4s          : Continuous flicker animation engages (flame is "alive").
    ///   2.0s          : Title fades in with warm gradient, as if lit by flame.
    ///   2.6s          : Everything is settled; brief hold.
    ///   3.2s          : onComplete fires; parent dissolves the splash.
    private func beginIgnitionSequence() {
        let now = Date()
        startDate = now

        // --- Spark appears ---
        DispatchQueue.main.asyncAfter(deadline: .now() + S.sparkDelay) {
            withAnimation(.easeOut(duration: 0.1)) {
                sparkVisible = true
            }
        }

        // --- Spark flashes ---
        DispatchQueue.main.asyncAfter(deadline: .now() + S.sparkDelay + 0.15) {
            withAnimation(.easeOut(duration: S.sparkFlashDuration)) {
                sparkFlashed = true
            }
        }

        // --- Flame images begin revealing ---
        DispatchQueue.main.asyncAfter(deadline: .now() + S.flameGrowDelay) {
            withAnimation(.easeOut(duration: S.flameGrowDuration)) {
                flameRevealed = true
            }
        }

        // --- Enable continuous flicker ---
        DispatchQueue.main.asyncAfter(deadline: .now() + S.flameGrowDelay + 0.4) {
            flameLive = true
        }

        // --- Title reveals ---
        DispatchQueue.main.asyncAfter(deadline: .now() + S.titleDelay) {
            withAnimation(.easeOut(duration: S.titleFadeDuration)) {
                titleRevealed = true
            }
        }

        // --- Sequence complete ---
        DispatchQueue.main.asyncAfter(deadline: .now() + S.totalDuration) {
            flameLive = false
            completed = true
            onComplete()
        }
    }

    /// Accessibility: skip all animation, show final state immediately.
    private func showEverythingImmediately() {
        startDate = Date()
        sparkVisible = true
        sparkFlashed = true
        flameRevealed = true
        flameLive = false // no continuous motion
        titleRevealed = true

        DispatchQueue.main.asyncAfter(deadline: .now() + 0.8) {
            completed = true
            onComplete()
        }
    }
}

// MARK: - Preview

struct SplashView_Previews: PreviewProvider {
    static var previews: some View {
        SplashView(onComplete: {})
            .previewDisplayName("Cinematic Splash")
    }
}
