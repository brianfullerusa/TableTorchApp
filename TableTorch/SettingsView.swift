//
//  SettingsView.swift
//  TableTorch
//
//  Created by Brian Fuller on 1/4/25.
//


import SwiftUI

struct SettingsView: View {
    @ObservedObject var settings: AppSettings

    // For the two-column layout of torch colors
    private let gridColumns = [
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8)
    ]
    
    var body: some View {
        ZStack {
            Color.black
                .edgesIgnoringSafeArea(.all)

            Form {
                // 1) Button Colors
                Section(header:
                    Text("Torch Colors")
                        .font(.title2)
                        .fontWeight(.semibold)
                        .foregroundColor(.orange)
                ) {
                    LazyVGrid(columns: gridColumns, spacing: 8) {
                        ForEach(0..<settings.selectedColors.count, id: \.self) { index in
                            // Show Torch # + flame icon
                            //FlameColorPicker(label: "Torch \(index + 1)", color: $settings.selectedColors[index])
                            FlameColorPicker(label: "\(index + 1)", color: $settings.selectedColors[index])
                        }
                    }
                    
                    
                    // Add a "Restore Defaults" button here
                    Button("Restore Default Colors") {
                        // This resets the colors to your original defaults
                        //settings.selectedColors = [.white, .blue, .green, .red]
                        settings.selectedColors = [(Color(red: 255/255, green: 255/255, blue: 255/255)), // white
                                                   (Color(red: 255/255, green: 200/255, blue: 150/255)), //soft white
                                                   (Color(red: 152/255, green: 255/255, blue: 152/255)), //mint green
                                                   (Color(red: 70/255, green: 130/255, blue: 180/255)), //steel blue
                                                   //(Color(red: 255/255, green: 100/255, blue: 0/255)), //orange
                                                   (Color(red: 255/255, green: 0/255, blue: 0/255)), //red
                                                   (Color(red: 128/255, green: 0/255, blue: 0/255))] //dark red
                    }
                    .foregroundColor(.blue)
                }
                .listRowBackground(Color.black)

                // Brightness + toggle for "Use Default Brightness on Launch"
                Section(header:
                    Text("Brightness Settings")
                    .font(.title2)
                    .fontWeight(.semibold)
                        .foregroundColor(.orange)
                ) {
                    HStack {
                        Slider(value: $settings.defaultBrightness, in: 0...1, step: 0.01)
                        Text("\(Int(settings.defaultBrightness * 100))%")
                            .foregroundColor(.white)
                    }
                    .listRowBackground(Color.black)

                    Toggle("Use Default Brightness on Launch", isOn: $settings.useDefaultBrightnessOnAppear)
                        .toggleStyle(SwitchToggleStyle(tint: .blue))
                        .listRowBackground(Color.black)
                    
                    Toggle("Prevent Screen Lock", isOn: $settings.preventScreenLock)
                        .toggleStyle(SwitchToggleStyle(tint: .blue))
                    
                    Text("When enabled, Table Torch will keep the screen on and prevent auto-lock.")
                        .foregroundColor(.white)
                        .font(.footnote)
                }
                .listRowBackground(Color.black)
               
                // 3) Angle-Based Brightness
                Section(header:
                    Text("Tilt Brightness Control")
                        .font(.title2)
                        .fontWeight(.semibold)
                        .foregroundColor(.orange)
                ) {
                    Toggle("Enable Tilt Brightness Control", isOn: $settings.isAngleBasedBrightnessActive)
                        .toggleStyle(SwitchToggleStyle(tint: .blue))
                    Text("Tilt phone:\nVertical=30% brightness,\nFlat=100% brightness")
                        .foregroundColor(.white)
                        .font(.footnote)
                }
                .listRowBackground(Color.black)
            }
            .scrollContentBackground(.hidden) // iOS 16+ to remove default form background
            .navigationBarTitle("Settings", displayMode: .inline)
            .foregroundColor(.white)
        }
    }
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        SettingsView(settings: AppSettings())
            //.environment(\.locale, .init(identifier: "hrv"))
    }
}



