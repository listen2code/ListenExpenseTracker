package com.listen.expensetracker.ui.theme

import androidx.compose.ui.graphics.Color
import com.listen.expensetracker.data.model.AccentColor

// Base Dark & Light Colors
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2C2C2C)
val DarkOnBackground = Color(0xFFE0E0E0)
val DarkOnSurface = Color(0xFFF5F5F5)

val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F3F5)
val LightOnBackground = Color(0xFF212529)
val LightOnSurface = Color(0xFF1A1A1A)

val IncomeGreen = Color(0xFF10B981)
val ExpenseRed = Color(0xFFEF4444)

fun parseHexColor(hex: String, fallback: Color = Color(0xFF10B981)): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val colorInt = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(colorInt or 0xFF00000000)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        fallback
    }
}

fun getAccentComposeColor(accent: AccentColor): Color {
    return parseHexColor(accent.colorHex)
}