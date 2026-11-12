package minicla03.cashmate.feature.profile.preferences

import minicla03.cashmate.BuildConfig
import minicla03.cashmate.core.ui.UiError
import minicla03.cashmate.domain.model.AppThemeMode
import java.util.Currency

data class PreferencesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val selectedCurrency: Currency = Currency.getInstance("EUR"),
    val availableCurrencies: List<Currency> = SUPPORTED_CURRENCIES,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val loadError: UiError? = null,
    val actionError: UiError? = null
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

sealed interface PreferencesEvent {
    data object Refresh : PreferencesEvent
    data class ThemeSelected(val themeMode: AppThemeMode) : PreferencesEvent
    data class CurrencySelected(val currency: Currency) : PreferencesEvent
    data object DismissActionError : PreferencesEvent
}

