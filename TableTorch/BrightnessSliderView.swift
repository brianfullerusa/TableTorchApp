//
//  BrightnessSliderView.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//

import SwiftUI

struct BrightnessSliderView: View {
    @Binding var brightness: CGFloat

    var body: some View {
        HStack {
            Text("Brightness")
                .foregroundColor(.white)
                .font(.footnote)
            Slider(value: $brightness, in: 0...1)
            Text("\(Int(brightness * 100))%")
                .foregroundColor(.white)
                .font(.footnote)
        }
        .padding(.horizontal, 10)
    }
}


