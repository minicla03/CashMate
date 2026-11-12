package minicla03.cashmate.feature.auth.forgotpassword

import androidx.lifecycle.SavedStateHandle
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
import minicla03.cashmate.domain.usecase.ResetPasswordUseCase
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val initialEmail: String = savedStateHandle.get<String>("email").orEmpty()

    private val _uiState = MutableStateFlow(ResetPasswordUiState(email = initialEmail))
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ResetPasswordEffect>(Channel.BUFFERED)
    val effects: Flow<ResetPasswordEffect> = _effects.receiveAsFlow()

    fun onEvent(event: ResetPasswordEvent) {
        when (event) {
            is ResetPasswordEvent.EmailChanged -> _uiState.update {
                it.copy(email = event.value, emailErrorResId = null, error = null)
            }
            is ResetPasswordEvent.TokenChanged -> _uiState.update {
                it.copy(token = event.value, tokenErrorResId = null, error = null)
            }
            is ResetPasswordEvent.NewPasswordChanged -> _uiState.update {
                it.copy(newPassword = event.value, newPasswordErrorResId = null, error = null)
            }
            is ResetPasswordEvent.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = event.value, confirmPasswordErrorResId = null, error = null)
            }
            ResetPasswordEvent.SubmitClick -> submit()
            ResetPasswordEvent.ContinueToLoginClick -> viewModelScope.launch {
                _effects.send(ResetPasswordEffect.NavigateToLogin)
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
                    tokenErrorResId = null,
                    newPasswordErrorResId = null,
                    confirmPasswordErrorResId = null
                )
            }

            when (val result = resetPasswordUseCase(
                email = current.email,
                token = current.token,
                newPassword = current.newPassword,
                confirmPassword = current.confirmPassword
            )) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isSubmitting = false, isCompleted = true, error = null)
                }
                is AppResult.Error -> when (val err = result.error) {
                    is AppError.Validation -> {
                        val fields = err.toFieldMessageResIds()
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                emailErrorResId = fields[ValidationField.EMAIL],
                                tokenErrorResId = fields[ValidationField.RESET_TOKEN],
                                newPasswordErrorResId = fields[ValidationField.NEW_PASSWORD],
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

