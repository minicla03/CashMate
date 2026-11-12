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

interface AuthRemoteDataSource {
    suspend fun login(request: LoginRequest): AuthResponseDto
    suspend fun register(request: RegisterRequest): RegisterResponseDto
    suspend fun logout(request: LogoutRequest)
    suspend fun refreshToken(request: RefreshTokenRequest): AuthTokensDto
    suspend fun verifyEmail(request: VerifyEmailRequest): EmailVerificationResponseDto
    suspend fun resendVerificationEmail(request: ResendVerificationRequest)
    suspend fun requestPasswordReset(request: PasswordResetRequestDto)
    suspend fun resetPassword(request: ResetPasswordRequest)
}

