package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.repository.TransactionRepository
import javax.inject.Inject

class CreateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val validator: TransactionValidator
) {
    suspend operator fun invoke(transaction: Transaction): AppResult<Transaction> {
        val sanitized = transaction.copy(description = transaction.description.trim())
        validator.validate(sanitized)?.let { return AppResult.Error(it) }
        return transactionRepository.createTransaction(sanitized)
    }
}

