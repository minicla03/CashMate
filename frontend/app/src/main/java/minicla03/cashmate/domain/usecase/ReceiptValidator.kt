package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.error.ValidationReason
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Receipt
import javax.inject.Inject

/**
 * Pure domain validation for edited [Receipt] fields before backend confirmation.
 * Complex financial reconciliation remains authoritative on the backend.
 */
class ReceiptValidator @Inject constructor() {

    fun validate(receipt: Receipt): AppError.Validation? {
        val errors = buildMap {
            val trimmedMerchant = receipt.merchant.trim()
            when {
                trimmedMerchant.isEmpty() ->
                    put(ValidationField.MERCHANT, ValidationReason.REQUIRED)
                trimmedMerchant.length > MAX_MERCHANT_LENGTH ->
                    put(ValidationField.MERCHANT, ValidationReason.TOO_LONG)
            }

            if (receipt.date.year !in MIN_YEAR..MAX_YEAR) {
                put(ValidationField.DATE, ValidationReason.INVALID_RANGE)
            }

            if (receipt.total <= Money.ZERO) {
                put(ValidationField.TOTAL, ValidationReason.MUST_BE_POSITIVE)
            }

            if (receipt.subtotal < Money.ZERO || receipt.totalDiscount < Money.ZERO) {
                put(ValidationField.TOTAL, ValidationReason.MUST_BE_NON_NEGATIVE)
            }

            if (receipt.categoryId.isNullOrBlank()) {
                put(ValidationField.CATEGORY, ValidationReason.REQUIRED)
            }

            if (receipt.accountId.isNullOrBlank()) {
                put(ValidationField.ACCOUNT, ValidationReason.REQUIRED)
            }

            for (item in receipt.items) {
                if (item.description.trim().isEmpty()) {
                    put(ValidationField.ITEM_DESCRIPTION, ValidationReason.REQUIRED)
                }
                if (item.quantity <= 0) {
                    put(ValidationField.ITEM_QUANTITY, ValidationReason.MUST_BE_POSITIVE)
                }
                if (item.unitPrice < Money.ZERO ||
                    item.grossAmount < Money.ZERO ||
                    item.discountAmount < Money.ZERO ||
                    item.netAmount < Money.ZERO
                ) {
                    put(ValidationField.ITEM_PRICE, ValidationReason.MUST_BE_NON_NEGATIVE)
                }
            }

            for (adjustment in receipt.adjustments) {
                if (adjustment.description.trim().isEmpty()) {
                    put(ValidationField.ADJUSTMENT_DESCRIPTION, ValidationReason.REQUIRED)
                }
                if (adjustment.amount < Money.ZERO) {
                    put(ValidationField.ADJUSTMENT_AMOUNT, ValidationReason.MUST_BE_NON_NEGATIVE)
                }
            }
        }
        return if (errors.isEmpty()) null else AppError.Validation(errors)
    }

    companion object {
        const val MAX_MERCHANT_LENGTH = 120
        const val MIN_YEAR = 1970
        const val MAX_YEAR = 2100
    }
}

