package minicla03.cashmate.feature.auth.verification

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
import minicla03.cashmate.R
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.ui.toFieldMessageResIds
import minicla03.cashmate.core.ui.toUiError
import minicla03.cashmate.domain.usecase.ResendVerificationEmailUseCase
import minicla03.cashmate.domain.usecase.VerifyEmailUseCase
import javax.inject.Inject

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val verifyEmailUseCase: VerifyEmailUseCase,
    private val resendVerificationEmailUseCase: ResendVerificationEmailUseCase
) : ViewModel() {

    private val initialEmail: String = savedStateHandle.get<String>("email").orEmpty()

    private val _uiState = MutableStateFlow(
        EmailVerificationUiState(email = initialEmail)
    )
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    private val _effects = Channel<EmailVerificationEffect>(Channel.BUFFERED)
    val effects: Flow<EmailVerificationEffect> = _effects.receiveAsFlow()

    fun onEvent(event: EmailVerificationEvent) {
        when (event) {
            is EmailVerificationEvent.EmailChanged -> _uiState.update {
                it.copy(email = event.value, emailErrorResId = null, error = null)
            }
            is EmailVerificationEvent.TokenChanged -> _uiState.update {
                it.copy(token = event.value, tokenErrorResId = null, error = null)
            }
            EmailVerificationEvent.VerifyClick -> verify()
            EmailVerificationEvent.ResendClick -> resend()
            EmailVerificationEvent.ContinueToLoginClick -> viewModelScope.launch {
                _effects.send(EmailVerificationEffect.NavigateToLogin)
            }
            EmailVerificationEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun verify() {
        val current = _uiState.value
        if (current.isVerifying || current.isResending) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isVerifying = true,
                    error = null,
                    emailErrorResId = null,
                    tokenErrorResId = null,
                    resendMessageResId = null
                )
            }

            when (val result = verifyEmailUseCase(current.email, current.token)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isVerifying = false,
                        isVerified = result.data.verified,
                        error = if (result.data.verified) null else AppError.Auth.InvalidVerificationToken.toUiError()
                    )
                }
                is AppResult.Error -> when (val err = result.error) {
                    is AppError.Validation -> {
                        val fields = err.toFieldMessageResIds()
                        _uiState.update {
                            it.copy(
                                isVerifying = false,
                                emailErrorResId = fields[ValidationField.EMAIL],
                                tokenErrorResId = fields[ValidationField.VERIFICATION_TOKEN]
                            )
                        }
                    }
                    else -> _uiState.update {
                        it.copy(isVerifying = false, error = err.toUiError())
                    }
                }
            }
        }
    }

    private fun resend() {
        val current = _uiState.value
        if (current.isVerifying || current.isResending) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isResending = true,
                    error = null,
                    emailErrorResId = null,
                    resendMessageResId = null
                )
            }

            when (val result = resendVerificationEmailUseCase(current.email)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isResending = false,
                        resendMessageResId = R.string.auth_verification_sent_message
                    )
                }
                is AppResult.Error -> when (val err = result.error) {
                    is AppError.Validation -> {
                        val fields = err.toFieldMessageResIds()
                        _uiState.update {
                            it.copy(
                                isResending = false,
                                emailErrorResId = fields[ValidationField.EMAIL]
                            )
                        }
                    }
                    else -> _uiState.update {
                        it.copy(isResending = false, error = err.toUiError())
                    }
                }
            }
        }
    }
}

