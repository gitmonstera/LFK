package com.example.lf.ui.theme

import androidx.compose.ui.graphics.Color

// Основные цвета
val Primary = Color(0xFF6366F1)
val PrimaryDark = Color(0xFF4F46E5)
val PrimaryLight = Color(0xFF818CF8)
val Secondary = Color(0xFFEC4899)
val SecondaryDark = Color(0xFFDB2777)
val Tertiary = Color(0xFF06B6D4)
val TertiaryDark = Color(0xFF0891B2)

// Фоновые цвета
val BackgroundDark = Color(0xFF0F0F1A)
val BackgroundCard = Color(0xFF1A1A2E)
val BackgroundSurface = Color(0xFF16213E)

// Акценты
val Success = Color(0xFF10B981)
val Error = Color(0xFFEF4444)
val Warning = Color(0xFFF59E0B)
val Info = Color(0xFF3B82F6)

// Градиенты
val GradientPrimary = listOf(Primary, PrimaryDark)
val GradientSecondary = listOf(Secondary, SecondaryDark)
val GradientSuccess = listOf(Success, Color(0xFF059669))
val GradientBg = listOf(BackgroundDark, Color(0xFF0A0A15))

// Прозрачности
val SurfaceTransparent = BackgroundCard.copy(alpha = 0.8f)
val GlassEffect = Color(0x1AFFFFFF)

// Текст
val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0xFF9CA3AF)
val TextHint = Color(0xFF6B7280)