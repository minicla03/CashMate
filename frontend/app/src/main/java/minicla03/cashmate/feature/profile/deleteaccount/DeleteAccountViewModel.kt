package minicla03.cashmate.feature.profile.deleteaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.ui.toFieldMessageResIds
import minicla03.cashmate.core.ui.toUiError
import minicla03.cashmate.domain.usecase.AuthValidator
import minicla03.cashmate.domain.usecase.DeleteAccountUseCase
import javax.inject.Inject

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val authValidator: AuthValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeleteAccountUiState())
    val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

    fun onEvent(event: DeleteAccountEvent) {
        when (event) {
            is DeleteAccountEvent.ConfirmationTextChanged -> _uiState.update {
                it.copy(confirmationText = event.value, confirmationErrorResId = null, error = null)
            }
            is DeleteAccountEvent.PasswordChanged -> _uiState.update {
                it.copy(password = event.value, error = null)
            }
            is DeleteAccountEvent.AcknowledgedChanged -> _uiState.update {
                it.copy(acknowledgedConsequences = event.acknowledged, error = null)
            }
            DeleteAccountEvent.RequestDeleteClick -> openConfirmationDialog()
            DeleteAccountEvent.DismissDialog -> _uiState.update {
                it.copy(showFinalConfirmationDialog = false)
            }
            DeleteAccountEvent.ConfirmDeleteClick -> performDelete()
        }
    }

    private fun openConfirmationDialog() {
        val current = _uiState.value
        if (current.isDeleting || !current.acknowledgedConsequences) return

        val validation = authValidator.validateDeleteConfirmation(current.confirmationText)
        if (validation != null) {
            val fields = validation.toFieldMessageResIds()
            _uiState.update {
                it.copy(confirmationErrorResId = fields[ValidationField.DELETE_CONFIRMATION])
            }
            return
        }

        _uiState.update { it.copy(showFinalConfirmationDialog = true, confirmationErrorResId = null) }
    }

    private fun performDelete() {
        val current = _uiState.value
        if (current.isDeleting) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showFinalConfirmationDialog = false,
                    isDeleting = true,
                    error = null,
                    confirmationErrorResId = null
                )
            }

            when (val result = deleteAccountUseCase(current.confirmationText, current.password)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isDeleting = false, error = null)
                }
                is AppResult.Error -> when (val err = result.error) {
                    is AppError.Validation -> {
                        val fields = err.toFieldMessageResIds()
                        _uiState.update {
                            it.copy(
                                isDeleting = false,
                                confirmationErrorResId = fields[ValidationField.DELETE_CONFIRMATION]
                            )
                        }
                    }
                    else -> _uiState.update {
                        it.copy(isDeleting = false, error = err.toUiError())
                    }
                }
            }
        }
    }
}

