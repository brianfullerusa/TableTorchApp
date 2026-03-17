//
//  ColorPalette.swift
//  TableTorch
//
//  Named color palette for saving and switching torch color sets
//

import SwiftUI

struct ColorPalette: Codable, Identifiable, Equatable {
    static let colorCount = 6

    let id: UUID
    var name: String
    var colors: [CodableColor] {
        didSet { colors = Self.normalized(colors) }
    }
    let isBuiltIn: Bool

    init(id: UUID, name: String, colors: [CodableColor], isBuiltIn: Bool) {
        self.id = id
        self.name = name
        self.colors = Self.normalized(colors)
        self.isBuiltIn = isBuiltIn
    }

    /// Pad or truncate to exactly `colorCount` entries.
    private static func normalized(_ colors: [CodableColor]) -> [CodableColor] {
        if colors.count == colorCount { return colors }
        if colors.count > colorCount { return Array(colors.prefix(colorCount)) }
        // Pad with white
        let filler = CodableColor(color: .white)
        return colors + Array(repeating: filler, count: colorCount - colors.count)
    }

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
        name: String(localized: "Low Light"),
        colors: AppSettings.lowLightColors.map { CodableColor(color: $0) },
        isBuiltIn: true
    )

    static let bright = ColorPalette(
        id: brightUUID,
        name: String(localized: "Bright"),
        colors: AppSettings.defaultColors.map { CodableColor(color: $0) },
        isBuiltIn: true
    )

    static let party = ColorPalette(
        id: partyUUID,
        name: String(localized: "Party"),
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
