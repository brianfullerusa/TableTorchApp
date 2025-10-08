//
//  ColorButtonsView.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//


import SwiftUI

struct ColorButtonsView: View {
    @Binding var selectedIndex: Int
    let buttonColors: [Color]

    var body: some View {
        HStack(spacing: 10) {
            ForEach(buttonColors.indices, id: \.self) { index in
                let color = buttonColors[index]
                let isSelected = (selectedIndex == index)

                Button {
                    selectedIndex = index
                } label: {
                    Image(systemName: isSelected ? "flame.fill" : "flame")
                        .resizable()
                        .scaledToFit()
                        .foregroundColor(color)
                        .frame(width: 40, height: 40)
                        .opacity(isSelected ? 1.0 : 0.5)
                        .accessibilityLabel("Color \(index + 1)")
                        .accessibilityAddTraits(isSelected ? .isSelected : [])
                }
            }
        }
    }
}

