package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(id: String): AppResult<Unit> =
        transactionRepository.deleteTransaction(id)
}

