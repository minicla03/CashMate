package minicla03.cashmate.domain.usecase

import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.ReceiptAdjustmentType
import minicla03.cashmate.domain.model.ReceiptItem

/**
 * Deterministic financial calculations for receipt line items and receipt totals.
 * Uses exact integer arithmetic via [Money] without floating-point operations.
 */
object ReceiptCalculator {

    fun calculateItem(
        id: String,
        description: String,
        quantity: Int,
        unitPrice: Money,
        discountAmount: Money,
        category: String
    ): ReceiptItem {
        val grossMinor = unitPrice.minorUnits * quantity.toLong()
        val netMinor = (grossMinor - discountAmount.minorUnits).coerceAtLeast(0L)
        return ReceiptItem(
            id = id,
            description = description,
            quantity = quantity,
            unitPrice = unitPrice,
            grossAmount = Money(grossMinor),
            discountAmount = discountAmount,
            netAmount = Money(netMinor),
            category = category
        )
    }

    fun recalculateTotals(
        receipt: Receipt,
        fallbackTotalWhenNoItems: Money? = null
    ): Receipt {
        val subtotalMinor = receipt.items.sumOf { it.grossAmount.minorUnits }
        val itemDiscountsMinor = receipt.items.sumOf { it.discountAmount.minorUnits }
        val itemsNetMinor = receipt.items.sumOf { it.netAmount.minorUnits }

        val adjustmentDiscountsMinor = receipt.adjustments
            .filter { it.type == ReceiptAdjustmentType.DISCOUNT }
            .sumOf { it.amount.minorUnits }

        val adjustmentAdditionsMinor = receipt.adjustments
            .filter {
                it.type == ReceiptAdjustmentType.SURCHARGE ||
                    it.type == ReceiptAdjustmentType.TAX
            }
            .sumOf { it.amount.minorUnits }

        val totalDiscountMinor = itemDiscountsMinor + adjustmentDiscountsMinor
        val totalMinor = if (receipt.items.isNotEmpty() || fallbackTotalWhenNoItems == null) {
            (itemsNetMinor - adjustmentDiscountsMinor + adjustmentAdditionsMinor).coerceAtLeast(0L)
        } else {
            fallbackTotalWhenNoItems.minorUnits
        }

        return receipt.copy(
            subtotal = Money(subtotalMinor),
            totalDiscount = Money(totalDiscountMinor),
            total = Money(totalMinor)
        )
    }
}

