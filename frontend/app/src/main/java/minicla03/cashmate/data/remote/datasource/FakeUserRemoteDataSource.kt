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
import javax.inject.Inject

class FakeUserRemoteDataSource @Inject constructor(
    private val store: FakeRemoteStore
) : UserRemoteDataSource {

    override suspend fun getCurrentUser(): UserDto = store.getCurrentUser()

    override suspend fun updateProfile(request: UpdateProfileRequest): UserDto = store.updateProfile(request)

    override suspend fun changePassword(request: ChangePasswordRequest) = store.changePassword(request)

    override suspend fun getPreferences(): UserPreferencesDto = store.getPreferences()

    override suspend fun updatePreferences(request: UserPreferencesDto): UserPreferencesDto =
        store.updatePreferences(request)

    override suspend fun getPrivacyInfo(): PrivacyInfoDto = store.getPrivacyInfo()

    override suspend fun updateConsent(
        consentId: String,
        request: UpdateConsentRequest
    ): List<ConsentItemDto> = store.updateConsent(consentId, request)

    override suspend fun exportUserData(): UserDataExportDto = store.exportUserData()

    override suspend fun deleteAccount(request: DeleteAccountRequest) = store.deleteAccount(request)
}

