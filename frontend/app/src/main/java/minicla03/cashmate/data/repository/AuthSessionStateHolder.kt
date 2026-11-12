package minicla03.cashmate.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import minicla03.cashmate.core.network.TokenStorage
import minicla03.cashmate.domain.model.AppThemeMode
import minicla03.cashmate.domain.model.AuthenticationState
import minicla03.cashmate.domain.model.User
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the single source of truth [AuthenticationState] shared between
 * authentication and user repositories without introducing DI cycles.
 */
@Singleton
class AuthSessionStateHolder @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val _authenticationState =
        MutableStateFlow<AuthenticationState>(AuthenticationState.Loading)
    val authenticationState: StateFlow<AuthenticationState> = _authenticationState.asStateFlow()

    fun setLoading() {
        _authenticationState.value = AuthenticationState.Loading
    }

    fun setAuthenticated(user: User) {
        _authenticationState.value = AuthenticationState.Authenticated(user)
    }

    fun updateUserIfAuthenticated(user: User) {
        if (_authenticationState.value is AuthenticationState.Authenticated) {
            _authenticationState.value = AuthenticationState.Authenticated(user)
        }
    }

    fun clearSession() {
        tokenStorage.clearTokens()
        userPreferencesRepository.setThemeMode(AppThemeMode.SYSTEM)
        userPreferencesRepository.setCurrency(Currency.getInstance("EUR"))
        _authenticationState.value = AuthenticationState.Unauthenticated
    }
}

