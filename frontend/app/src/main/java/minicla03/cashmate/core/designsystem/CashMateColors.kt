package minicla03.cashmate.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val CashMateLightColorScheme = lightColorScheme(
    primary = Color(0xFF1B4965),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0E4F5),
    onPrimaryContainer = Color(0xFF0A2538),
    secondary = Color(0xFF2D6A4F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8F3DC),
    onSecondaryContainer = Color(0xFF0D3322),
    tertiary = Color(0xFF5E35B1),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDE7F6),
    onTertiaryContainer = Color(0xFF2B125E),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE9EEF4),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFB91C1C),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val CashMateDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8ECAE6),
    onPrimary = Color(0xFF0A2538),
    primaryContainer = Color(0xFF163A52),
    onPrimaryContainer = Color(0xFFD0E4F5),
    secondary = Color(0xFF74C69D),
    onSecondary = Color(0xFF0A2E1E),
    secondaryContainer = Color(0xFF1B4332),
    onSecondaryContainer = Color(0xFFD8F3DC),
    tertiary = Color(0xFFB39DDB),
    onTertiary = Color(0xFF2B125E),
    tertiaryContainer = Color(0xFF4527A0),
    onTertiaryContainer = Color(0xFFEDE7F6),
    background = Color(0xFF0F141A),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF18202A),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF232E3B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF3B4A5A),
    outlineVariant = Color(0xFF2A3644),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

@Immutable
data class CashMateSemanticColors(
    val income: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color
)

val LightSemanticColors = CashMateSemanticColors(
    income = Color(0xFF15803D),
    incomeContainer = Color(0xFFDCFCE7),
    onIncomeContainer = Color(0xFF14532D),
    expense = Color(0xFFB91C1C),
    expenseContainer = Color(0xFFFEE2E2),
    onExpenseContainer = Color(0xFF7F1D1D)
)

val DarkSemanticColors = CashMateSemanticColors(
    income = Color(0xFF4ADE80),
    incomeContainer = Color(0xFF14532D),
    onIncomeContainer = Color(0xFFDCFCE7),
    expense = Color(0xFFF87171),
    expenseContainer = Color(0xFF7F1D1D),
    onExpenseContainer = Color(0xFFFEE2E2)
)

val LocalCashMateSemanticColors = staticCompositionLocalOf { LightSemanticColors }

