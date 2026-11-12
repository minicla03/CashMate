package minicla03.cashmate.domain.model

import java.util.Currency

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppPreferences(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val currency: Currency = Currency.getInstance("EUR")
)

typealias UserPreferences = AppPreferences

