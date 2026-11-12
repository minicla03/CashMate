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
        val token = tokenProvider.getAccessToken()?.takeIf { it.isNotBlank() }
            ?: return chain.proceed(originalRequest)

        val authenticatedRequest = originalRequest.newBuilder()
            .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$token")
            .build()

        return chain.proceed(authenticatedRequest)
    }

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
    }
}

