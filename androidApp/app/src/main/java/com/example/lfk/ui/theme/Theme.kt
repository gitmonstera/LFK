package com.example.lfk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Наши новые цвета (фиолетово-синий градиент)
val PrimaryBlue = Color(0xFF667eea)
val PrimaryPurple = Color(0xFF764ba2)

val SurfaceWhite = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF333333)
val TextSecondary = Color.Gray
val SuccessGreen = Color(0xFF4CAF50)
val WarningOrange = Color(0xFFFF9800)
val ErrorRed = Color(0xFFF44336)
val InfoBlue = Color(0xFF2196F3)

// Цвета для темной темы
val DarkSurface = Color(0xFF1E1E1E)
val DarkBackground = Color(0xFF121212)
val DarkTextPrimary = Color.White
val DarkTextSecondary = Color.LightGray
val DarkSurfaceVariant = Color(0xFF2D2D2D)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlue.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryBlue,

    secondary = PrimaryPurple,
    onSecondary = Color.White,
    secondaryContainer = PrimaryPurple.copy(alpha = 0.1f),
    onSecondaryContainer = PrimaryPurple,

    tertiary = SuccessGreen,
    onTertiary = Color.White,
    tertiaryContainer = SuccessGreen.copy(alpha = 0.1f),
    onTertiaryContainer = SuccessGreen,

    background = Color.White,
    onBackground = TextPrimary,

    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = TextSecondary,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRed.copy(alpha = 0.1f),
    onErrorContainer = ErrorRed,

    outline = TextSecondary.copy(alpha = 0.3f)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.Black,  // Черный текст на синем фоне для контраста
    primaryContainer = PrimaryBlue.copy(alpha = 0.2f),
    onPrimaryContainer = PrimaryBlue.copy(alpha = 0.9f), // Ярче для читаемости

    secondary = PrimaryPurple,
    onSecondary = Color.Black,  // Черный текст на фиолетовом фоне
    secondaryContainer = PrimaryPurple.copy(alpha = 0.2f),
    onSecondaryContainer = PrimaryPurple.copy(alpha = 0.9f),

    tertiary = SuccessGreen,
    onTertiary = Color.Black,  // Черный текст на зеленом фоне
    tertiaryContainer = SuccessGreen.copy(alpha = 0.2f),
    onTertiaryContainer = SuccessGreen.copy(alpha = 0.9f),

    background = DarkBackground,
    onBackground = DarkTextPrimary,  // Белый текст на темном фоне

    surface = DarkSurface,
    onSurface = DarkTextPrimary,  // Белый текст на поверхности
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,  // Светло-серый текст

    error = ErrorRed,
    onError = Color.Black,  // Черный текст на красном фоне
    errorContainer = ErrorRed.copy(alpha = 0.2f),
    onErrorContainer = ErrorRed.copy(alpha = 0.9f),

    outline = DarkTextSecondary.copy(alpha = 0.5f)
)

// Наши кастомные Shapes
val Shapes = androidx.compose.material3.Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Наша типография
fun Typography() = Typography(
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineSmall = androidx.compose.ui.text.TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = androidx.compose.ui.text.TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 16.sp
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 14.sp
    ),
    bodySmall = androidx.compose.ui.text.TextStyle(
        fontSize = 12.sp
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 12.sp
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontSize = 10.sp
    )
)

@Composable
fun LFKClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        shapes = Shapes,
        content = content
    )
}

// Вспомогательные функции для получения цветов из темы
val MaterialTheme.primaryGradientStart: Color
    @Composable
    get() = PrimaryBlue

val MaterialTheme.primaryGradientEnd: Color
    @Composable
    get() = PrimaryPurple

val MaterialTheme.success: Color
    @Composable
    get() = SuccessGreen

val MaterialTheme.warning: Color
    @Composable
    get() = WarningOrange

val MaterialTheme.info: Color
    @Composable
    get() = InfoBlue

val MaterialTheme.textPrimary: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkTextPrimary else TextPrimary

val MaterialTheme.textSecondary: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkTextSecondary else TextSecondary