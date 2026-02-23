//
//  ColorPalette.swift
//  TableTorch
//
//  Named color palette for saving and switching torch color sets
//

import SwiftUI

struct ColorPalette: Codable, Identifiable, Equatable {
    let id: UUID
    var name: String
    var colors: [CodableColor]   // Always exactly 6
    let isBuiltIn: Bool

    var swiftUIColors: [Color] {
        colors.map { $0.color }
    }

    func matches(colors otherColors: [Color]) -> Bool {
        guard self.colors.count == otherColors.count else { return false }
        let otherCodable = otherColors.map { CodableColor(color: $0) }
        return zip(self.colors, otherCodable).allSatisfy { a, b in
            abs(a.red - b.red) < 0.01 &&
            abs(a.green - b.green) < 0.01 &&
            abs(a.blue - b.blue) < 0.01 &&
            abs(a.alpha - b.alpha) < 0.01
        }
    }
}

// MARK: - Built-In Presets

extension ColorPalette {
    // Stable UUIDs so built-in presets are always identifiable
    private static let lowLightUUID = UUID(uuidString: "00000000-0000-0000-0000-000000000002")!
    private static let brightUUID   = UUID(uuidString: "00000000-0000-0000-0000-000000000001")!
    private static let partyUUID    = UUID(uuidString: "00000000-0000-0000-0000-000000000003")!

    static let lowLight = ColorPalette(
        id: lowLightUUID,
        name: "Low Light",
        colors: AppSettings.lowLightColors.map { CodableColor(color: $0) },
        isBuiltIn: true
    )

    static let bright = ColorPalette(
        id: brightUUID,
        name: "Bright",
        colors: AppSettings.defaultColors.map { CodableColor(color: $0) },
        isBuiltIn: true
    )

    static let party = ColorPalette(
        id: partyUUID,
        name: "Party",
        colors: AppSettings.partyColors.map { CodableColor(color: $0) },
        isBuiltIn: true
    )

    static let builtInPresets: [ColorPalette] = [lowLight, bright, party]

    var icon: String {
        switch id {
        case ColorPalette.lowLightUUID: return "moon.stars"
        case ColorPalette.brightUUID:   return "paintpalette"
        case ColorPalette.partyUUID:    return "party.popper"
        default: return "flame.fill"
        }
    }
}
