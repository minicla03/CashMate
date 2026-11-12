package minicla03.cashmate.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import minicla03.cashmate.R
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.BusinessErrorReason
import minicla03.cashmate.data.platform.ExportFileHandler
import minicla03.cashmate.data.repository.InMemoryUserPreferencesRepository
import minicla03.cashmate.domain.model.AppPreferences
import minicla03.cashmate.domain.model.AppThemeMode
import minicla03.cashmate.domain.model.AuthenticationState
import minicla03.cashmate.domain.model.ConsentItem
import minicla03.cashmate.domain.model.EmailVerificationResult
import minicla03.cashmate.domain.model.PrivacyDataCategory
import minicla03.cashmate.domain.model.PrivacyInfo
import minicla03.cashmate.domain.model.RegistrationResult
import minicla03.cashmate.domain.model.User
import minicla03.cashmate.domain.model.UserDataExport
import minicla03.cashmate.domain.repository.AuthRepository
import minicla03.cashmate.domain.repository.UserRepository
import minicla03.cashmate.domain.usecase.AuthValidator
import minicla03.cashmate.domain.usecase.DeleteAccountUseCase
import minicla03.cashmate.domain.usecase.ExportUserDataUseCase
import minicla03.cashmate.domain.usecase.GetCurrentUserUseCase
import minicla03.cashmate.domain.usecase.GetPreferencesUseCase
import minicla03.cashmate.domain.usecase.GetPrivacyInfoUseCase
import minicla03.cashmate.domain.usecase.LogoutUseCase
import minicla03.cashmate.domain.usecase.ObserveAuthStateUseCase
import minicla03.cashmate.domain.usecase.UpdateConsentUseCase
import minicla03.cashmate.domain.usecase.UpdatePreferencesUseCase
import minicla03.cashmate.domain.usecase.UpdateProfileUseCase
import minicla03.cashmate.feature.profile.deleteaccount.DeleteAccountEvent
import minicla03.cashmate.feature.profile.deleteaccount.DeleteAccountViewModel
import minicla03.cashmate.feature.profile.export.ExportDataEvent
import minicla03.cashmate.feature.profile.export.ExportDataViewModel
import minicla03.cashmate.feature.profile.preferences.PreferencesEvent
import minicla03.cashmate.feature.profile.preferences.PreferencesViewModel
import minicla03.cashmate.feature.profile.privacy.PrivacyEvent
import minicla03.cashmate.feature.profile.privacy.PrivacyViewModel
import minicla03.cashmate.feature.profile.profile.EditProfileEffect
import minicla03.cashmate.feature.profile.profile.EditProfileEvent
import minicla03.cashmate.feature.profile.profile.EditProfileViewModel
import minicla03.cashmate.feature.profile.profile.ProfileEvent
import minicla03.cashmate.feature.profile.profile.ProfileViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Currency

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileAndAccountViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()
    private val validator = AuthValidator()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `profile view model loads user and handles logout`() = runTest(testDispatcher) {
        val sampleUser = User("u1", "cla", "user@example.com", emailVerified = true)
        val authState = MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(sampleUser))
        var loggedOut = false

        val authRepo = object : StubAuthRepo(authState) {
            override suspend fun logout(): AppResult<Unit> {
                loggedOut = true
                authState.value = AuthenticationState.Unauthenticated
                return AppResult.Success(Unit)
            }
        }
        val userRepo = object : StubUserRepo() {
            override suspend fun getCurrentUser() = AppResult.Success(sampleUser)
        }

        val vm = ProfileViewModel(
            getCurrentUserUseCase = GetCurrentUserUseCase(userRepo),
            logoutUseCase = LogoutUseCase(authRepo),
            observeAuthStateUseCase = ObserveAuthStateUseCase(authRepo)
        )
        advanceUntilIdle()
        assertEquals("cla", vm.uiState.value.user?.name)

        vm.onEvent(ProfileEvent.LogoutClick)
        assertTrue(vm.uiState.value.showLogoutDialog)

        vm.onEvent(ProfileEvent.ConfirmLogout)
        advanceUntilIdle()
        assertTrue(loggedOut)
        assertEquals(AuthenticationState.Unauthenticated, authState.value)
    }

    @Test
    fun `edit profile view model loads and updates profile`() = runTest(testDispatcher) {
        val userRepo = object : StubUserRepo() {
            override suspend fun getCurrentUser() =
                AppResult.Success(User("u1", "cla", "user@example.com", emailVerified = true))

            override suspend fun updateProfile(name: String, email: String) =
                AppResult.Success(User("u1", name, email, emailVerified = true))
        }

        val vm = EditProfileViewModel(
            getCurrentUserUseCase = GetCurrentUserUseCase(userRepo),
            updateProfileUseCase = UpdateProfileUseCase(userRepo, validator)
        )
        advanceUntilIdle()
        assertEquals("cla", vm.uiState.value.name)

        vm.onEvent(EditProfileEvent.NameChanged("Claudio"))
        vm.onEvent(EditProfileEvent.EmailChanged("claudio@example.com"))
        vm.onEvent(EditProfileEvent.SaveClick)
        advanceUntilIdle()

        assertEquals("Claudio", vm.uiState.value.name)
        assertEquals(EditProfileEffect.ProfileUpdated, vm.effects.first())
    }

    @Test
    fun `preferences view model loads and updates theme and currency`() = runTest(testDispatcher) {
        val prefsRepo = InMemoryUserPreferencesRepository()
        var remotePrefs = AppPreferences(AppThemeMode.LIGHT, Currency.getInstance("EUR"))
        val userRepo = object : StubUserRepo() {
            override suspend fun getPreferences() = AppResult.Success(remotePrefs).also {
                prefsRepo.setThemeMode(remotePrefs.themeMode)
                prefsRepo.setCurrency(remotePrefs.currency)
            }

            override suspend fun updatePreferences(preferences: AppPreferences): AppResult<AppPreferences> {
                remotePrefs = preferences
                prefsRepo.setThemeMode(preferences.themeMode)
                prefsRepo.setCurrency(preferences.currency)
                return AppResult.Success(preferences)
            }
        }

        val vm = PreferencesViewModel(
            getPreferencesUseCase = GetPreferencesUseCase(userRepo),
            updatePreferencesUseCase = UpdatePreferencesUseCase(userRepo),
            userPreferencesRepository = prefsRepo
        )
        advanceUntilIdle()
        assertEquals(AppThemeMode.LIGHT, vm.uiState.value.themeMode)

        vm.onEvent(PreferencesEvent.ThemeSelected(AppThemeMode.DARK))
        advanceUntilIdle()
        assertEquals(AppThemeMode.DARK, vm.uiState.value.themeMode)

        vm.onEvent(PreferencesEvent.CurrencySelected(Currency.getInstance("USD")))
        advanceUntilIdle()
        assertEquals(Currency.getInstance("USD"), vm.uiState.value.selectedCurrency)
    }

    @Test
    fun `privacy view model loads data categories and updates optional consents`() = runTest(testDispatcher) {
        val consents = listOf(
            ConsentItem("c-req", "Essential", "Required", required = true, granted = true),
            ConsentItem("c-opt", "Analytics", "Optional", required = false, granted = false)
        )
        val userRepo = object : StubUserRepo() {
            override suspend fun getPrivacyInfo() = AppResult.Success(
                PrivacyInfo(
                    summary = "Stored data summary",
                    dataCategories = listOf(PrivacyDataCategory("d1", "Account", "Email & name")),
                    consents = consents
                )
            )

            override suspend fun updateConsent(consentId: String, granted: Boolean) =
                if (consentId == "c-req" && !granted) {
                    AppResult.Error(AppError.BusinessRule(BusinessErrorReason.REQUIRED_CONSENT_CANNOT_BE_REVOKED))
                } else {
                    AppResult.Success(
                        consents.map { if (it.id == consentId) it.copy(granted = granted) else it }
                    )
                }
        }

        val vm = PrivacyViewModel(
            getPrivacyInfoUseCase = GetPrivacyInfoUseCase(userRepo),
            updateConsentUseCase = UpdateConsentUseCase(userRepo)
        )
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.privacyInfo?.dataCategories?.size)

        vm.onEvent(PrivacyEvent.ConsentToggled("c-opt", true))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.privacyInfo?.consents?.first { it.id == "c-opt" }?.granted == true)
    }

    @Test
    fun `export data view model handles loading success and error`() = runTest(testDispatcher) {
        var result: AppResult<UserDataExport> = AppResult.Error(AppError.Network.ServerError)
        val userRepo = object : StubUserRepo() {
            override suspend fun exportUserData() = result
        }
        val fileHandler = object : ExportFileHandler {
            override suspend fun prepareShareableUri(export: UserDataExport): String? =
                "content://minicla03.cashmate.fileprovider/exports/${export.fileName}"
        }

        val vm = ExportDataViewModel(ExportUserDataUseCase(userRepo), fileHandler)
        vm.onEvent(ExportDataEvent.RequestExportClick)
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(R.string.error_server, vm.uiState.value.error?.messageResId)

        result = AppResult.Success(
            UserDataExport("cashmate-export.json", "application/json", "{}".toByteArray())
        )
        vm.onEvent(ExportDataEvent.RequestExportClick)
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
        assertNotNull(vm.uiState.value.export)
        assertNotNull(vm.uiState.value.preparedUriString)
    }

    @Test
    fun `delete account view model requires acknowledgement and DELETE confirmation before calling API`() =
        runTest(testDispatcher) {
            var deleteCalled = false
            val userRepo = object : StubUserRepo() {
                override suspend fun deleteAccount(confirmationText: String, password: String?): AppResult<Unit> {
                    deleteCalled = true
                    return AppResult.Success(Unit)
                }
            }

            val vm = DeleteAccountViewModel(DeleteAccountUseCase(userRepo, validator), validator)
            vm.onEvent(DeleteAccountEvent.AcknowledgedChanged(true))
            vm.onEvent(DeleteAccountEvent.ConfirmationTextChanged("NO"))
            vm.onEvent(DeleteAccountEvent.RequestDeleteClick)
            assertNotNull(vm.uiState.value.confirmationErrorResId)
            assertFalse(vm.uiState.value.showFinalConfirmationDialog)

            vm.onEvent(DeleteAccountEvent.ConfirmationTextChanged("DELETE"))
            vm.onEvent(DeleteAccountEvent.RequestDeleteClick)
            assertTrue(vm.uiState.value.showFinalConfirmationDialog)

            vm.onEvent(DeleteAccountEvent.ConfirmDeleteClick)
            advanceUntilIdle()
            assertTrue(deleteCalled)
            assertNull(vm.uiState.value.error)
        }

    private open class StubAuthRepo(
        private val state: MutableStateFlow<AuthenticationState>
    ) : AuthRepository {
        override val authenticationState: StateFlow<AuthenticationState> = state
        override suspend fun restoreSession() {}
        override suspend fun login(email: String, password: String): AppResult<User> =
            AppResult.Error(AppError.Unknown)
        override suspend fun register(name: String, email: String, password: String): AppResult<RegistrationResult> =
            AppResult.Error(AppError.Unknown)
        override suspend fun logout(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun refreshSession(): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun verifyEmail(email: String, token: String): AppResult<EmailVerificationResult> =
            AppResult.Error(AppError.Unknown)
        override suspend fun resendVerificationEmail(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun requestPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun resetPassword(email: String, token: String, newPassword: String): AppResult<Unit> =
            AppResult.Success(Unit)
        override fun clearSessionLocally() {
            state.value = AuthenticationState.Unauthenticated
        }
    }

    private open class StubUserRepo : UserRepository {
        override suspend fun getCurrentUser(): AppResult<User> = AppResult.Error(AppError.Unknown)
        override suspend fun updateProfile(name: String, email: String): AppResult<User> = AppResult.Error(AppError.Unknown)
        override suspend fun changePassword(currentPassword: String, newPassword: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getPreferences(): AppResult<AppPreferences> = AppResult.Success(AppPreferences())
        override suspend fun updatePreferences(preferences: AppPreferences): AppResult<AppPreferences> = AppResult.Success(preferences)
        override suspend fun getPrivacyInfo(): AppResult<PrivacyInfo> = AppResult.Error(AppError.Unknown)
        override suspend fun updateConsent(consentId: String, granted: Boolean): AppResult<List<ConsentItem>> = AppResult.Success(emptyList())
        override suspend fun exportUserData(): AppResult<UserDataExport> = AppResult.Error(AppError.Unknown)
        override suspend fun deleteAccount(confirmationText: String, password: String?): AppResult<Unit> = AppResult.Success(Unit)
    }
}
