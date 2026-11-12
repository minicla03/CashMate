package minicla03.cashmate.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.ui.toFieldMessageResIds
import minicla03.cashmate.core.ui.toUiError
import minicla03.cashmate.domain.usecase.LoginUseCase
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects: Flow<LoginEffect> = _effects.receiveAsFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> _uiState.update {
                it.copy(
                    email = event.value,
                    emailErrorResId = null,
                    error = null,
                    requiresEmailVerification = false
                )
            }
            is LoginEvent.PasswordChanged -> _uiState.update {
                it.copy(
                    password = event.value,
                    passwordErrorResId = null,
                    error = null
                )
            }
            LoginEvent.SubmitClick -> submit()
            LoginEvent.ForgotPasswordClick -> sendEffect(LoginEffect.NavigateToForgotPassword)
            LoginEvent.CreateAccountClick -> sendEffect(LoginEffect.NavigateToRegister)
            LoginEvent.VerifyEmailClick -> sendEffect(
                LoginEffect.NavigateToEmailVerification(_uiState.value.email.trim())
            )
            LoginEvent.DismissError -> _uiState.update {
                it.copy(error = null, requiresEmailVerification = false)
            }
        }
    }

    private fun submit() {
        val current = _uiState.value
        if (current.isSubmitting) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    error = null,
                    emailErrorResId = null,
                    passwordErrorResId = null,
                    requiresEmailVerification = false
                )
            }

            when (val result = loginUseCase(current.email, current.password)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, error = null) }
                }
                is AppResult.Error -> {
                    when (val err = result.error) {
                        is AppError.Validation -> {
                            val fieldMessages = err.toFieldMessageResIds()
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    emailErrorResId = fieldMessages[ValidationField.EMAIL],
                                    passwordErrorResId = fieldMessages[ValidationField.PASSWORD]
                                )
                            }
                        }
                        AppError.Auth.EmailNotVerified -> _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                error = err.toUiError(),
                                requiresEmailVerification = true
                            )
                        }
                        else -> _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                error = err.toUiError(),
                                requiresEmailVerification = false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sendEffect(effect: LoginEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}

