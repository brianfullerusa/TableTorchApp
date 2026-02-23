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
    var particleShape: ParticleShape = .embers

    @State private var particles: [Particle] = []
    @State private var lastSpawnTime: Date = Date()
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private let spawnInterval: Double = 1.0 / AnimationConstants.Particles.spawnRate

    /// Particles show on all colors
    private var isActiveColor: Bool {
        true
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
                        if let symbolName = particleShape.sfSymbolName {
                            var glowResolved = context.resolve(Image(systemName: symbolName))
                            glowResolved.shading = .color(particle.color)
                            context.draw(glowResolved, in: glowRect)
                        } else {
                            context.fill(
                                Circle().path(in: glowRect),
                                with: .color(particle.color)
                            )
                        }

                        // Draw the particle shape
                        context.opacity = opacity
                        if let symbolName = particleShape.sfSymbolName {
                            var resolved = context.resolve(Image(systemName: symbolName))
                            resolved.shading = .color(particle.color)
                            context.draw(resolved, in: rect)
                        } else {
                            context.fill(
                                Circle().path(in: rect),
                                with: .color(particle.color)
                            )
                        }
                    }
                }
                .onChange(of: timeline.date) { _ in
                    updateParticles(in: geometry.size, at: timeline.date)
                }
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
        .opacity(isEnabled && isActiveColor && !reduceMotion ? 1 : 0)
        .animation(.easeInOut(duration: 0.5), value: isEnabled && isActiveColor)
    }

    private func updateParticles(in size: CGSize, at date: Date) {
        guard isEnabled && isActiveColor && !reduceMotion else {
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
        EmberParticleView(color: .orange, isEnabled: true, particleShape: .embers)
    }
    .ignoresSafeArea()
}
