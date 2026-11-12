package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.api.CashMateApi
import minicla03.cashmate.data.remote.dto.ChangePasswordRequest
import minicla03.cashmate.data.remote.dto.ConsentItemDto
import minicla03.cashmate.data.remote.dto.DeleteAccountRequest
import minicla03.cashmate.data.remote.dto.PrivacyInfoDto
import minicla03.cashmate.data.remote.dto.UpdateConsentRequest
import minicla03.cashmate.data.remote.dto.UpdateProfileRequest
import minicla03.cashmate.data.remote.dto.UserDataExportDto
import minicla03.cashmate.data.remote.dto.UserDto
import minicla03.cashmate.data.remote.dto.UserPreferencesDto
import minicla03.cashmate.data.remote.mapper.ConflictException
import minicla03.cashmate.data.remote.mapper.EmailAlreadyExistsException
import minicla03.cashmate.data.remote.mapper.InvalidCurrentPasswordException
import minicla03.cashmate.data.remote.mapper.RequiredConsentCannotBeRevokedException
import minicla03.cashmate.data.remote.mapper.UnprocessableEntityException
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject

class RetrofitUserRemoteDataSource @Inject constructor(
    private val api: CashMateApi
) : UserRemoteDataSource {

    override suspend fun getCurrentUser(): UserDto = api.getCurrentUser()

    override suspend fun updateProfile(request: UpdateProfileRequest): UserDto = try {
        api.updateProfile(request)
    } catch (e: HttpException) {
        when (e.code()) {
            409 -> throw EmailAlreadyExistsException()
            422 -> throw UnprocessableEntityException()
            else -> throw e
        }
    }

    override suspend fun changePassword(request: ChangePasswordRequest) {
        try {
            ensureSuccessful(api.changePassword(request))
        } catch (e: HttpException) {
            when (e.code()) {
                400, 422 -> throw InvalidCurrentPasswordException()
                else -> throw e
            }
        }
    }

    override suspend fun getPreferences(): UserPreferencesDto = api.getPreferences()

    override suspend fun updatePreferences(request: UserPreferencesDto): UserPreferencesDto = try {
        api.updatePreferences(request)
    } catch (e: HttpException) {
        if (e.code() == 422) throw UnprocessableEntityException()
        throw e
    }

    override suspend fun getPrivacyInfo(): PrivacyInfoDto = api.getPrivacyInfo()

    override suspend fun updateConsent(
        consentId: String,
        request: UpdateConsentRequest
    ): List<ConsentItemDto> = try {
        api.updateConsent(consentId, request)
    } catch (e: HttpException) {
        when (e.code()) {
            400, 422 -> throw RequiredConsentCannotBeRevokedException()
            409 -> throw ConflictException()
            else -> throw e
        }
    }

    override suspend fun exportUserData(): UserDataExportDto {
        val response = api.exportUserData()
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val body = response.body() ?: throw HttpException(response)
        val bytes = body.bytes()
        val mimeType = response.headers()["Content-Type"]
            ?.substringBefore(';')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "application/json"
        val fileName = parseContentDispositionFileName(response.headers()["Content-Disposition"])
            ?: "cashmate-export.json"
        return UserDataExportDto(
            fileName = fileName,
            mimeType = mimeType,
            contentBytes = bytes
        )
    }

    override suspend fun deleteAccount(request: DeleteAccountRequest) {
        try {
            ensureSuccessful(api.deleteAccount(request))
        } catch (e: HttpException) {
            when (e.code()) {
                409 -> throw ConflictException()
                422 -> throw UnprocessableEntityException()
                else -> throw e
            }
        }
    }

    private fun ensureSuccessful(response: Response<Unit>) {
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }

    private fun parseContentDispositionFileName(header: String?): String? {
        if (header.isNullOrBlank()) return null
        val match = Regex("""filename="?([^";]+)"?""", RegexOption.IGNORE_CASE).find(header)
        return match?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }
}

