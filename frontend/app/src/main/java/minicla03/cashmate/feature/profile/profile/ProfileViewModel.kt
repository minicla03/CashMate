package minicla03.cashmate.feature.profile.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.ui.toUiError
import minicla03.cashmate.domain.model.AuthenticationState
import minicla03.cashmate.domain.usecase.GetCurrentUserUseCase
import minicla03.cashmate.domain.usecase.LogoutUseCase
import minicla03.cashmate.domain.usecase.ObserveAuthStateUseCase
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    observeAuthStateUseCase: ObserveAuthStateUseCase
) : ViewModel() {

    private val initialUser = (observeAuthStateUseCase().value as? AuthenticationState.Authenticated)?.user

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            isLoading = initialUser == null,
            user = initialUser
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects: Flow<ProfileEffect> = _effects.receiveAsFlow()

    init {
        observeAuthStateUseCase()
            .onEach { authState ->
                if (authState is AuthenticationState.Authenticated) {
                    _uiState.update {
                        it.copy(user = authState.user, isLoading = false, error = null)
                    }
                }
            }
            .launchIn(viewModelScope)

        loadProfile()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.Refresh -> loadProfile()
            ProfileEvent.EditProfileClick -> sendEffect(ProfileEffect.NavigateToEditProfile)
            ProfileEvent.ChangePasswordClick -> sendEffect(ProfileEffect.NavigateToChangePassword)
            ProfileEvent.PreferencesClick -> sendEffect(ProfileEffect.NavigateToPreferences)
            ProfileEvent.ManageCategoriesClick -> sendEffect(ProfileEffect.NavigateToCategories)
            ProfileEvent.PrivacyClick -> sendEffect(ProfileEffect.NavigateToPrivacy)
            ProfileEvent.ExportDataClick -> sendEffect(ProfileEffect.NavigateToExportData)
            ProfileEvent.DeleteAccountClick -> sendEffect(ProfileEffect.NavigateToDeleteAccount)
            ProfileEvent.LogoutClick -> _uiState.update { it.copy(showLogoutDialog = true) }
            ProfileEvent.DismissLogoutDialog -> _uiState.update { it.copy(showLogoutDialog = false) }
            ProfileEvent.ConfirmLogout -> performLogout()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.user == null, error = null) }
            when (val result = getCurrentUserUseCase()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isLoading = false, user = result.data, error = null)
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = if (it.user == null) result.error.toUiError() else null
                    )
                }
            }
        }
    }

    private fun performLogout() {
        if (_uiState.value.isLoggingOut) return
        viewModelScope.launch {
            _uiState.update { it.copy(showLogoutDialog = false, isLoggingOut = true) }
            logoutUseCase()
            _uiState.update { it.copy(isLoggingOut = false) }
        }
    }

    private fun sendEffect(effect: ProfileEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}

