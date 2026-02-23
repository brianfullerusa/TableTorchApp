//
//  FlameColorPicker.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/10/25.
//


import SwiftUI

struct FlameColorPicker: View {
    /// Text label (e.g., "Torch 1")
    let label: String
    
    
    /// The bound color selection
    @Binding var color: Color
    
    var body: some View {
        HStack(spacing: 8) {
            // The label, e.g., "Torch 1"
            Text("Torch \(label)")
                .foregroundColor(.white)
                
            
            // Overlay trick: Hide the default color well, show the flame
            ZStack {
                // The real ColorPicker, nearly transparent
                ColorPicker("", selection: $color, supportsOpacity: false)
                    .labelsHidden()
                    .frame(width: 40, height: 40)
                    .opacity(0.1)//.opacity(0.01) // hide system color well
                    .allowsHitTesting(true)     // still tap-enabled
                    .contentShape(Rectangle())  // define a tappable shape
                    .accessibilityLabel("Choose color for Torch \(label)")
                    .accessibilityHint("Double-tap to select a color")
                // The SF Symbol (flame) tinted by the bound color
                
                Image(systemName: "flame")
                    .resizable()
                    .scaledToFit()
                    .foregroundColor(color)
                    .frame(width: 24, height: 24)
                    .accessibilityHidden(true)
                
                
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
