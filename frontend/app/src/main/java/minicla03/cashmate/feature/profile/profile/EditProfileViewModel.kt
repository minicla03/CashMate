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
import minicla03.cashmate.domain.usecase.GetCurrentUserUseCase
import minicla03.cashmate.domain.usecase.UpdateProfileUseCase
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _effects = Channel<EditProfileEffect>(Channel.BUFFERED)
    val effects: Flow<EditProfileEffect> = _effects.receiveAsFlow()

    init {
        loadUser()
    }

    fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.NameChanged -> _uiState.update {
                it.copy(name = event.value, nameErrorResId = null, submitError = null)
            }
            is EditProfileEvent.EmailChanged -> _uiState.update {
                it.copy(email = event.value, emailErrorResId = null, submitError = null)
            }
            EditProfileEvent.SaveClick -> submit()
            EditProfileEvent.RetryLoad -> loadUser()
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            when (val result = getCurrentUserUseCase()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        name = result.data.name,
                        email = result.data.email,
                        loadError = null
                    )
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(isLoading = false, loadError = result.error.toUiError())
                }
            }
        }
    }

    private fun submit() {
        val current = _uiState.value
        if (current.isSubmitting || current.isLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    submitError = null,
                    nameErrorResId = null,
                    emailErrorResId = null
                )
            }

            when (val result = updateProfileUseCase(current.name, current.email)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            name = result.data.name,
                            email = result.data.email,
                            submitError = null
                        )
                    }
                    _effects.send(EditProfileEffect.ProfileUpdated)
                }
                is AppResult.Error -> when (val err = result.error) {
                    is AppError.Validation -> {
                        val fields = err.toFieldMessageResIds()
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                nameErrorResId = fields[ValidationField.NAME],
                                emailErrorResId = fields[ValidationField.EMAIL]
                            )
                        }
                    }
                    else -> _uiState.update {
                        it.copy(isSubmitting = false, submitError = err.toUiError())
                    }
                }
            }
        }
    }
}

