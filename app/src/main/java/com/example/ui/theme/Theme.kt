package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun LeovanChessTheme(
    themeOption: AppThemeOption = AppThemeOption.CLASSIC_WOOD,
    content: @Composable () -> Unit
) {
    val chessColors = when (themeOption) {
        AppThemeOption.CLASSIC_WOOD -> ClassicWoodPalette
        AppThemeOption.ROYAL_BLUE -> RoyalBluePalette
        AppThemeOption.MONOCHROME -> MonochromePalette
    }

    val materialColorScheme = darkColorScheme(
        primary = chessColors.accentPrimary,
        onPrimary = chessColors.backgroundPrimary,
        secondary = chessColors.accentSecondary,
        onSecondary = chessColors.backgroundPrimary,
        background = chessColors.backgroundPrimary,
        onBackground = chessColors.textPrimary,
        surface = chessColors.surfaceCard,
        onSurface = chessColors.textPrimary,
        surfaceVariant = chessColors.surfaceElevated,
        onSurfaceVariant = chessColors.textSecondary,
        outline = chessColors.surfaceBorder,
        error = chessColors.statusDanger,
        onError = Color.White
    )

    CompositionLocalProvider(LocalChessThemeColors provides chessColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content
        )
    }
}
