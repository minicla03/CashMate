package minicla03.cashmate.domain.repository

import kotlinx.coroutines.flow.StateFlow
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.AuthenticationState
import minicla03.cashmate.domain.model.EmailVerificationResult
import minicla03.cashmate.domain.model.RegistrationResult
import minicla03.cashmate.domain.model.User

/**
 * Single source of truth for user authentication state and auth flows.
 * All credential verification, token issuance, and lifecycle rules remain on the backend.
 */
interface AuthRepository {
    val authenticationState: StateFlow<AuthenticationState>

    suspend fun restoreSession()

    suspend fun login(email: String, password: String): AppResult<User>

    suspend fun register(
        name: String,
        email: String,
        password: String
    ): AppResult<RegistrationResult>

    suspend fun logout(): AppResult<Unit>

    suspend fun refreshSession(): AppResult<Unit>

    suspend fun verifyEmail(email: String, token: String): AppResult<EmailVerificationResult>

    suspend fun resendVerificationEmail(email: String): AppResult<Unit>

    suspend fun requestPasswordReset(email: String): AppResult<Unit>

    suspend fun resetPassword(
        email: String,
        token: String,
        newPassword: String
    ): AppResult<Unit>

    fun clearSessionLocally()
}

