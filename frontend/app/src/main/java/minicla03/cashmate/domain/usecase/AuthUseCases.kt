package minicla03.cashmate.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.AuthenticationState
import minicla03.cashmate.domain.model.EmailVerificationResult
import minicla03.cashmate.domain.model.RegistrationResult
import minicla03.cashmate.domain.model.User
import minicla03.cashmate.domain.repository.AuthRepository
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): StateFlow<AuthenticationState> = authRepository.authenticationState
}

class RestoreSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.restoreSession()
    }
}

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val validator: AuthValidator
) {
    suspend operator fun invoke(email: String, password: String): AppResult<User> {
        val trimmedEmail = email.trim()
        validator.validateLogin(trimmedEmail, password)?.let { return AppResult.Error(it) }
        return authRepository.login(trimmedEmail, password)
    }
}

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val validator: AuthValidator
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): AppResult<RegistrationResult> {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        validator.validateRegistration(trimmedName, trimmedEmail, password, confirmPassword)
            ?.let { return AppResult.Error(it) }
        return authRepository.register(trimmedName, trimmedEmail, password)
    }
}

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): AppResult<Unit> = authRepository.logout()
}

class VerifyEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val validator: AuthValidator
) {
    suspend operator fun invoke(email: String, token: String): AppResult<EmailVerificationResult> {
        val trimmedEmail = email.trim()
        val trimmedToken = token.trim()
        validator.validateEmailVerification(trimmedEmail, trimmedToken)
            ?.let { return AppResult.Error(it) }
        return authRepository.verifyEmail(trimmedEmail, trimmedToken)
    }
}

class ResendVerificationEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val validator: AuthValidator
) {
    suspend operator fun invoke(email: String): AppResult<Unit> {
        val trimmedEmail = email.trim()
        validator.validateEmailOnly(trimmedEmail)?.let { return AppResult.Error(it) }
        return authRepository.resendVerificationEmail(trimmedEmail)
    }
}

class RequestPasswordResetUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val validator: AuthValidator
) {
    suspend operator fun invoke(email: String): AppResult<Unit> {
        val trimmedEmail = email.trim()
        validator.validateEmailOnly(trimmedEmail)?.let { return AppResult.Error(it) }
        return authRepository.requestPasswordReset(trimmedEmail)
    }
}

class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val validator: AuthValidator
) {
    suspend operator fun invoke(
        email: String,
        token: String,
        newPassword: String,
        confirmPassword: String
    ): AppResult<Unit> {
        val trimmedEmail = email.trim()
        val trimmedToken = token.trim()
        validator.validatePasswordReset(trimmedEmail, trimmedToken, newPassword, confirmPassword)
            ?.let { return AppResult.Error(it) }
        return authRepository.resetPassword(trimmedEmail, trimmedToken, newPassword)
    }
}

