package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.dto.ChangePasswordRequest
import minicla03.cashmate.data.remote.dto.ConsentItemDto
import minicla03.cashmate.data.remote.dto.DeleteAccountRequest
import minicla03.cashmate.data.remote.dto.PrivacyInfoDto
import minicla03.cashmate.data.remote.dto.UpdateConsentRequest
import minicla03.cashmate.data.remote.dto.UpdateProfileRequest
import minicla03.cashmate.data.remote.dto.UserDataExportDto
import minicla03.cashmate.data.remote.dto.UserDto
import minicla03.cashmate.data.remote.dto.UserPreferencesDto

interface UserRemoteDataSource {
    suspend fun getCurrentUser(): UserDto
    suspend fun updateProfile(request: UpdateProfileRequest): UserDto
    suspend fun changePassword(request: ChangePasswordRequest)
    suspend fun getPreferences(): UserPreferencesDto
    suspend fun updatePreferences(request: UserPreferencesDto): UserPreferencesDto
    suspend fun getPrivacyInfo(): PrivacyInfoDto
    suspend fun updateConsent(consentId: String, request: UpdateConsentRequest): List<ConsentItemDto>
    suspend fun exportUserData(): UserDataExportDto
    suspend fun deleteAccount(request: DeleteAccountRequest)
}

