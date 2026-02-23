//
//  EmberParticleView.swift
//  TableTorch
//
//  Canvas-based particle system for warm color ember effects
//

import SwiftUI

struct EmberParticleView: View {
    let color: Color
    let isEnabled: Bool

    @State private var particles: [Particle] = []
    @State private var lastSpawnTime: Date = Date()
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private let spawnInterval: Double = 1.0 / AnimationConstants.Particles.spawnRate

    /// Check if the color is warm (reds, oranges, yellows)
    private var isWarmColor: Bool {
        let uiColor = UIColor(color)
        var hue: CGFloat = 0
        var saturation: CGFloat = 0
        var brightness: CGFloat = 0
        uiColor.getHue(&hue, saturation: &saturation, brightness: &brightness, alpha: nil)

        // Warm colors: reds, oranges, yellows (hue 0-60 degrees and 330-360)
        let hueDegrees = hue * 360
        return saturation > 0.2 && (hueDegrees < 60 || hueDegrees > 330)
    }

    var body: some View {
        GeometryReader { geometry in
            TimelineView(.animation) { timeline in
                Canvas { context, size in
                    for particle in particles {
                        let elapsed = timeline.date.timeIntervalSince(particle.spawnTime)
                        let progress = elapsed / particle.lifetime

                        guard progress < 1.0 else { continue }

                        // Calculate position
                        let y = particle.startY - CGFloat(elapsed) * AnimationConstants.Particles.driftSpeed
                        let wobble = sin(elapsed * 3) * AnimationConstants.Particles.wobbleAmplitude
                        let x = particle.startX + wobble

                        // Calculate opacity (fade as it rises)
                        let opacity = (1.0 - progress) * 0.6

                        // Calculate size (shrink as it rises)
                        let scale = 1.0 - (progress * 0.5)
                        let size = particle.size * scale

                        // Draw the ember
                        let rect = CGRect(
                            x: x - size / 2,
                            y: y - size / 2,
                            width: size,
                            height: size
                        )

                        context.opacity = opacity
                        context.fill(
                            Circle().path(in: rect),
                            with: .color(particle.color)
                        )

                        // Add glow
                        context.opacity = opacity * 0.3
                        let glowRect = rect.insetBy(dx: -size * 0.5, dy: -size * 0.5)
                        context.fill(
                            Circle().path(in: glowRect),
                            with: .color(particle.color)
                        )
                    }
                }
                .onChange(of: timeline.date) { _ in
                    updateParticles(in: geometry.size, at: timeline.date)
                }
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
        .opacity(isEnabled && isWarmColor && !reduceMotion ? 1 : 0)
        .animation(.easeInOut(duration: 0.5), value: isEnabled && isWarmColor)
    }

    private func updateParticles(in size: CGSize, at date: Date) {
        guard isEnabled && isWarmColor && !reduceMotion else {
            particles.removeAll()
            return
        }

        // Remove expired particles
        particles.removeAll { particle in
            date.timeIntervalSince(particle.spawnTime) > particle.lifetime
        }

        // Spawn new particles
        if date.timeIntervalSince(lastSpawnTime) >= spawnInterval {
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

    /// Generate a color variation for the ember
    private var emberColor: Color {
        let uiColor = UIColor(color)
        var hue: CGFloat = 0
        var saturation: CGFloat = 0
        var brightness: CGFloat = 0
        uiColor.getHue(&hue, saturation: &saturation, brightness: &brightness, alpha: nil)

        // Slightly vary the hue and brightness
        let hueVariation = CGFloat.random(in: -0.02...0.02)
        let brightnessVariation = CGFloat.random(in: 0.8...1.2)

        return Color(
            hue: Double((hue + hueVariation).clamped(to: 0...1)),
            saturation: Double(saturation),
            brightness: Double((brightness * brightnessVariation).clamped(to: 0...1))
        )
    }
}

private struct Particle: Identifiable {
    let id = UUID()
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
        EmberParticleView(color: .orange, isEnabled: true)
    }
    .ignoresSafeArea()
}
