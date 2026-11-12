package minicla03.cashmate.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import minicla03.cashmate.domain.model.AppPreferences
import minicla03.cashmate.domain.model.AppThemeMode
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ephemeral in-memory preferences store for the active session.
 * Does not persist financial data locally.
 */
@Singleton
class InMemoryUserPreferencesRepository @Inject constructor() : UserPreferencesRepository {

    private val _preferences = MutableStateFlow(AppPreferences())
    override val preferences: StateFlow<AppPreferences> = _preferences.asStateFlow()

    override fun setThemeMode(themeMode: AppThemeMode) {
        _preferences.update { it.copy(themeMode = themeMode) }
    }

    override fun setCurrency(currency: Currency) {
        _preferences.update { it.copy(currency = currency) }
    }
}

