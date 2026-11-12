package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.error.ValidationReason
import javax.inject.Inject

/**
 * Pure Kotlin UX validation helpers for authentication and account forms.
 * Backend remains the authority for all security and validation rules.
 */
class AuthValidator @Inject constructor() {

    fun validateLogin(email: String, password: String): AppError.Validation? {
        val errors = buildMap {
            validateEmail(email)?.let { put(ValidationField.EMAIL, it) }
            if (password.isBlank()) {
                put(ValidationField.PASSWORD, ValidationReason.REQUIRED)
            }
        }
        return errors.toValidationErrorOrNull()
    }

    fun validateRegistration(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): AppError.Validation? {
        val errors = buildMap {
            validateName(name)?.let { put(ValidationField.NAME, it) }
            validateEmail(email)?.let { put(ValidationField.EMAIL, it) }
            validateNewPassword(password)?.let { put(ValidationField.PASSWORD, it) }
            validateConfirmPassword(password, confirmPassword)?.let {
                put(ValidationField.CONFIRM_PASSWORD, it)
            }
        }
        return errors.toValidationErrorOrNull()
    }

    fun validateEmailVerification(email: String, token: String): AppError.Validation? {
        val errors = buildMap {
            validateEmail(email)?.let { put(ValidationField.EMAIL, it) }
            if (token.isBlank()) {
                put(ValidationField.VERIFICATION_TOKEN, ValidationReason.REQUIRED)
            }
        }
        return errors.toValidationErrorOrNull()
    }

    fun validateEmailOnly(email: String): AppError.Validation? {
        val errors = buildMap {
            validateEmail(email)?.let { put(ValidationField.EMAIL, it) }
        }
        return errors.toValidationErrorOrNull()
    }

    fun validatePasswordReset(
        email: String,
        token: String,
        newPassword: String,
        confirmPassword: String
    ): AppError.Validation? {
        val errors = buildMap {
            validateEmail(email)?.let { put(ValidationField.EMAIL, it) }
            if (token.isBlank()) {
                put(ValidationField.RESET_TOKEN, ValidationReason.REQUIRED)
            }
            validateNewPassword(newPassword)?.let { put(ValidationField.NEW_PASSWORD, it) }
            validateConfirmPassword(newPassword, confirmPassword)?.let {
                put(ValidationField.CONFIRM_PASSWORD, it)
            }
        }
        return errors.toValidationErrorOrNull()
    }

    fun validateProfileUpdate(name: String, email: String): AppError.Validation? {
        val errors = buildMap {
            validateName(name)?.let { put(ValidationField.NAME, it) }
            validateEmail(email)?.let { put(ValidationField.EMAIL, it) }
        }
        return errors.toValidationErrorOrNull()
    }

    fun validateChangePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): AppError.Validation? {
        val errors = buildMap {
            if (currentPassword.isBlank()) {
                put(ValidationField.CURRENT_PASSWORD, ValidationReason.REQUIRED)
            }
            validateNewPassword(newPassword)?.let { put(ValidationField.NEW_PASSWORD, it) }
            validateConfirmPassword(newPassword, confirmPassword)?.let {
                put(ValidationField.CONFIRM_PASSWORD, it)
            }
        }
        return errors.toValidationErrorOrNull()
    }

    fun validateDeleteConfirmation(confirmationText: String): AppError.Validation? {
        if (confirmationText.trim() != REQUIRED_DELETE_CONFIRMATION) {
            return AppError.Validation(
                mapOf(ValidationField.DELETE_CONFIRMATION to ValidationReason.MISMATCH)
            )
        }
        return null
    }

    private fun validateName(name: String): ValidationReason? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> ValidationReason.REQUIRED
            trimmed.length > MAX_NAME_LENGTH -> ValidationReason.TOO_LONG
            else -> null
        }
    }

    private fun validateEmail(email: String): ValidationReason? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> ValidationReason.REQUIRED
            trimmed.length > MAX_EMAIL_LENGTH -> ValidationReason.TOO_LONG
            !EMAIL_REGEX.matches(trimmed) -> ValidationReason.INVALID_FORMAT
            else -> null
        }
    }

    private fun validateNewPassword(password: String): ValidationReason? = when {
        password.isEmpty() -> ValidationReason.REQUIRED
        password.length < MIN_PASSWORD_LENGTH -> ValidationReason.TOO_SHORT
        password.length > MAX_PASSWORD_LENGTH -> ValidationReason.TOO_LONG
        else -> null
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): ValidationReason? = when {
        confirmPassword.isEmpty() -> ValidationReason.REQUIRED
        password != confirmPassword -> ValidationReason.MISMATCH
        else -> null
    }

    private fun Map<ValidationField, ValidationReason>.toValidationErrorOrNull(): AppError.Validation? =
        if (isEmpty()) null else AppError.Validation(this)

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
        const val MAX_PASSWORD_LENGTH = 128
        const val MAX_NAME_LENGTH = 80
        const val MAX_EMAIL_LENGTH = 254
        const val REQUIRED_DELETE_CONFIRMATION = "DELETE"
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

