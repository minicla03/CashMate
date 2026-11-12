package minicla03.cashmate.data.remote.api

import minicla03.cashmate.core.network.ApiEndpoints
import minicla03.cashmate.data.remote.dto.AuthTokensDto
import minicla03.cashmate.data.remote.dto.RefreshTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Dedicated Retrofit API used solely by [minicla03.cashmate.core.network.TokenRefreshAuthenticator]
 * on an unauthenticated OkHttp client to avoid interceptor/authenticator recursion.
 */
interface AuthRefreshApi {
    @POST(ApiEndpoints.AUTH_REFRESH)
    fun refreshTokenCall(@Body request: RefreshTokenRequest): retrofit2.Call<AuthTokensDto>
}

