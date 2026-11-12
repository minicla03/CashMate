package minicla03.cashmate.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import minicla03.cashmate.domain.model.AppPreferences
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val preferences: StateFlow<AppPreferences> = userPreferencesRepository.preferences
}

