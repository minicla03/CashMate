package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.api.CashMateApi
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
import minicla03.cashmate.data.remote.mapper.EmailAlreadyExistsException
import minicla03.cashmate.data.remote.mapper.EmailNotVerifiedException
import minicla03.cashmate.data.remote.mapper.InvalidCredentialsException
import minicla03.cashmate.data.remote.mapper.InvalidResetTokenException
import minicla03.cashmate.data.remote.mapper.InvalidVerificationTokenException
import minicla03.cashmate.data.remote.mapper.UnprocessableEntityException
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject

class RetrofitAuthRemoteDataSource @Inject constructor(
    private val api: CashMateApi
) : AuthRemoteDataSource {

    override suspend fun login(request: LoginRequest): AuthResponseDto = try {
        api.login(request)
    } catch (e: HttpException) {
        val bodyText = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
        when {
            e.code() == 401 -> throw InvalidCredentialsException()
            e.code() == 403 && bodyText.contains("VERIF", ignoreCase = true) ->
                throw EmailNotVerifiedException()
            e.code() == 422 -> throw UnprocessableEntityException()
            else -> throw e
        }
    }

    override suspend fun register(request: RegisterRequest): RegisterResponseDto = try {
        api.register(request)
    } catch (e: HttpException) {
        when (e.code()) {
            409 -> throw EmailAlreadyExistsException()
            422 -> throw UnprocessableEntityException()
            else -> throw e
        }
    }

    override suspend fun logout(request: LogoutRequest) {
        ensureSuccessful(api.logout(request))
    }

    override suspend fun refreshToken(request: RefreshTokenRequest): AuthTokensDto =
        api.refreshToken(request)

    override suspend fun verifyEmail(request: VerifyEmailRequest): EmailVerificationResponseDto = try {
        api.verifyEmail(request)
    } catch (e: HttpException) {
        when (e.code()) {
            400, 404, 422 -> throw InvalidVerificationTokenException()
            else -> throw e
        }
    }

    override suspend fun resendVerificationEmail(request: ResendVerificationRequest) {
        try {
            ensureSuccessful(api.resendVerificationEmail(request))
        } catch (e: HttpException) {
            if (e.code() == 422) throw UnprocessableEntityException()
            throw e
        }
    }

    override suspend fun requestPasswordReset(request: PasswordResetRequestDto) {
        try {
            ensureSuccessful(api.requestPasswordReset(request))
        } catch (e: HttpException) {
            if (e.code() == 422) throw UnprocessableEntityException()
            throw e
        }
    }

    override suspend fun resetPassword(request: ResetPasswordRequest) {
        try {
            ensureSuccessful(api.resetPassword(request))
        } catch (e: HttpException) {
            when (e.code()) {
                400, 404, 422 -> throw InvalidResetTokenException()
                else -> throw e
            }
        }
    }

    private fun ensureSuccessful(response: Response<Unit>) {
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }
}

