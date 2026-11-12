package minicla03.cashmate.domain.repository

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.AppPreferences
import minicla03.cashmate.domain.model.ConsentItem
import minicla03.cashmate.domain.model.PrivacyInfo
import minicla03.cashmate.domain.model.User
import minicla03.cashmate.domain.model.UserDataExport

/**
 * Manages authenticated user profile, account preferences, privacy disclosures,
 * consent states, data export, and account deletion via the backend.
 */
interface UserRepository {
    suspend fun getCurrentUser(): AppResult<User>

    suspend fun updateProfile(name: String, email: String): AppResult<User>

    suspend fun changePassword(currentPassword: String, newPassword: String): AppResult<Unit>

    suspend fun getPreferences(): AppResult<AppPreferences>

    suspend fun updatePreferences(preferences: AppPreferences): AppResult<AppPreferences>

    suspend fun getPrivacyInfo(): AppResult<PrivacyInfo>

    suspend fun updateConsent(consentId: String, granted: Boolean): AppResult<List<ConsentItem>>

    suspend fun exportUserData(): AppResult<UserDataExport>

    suspend fun deleteAccount(confirmationText: String, password: String?): AppResult<Unit>
}

