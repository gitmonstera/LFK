package com.example.lfk.ui.theme

import androidx.compose.ui.graphics.Color

object ColorUtils {

    fun parseCategoryColor(colorString: String?): Color {
        return when (colorString?.lowercase()) {
            "red" -> Color(0xFFF44336)
            "pink" -> Color(0xFFE91E63)
            "purple" -> Color(0xFF9C27B0)
            "deep_purple" -> Color(0xFF673AB7)
            "indigo" -> Color(0xFF3F51B5)
            "blue" -> Color(0xFF2196F3)
            "light_blue" -> Color(0xFF03A9F4)
            "cyan" -> Color(0xFF00BCD4)
            "teal" -> Color(0xFF009688)
            "green" -> Color(0xFF4CAF50)
            "light_green" -> Color(0xFF8BC34A)
            "lime" -> Color(0xFFCDDC39)
            "yellow" -> Color(0xFFFFEB3B)
            "amber" -> Color(0xFFFFC107)
            "orange" -> Color(0xFFFF9800)
            "deep_orange" -> Color(0xFFFF5722)
            "brown" -> Color(0xFF795548)
            "grey", "gray" -> Color(0xFF9E9E9E)
            "blue_grey", "blue_gray" -> Color(0xFF607D8B)
            else -> Color(0xFF6200EE) // По умолчанию фиолетовый
        }
    }

    fun getDifficultyColor(level: Int): Color {
        return when (level) {
            1 -> Color(0xFF4CAF50) // Зеленый - легкий
            2 -> Color(0xFF8BC34A) // Светло-зеленый
            3 -> Color(0xFFFFC107) // Желтый - средний
            4 -> Color(0xFFFF9800) // Оранжевый
            5 -> Color(0xFFF44336) // Красный - сложный
            else -> Color(0xFF9E9E9E) // Серый
        }
    }

    fun getCategoryIcon(iconName: String?): String {
        return when (iconName) {
            "💪" -> "💪"
            "🦵" -> "🦵"
            "🧘" -> "🧘"
            "🤸" -> "🤸"
            "🏃" -> "🏃"
            "🤲" -> "🤲"
            "👊" -> "👊"
            "✋" -> "✋"
            else -> "🏋️"
        }
    }
}