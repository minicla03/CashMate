package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.core.network.RefreshedTokenPair
import minicla03.cashmate.core.network.TokenRefresher
import minicla03.cashmate.data.remote.api.AuthRefreshApi
import minicla03.cashmate.data.remote.dto.RefreshTokenRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performs synchronous token refresh via [AuthRefreshApi] on behalf of [TokenRefreshAuthenticator].
 */
@Singleton
class RetrofitTokenRefresher @Inject constructor(
    private val authRefreshApi: AuthRefreshApi
) : TokenRefresher {

    override fun refreshTokensSync(refreshToken: String): RefreshedTokenPair? {
        val response = authRefreshApi.refreshTokenCall(RefreshTokenRequest(refreshToken)).execute()
        if (!response.isSuccessful) return null
        val body = response.body() ?: return null
        return RefreshedTokenPair(
            accessToken = body.accessToken,
            refreshToken = body.refreshToken
        )
    }
}

