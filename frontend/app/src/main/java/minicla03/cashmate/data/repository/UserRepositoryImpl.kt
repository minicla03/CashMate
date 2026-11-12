package minicla03.cashmate.data.repository

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.data.remote.datasource.UserRemoteDataSource
import minicla03.cashmate.data.remote.dto.ChangePasswordRequest
import minicla03.cashmate.data.remote.dto.DeleteAccountRequest
import minicla03.cashmate.data.remote.dto.UpdateConsentRequest
import minicla03.cashmate.data.remote.dto.UpdateProfileRequest
import minicla03.cashmate.data.remote.mapper.toDomain
import minicla03.cashmate.data.remote.mapper.toDto
import minicla03.cashmate.domain.model.AppPreferences
import minicla03.cashmate.domain.model.ConsentItem
import minicla03.cashmate.domain.model.PrivacyInfo
import minicla03.cashmate.domain.model.User
import minicla03.cashmate.domain.model.UserDataExport
import minicla03.cashmate.domain.repository.UserPreferencesRepository
import minicla03.cashmate.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val sessionStateHolder: AuthSessionStateHolder,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val safeRemoteCall: SafeRemoteCall
) : UserRepository {

    override suspend fun getCurrentUser(): AppResult<User> {
        val result = safeRemoteCall { userRemoteDataSource.getCurrentUser().toDomain() }
        if (result is AppResult.Success) {
            sessionStateHolder.updateUserIfAuthenticated(result.data)
        }
        return result
    }

    override suspend fun updateProfile(name: String, email: String): AppResult<User> {
        val result = safeRemoteCall {
            userRemoteDataSource.updateProfile(
                UpdateProfileRequest(name = name, email = email)
            ).toDomain()
        }
        if (result is AppResult.Success) {
            sessionStateHolder.updateUserIfAuthenticated(result.data)
        }
        return result
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): AppResult<Unit> =
        safeRemoteCall {
            userRemoteDataSource.changePassword(
                ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword)
            )
        }

    override suspend fun getPreferences(): AppResult<AppPreferences> {
        val result = safeRemoteCall { userRemoteDataSource.getPreferences().toDomain() }
        if (result is AppResult.Success) {
            userPreferencesRepository.setThemeMode(result.data.themeMode)
            userPreferencesRepository.setCurrency(result.data.currency)
        }
        return result
    }

    override suspend fun updatePreferences(preferences: AppPreferences): AppResult<AppPreferences> {
        val result = safeRemoteCall {
            userRemoteDataSource.updatePreferences(preferences.toDto()).toDomain()
        }
        if (result is AppResult.Success) {
            userPreferencesRepository.setThemeMode(result.data.themeMode)
            userPreferencesRepository.setCurrency(result.data.currency)
        }
        return result
    }

    override suspend fun getPrivacyInfo(): AppResult<PrivacyInfo> = safeRemoteCall {
        userRemoteDataSource.getPrivacyInfo().toDomain()
    }

    override suspend fun updateConsent(
        consentId: String,
        granted: Boolean
    ): AppResult<List<ConsentItem>> = safeRemoteCall {
        userRemoteDataSource.updateConsent(
            consentId = consentId,
            request = UpdateConsentRequest(granted = granted)
        ).map { it.toDomain() }
    }

    override suspend fun exportUserData(): AppResult<UserDataExport> = safeRemoteCall {
        userRemoteDataSource.exportUserData().toDomain()
    }

    override suspend fun deleteAccount(confirmationText: String, password: String?): AppResult<Unit> {
        val result = safeRemoteCall {
            userRemoteDataSource.deleteAccount(
                DeleteAccountRequest(confirmation = confirmationText, password = password)
            )
        }
        if (result is AppResult.Success) {
            sessionStateHolder.clearSession()
        }
        return result
    }
}

