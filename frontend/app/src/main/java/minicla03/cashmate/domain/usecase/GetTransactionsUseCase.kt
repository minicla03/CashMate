package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.common.map
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.error.ValidationReason
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionFilter
import minicla03.cashmate.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        filter: TransactionFilter = TransactionFilter(),
        categoriesById: Map<String, Category> = emptyMap()
    ): AppResult<List<Transaction>> {
        if (filter.fromDate != null && filter.toDate != null && filter.fromDate.isAfter(filter.toDate)) {
            return AppResult.Error(
                AppError.Validation(mapOf(ValidationField.DATE to ValidationReason.INVALID_RANGE))
            )
        }
        return transactionRepository.getTransactions(filter).map { transactions ->
            FinancialCalculator.filterAndSortTransactions(
                transactions = transactions,
                filter = filter,
                categoriesById = categoriesById
            )
        }
    }
}

