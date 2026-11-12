package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.AppPreferences
import minicla03.cashmate.domain.model.ConsentItem
import minicla03.cashmate.domain.model.PrivacyInfo
import minicla03.cashmate.domain.model.User
import minicla03.cashmate.domain.model.UserDataExport
import minicla03.cashmate.domain.repository.UserRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): AppResult<User> = userRepository.getCurrentUser()
}

class UpdateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val validator: AuthValidator
) {
    suspend operator fun invoke(name: String, email: String): AppResult<User> {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        validator.validateProfileUpdate(trimmedName, trimmedEmail)
            ?.let { return AppResult.Error(it) }
        return userRepository.updateProfile(trimmedName, trimmedEmail)
    }
}

class ChangePasswordUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val validator: AuthValidator
) {
    suspend operator fun invoke(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): AppResult<Unit> {
        validator.validateChangePassword(currentPassword, newPassword, confirmPassword)
            ?.let { return AppResult.Error(it) }
        return userRepository.changePassword(currentPassword, newPassword)
    }
}

class GetPreferencesUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): AppResult<AppPreferences> = userRepository.getPreferences()
}

class UpdatePreferencesUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(preferences: AppPreferences): AppResult<AppPreferences> =
        userRepository.updatePreferences(preferences)
}

class GetPrivacyInfoUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): AppResult<PrivacyInfo> = userRepository.getPrivacyInfo()
}

class UpdateConsentUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(consentId: String, granted: Boolean): AppResult<List<ConsentItem>> =
        userRepository.updateConsent(consentId, granted)
}

class ExportUserDataUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): AppResult<UserDataExport> = userRepository.exportUserData()
}

class DeleteAccountUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val validator: AuthValidator
) {
    suspend operator fun invoke(
        confirmationText: String,
        password: String? = null
    ): AppResult<Unit> {
        validator.validateDeleteConfirmation(confirmationText)
            ?.let { return AppResult.Error(it) }
        return userRepository.deleteAccount(confirmationText.trim(), password?.takeIf { it.isNotBlank() })
    }
}

