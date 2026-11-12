package minicla03.cashmate.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class LogoutRequest(
    val refreshToken: String? = null
)

@Serializable
data class VerifyEmailRequest(
    val email: String,
    val token: String
)

@Serializable
data class ResendVerificationRequest(
    val email: String
)

@Serializable
data class PasswordResetRequestDto(
    val email: String
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val token: String,
    val newPassword: String
)

@Serializable
data class AuthTokensDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long? = null
)

@Serializable
data class AuthResponseDto(
    val user: UserDto,
    val tokens: AuthTokensDto
)

@Serializable
data class RegisterResponseDto(
    val user: UserDto,
    val requiresEmailVerification: Boolean = false,
    val tokens: AuthTokensDto? = null
)

@Serializable
data class EmailVerificationResponseDto(
    val email: String,
    val verified: Boolean
)

