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
                    // Update which “button” is selected
                    selectedIndex = index
                } label: {
                    if isSelected {
                        // SELECTED: Torch icon filled with the color
                        Image(systemName: "flame.fill") // or "flashlight.on.fill"
                            .resizable()
                            .scaledToFit()
                            .foregroundColor(color)
                            .frame(width: 40, height: 40)
                    } else {
                        // UNSELECTED: Torch icon primarily black with a colored outline
                        ZStack {
                            // Base icon in black
                            Image(systemName: "flame") //or "flashlight.off.fill
                                .resizable()
                                .scaledToFit()
                                .foregroundColor(.black)
                                .frame(width: 40, height: 40)

                            // Overlaid icon that “bleeds” around edges to create a color outline
                            Image(systemName: "flame")
                                .resizable()
                                .scaledToFit()
                                .foregroundColor(color)
                                .frame(width: 40, height: 40)
                                // The mask trick tries to show only a subtle outline
                                .mask(
                                    Image(systemName: "flame")
                                        .resizable()
                                        .scaledToFit()
                                        .blur(radius: 1.0)
                                )
                        }
                    }
                }
            }
        }
    }
}


