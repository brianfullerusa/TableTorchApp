//
//  ParticleShape.swift
//  TableTorch
//
//  Selectable particle shapes for the ember particle system
//

import Foundation

enum ParticleShape: String, CaseIterable, Identifiable {
    case embers, hearts, stars, snowflakes, musicNotes

    var id: String { rawValue }

    var displayName: String {
        switch self {
        case .embers:     return "Embers"
        case .hearts:     return "Hearts"
        case .stars:      return "Stars"
        case .snowflakes: return "Snowflakes"
        case .musicNotes: return "Music Notes"
        }
    }

    /// SF Symbol name used to draw the particle on the Canvas.
    /// `nil` for embers, which are drawn as filled circles.
    var sfSymbolName: String? {
        switch self {
        case .embers:     return nil
        case .hearts:     return "heart.fill"
        case .stars:      return "star.fill"
        case .snowflakes: return "snowflake"
        case .musicNotes: return "music.note"
        }
    }

    /// SF Symbol shown in the settings picker row.
    var pickerSymbolName: String {
        switch self {
        case .embers:     return "flame.fill"
        case .hearts:     return "heart.fill"
        case .stars:      return "star.fill"
        case .snowflakes: return "snowflake"
        case .musicNotes: return "music.note"
        }
    }
}
