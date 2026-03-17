//
//  EmberToggleView.swift
//  TableTorch
//
//  Custom toggle with ember glow effect using native Toggle + ToggleStyle
//

import SwiftUI

struct EmberToggleView: View {
    let title: LocalizedStringKey
    @Binding var isOn: Bool
    let subtitle: LocalizedStringKey?

    init(title: LocalizedStringKey, isOn: Binding<Bool>, subtitle: LocalizedStringKey? = nil) {
        self.title = title
        self._isOn = isOn
        self.subtitle = subtitle
    }

    var body: some View {
        Toggle(isOn: $isOn) {
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
        }
        .toggleStyle(EmberToggleStyle())
    }
}

// MARK: - Custom Toggle Style

private struct EmberToggleStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack {
            configuration.label

            Spacer()

            ZStack {
                // Track background with glow
                Capsule()
                    .fill(
                        configuration.isOn ?
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
                    .shadow(color: configuration.isOn ? .orange.opacity(0.5) : .clear, radius: 8)

                // Thumb
                Circle()
                    .fill(Color.white)
                    .frame(width: 27, height: 27)
                    .shadow(color: .black.opacity(0.2), radius: 2, y: 1)
                    .offset(x: configuration.isOn ? 10 : -10)
            }
            .animation(AnimationConstants.quickResponse, value: configuration.isOn)
        }
        .contentShape(Rectangle())
        .onTapGesture {
            configuration.isOn.toggle()
            HapticEngine.shared.toggleChanged()
        }
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
