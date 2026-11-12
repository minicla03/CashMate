package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.dto.AuthResponseDto
import minicla03.cashmate.data.remote.dto.AuthTokensDto
import minicla03.cashmate.data.remote.dto.EmailVerificationResponseDto
import minicla03.cashmate.data.remote.dto.LoginRequest
import minicla03.cashmate.data.remote.dto.LogoutRequest
import minicla03.cashmate.data.remote.dto.PasswordResetRequestDto
import minicla03.cashmate.data.remote.dto.RefreshTokenRequest
import minicla03.cashmate.data.remote.dto.RegisterRequest
import minicla03.cashmate.data.remote.dto.RegisterResponseDto
import minicla03.cashmate.data.remote.dto.ResendVerificationRequest
import minicla03.cashmate.data.remote.dto.ResetPasswordRequest
import minicla03.cashmate.data.remote.dto.VerifyEmailRequest
import javax.inject.Inject

class FakeAuthRemoteDataSource @Inject constructor(
    private val store: FakeRemoteStore
) : AuthRemoteDataSource {

    override suspend fun login(request: LoginRequest): AuthResponseDto = store.login(request)

    override suspend fun register(request: RegisterRequest): RegisterResponseDto = store.register(request)

    override suspend fun logout(request: LogoutRequest) = store.logout(request)

    override suspend fun refreshToken(request: RefreshTokenRequest): AuthTokensDto = store.refreshToken(request)

    override suspend fun verifyEmail(request: VerifyEmailRequest): EmailVerificationResponseDto =
        store.verifyEmail(request)

    override suspend fun resendVerificationEmail(request: ResendVerificationRequest) =
        store.resendVerificationEmail(request)

    override suspend fun requestPasswordReset(request: PasswordResetRequestDto) =
        store.requestPasswordReset(request)

    override suspend fun resetPassword(request: ResetPasswordRequest) = store.resetPassword(request)
}

