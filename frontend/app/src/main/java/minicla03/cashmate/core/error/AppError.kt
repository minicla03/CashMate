package minicla03.cashmate.core.error

/**
 * Application-level error hierarchy shared across Domain, Data, and Presentation layers.
 * Technical exceptions (Retrofit, OkHttp, IO) must be mapped to this hierarchy in the Data layer.
 */
sealed interface AppError {

    sealed interface Network : AppError {
        data object NoInternet : Network
        data object Timeout : Network
        data object Unauthorized : Network
        data object Forbidden : Network
        data object BadRequest : Network
        data object NotFound : Network
        data object ServerError : Network
        data object Unknown : Network
    }

    sealed interface Receipt : AppError {
        data object ProcessingFailed : Receipt
        data object InvalidReceipt : Receipt
        data object UnsupportedImage : Receipt
    }

    data class Validation(
        val fieldErrors: Map<ValidationField, ValidationReason>
    ) : AppError

    data class BusinessRule(
        val reason: BusinessErrorReason
    ) : AppError

    data object Unknown : AppError
}

enum class ValidationField {
    AMOUNT,
    DESCRIPTION,
    CATEGORY,
    ACCOUNT,
    DATE,
    CATEGORY_NAME,
    MERCHANT,
    TOTAL,
    ITEM_DESCRIPTION,
    ITEM_QUANTITY,
    ITEM_PRICE,
    ADJUSTMENT_DESCRIPTION,
    ADJUSTMENT_AMOUNT
}

enum class ValidationReason {
    REQUIRED,
    MUST_BE_POSITIVE,
    MUST_BE_NON_NEGATIVE,
    INVALID_FORMAT,
    TOO_LONG,
    INVALID_RANGE
}

enum class BusinessErrorReason {
    CATEGORY_IN_USE,
    CATEGORY_ALREADY_EXISTS,
    CANNOT_DELETE_LAST_CATEGORY,
    RECEIPT_NOT_READY
}

