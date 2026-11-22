package com.uth.proyectofinal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Light = lightColorScheme(
    primary = Color(0xFF1B7F5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F2E5),
    onPrimaryContainer = Color(0xFF00281A),
    secondary = Color(0xFF4F6358),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4ECE7),
    onSecondaryContainer = Color(0xFF1C2A23),
    tertiary = Color(0xFFB7791F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEBC7),
    onTertiaryContainer = Color(0xFF3A2400),
    background = Color(0xFFF2F5F3),
    onBackground = Color(0xFF17211C),
    surface = Color.White,
    onSurface = Color(0xFF17211C),
    surfaceVariant = Color(0xFFEEF3F0),
    onSurfaceVariant = Color(0xFF5B6861),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FBFA),
    surfaceContainer = Color(0xFFF2F6F4),
    surfaceContainerHigh = Color(0xFFECF1EE),
    surfaceContainerHighest = Color.White,
    outline = Color(0xFFB9C6BF),
    outlineVariant = Color(0xFFE1E8E4),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFDE7E7),
    onErrorContainer = Color(0xFF5F1111),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7DD8B2),
    onPrimary = Color(0xFF00382A),
    primaryContainer = Color(0xFF0E5A41),
    onPrimaryContainer = Color(0xFFD5F2E5),
    secondary = Color(0xFFB4CCBF),
    onSecondary = Color(0xFF1F352A),
    secondaryContainer = Color(0xFF2A3A32),
    onSecondaryContainer = Color(0xFFD0E8DA),
    tertiary = Color(0xFFF2C27A),
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF5C3F0A),
    onTertiaryContainer = Color(0xFFFFEBC7),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDDE5E0),
    surface = Color(0xFF151C18),
    onSurface = Color(0xFFDDE5E0),
    surfaceVariant = Color(0xFF1E2823),
    onSurfaceVariant = Color(0xFFA9B8B0),
    surfaceContainerLowest = Color(0xFF0B100D),
    surfaceContainerLow = Color(0xFF151C18),
    surfaceContainer = Color(0xFF19211C),
    surfaceContainerHigh = Color(0xFF212A25),
    surfaceContainerHighest = Color(0xFF2A342F),
    outline = Color(0xFF55635B),
    outlineVariant = Color(0xFF2C3731),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5F1111),
    onErrorContainer = Color(0xFFFDE7E7),
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val Base = Typography()

private val Tipografia = Base.copy(
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun PetCareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        shapes = Formas,
        typography = Tipografia,
        content = content,
    )
}
