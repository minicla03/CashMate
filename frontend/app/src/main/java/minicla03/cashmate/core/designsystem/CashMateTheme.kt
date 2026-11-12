package minicla03.cashmate.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import minicla03.cashmate.domain.model.AppThemeMode

object CashMateTheme {
    val semanticColors: CashMateSemanticColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCashMateSemanticColors.current

    val spacing: CashMateSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalCashMateSpacing.current
}

@Composable
fun CashMateTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (useDarkTheme) CashMateDarkColorScheme else CashMateLightColorScheme
    val semanticColors = if (useDarkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(
        LocalCashMateSemanticColors provides semanticColors,
        LocalCashMateSpacing provides CashMateSpacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CashMateTypography,
            shapes = CashMateShapes,
            content = content
        )
    }
}

