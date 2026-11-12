package minicla03.cashmate.feature

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import minicla03.cashmate.R
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.domain.model.AuthenticationState
import minicla03.cashmate.domain.model.EmailVerificationResult
import minicla03.cashmate.domain.model.RegistrationResult
import minicla03.cashmate.domain.model.User
import minicla03.cashmate.domain.repository.AuthRepository
import minicla03.cashmate.domain.usecase.AuthValidator
import minicla03.cashmate.domain.usecase.LoginUseCase
import minicla03.cashmate.domain.usecase.RegisterUseCase
import minicla03.cashmate.domain.usecase.RequestPasswordResetUseCase
import minicla03.cashmate.domain.usecase.ResendVerificationEmailUseCase
import minicla03.cashmate.domain.usecase.ResetPasswordUseCase
import minicla03.cashmate.domain.usecase.VerifyEmailUseCase
import minicla03.cashmate.feature.auth.forgotpassword.ForgotPasswordEffect
import minicla03.cashmate.feature.auth.forgotpassword.ForgotPasswordEvent
import minicla03.cashmate.feature.auth.forgotpassword.ForgotPasswordViewModel
import minicla03.cashmate.feature.auth.forgotpassword.ResetPasswordEffect
import minicla03.cashmate.feature.auth.forgotpassword.ResetPasswordEvent
import minicla03.cashmate.feature.auth.forgotpassword.ResetPasswordViewModel
import minicla03.cashmate.feature.auth.login.LoginEffect
import minicla03.cashmate.feature.auth.login.LoginEvent
import minicla03.cashmate.feature.auth.login.LoginViewModel
import minicla03.cashmate.feature.auth.register.RegisterEffect
import minicla03.cashmate.feature.auth.register.RegisterEvent
import minicla03.cashmate.feature.auth.register.RegisterViewModel
import minicla03.cashmate.feature.auth.verification.EmailVerificationEvent
import minicla03.cashmate.feature.auth.verification.EmailVerificationViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()
    private val validator = AuthValidator()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login view model handles success invalid credentials 401 unverified and network error`() =
        runTest(testDispatcher) {
            var nextResult: AppResult<User> = AppResult.Error(AppError.Auth.InvalidCredentials)
            val repo = object : StubAuthRepository() {
                override suspend fun login(email: String, password: String) = nextResult
            }
            val vm = LoginViewModel(LoginUseCase(repo, validator))

            // Invalid credentials
            vm.onEvent(LoginEvent.EmailChanged("user@example.com"))
            vm.onEvent(LoginEvent.PasswordChanged("wrong"))
            vm.onEvent(LoginEvent.SubmitClick)
            advanceUntilIdle()
            assertEquals(R.string.error_auth_invalid_credentials, vm.uiState.value.error?.messageResId)

            // Network error
            nextResult = AppResult.Error(AppError.Network.NoInternet)
            vm.onEvent(LoginEvent.SubmitClick)
            advanceUntilIdle()
            assertEquals(R.string.error_no_internet, vm.uiState.value.error?.messageResId)

            // 401 Unauthorized
            nextResult = AppResult.Error(AppError.Network.Unauthorized)
            vm.onEvent(LoginEvent.SubmitClick)
            advanceUntilIdle()
            assertEquals(R.string.error_unauthorized, vm.uiState.value.error?.messageResId)

            // Unverified account prompts verification navigation
            nextResult = AppResult.Error(AppError.Auth.EmailNotVerified)
            vm.onEvent(LoginEvent.SubmitClick)
            advanceUntilIdle()
            assertTrue(vm.uiState.value.requiresEmailVerification)
            vm.onEvent(LoginEvent.VerifyEmailClick)
            assertEquals(
                LoginEffect.NavigateToEmailVerification("user@example.com"),
                vm.effects.first()
            )

            // Success
            nextResult = AppResult.Success(User("u1", "cla", "user@example.com", emailVerified = true))
            vm.onEvent(LoginEvent.SubmitClick)
            advanceUntilIdle()
            assertNull(vm.uiState.value.error)
            assertFalse(vm.uiState.value.isSubmitting)
        }

    @Test
    fun `register view model validates inputs surfaces server errors and navigates on success`() =
        runTest(testDispatcher) {
            var registerResult: AppResult<RegistrationResult> =
                AppResult.Error(AppError.Network.ServerError)
            val repo = object : StubAuthRepository() {
                override suspend fun register(name: String, email: String, password: String) =
                    registerResult
            }
            val vm = RegisterViewModel(RegisterUseCase(repo, validator))

            // Client-side validation error
            vm.onEvent(RegisterEvent.NameChanged(""))
            vm.onEvent(RegisterEvent.EmailChanged("invalid-email"))
            vm.onEvent(RegisterEvent.PasswordChanged("short"))
            vm.onEvent(RegisterEvent.ConfirmPasswordChanged("different"))
            vm.onEvent(RegisterEvent.SubmitClick)
            advanceUntilIdle()
            assertNotNull(vm.uiState.value.nameErrorResId)
            assertNotNull(vm.uiState.value.emailErrorResId)
            assertNotNull(vm.uiState.value.passwordErrorResId)
            assertNotNull(vm.uiState.value.confirmPasswordErrorResId)

            // Server error
            vm.onEvent(RegisterEvent.NameChanged("Claudio"))
            vm.onEvent(RegisterEvent.EmailChanged("cla@example.com"))
            vm.onEvent(RegisterEvent.PasswordChanged("Password123!"))
            vm.onEvent(RegisterEvent.ConfirmPasswordChanged("Password123!"))
            vm.onEvent(RegisterEvent.SubmitClick)
            advanceUntilIdle()
            assertEquals(R.string.error_server, vm.uiState.value.error?.messageResId)

            // Success with verification required
            registerResult = AppResult.Success(
                RegistrationResult(
                    user = User("u1", "Claudio", "cla@example.com", emailVerified = false),
                    requiresEmailVerification = true
                )
            )
            vm.onEvent(RegisterEvent.SubmitClick)
            advanceUntilIdle()
            assertNull(vm.uiState.value.error)
            assertEquals(
                RegisterEffect.NavigateToEmailVerification("cla@example.com"),
                vm.effects.first()
            )
        }

    @Test
    fun `email verification and password reset view models handle flows and errors`() =
        runTest(testDispatcher) {
            val repo = object : StubAuthRepository() {
                override suspend fun verifyEmail(email: String, token: String): AppResult<EmailVerificationResult> =
                    if (token == "123456") {
                        AppResult.Success(EmailVerificationResult(email, verified = true))
                    } else {
                        AppResult.Error(AppError.Auth.InvalidVerificationToken)
                    }

                override suspend fun resetPassword(email: String, token: String, newPassword: String) =
                    if (token == "reset-ok") {
                        AppResult.Success(Unit)
                    } else {
                        AppResult.Error(AppError.Auth.InvalidResetToken)
                    }
            }

            // Email verification
            val verifyVm = EmailVerificationViewModel(
                savedStateHandle = SavedStateHandle(mapOf("email" to "cla@example.com")),
                verifyEmailUseCase = VerifyEmailUseCase(repo, validator),
                resendVerificationEmailUseCase = ResendVerificationEmailUseCase(repo, validator)
            )
            verifyVm.onEvent(EmailVerificationEvent.ResendClick)
            advanceUntilIdle()
            assertEquals(R.string.auth_verification_sent_message, verifyVm.uiState.value.resendMessageResId)

            verifyVm.onEvent(EmailVerificationEvent.TokenChanged("bad"))
            verifyVm.onEvent(EmailVerificationEvent.VerifyClick)
            advanceUntilIdle()
            assertEquals(
                R.string.error_auth_invalid_verification_token,
                verifyVm.uiState.value.error?.messageResId
            )

            verifyVm.onEvent(EmailVerificationEvent.TokenChanged("123456"))
            verifyVm.onEvent(EmailVerificationEvent.VerifyClick)
            advanceUntilIdle()
            assertTrue(verifyVm.uiState.value.isVerified)

            // Forgot password -> Reset password
            val forgotVm = ForgotPasswordViewModel(RequestPasswordResetUseCase(repo, validator))
            forgotVm.onEvent(ForgotPasswordEvent.EmailChanged("cla@example.com"))
            forgotVm.onEvent(ForgotPasswordEvent.SubmitClick)
            advanceUntilIdle()
            assertTrue(forgotVm.uiState.value.requestSent)
            assertEquals(
                ForgotPasswordEffect.NavigateToResetPassword("cla@example.com"),
                forgotVm.effects.first()
            )

            val resetVm = ResetPasswordViewModel(
                savedStateHandle = SavedStateHandle(mapOf("email" to "cla@example.com")),
                resetPasswordUseCase = ResetPasswordUseCase(repo, validator)
            )
            resetVm.onEvent(ResetPasswordEvent.TokenChanged("reset-ok"))
            resetVm.onEvent(ResetPasswordEvent.NewPasswordChanged("NewPassword1!"))
            resetVm.onEvent(ResetPasswordEvent.ConfirmPasswordChanged("NewPassword1!"))
            resetVm.onEvent(ResetPasswordEvent.SubmitClick)
            advanceUntilIdle()
            assertTrue(resetVm.uiState.value.isCompleted)
            resetVm.onEvent(ResetPasswordEvent.ContinueToLoginClick)
            assertEquals(ResetPasswordEffect.NavigateToLogin, resetVm.effects.first())
        }

    private open class StubAuthRepository : AuthRepository {
        private val state = MutableStateFlow<AuthenticationState>(AuthenticationState.Unauthenticated)
        override val authenticationState: StateFlow<AuthenticationState> = state
        override suspend fun restoreSession() {}
        override suspend fun login(email: String, password: String): AppResult<User> =
            AppResult.Error(AppError.Unknown)
        override suspend fun register(name: String, email: String, password: String): AppResult<RegistrationResult> =
            AppResult.Error(AppError.Unknown)
        override suspend fun logout(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun refreshSession(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun verifyEmail(email: String, token: String): AppResult<EmailVerificationResult> =
            AppResult.Error(AppError.Unknown)
        override suspend fun resendVerificationEmail(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun requestPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun resetPassword(email: String, token: String, newPassword: String): AppResult<Unit> =
            AppResult.Success(Unit)
        override fun clearSessionLocally() {
            state.value = AuthenticationState.Unauthenticated
        }
    }
}

