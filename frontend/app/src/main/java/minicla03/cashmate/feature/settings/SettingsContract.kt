package minicla03.cashmate.feature.settings

import minicla03.cashmate.BuildConfig
import minicla03.cashmate.domain.model.AppThemeMode
import java.util.Currency

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val selectedCurrency: Currency = Currency.getInstance("EUR"),
    val availableCurrencies: List<Currency> = SUPPORTED_CURRENCIES,
    val appVersion: String = BuildConfig.VERSION_NAME
) {
    companion object {
        val SUPPORTED_CURRENCIES: List<Currency> = listOf(
            Currency.getInstance("EUR"),
            Currency.getInstance("USD"),
            Currency.getInstance("GBP"),
            Currency.getInstance("CHF")
        )
    }
}

sealed interface SettingsEvent {
    data class ThemeSelected(val themeMode: AppThemeMode) : SettingsEvent
    data class CurrencySelected(val currency: Currency) : SettingsEvent
    data object ManageCategoriesClick : SettingsEvent
}

sealed interface SettingsEffect {
    data object NavigateToCategories : SettingsEffect
}

