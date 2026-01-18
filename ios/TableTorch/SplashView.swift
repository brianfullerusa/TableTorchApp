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
            Color.black
                .ignoresSafeArea()

            VStack(spacing: 24) {
                ZStack {
                    Image("FlameLayerPrimary")
                        .resizable()
                        .scaledToFit()

                }
                .frame(width: 160, height: 160)
                .accessibilityHidden(true)

                Text("Table Torch")
                    .font(.system(size: 34, weight: .semibold, design: .rounded))
                    .foregroundColor(.white)
                    .tracking(0.8)
                    .accessibilityAddTraits(.isHeader)
            }
            .padding(.horizontal, 32)
        }
    }
}

struct SplashView_Previews: PreviewProvider {
    static var previews: some View {
        SplashView()
            .previewDisplayName("SplashView Preview")
    }
}
