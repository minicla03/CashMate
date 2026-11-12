package minicla03.cashmate.feature.auth.register

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
import minicla03.cashmate.domain.usecase.RegisterUseCase
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _effects = Channel<RegisterEffect>(Channel.BUFFERED)
    val effects: Flow<RegisterEffect> = _effects.receiveAsFlow()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.NameChanged -> _uiState.update {
                it.copy(name = event.value, nameErrorResId = null, error = null)
            }
            is RegisterEvent.EmailChanged -> _uiState.update {
                it.copy(email = event.value, emailErrorResId = null, error = null)
            }
            is RegisterEvent.PasswordChanged -> _uiState.update {
                it.copy(password = event.value, passwordErrorResId = null, error = null)
            }
            is RegisterEvent.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = event.value, confirmPasswordErrorResId = null, error = null)
            }
            RegisterEvent.SubmitClick -> submit()
            RegisterEvent.SignInClick -> sendEffect(RegisterEffect.NavigateBackToLogin)
            RegisterEvent.DismissError -> _uiState.update { it.copy(error = null) }
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
                    nameErrorResId = null,
                    emailErrorResId = null,
                    passwordErrorResId = null,
                    confirmPasswordErrorResId = null
                )
            }

            when (val result = registerUseCase(
                name = current.name,
                email = current.email,
                password = current.password,
                confirmPassword = current.confirmPassword
            )) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, error = null) }
                    if (result.data.requiresEmailVerification) {
                        sendEffect(RegisterEffect.NavigateToEmailVerification(result.data.user.email))
                    }
                }
                is AppResult.Error -> {
                    when (val err = result.error) {
                        is AppError.Validation -> {
                            val fields = err.toFieldMessageResIds()
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    nameErrorResId = fields[ValidationField.NAME],
                                    emailErrorResId = fields[ValidationField.EMAIL],
                                    passwordErrorResId = fields[ValidationField.PASSWORD],
                                    confirmPasswordErrorResId = fields[ValidationField.CONFIRM_PASSWORD]
                                )
                            }
                        }
                        else -> _uiState.update {
                            it.copy(isSubmitting = false, error = err.toUiError())
                        }
                    }
                }
            }
        }
    }

    private fun sendEffect(effect: RegisterEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}

