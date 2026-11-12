package minicla03.cashmate.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Attaches Bearer authorization headers when an access token is available.
 * Prepared for future Spring Boot JWT/OIDC integration without hardcoding credentials.
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (originalRequest.header(HEADER_AUTHORIZATION) != null || isPublicAuthPath(originalRequest.url.encodedPath)) {
            return chain.proceed(originalRequest)
        }

        val token = tokenProvider.getAccessToken()?.takeIf { it.isNotBlank() }
            ?: return chain.proceed(originalRequest)

        val authenticatedRequest = originalRequest.newBuilder()
            .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
            .build()

        return chain.proceed(authenticatedRequest)
    }

    private fun isPublicAuthPath(path: String): Boolean = PUBLIC_AUTH_PATHS.any { path.endsWith(it) }

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
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

