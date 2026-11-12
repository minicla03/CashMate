package minicla03.cashmate.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.network.SessionExpiryNotifier
import minicla03.cashmate.core.network.TokenStorage
import minicla03.cashmate.data.remote.datasource.AuthRemoteDataSource
import minicla03.cashmate.data.remote.datasource.UserRemoteDataSource
import minicla03.cashmate.data.remote.dto.LoginRequest
import minicla03.cashmate.data.remote.dto.LogoutRequest
import minicla03.cashmate.data.remote.dto.PasswordResetRequestDto
import minicla03.cashmate.data.remote.dto.RefreshTokenRequest
import minicla03.cashmate.data.remote.dto.RegisterRequest
import minicla03.cashmate.data.remote.dto.ResendVerificationRequest
import minicla03.cashmate.data.remote.dto.ResetPasswordRequest
import minicla03.cashmate.data.remote.dto.VerifyEmailRequest
import minicla03.cashmate.data.remote.mapper.toDomain
import minicla03.cashmate.domain.model.AuthenticationState
import minicla03.cashmate.domain.model.EmailVerificationResult
import minicla03.cashmate.domain.model.RegistrationResult
import minicla03.cashmate.domain.model.User
import minicla03.cashmate.domain.repository.AuthRepository
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val userRemoteDataSource: UserRemoteDataSource,
    private val tokenStorage: TokenStorage,
    private val sessionStateHolder: AuthSessionStateHolder,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val sessionExpiryNotifier: SessionExpiryNotifier,
    private val safeRemoteCall: SafeRemoteCall
) : AuthRepository {

    override val authenticationState: StateFlow<AuthenticationState> =
        sessionStateHolder.authenticationState

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            sessionExpiryNotifier.sessionExpired.collect {
                sessionStateHolder.clearSession()
            }
        }
    }

    override suspend fun restoreSession() {
        val accessToken = tokenStorage.getAccessToken()
        val refreshToken = tokenStorage.getRefreshToken()
        if (accessToken.isNullOrBlank() && refreshToken.isNullOrBlank()) {
            sessionStateHolder.clearSession()
            return
        }

        sessionStateHolder.setLoading()

        when (val userResult = safeRemoteCall { userRemoteDataSource.getCurrentUser().toDomain() }) {
            is AppResult.Success -> {
                syncPreferencesBestEffort()
                sessionStateHolder.setAuthenticated(userResult.data)
            }
            is AppResult.Error -> {
                if (userResult.error == AppError.Network.Unauthorized && !refreshToken.isNullOrBlank()) {
                    when (refreshSession()) {
                        is AppResult.Success -> {
                            when (val retried = safeRemoteCall { userRemoteDataSource.getCurrentUser().toDomain() }) {
                                is AppResult.Success -> {
                                    syncPreferencesBestEffort()
                                    sessionStateHolder.setAuthenticated(retried.data)
                                }
                                is AppResult.Error -> sessionStateHolder.clearSession()
                            }
                        }
                        is AppResult.Error -> sessionStateHolder.clearSession()
                    }
                } else {
                    sessionStateHolder.clearSession()
                }
            }
        }
    }

    override suspend fun login(email: String, password: String): AppResult<User> {
        val result = safeRemoteCall {
            authRemoteDataSource.login(LoginRequest(email = email, password = password))
        }
        return when (result) {
            is AppResult.Success -> {
                val tokens = result.data.tokens
                tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken)
                val user = result.data.user.toDomain()
                syncPreferencesBestEffort()
                sessionStateHolder.setAuthenticated(user)
                AppResult.Success(user)
            }
            is AppResult.Error -> result
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String
    ): AppResult<RegistrationResult> {
        val result = safeRemoteCall {
            authRemoteDataSource.register(
                RegisterRequest(name = name, email = email, password = password)
            )
        }
        return when (result) {
            is AppResult.Success -> {
                val dto = result.data
                val domain = dto.toDomain()
                if (!domain.requiresEmailVerification && dto.tokens != null) {
                    tokenStorage.saveTokens(dto.tokens.accessToken, dto.tokens.refreshToken)
                    syncPreferencesBestEffort()
                    sessionStateHolder.setAuthenticated(domain.user)
                }
                AppResult.Success(domain)
            }
            is AppResult.Error -> result
        }
    }

    override suspend fun logout(): AppResult<Unit> {
        val refreshToken = tokenStorage.getRefreshToken()
        safeRemoteCall {
            authRemoteDataSource.logout(LogoutRequest(refreshToken = refreshToken))
        }
        sessionStateHolder.clearSession()
        return AppResult.Success(Unit)
    }

    override suspend fun refreshSession(): AppResult<Unit> {
        val refreshToken = tokenStorage.getRefreshToken()?.takeIf { it.isNotBlank() }
        if (refreshToken == null) {
            sessionStateHolder.clearSession()
            return AppResult.Error(AppError.Auth.SessionExpired)
        }
        return when (val result = safeRemoteCall {
            authRemoteDataSource.refreshToken(RefreshTokenRequest(refreshToken))
        }) {
            is AppResult.Success -> {
                tokenStorage.saveTokens(result.data.accessToken, result.data.refreshToken)
                AppResult.Success(Unit)
            }
            is AppResult.Error -> {
                sessionStateHolder.clearSession()
                AppResult.Error(AppError.Auth.SessionExpired)
            }
        }
    }

    override suspend fun verifyEmail(
        email: String,
        token: String
    ): AppResult<EmailVerificationResult> = safeRemoteCall {
        authRemoteDataSource.verifyEmail(VerifyEmailRequest(email = email, token = token)).toDomain()
    }

    override suspend fun resendVerificationEmail(email: String): AppResult<Unit> = safeRemoteCall {
        authRemoteDataSource.resendVerificationEmail(ResendVerificationRequest(email = email))
    }

    override suspend fun requestPasswordReset(email: String): AppResult<Unit> = safeRemoteCall {
        authRemoteDataSource.requestPasswordReset(PasswordResetRequestDto(email = email))
    }

    override suspend fun resetPassword(
        email: String,
        token: String,
        newPassword: String
    ): AppResult<Unit> = safeRemoteCall {
        authRemoteDataSource.resetPassword(
            ResetPasswordRequest(email = email, token = token, newPassword = newPassword)
        )
    }

    override fun clearSessionLocally() {
        sessionStateHolder.clearSession()
    }

    private suspend fun syncPreferencesBestEffort() {
        val prefsResult = safeRemoteCall { userRemoteDataSource.getPreferences().toDomain() }
        if (prefsResult is AppResult.Success) {
            userPreferencesRepository.setThemeMode(prefsResult.data.themeMode)
            userPreferencesRepository.setCurrency(prefsResult.data.currency)
        }
    }
}

