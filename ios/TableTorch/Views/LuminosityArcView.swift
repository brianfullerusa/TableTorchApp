//
//  LuminosityArcView.swift
//  TableTorch
//
//  Semi-circular brightness control spanning the bottom of the screen.
//  Implements a hybrid gesture system: horizontal drag maps to brightness,
//  with tap-to-set and direct thumb manipulation as secondary interactions.
//
//  Design follows iOS HIG principles:
//  - Large touch targets (entire arc region is interactive)
//  - Horizontal gesture maps to linear mental model (left=dim, right=bright)
//  - Haptic feedback at key thresholds
//  - Full accessibility support with VoiceOver and Dynamic Type
//

import SwiftUI

struct LuminosityArcView: View {
    @Binding var brightness: CGFloat
    let isEnabled: Bool
    let onBrightnessChanged: (CGFloat) -> Void

    // MARK: - State

    @State private var isDragging: Bool = false
    @State private var animatedAppearance: Bool = false
    @State private var dragStartBrightness: CGFloat = 0.0
    @State private var lastThreshold: CGFloat? = nil

    // MARK: - Layout Constants

    /// Arc width as a fraction of screen width (0.6 = 60% of screen width)
    /// Smaller arc keeps thumb closer to center display
    private let arcWidthFraction: CGFloat = 0.6

    /// Stroke width for the arc track
    private let arcStrokeWidth: CGFloat = 10

    /// Size of the draggable thumb indicator
    private let thumbDiameter: CGFloat = 28

    /// Vertical padding from screen bottom to arc center
    private let bottomInset: CGFloat = 20

    /// Height of the interactive gesture region above the arc
    private let gestureRegionHeight: CGFloat = 100

    /// Thresholds for haptic feedback
    private let hapticThresholds: [CGFloat] = [0.0, 0.25, 0.50, 0.75, 1.0]

    // MARK: - Body

    var body: some View {
        GeometryReader { geometry in
            let arcConfig = ArcConfiguration(
                screenWidth: geometry.size.width,
                arcWidthFraction: arcWidthFraction,
                viewHeight: geometry.size.height
            )

            ZStack {
                // Interactive gesture region (invisible but tappable)
                gestureRegion(config: arcConfig, in: geometry)

                // Background arc track
                arcTrack(config: arcConfig)

                // Filled arc showing current brightness level
                arcFill(config: arcConfig)

                // Draggable thumb indicator
                thumbIndicator(config: arcConfig)

                // Center content: icon and percentage
                centerDisplay(config: arcConfig)
            }
            .opacity(animatedAppearance ? 1.0 : 0.0)
            .offset(y: animatedAppearance ? 0 : 40)
            .animation(AnimationConstants.smoothTransition.delay(0.1), value: animatedAppearance)
        }
        .frame(height: calculateViewHeight())
        .onAppear { animatedAppearance = true }
        .onDisappear { animatedAppearance = false }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Brightness control")
        .accessibilityValue("\(Int(brightness * 100)) percent")
        .accessibilityHint("Swipe left or right to adjust brightness")
        .accessibilityAdjustableAction { direction in
            let step: CGFloat = 0.1
            switch direction {
            case .increment:
                brightness = min(1.0, brightness + step)
            case .decrement:
                brightness = max(0.0, brightness - step)
            @unknown default:
                break
            }
            onBrightnessChanged(brightness)
            HapticEngine.shared.selectionChanged()
        }
    }

    // MARK: - View Components

    /// Invisible region that captures gestures across the entire arc area
    @ViewBuilder
    private func gestureRegion(config: ArcConfiguration, in geometry: GeometryProxy) -> some View {
        Rectangle()
            .fill(Color.clear)
            .contentShape(Rectangle())
            .frame(
                width: geometry.size.width,
                height: config.radius + gestureRegionHeight
            )
            .position(
                x: config.arcCenter.x,
                y: geometry.size.height - (config.radius + gestureRegionHeight) / 2
            )
            .gesture(
                DragGesture(minimumDistance: 1)
                    .onChanged { value in
                        handleDrag(value: value, config: config, screenWidth: geometry.size.width)
                    }
                    .onEnded { _ in
                        handleDragEnd()
                    }
            )
            .simultaneousGesture(
                // Tap gesture for quick position setting
                TapGesture()
                    .onEnded {
                        // Tap handling is done via the drag gesture's initial position
                    }
            )
            .onTapGesture { location in
                handleTap(at: location, config: config, screenWidth: geometry.size.width)
            }
    }

    /// Background track showing the full arc path
    @ViewBuilder
    private func arcTrack(config: ArcConfiguration) -> some View {
        SemicircleArc(progress: 1.0)
            .stroke(
                Color.white.opacity(0.15),
                style: StrokeStyle(lineWidth: arcStrokeWidth, lineCap: .round)
            )
            .frame(width: config.diameter, height: config.radius)
            .position(config.arcDrawPosition)
    }

    /// Filled portion of the arc representing current brightness
    @ViewBuilder
    private func arcFill(config: ArcConfiguration) -> some View {
        SemicircleArc(progress: brightness)
            .stroke(
                brightnessGradient,
                style: StrokeStyle(lineWidth: arcStrokeWidth, lineCap: .round)
            )
            .frame(width: config.diameter, height: config.radius)
            .position(config.arcDrawPosition)
            .shadow(
                color: currentGlowColor.opacity(isDragging ? 0.6 : 0.3),
                radius: isDragging ? 16 : 8
            )
    }

    /// Thumb indicator positioned along the arc
    @ViewBuilder
    private func thumbIndicator(config: ArcConfiguration) -> some View {
        let thumbPosition = config.pointOnArc(forProgress: brightness)

        ZStack {
            // Outer glow when dragging
            Circle()
                .fill(currentGlowColor.opacity(0.3))
                .frame(width: thumbDiameter + 16, height: thumbDiameter + 16)
                .blur(radius: 8)
                .opacity(isDragging ? 1.0 : 0.0)

            // Main thumb
            Circle()
                .fill(
                    LinearGradient(
                        colors: [.white, Color.white.opacity(0.9)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .frame(width: thumbDiameter, height: thumbDiameter)
                .shadow(color: .black.opacity(0.25), radius: 4, y: 2)
                .shadow(color: currentGlowColor.opacity(0.4), radius: 8)
        }
        .scaleEffect(isDragging ? 1.15 : 1.0)
        .animation(AnimationConstants.quickResponse, value: isDragging)
        .position(thumbPosition)
    }

    /// Sun icon and percentage display in the arc's center
    @ViewBuilder
    private func centerDisplay(config: ArcConfiguration) -> some View {
        // Position display inside the bowl of the semicircle
        // (below the arc curve, above the arc center/bottom)
        let displayCenter = CGPoint(
            x: config.arcCenter.x,
            y: config.arcCenter.y - config.radius * 0.45
        )

        VStack(spacing: 4) {
            // Adaptive sun/moon icon
            sunMoonIcon
                .font(.system(size: 24, weight: .medium))
                .foregroundStyle(iconGradient)
                .modifier(PulseEffectModifier(isActive: isDragging))

            // Percentage text
            Text("\(Int(brightness * 100))%")
                .font(.system(size: 20, weight: .semibold, design: .rounded))
                .monospacedDigit()
                .foregroundColor(.white)
                .modifier(NumericTransitionModifier(value: brightness))
        }
        .position(displayCenter)
    }

    // MARK: - Computed Properties

    /// Icon changes based on brightness level
    @ViewBuilder
    private var sunMoonIcon: some View {
        Group {
            if brightness < 0.15 {
                Image(systemName: "moon.fill")
            } else if brightness < 0.35 {
                Image(systemName: "sun.min.fill")
            } else if brightness < 0.65 {
                Image(systemName: "sun.max.fill")
            } else {
                Image(systemName: "sun.max.fill")
                    .symbolRenderingMode(.hierarchical)
            }
        }
    }

    /// Gradient for the filled arc portion
    private var brightnessGradient: LinearGradient {
        LinearGradient(
            colors: [
                Color.orange.opacity(0.8),
                Color.yellow,
                Color.white
            ],
            startPoint: .leading,
            endPoint: .trailing
        )
    }

    /// Gradient for the center icon
    private var iconGradient: LinearGradient {
        LinearGradient(
            colors: brightness < 0.15
                ? [Color.blue.opacity(0.8), Color.purple.opacity(0.6)]
                : [Color.orange, Color.yellow],
            startPoint: .top,
            endPoint: .bottom
        )
    }

    /// Current accent color based on brightness
    private var currentGlowColor: Color {
        if brightness < 0.15 {
            return .blue
        } else if brightness < 0.5 {
            return .orange
        } else {
            return .yellow
        }
    }

    // MARK: - Gesture Handlers

    /// Handles drag gesture for brightness adjustment
    /// Uses HORIZONTAL delta mapping regardless of finger position on arc
    private func handleDrag(value: DragGesture.Value, config: ArcConfiguration, screenWidth: CGFloat) {
        guard isEnabled else { return }

        if !isDragging {
            // Gesture just started
            isDragging = true
            dragStartBrightness = brightness
            HapticEngine.shared.prepare()
        }

        // Map horizontal translation to brightness change
        // Full screen width = full brightness range
        let horizontalDelta = value.translation.width / screenWidth
        let newBrightness = (dragStartBrightness + horizontalDelta).clamped(to: 0...1)

        brightness = newBrightness
        checkHapticThreshold(newBrightness)
    }

    /// Handles tap to set brightness at horizontal position
    private func handleTap(at location: CGPoint, config: ArcConfiguration, screenWidth: CGFloat) {
        guard isEnabled else { return }

        // Map tap X position to brightness within the arc's horizontal span
        let arcStartX = (screenWidth - config.diameter) / 2
        let relativeX = location.x - arcStartX
        let newBrightness = (relativeX / config.diameter).clamped(to: 0...1)

        withAnimation(AnimationConstants.quickResponse) {
            brightness = newBrightness
        }

        HapticEngine.shared.brightnessThreshold()
        onBrightnessChanged(newBrightness)
    }

    /// Called when drag gesture ends
    private func handleDragEnd() {
        isDragging = false
        lastThreshold = nil
        onBrightnessChanged(brightness)
    }

    /// Triggers haptic feedback when crossing predefined thresholds
    private func checkHapticThreshold(_ value: CGFloat) {
        for threshold in hapticThresholds {
            if abs(value - threshold) < 0.02 {
                if lastThreshold != threshold {
                    lastThreshold = threshold
                    HapticEngine.shared.brightnessThreshold()
                }
                return
            }
        }
    }

    /// Calculates total view height based on arc geometry
    private func calculateViewHeight() -> CGFloat {
        // Estimate based on screen width (actual calc happens in GeometryReader)
        let estimatedRadius = (UIScreen.main.bounds.width * arcWidthFraction) / 2
        return estimatedRadius + gestureRegionHeight + bottomInset
    }
}

// MARK: - Arc Configuration

/// Computed geometry values for the arc based on screen dimensions
private struct ArcConfiguration {
    let screenWidth: CGFloat
    let arcWidthFraction: CGFloat
    let viewHeight: CGFloat

    /// Full diameter of the semicircle (fraction of screen width)
    var diameter: CGFloat {
        screenWidth * arcWidthFraction
    }

    /// Radius of the semicircle
    var radius: CGFloat {
        diameter / 2
    }

    /// Center of the semicircle arc (at the bottom of the view, horizontally centered)
    /// This is where the flat bottom of the semicircle sits
    var arcCenter: CGPoint {
        CGPoint(x: screenWidth / 2, y: viewHeight)
    }

    /// Position for drawing the arc shape frame
    /// The frame is centered here, with width=diameter and height=radius
    var arcDrawPosition: CGPoint {
        // Frame center needs to be radius/2 above the arc center
        // because SemicircleArc draws with center at rect.maxY (bottom of frame)
        CGPoint(x: screenWidth / 2, y: viewHeight - radius / 2)
    }

    /// Calculates a point on the arc for a given progress (0-1)
    /// Progress 0 = left (180 degrees), Progress 1 = right (0 degrees)
    func pointOnArc(forProgress progress: CGFloat) -> CGPoint {
        let angle = CGFloat.pi * (1 - progress) // Map 0-1 to pi-0
        return CGPoint(
            x: arcCenter.x + radius * cos(angle),
            y: arcCenter.y - radius * sin(angle)
        )
    }
}

// MARK: - Semicircle Arc Shape

/// Custom shape that draws a semicircle arc from left to right
private struct SemicircleArc: Shape {
    var progress: CGFloat

    var animatableData: CGFloat {
        get { progress }
        set { progress = newValue }
    }

    func path(in rect: CGRect) -> Path {
        var path = Path()

        // Center is at bottom-middle of the rect
        let center = CGPoint(x: rect.midX, y: rect.maxY)
        let radius = rect.width / 2

        // Start angle: 180 degrees (left side, pointing left)
        let startAngle = Angle.degrees(180)

        // End angle: calculated from progress
        // Progress 0 = still at 180 degrees
        // Progress 1 = at 0 degrees (right side)
        let endAngle = Angle.degrees(180 - (180 * Double(progress)))

        path.addArc(
            center: center,
            radius: radius,
            startAngle: startAngle,
            endAngle: endAngle,
            clockwise: true
        )

        return path
    }
}

// MARK: - iOS Version Compatibility Modifiers

/// Applies pulse effect on iOS 17+, no-op on earlier versions
private struct PulseEffectModifier: ViewModifier {
    let isActive: Bool

    func body(content: Content) -> some View {
        if #available(iOS 17.0, *) {
            content
                .symbolEffect(.pulse, options: .repeating, value: isActive)
        } else {
            content
                .opacity(isActive ? 0.8 : 1.0)
                .animation(.easeInOut(duration: 0.3).repeatForever(autoreverses: true), value: isActive)
        }
    }
}

/// Applies numeric text transition on iOS 17+, standard animation on earlier versions
private struct NumericTransitionModifier: ViewModifier {
    let value: CGFloat

    func body(content: Content) -> some View {
        if #available(iOS 17.0, *) {
            content
                .contentTransition(.numericText(value: value))
                .animation(.snappy(duration: 0.2), value: value)
        } else {
            content
                .animation(.easeInOut(duration: 0.2), value: value)
        }
    }
}

// MARK: - Comparable Extension

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self {
        min(max(self, range.lowerBound), range.upperBound)
    }
}

// MARK: - Preview

#Preview("Light Background") {
    ZStack {
        Color.orange.opacity(0.3)
            .ignoresSafeArea()

        VStack {
            Spacer()
            LuminosityArcView(
                brightness: .constant(0.65),
                isEnabled: true,
                onBrightnessChanged: { _ in }
            )
        }
    }
}

#Preview("Dark Background") {
    ZStack {
        Color.black.opacity(0.85)
            .ignoresSafeArea()

        VStack {
            Spacer()
            LuminosityArcView(
                brightness: .constant(0.25),
                isEnabled: true,
                onBrightnessChanged: { _ in }
            )
        }
    }
}

#Preview("Interactive") {
    struct PreviewWrapper: View {
        @State private var brightness: CGFloat = 0.5

        var body: some View {
            ZStack {
                Color.black.opacity(0.7)
                    .ignoresSafeArea()

                VStack {
                    Spacer()
                    LuminosityArcView(
                        brightness: $brightness,
                        isEnabled: true,
                        onBrightnessChanged: { print("Brightness: \($0)") }
                    )
                }
            }
        }
    }

    return PreviewWrapper()
}
