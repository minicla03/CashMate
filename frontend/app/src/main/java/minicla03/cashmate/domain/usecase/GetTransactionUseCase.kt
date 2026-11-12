package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(id: String): AppResult<Transaction> =
        transactionRepository.getTransaction(id)
}

