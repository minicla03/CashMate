package minicla03.cashmate.feature.auth.forgotpassword

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
import minicla03.cashmate.domain.usecase.RequestPasswordResetUseCase
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ForgotPasswordEffect>(Channel.BUFFERED)
    val effects: Flow<ForgotPasswordEffect> = _effects.receiveAsFlow()

    fun onEvent(event: ForgotPasswordEvent) {
        when (event) {
            is ForgotPasswordEvent.EmailChanged -> _uiState.update {
                it.copy(email = event.value, emailErrorResId = null, error = null)
            }
            ForgotPasswordEvent.SubmitClick -> submit()
            ForgotPasswordEvent.ProceedToResetClick -> viewModelScope.launch {
                _effects.send(ForgotPasswordEffect.NavigateToResetPassword(_uiState.value.email.trim()))
            }
        }
    }

    private fun submit() {
        val current = _uiState.value
        if (current.isSubmitting) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isSubmitting = true, error = null, emailErrorResId = null)
            }

            when (val result = requestPasswordResetUseCase(current.email)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, requestSent = true) }
                    _effects.send(ForgotPasswordEffect.NavigateToResetPassword(current.email.trim()))
                }
                is AppResult.Error -> when (val err = result.error) {
                    is AppError.Validation -> {
                        val fields = err.toFieldMessageResIds()
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                emailErrorResId = fields[ValidationField.EMAIL]
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

