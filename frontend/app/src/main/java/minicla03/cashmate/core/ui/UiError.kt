package minicla03.cashmate.core.ui

import androidx.annotation.StringRes
import minicla03.cashmate.R
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.BusinessErrorReason
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.error.ValidationReason

data class UiError(
    @StringRes val messageResId: Int,
    val canRetry: Boolean = true
)

fun AppError.toUiError(): UiError = when (this) {
    AppError.Network.NoInternet -> UiError(R.string.error_no_internet, canRetry = true)
    AppError.Network.Timeout -> UiError(R.string.error_timeout, canRetry = true)
    AppError.Network.Unauthorized -> UiError(R.string.error_unauthorized, canRetry = false)
    AppError.Network.Forbidden -> UiError(R.string.error_forbidden, canRetry = false)
    AppError.Network.BadRequest -> UiError(R.string.error_bad_request, canRetry = false)
    AppError.Network.NotFound -> UiError(R.string.error_not_found, canRetry = true)
    AppError.Network.Conflict -> UiError(R.string.error_conflict, canRetry = false)
    AppError.Network.UnprocessableEntity -> UiError(R.string.error_unprocessable_entity, canRetry = false)
    AppError.Network.TooManyRequests -> UiError(R.string.error_too_many_requests, canRetry = true)
    AppError.Network.ServerError -> UiError(R.string.error_server, canRetry = true)
    AppError.Network.Unknown -> UiError(R.string.error_network_unknown, canRetry = true)
    AppError.Auth.InvalidCredentials -> UiError(R.string.error_auth_invalid_credentials, canRetry = false)
    AppError.Auth.EmailNotVerified -> UiError(R.string.error_auth_email_not_verified, canRetry = false)
    AppError.Auth.SessionExpired -> UiError(R.string.error_auth_session_expired, canRetry = false)
    AppError.Auth.EmailAlreadyExists -> UiError(R.string.error_auth_email_already_exists, canRetry = false)
    AppError.Auth.InvalidVerificationToken -> UiError(R.string.error_auth_invalid_verification_token, canRetry = false)
    AppError.Auth.InvalidResetToken -> UiError(R.string.error_auth_invalid_reset_token, canRetry = false)
    AppError.Auth.InvalidCurrentPassword -> UiError(R.string.error_auth_invalid_current_password, canRetry = false)
    AppError.Receipt.ProcessingFailed -> UiError(R.string.error_receipt_processing_failed, canRetry = true)
    AppError.Receipt.InvalidReceipt -> UiError(R.string.error_receipt_invalid, canRetry = false)
    AppError.Receipt.UnsupportedImage -> UiError(R.string.error_receipt_unsupported_image, canRetry = false)
    is AppError.BusinessRule -> UiError(reason.toMessageResId(), canRetry = false)
    is AppError.Validation -> UiError(R.string.error_bad_request, canRetry = false)
    AppError.Unknown -> UiError(R.string.error_unknown, canRetry = true)
}

@StringRes
fun BusinessErrorReason.toMessageResId(): Int = when (this) {
    BusinessErrorReason.CATEGORY_IN_USE -> R.string.error_category_in_use
    BusinessErrorReason.CATEGORY_ALREADY_EXISTS -> R.string.error_category_duplicate
    BusinessErrorReason.CANNOT_DELETE_LAST_CATEGORY -> R.string.error_last_category
    BusinessErrorReason.RECEIPT_NOT_READY -> R.string.error_receipt_not_ready
    BusinessErrorReason.REQUIRED_CONSENT_CANNOT_BE_REVOKED -> R.string.error_required_consent_locked
}

fun AppError.Validation.toFieldMessageResIds(): Map<ValidationField, Int> =
    fieldErrors.mapValues { (field, reason) -> reason.toMessageResId(field) }

@StringRes
fun ValidationReason.toMessageResId(field: ValidationField): Int = when (field) {
    ValidationField.AMOUNT -> R.string.validation_amount_positive
    ValidationField.DESCRIPTION -> when (this) {
        ValidationReason.TOO_LONG -> R.string.validation_description_too_long
        else -> R.string.validation_description_required
    }
    ValidationField.CATEGORY -> R.string.validation_category_required
    ValidationField.ACCOUNT -> R.string.validation_account_required
    ValidationField.DATE -> when (this) {
        ValidationReason.INVALID_RANGE -> R.string.validation_date_range_invalid
        else -> R.string.validation_date_invalid
    }
    ValidationField.CATEGORY_NAME -> when (this) {
        ValidationReason.TOO_LONG -> R.string.validation_category_name_too_long
        else -> R.string.validation_category_name_required
    }
    ValidationField.MERCHANT -> when (this) {
        ValidationReason.TOO_LONG -> R.string.validation_merchant_too_long
        else -> R.string.validation_merchant_required
    }
    ValidationField.TOTAL -> R.string.validation_receipt_total_positive
    ValidationField.ITEM_DESCRIPTION -> R.string.validation_receipt_item_description_required
    ValidationField.ITEM_QUANTITY -> R.string.validation_receipt_item_quantity_positive
    ValidationField.ITEM_PRICE -> R.string.validation_receipt_item_price_non_negative
    ValidationField.ADJUSTMENT_DESCRIPTION -> R.string.validation_receipt_adjustment_description_required
    ValidationField.ADJUSTMENT_AMOUNT -> R.string.validation_receipt_adjustment_amount_non_negative
    ValidationField.EMAIL -> when (this) {
        ValidationReason.INVALID_FORMAT -> R.string.validation_email_invalid
        ValidationReason.TOO_LONG -> R.string.validation_email_too_long
        else -> R.string.validation_email_required
    }
    ValidationField.NAME -> when (this) {
        ValidationReason.TOO_LONG -> R.string.validation_name_too_long
        else -> R.string.validation_name_required
    }
    ValidationField.PASSWORD,
    ValidationField.NEW_PASSWORD -> when (this) {
        ValidationReason.TOO_SHORT -> R.string.validation_password_too_short
        ValidationReason.TOO_LONG -> R.string.validation_password_too_long
        else -> R.string.validation_password_required
    }
    ValidationField.CURRENT_PASSWORD -> R.string.validation_current_password_required
    ValidationField.CONFIRM_PASSWORD -> when (this) {
        ValidationReason.MISMATCH -> R.string.validation_passwords_do_not_match
        else -> R.string.validation_confirm_password_required
    }
    ValidationField.VERIFICATION_TOKEN -> R.string.validation_verification_token_required
    ValidationField.RESET_TOKEN -> R.string.validation_reset_token_required
    ValidationField.DELETE_CONFIRMATION -> R.string.validation_delete_confirmation_mismatch
}

