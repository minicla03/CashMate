package minicla03.cashmate.feature.profile.profile

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
import minicla03.cashmate.domain.usecase.ChangePasswordUseCase
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val changePasswordUseCase: ChangePasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ChangePasswordEffect>(Channel.BUFFERED)
    val effects: Flow<ChangePasswordEffect> = _effects.receiveAsFlow()

    fun onEvent(event: ChangePasswordEvent) {
        when (event) {
            is ChangePasswordEvent.CurrentPasswordChanged -> _uiState.update {
                it.copy(currentPassword = event.value, currentPasswordErrorResId = null, error = null)
            }
            is ChangePasswordEvent.NewPasswordChanged -> _uiState.update {
                it.copy(newPassword = event.value, newPasswordErrorResId = null, error = null)
            }
            is ChangePasswordEvent.ConfirmPasswordChanged -> _uiState.update {
                it.copy(confirmPassword = event.value, confirmPasswordErrorResId = null, error = null)
            }
            ChangePasswordEvent.SubmitClick -> submit()
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
                    currentPasswordErrorResId = null,
                    newPasswordErrorResId = null,
                    confirmPasswordErrorResId = null
                )
            }

            when (val result = changePasswordUseCase(
                currentPassword = current.currentPassword,
                newPassword = current.newPassword,
                confirmPassword = current.confirmPassword
            )) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, error = null) }
                    _effects.send(ChangePasswordEffect.PasswordChanged)
                }
                is AppResult.Error -> when (val err = result.error) {
                    is AppError.Validation -> {
                        val fields = err.toFieldMessageResIds()
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                currentPasswordErrorResId = fields[ValidationField.CURRENT_PASSWORD],
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

