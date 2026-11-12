package minicla03.cashmate.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val emailVerified: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class UpdateProfileRequest(
    val name: String,
    val email: String
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class UserPreferencesDto(
    val themeMode: String = "SYSTEM",
    val currency: String = "EUR"
)

@Serializable
data class ConsentItemDto(
    val id: String,
    val title: String,
    val description: String,
    val required: Boolean,
    val granted: Boolean,
    val updatedAt: String? = null
)

@Serializable
data class UpdateConsentRequest(
    val granted: Boolean
)

@Serializable
data class PrivacyDataCategoryDto(
    val id: String,
    val title: String,
    val description: String,
    val retentionSummary: String? = null
)

@Serializable
data class PrivacyInfoDto(
    val summary: String,
    val dataCategories: List<PrivacyDataCategoryDto>,
    val consents: List<ConsentItemDto>,
    val policyUpdatedAt: String? = null
)

@Serializable
data class DeleteAccountRequest(
    val confirmation: String,
    val password: String? = null
)

data class UserDataExportDto(
    val fileName: String,
    val mimeType: String,
    val contentBytes: ByteArray,
    val generatedAt: String? = null
)

