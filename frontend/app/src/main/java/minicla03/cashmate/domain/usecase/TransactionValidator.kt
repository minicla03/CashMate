package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.error.ValidationReason
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Transaction
import javax.inject.Inject

/**
 * Pure domain validation for [Transaction] inputs prior to submission.
 */
class TransactionValidator @Inject constructor() {

    fun validate(transaction: Transaction): AppError.Validation? {
        val errors = buildMap {
            if (transaction.amount <= Money.ZERO) {
                put(ValidationField.AMOUNT, ValidationReason.MUST_BE_POSITIVE)
            }
            val trimmedDescription = transaction.description.trim()
            when {
                trimmedDescription.isEmpty() ->
                    put(ValidationField.DESCRIPTION, ValidationReason.REQUIRED)
                trimmedDescription.length > MAX_DESCRIPTION_LENGTH ->
                    put(ValidationField.DESCRIPTION, ValidationReason.TOO_LONG)
            }
            if (transaction.categoryId.isBlank()) {
                put(ValidationField.CATEGORY, ValidationReason.REQUIRED)
            }
            if (transaction.accountId.isBlank()) {
                put(ValidationField.ACCOUNT, ValidationReason.REQUIRED)
            }
            if (transaction.date.year !in MIN_YEAR..MAX_YEAR) {
                put(ValidationField.DATE, ValidationReason.INVALID_RANGE)
            }
        }
        return if (errors.isEmpty()) null else AppError.Validation(errors)
    }

    companion object {
        const val MAX_DESCRIPTION_LENGTH = 120
        const val MIN_YEAR = 1970
        const val MAX_YEAR = 2100
    }
}

