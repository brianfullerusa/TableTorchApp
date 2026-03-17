//
//  EmberParticleView.swift
//  TableTorch
//
//  Canvas-based particle system for warm color ember effects
//

import SwiftUI

struct EmberParticleView: View {
    let allColors: [Color]
    let selectedIndex: Int
    let isEnabled: Bool
    var particleShape: ParticleShape = .embers

    @State private var particles: [Particle] = []
    @State private var lastSpawnTime: Date = Date()
    @State private var cachedHSB: (hue: CGFloat, saturation: CGFloat, brightness: CGFloat) = (0, 0, 1)
    @State private var cycleColorIndex: Int = 0
    @State private var cycleTimer: Timer?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private let spawnInterval: Double = 1.0 / AnimationConstants.Particles.spawnRate
    private let maxParticles = 150
    private let colorCycleDuration: TimeInterval = 3.0

    /// The non-selected colors from the palette that particles cycle through
    private var cycleColors: [Color] {
        allColors.enumerated()
            .filter { $0.offset != selectedIndex }
            .map(\.element)
    }

    /// The current color used for spawning new particles
    private var currentCycleColor: Color {
        let colors = cycleColors
        guard !colors.isEmpty else { return allColors.first ?? .orange }
        return colors[cycleColorIndex % colors.count]
    }

    var body: some View {
        GeometryReader { geometry in
            TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
                Canvas { context, size in
                    // Resolve SF Symbol once, reuse for all particles
                    let resolvedSymbol: GraphicsContext.ResolvedImage?
                    if let symbolName = particleShape.sfSymbolName {
                        resolvedSymbol = context.resolve(Image(systemName: symbolName))
                    } else {
                        resolvedSymbol = nil
                    }

                    for particle in particles {
                        let elapsed = timeline.date.timeIntervalSince(particle.spawnTime)
                        let progress = elapsed / particle.lifetime

                        guard progress < 1.0 else { continue }

                        // Calculate position
                        let y = particle.startY - CGFloat(elapsed) * AnimationConstants.Particles.driftSpeed
                        let wobble = sin(elapsed * 3) * AnimationConstants.Particles.wobbleAmplitude
                        let x = particle.startX + wobble

                        // Calculate opacity (fade as it rises)
                        let opacity = (1.0 - progress) * 0.9

                        // Calculate size (shrink as it rises)
                        let scale = 1.0 - (progress * 0.5)
                        let particleSize = particle.size * scale

                        // Draw rect for the particle
                        let rect = CGRect(
                            x: x - particleSize / 2,
                            y: y - particleSize / 2,
                            width: particleSize,
                            height: particleSize
                        )

                        // Glow (larger, faded copy behind the main shape)
                        context.opacity = opacity * 0.6
                        let glowRect = rect.insetBy(dx: -particleSize * 1.0, dy: -particleSize * 1.0)
                        if var glow = resolvedSymbol {
                            glow.shading = .color(particle.color)
                            context.draw(glow, in: glowRect)
                        } else {
                            context.fill(
                                Circle().path(in: glowRect),
                                with: .color(particle.color)
                            )
                        }

                        // Draw the particle shape
                        context.opacity = opacity
                        if var main = resolvedSymbol {
                            main.shading = .color(particle.color)
                            context.draw(main, in: rect)
                        } else {
                            context.fill(
                                Circle().path(in: rect),
                                with: .color(particle.color)
                            )
                        }
                    }
                }
                .onChange(of: timeline.date) { _, newDate in
                    updateParticles(in: geometry.size, at: newDate)
                }
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
        .opacity(isEnabled && !reduceMotion ? 1 : 0)
        .animation(.easeInOut(duration: 0.5), value: isEnabled)
        .onAppear {
            cacheHSB()
            startCycleTimer()
        }
        .onDisappear { stopCycleTimer() }
        .onChange(of: currentCycleColor) { _, _ in cacheHSB() }
        .onChange(of: selectedIndex) { _, _ in
            cycleColorIndex = 0
            cacheHSB()
        }
        .onChange(of: allColors) { _, _ in
            cycleColorIndex = 0
            cacheHSB()
        }
        .onChange(of: isEnabled) { _, newValue in
            if newValue { startCycleTimer() } else { stopCycleTimer() }
        }
    }

    private func updateParticles(in size: CGSize, at date: Date) {
        guard isEnabled && !reduceMotion else {
            particles.removeAll()
            return
        }

        // Remove expired particles
        particles.removeAll { particle in
            date.timeIntervalSince(particle.spawnTime) > particle.lifetime
        }

        // Spawn new particles (capped to prevent unbounded growth)
        if date.timeIntervalSince(lastSpawnTime) >= spawnInterval && particles.count < maxParticles {
            spawnParticle(in: size, at: date)
            lastSpawnTime = date
        }
    }

    private func spawnParticle(in size: CGSize, at date: Date) {
        let particle = Particle(
            startX: CGFloat.random(in: 0...size.width),
            startY: size.height + 20,
            size: CGFloat.random(in: AnimationConstants.Particles.minSize...AnimationConstants.Particles.maxSize),
            lifetime: Double.random(in: AnimationConstants.Particles.minLifetime...AnimationConstants.Particles.maxLifetime),
            spawnTime: date,
            color: emberColor
        )
        particles.append(particle)
    }

    private func cacheHSB() {
        let uiColor = UIColor(currentCycleColor)
        var h: CGFloat = 0, s: CGFloat = 0, b: CGFloat = 0
        uiColor.getHue(&h, saturation: &s, brightness: &b, alpha: nil)
        cachedHSB = (h, s, b)
    }

    private func startCycleTimer() {
        stopCycleTimer()
        cycleTimer = Timer.scheduledTimer(withTimeInterval: colorCycleDuration, repeats: true) { _ in
            Task { @MainActor in
                let count = cycleColors.count
                guard count > 0 else { return }
                cycleColorIndex = (cycleColorIndex + 1) % count
            }
        }
    }

    private func stopCycleTimer() {
        cycleTimer?.invalidate()
        cycleTimer = nil
    }

    /// Generate a color variation for the ember using cached HSB
    private var emberColor: Color {
        let hueVariation = CGFloat.random(in: -0.02...0.02)
        let brightnessVariation = CGFloat.random(in: 0.8...1.2)

        return Color(
            hue: Double((cachedHSB.hue + hueVariation).clamped(to: 0...1)),
            saturation: Double(cachedHSB.saturation),
            brightness: Double((cachedHSB.brightness * brightnessVariation).clamped(to: 0...1))
        )
    }
}

private struct Particle {
    let startX: CGFloat
    let startY: CGFloat
    let size: CGFloat
    let lifetime: Double
    let spawnTime: Date
    let color: Color
}

private extension CGFloat {
    func clamped(to range: ClosedRange<CGFloat>) -> CGFloat {
        return Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}

#Preview {
    ZStack {
        Color.orange
        EmberParticleView(
            allColors: [.orange, .red, .yellow, .blue, .green, .purple],
            selectedIndex: 0,
            isEnabled: true,
            particleShape: .embers
        )
    }
    .ignoresSafeArea()
}
