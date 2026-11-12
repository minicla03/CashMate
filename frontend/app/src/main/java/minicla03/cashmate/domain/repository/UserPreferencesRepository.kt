package minicla03.cashmate.domain.repository

import kotlinx.coroutines.flow.StateFlow
import minicla03.cashmate.domain.model.AppPreferences
import minicla03.cashmate.domain.model.AppThemeMode
import java.util.Currency

/**
 * Holds ephemeral in-memory session preferences (theme & currency).
 * Financial data is never stored locally.
 */
interface UserPreferencesRepository {
    val preferences: StateFlow<AppPreferences>
    fun setThemeMode(themeMode: AppThemeMode)
    fun setCurrency(currency: Currency)
}

