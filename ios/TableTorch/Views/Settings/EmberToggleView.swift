//
//  EmberToggleView.swift
//  TableTorch
//
//  Custom toggle with ember glow effect
//

import SwiftUI

struct EmberToggleView: View {
    let title: String
    @Binding var isOn: Bool
    let subtitle: String?

    @State private var isAnimating: Bool = false

    init(title: String, isOn: Binding<Bool>, subtitle: String? = nil) {
        self.title = title
        self._isOn = isOn
        self.subtitle = subtitle
    }

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.body)
                    .foregroundColor(.white)

                if let subtitle = subtitle {
                    Text(subtitle)
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.6))
                }
            }

            Spacer()

            // Custom toggle
            ZStack {
                // Track background with glow
                Capsule()
                    .fill(
                        isOn ?
                        LinearGradient(
                            colors: [.orange, .red],
                            startPoint: .leading,
                            endPoint: .trailing
                        ) :
                        LinearGradient(
                            colors: [Color.white.opacity(0.2), Color.white.opacity(0.2)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(width: 51, height: 31)
                    .shadow(color: isOn ? .orange.opacity(0.5) : .clear, radius: 8)

                // Thumb
                Circle()
                    .fill(Color.white)
                    .frame(width: 27, height: 27)
                    .shadow(color: .black.opacity(0.2), radius: 2, y: 1)
                    .offset(x: isOn ? 10 : -10)
            }
            .onTapGesture {
                withAnimation(AnimationConstants.quickResponse) {
                    isOn.toggle()
                }
                HapticEngine.shared.toggleChanged()
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(title)
        .accessibilityValue(isOn ? "On" : "Off")
        .accessibilityAddTraits(.isButton)
        .accessibilityHint("Double tap to toggle")
    }
}

#Preview {
    ZStack {
        Color.black
            .ignoresSafeArea()

        VStack(spacing: 20) {
            EmberToggleView(
                title: "Enable Feature",
                isOn: .constant(true),
                subtitle: "This is a subtitle"
            )

            EmberToggleView(
                title: "Disabled Feature",
                isOn: .constant(false),
                subtitle: nil
            )
        }
        .padding()
        .glassCard(tintColor: .orange)
        .padding()
    }
}
