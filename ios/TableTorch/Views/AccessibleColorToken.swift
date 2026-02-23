//
//  AccessibleColorToken.swift
//  TableTorch
//
//  Color button with triple redundancy: color + pattern overlay + number badge
//

import SwiftUI

struct AccessibleColorToken: View {
    let color: Color
    let index: Int
    let isSelected: Bool
    let colorName: String
    let onTap: () -> Void

    @Environment(\.accessibilityDifferentiateWithoutColor) private var differentiateWithoutColor
    @ScaledMetric private var tokenSize: CGFloat = 56

    /// Patterns for color differentiation
    private var patternOverlay: some View {
        GeometryReader { geometry in
            let size = geometry.size

            switch index % 6 {
            case 0: // Solid - no pattern
                EmptyView()
            case 1: // Horizontal stripes
                VStack(spacing: 4) {
                    ForEach(0..<5) { _ in
                        Rectangle()
                            .fill(Color.black.opacity(0.2))
                            .frame(height: 2)
                    }
                }
            case 2: // Vertical stripes
                HStack(spacing: 4) {
                    ForEach(0..<5) { _ in
                        Rectangle()
                            .fill(Color.black.opacity(0.2))
                            .frame(width: 2)
                    }
                }
            case 3: // Diagonal stripes
                Path { path in
                    let step: CGFloat = 8
                    var x: CGFloat = -size.height
                    while x < size.width {
                        path.move(to: CGPoint(x: x, y: size.height))
                        path.addLine(to: CGPoint(x: x + size.height, y: 0))
                        x += step
                    }
                }
                .stroke(Color.black.opacity(0.2), lineWidth: 2)
            case 4: // Dots
                LazyVGrid(columns: Array(repeating: GridItem(.fixed(8)), count: 4), spacing: 4) {
                    ForEach(0..<12) { _ in
                        Circle()
                            .fill(Color.black.opacity(0.2))
                            .frame(width: 4, height: 4)
                    }
                }
            case 5: // Cross-hatch
                ZStack {
                    // Horizontal
                    VStack(spacing: 8) {
                        ForEach(0..<4) { _ in
                            Rectangle()
                                .fill(Color.black.opacity(0.15))
                                .frame(height: 1)
                        }
                    }
                    // Vertical
                    HStack(spacing: 8) {
                        ForEach(0..<4) { _ in
                            Rectangle()
                                .fill(Color.black.opacity(0.15))
                                .frame(width: 1)
                        }
                    }
                }
            default:
                EmptyView()
            }
        }
    }

    var body: some View {
        Button(action: {
            onTap()
            HapticEngine.shared.colorChanged()
        }) {
            ZStack {
                // Base color circle
                Circle()
                    .fill(color)
                    .frame(width: tokenSize, height: tokenSize)

                // Pattern overlay for accessibility
                if differentiateWithoutColor {
                    Circle()
                        .fill(Color.clear)
                        .frame(width: tokenSize, height: tokenSize)
                        .overlay(patternOverlay.clipShape(Circle()))
                }

                // Selection ring
                if isSelected {
                    Circle()
                        .stroke(Color.white, lineWidth: 3)
                        .frame(width: tokenSize + 4, height: tokenSize + 4)
                        .shadow(color: color.opacity(0.5), radius: 8)
                }

                // Number badge (always visible for accessibility)
                Text("\(index + 1)")
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
                    .shadow(color: .black.opacity(0.5), radius: 2)
            }
        }
        .buttonStyle(.plain)
        .frame(width: AnimationConstants.Overlay.touchTargetSize, height: AnimationConstants.Overlay.touchTargetSize)
        .accessibilityLabel("Color \(index + 1), \(colorName)")
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
        .accessibilityHint(isSelected ? "Currently selected" : "Double tap to select")
    }
}

/// Helper to get approximate color names for accessibility
extension Color {
    var accessibleName: String {
        let uiColor = UIColor(self)
        var hue: CGFloat = 0
        var saturation: CGFloat = 0
        var brightness: CGFloat = 0
        var alpha: CGFloat = 0
        uiColor.getHue(&hue, saturation: &saturation, brightness: &brightness, alpha: &alpha)

        // Low saturation = white/gray/black
        if saturation < 0.1 {
            if brightness > 0.9 { return "White" }
            if brightness < 0.2 { return "Black" }
            return "Gray"
        }

        // Map hue to color names
        let hueDegrees = hue * 360
        switch hueDegrees {
        case 0..<15, 345..<360: return "Red"
        case 15..<45: return "Orange"
        case 45..<75: return "Yellow"
        case 75..<150: return "Green"
        case 150..<195: return "Cyan"
        case 195..<255: return "Blue"
        case 255..<285: return "Purple"
        case 285..<345: return "Pink"
        default: return "Color"
        }
    }
}

#Preview {
    ZStack {
        Color.black
        HStack(spacing: 20) {
            AccessibleColorToken(color: .white, index: 0, isSelected: true, colorName: "White") {}
            AccessibleColorToken(color: .orange, index: 1, isSelected: false, colorName: "Orange") {}
            AccessibleColorToken(color: .red, index: 2, isSelected: false, colorName: "Red") {}
        }
    }
    .ignoresSafeArea()
}
