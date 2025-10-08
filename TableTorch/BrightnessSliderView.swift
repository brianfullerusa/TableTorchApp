//
//  BrightnessSliderView.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//

import SwiftUI

struct BrightnessSliderView: View {
    @Binding var brightness: CGFloat
    var isEnabled: Bool = true

    var body: some View {
        HStack {
            Text("Brightness")
                .foregroundColor(.white)
                .font(.footnote)
            Slider(value: $brightness, in: 0...1)
                .disabled(!isEnabled)
            Text("\(Int(brightness * 100))%")
                .foregroundColor(.white)
                .font(.footnote)
        }
        .padding(.horizontal, 10)
        .opacity(isEnabled ? 1 : 0.55)
    }
}

