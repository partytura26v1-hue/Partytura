package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = StageAmber,
    onPrimary = StudioCharcoal,
    primaryContainer = StageAmberContainer,
    onPrimaryContainer = StageGoldSoft,
    secondary = MintTempo,
    onSecondary = StudioCharcoal,
    secondaryContainer = MintTempoContainer,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = MutedBrass,
    onTertiary = StudioCharcoal,
    background = StudioCharcoal,
    onBackground = WarmIvory,
    surface = StudioSurface,
    onSurface = WarmIvory,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = MutedBrass,
    surfaceContainerLowest = StudioCharcoal,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceHighest,
    outline = StudioOutline,
    outlineVariant = Color(0xFF332D27),
    error = StageCoral,
    onError = StudioCharcoal,
    errorContainer = StageCoralContainer,
    onErrorContainer = Color(0xFFFECACA)
)

private val ParchmentLightColorScheme = lightColorScheme(
    primary = AmberDeep,
    onPrimary = Color.White,
    primaryContainer = AmberLightContainer,
    onPrimaryContainer = AmberOnContainer,
    secondary = EmeraldDeep,
    onSecondary = Color.White,
    secondaryContainer = EmeraldLightContainer,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = BrassAccent,
    onTertiary = Color.White,
    background = ParchmentBg,
    onBackground = EspressoInk,
    surface = ParchmentSurface,
    onSurface = EspressoInk,
    surfaceVariant = ParchmentElevated,
    onSurfaceVariant = WarmUmber,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = ParchmentBg,
    surfaceContainer = ParchmentElevated,
    surfaceContainerHigh = Color(0xFFE7DEC9),
    outline = ParchmentOutline,
    outlineVariant = Color(0xFFE5DCCB),
    error = CrimsonLight,
    onError = Color.White
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PartyturaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else ParchmentLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
