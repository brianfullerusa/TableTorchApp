//
//  SplashView.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//


import SwiftUI

struct SplashView: View {
    var body: some View {
        ZStack {
            // Fixed dark navy background
            Color(red: 1/255, green: 5/255, blue: 28/255)
                .edgesIgnoringSafeArea(.all)

            VStack(spacing: 20) {
                // 128x128 Image placeholder
                Image("TableTorch_Spash")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 128, height: 128)

                // Red "X" + "Flashlights" (dark gray)
                HStack(spacing: 10) {
                    Text("X")
                        .font(.title)
                        .foregroundColor((Color(red: 128/255, green: 0/255, blue: 0/255)))

                    Text("Flashlights")
                        .font(.title)
                        .foregroundColor(.gray)
                }

                // Green checkmark + "Table Torch" (orange)
                HStack(spacing: 10) {
                    Text("✓")
                        .font(.title)
                        .foregroundColor(.green)

                    Text("Table Torch")
                        .font(.title)
                        .foregroundColor(.orange)
                }

                // Tagline in light gray
                Text("Light Your Menu")
                    .font(.title3)
                    .foregroundColor(Color(white: 0.8))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 20)
                Text("Not the entire Restaurant!")
                    .font(.title3)
                    .foregroundColor(Color(white: 0.8))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 20)
            }
        }
    }
}

struct SplashView_Previews: PreviewProvider {
    static var previews: some View {
        SplashView()
            .previewDisplayName("SplashView Preview")
    }
}
