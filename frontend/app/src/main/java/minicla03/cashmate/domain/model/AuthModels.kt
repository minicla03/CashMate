package minicla03.cashmate.domain.model

/**
 * JWT token pair received from the backend authentication endpoints.
 */
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long? = null
) {
    override fun toString(): String = "AuthTokens(accessToken=***, refreshToken=***, expiresInSeconds=$expiresInSeconds)"
}

/**
 * Single source of truth for application authentication state.
 */
sealed interface AuthenticationState {
    data object Loading : AuthenticationState
    data class Authenticated(val user: User) : AuthenticationState
    data object Unauthenticated : AuthenticationState
}

/**
 * Outcome of registration returned by the backend.
 * If [requiresEmailVerification] is true, the user must verify their email before accessing the app.
 */
data class RegistrationResult(
    val user: User,
    val requiresEmailVerification: Boolean
)

/**
 * Result of verifying an email address via backend token.
 */
data class EmailVerificationResult(
    val email: String,
    val verified: Boolean
)

