package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Atomically updates a receipt-originated transaction and its associated receipt,
 * ensuring [Transaction.amount] stays synchronized with [Receipt.total].
 */
class UpdateTransactionWithReceiptUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val transactionValidator: TransactionValidator,
    private val receiptValidator: ReceiptValidator
) {
    suspend operator fun invoke(
        transaction: Transaction,
        receipt: Receipt
    ): AppResult<Transaction> {
        val trimmedDescription = transaction.description.trim()
        val sanitizedItems = receipt.items.map { item ->
            ReceiptCalculator.calculateItem(
                id = item.id,
                description = item.description.trim(),
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                discountAmount = item.discountAmount,
                category = item.category.trim()
            )
        }
        val sanitizedReceiptBase = receipt.copy(
            merchant = trimmedDescription.ifBlank { receipt.merchant.trim() },
            date = transaction.date,
            categoryId = transaction.categoryId,
            accountId = transaction.accountId,
            items = sanitizedItems,
            adjustments = receipt.adjustments.map { it.copy(description = it.description.trim()) }
        )
        val recalculatedReceipt = ReceiptCalculator.recalculateTotals(
            receipt = sanitizedReceiptBase,
            fallbackTotalWhenNoItems = transaction.amount
        )
        val synchronizedTransaction = transaction.copy(
            description = trimmedDescription,
            amount = recalculatedReceipt.total
        )

        transactionValidator.validate(synchronizedTransaction)?.let { return AppResult.Error(it) }
        receiptValidator.validate(recalculatedReceipt)?.let { return AppResult.Error(it) }

        return transactionRepository.updateTransactionWithReceipt(
            transaction = synchronizedTransaction,
            receipt = recalculatedReceipt
        )
    }
}

