package minicla03.cashmate.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emits session expiration events when token refresh fails so the auth state
 * transitions cleanly to Unauthenticated without creating DI cycles.
 */
@Singleton
class SessionExpiryNotifier @Inject constructor() {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    fun notifySessionExpired() {
        _sessionExpired.tryEmit(Unit)
    }
}

/**
 * Refreshes an expired access token using the stored refresh token.
 */
interface TokenRefresher {
    fun refreshTokensSync(refreshToken: String): RefreshedTokenPair?
}

data class RefreshedTokenPair(
    val accessToken: String,
    val refreshToken: String
)

/**
 * Centralized OkHttp [Authenticator] that intercepts 401 responses, synchronizes refresh
 * attempts across concurrent requests, avoids infinite retry loops, and clears the session
 * if refresh fails.
 */
@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val tokenRefresher: TokenRefresher,
    private val sessionExpiryNotifier: SessionExpiryNotifier
) : Authenticator {

    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (isPublicAuthPath(response.request.url.encodedPath)) {
            return null
        }

        // Prevent infinite refresh/retry loops (original + 1 retried attempt = 2 responses max).
        if (responseCount(response) >= MAX_AUTH_ATTEMPTS) {
            handleRefreshFailure()
            return null
        }

        val failedToken = response.request.header(AuthInterceptor.HEADER_AUTHORIZATION)
            ?.removePrefix(BEARER_PREFIX)
            ?.trim()

        synchronized(refreshLock) {
            val latestAccessToken = tokenStorage.getAccessToken()?.takeIf { it.isNotBlank() }
            if (latestAccessToken != null && latestAccessToken != failedToken) {
                return response.request.newBuilder()
                    .header(AuthInterceptor.HEADER_AUTHORIZATION, "$BEARER_PREFIX$latestAccessToken")
                    .build()
            }

            val refreshToken = tokenStorage.getRefreshToken()?.takeIf { it.isNotBlank() }
            if (refreshToken == null) {
                handleRefreshFailure()
                return null
            }

            val refreshed = runCatching { tokenRefresher.refreshTokensSync(refreshToken) }.getOrNull()
            if (refreshed == null || refreshed.accessToken.isBlank() || refreshed.refreshToken.isBlank()) {
                handleRefreshFailure()
                return null
            }

            tokenStorage.saveTokens(refreshed.accessToken, refreshed.refreshToken)
            return response.request.newBuilder()
                .header(AuthInterceptor.HEADER_AUTHORIZATION, "$BEARER_PREFIX${refreshed.accessToken}")
                .build()
        }
    }

    private fun handleRefreshFailure() {
        tokenStorage.clearTokens()
        sessionExpiryNotifier.notifySessionExpired()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private fun isPublicAuthPath(path: String): Boolean = PUBLIC_AUTH_PATHS.any { path.endsWith(it) }

    companion object {
        private const val MAX_AUTH_ATTEMPTS = 2
        private const val BEARER_PREFIX = "Bearer "
        private val PUBLIC_AUTH_PATHS = setOf(
            "/${ApiEndpoints.AUTH_LOGIN}",
            "/${ApiEndpoints.AUTH_REGISTER}",
            "/${ApiEndpoints.AUTH_REFRESH}",
            "/${ApiEndpoints.AUTH_VERIFY_EMAIL}",
            "/${ApiEndpoints.AUTH_RESEND_VERIFICATION}",
            "/${ApiEndpoints.AUTH_PASSWORD_RESET_REQUEST}",
            "/${ApiEndpoints.AUTH_PASSWORD_RESET_CONFIRM}"
        )
    }
}

