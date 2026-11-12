package minicla03.cashmate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import minicla03.cashmate.domain.model.AppPreferences
import minicla03.cashmate.domain.model.AuthenticationState
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import minicla03.cashmate.domain.usecase.ObserveAuthStateUseCase
import minicla03.cashmate.domain.usecase.RestoreSessionUseCase
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val restoreSessionUseCase: RestoreSessionUseCase,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val authenticationState: StateFlow<AuthenticationState> = observeAuthStateUseCase()
    val preferences: StateFlow<AppPreferences> = userPreferencesRepository.preferences

    init {
        viewModelScope.launch {
            restoreSessionUseCase()
        }
    }
}
