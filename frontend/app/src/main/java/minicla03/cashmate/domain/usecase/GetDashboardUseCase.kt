package minicla03.cashmate.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.DashboardSummary
import minicla03.cashmate.domain.model.TransactionFilter
import minicla03.cashmate.domain.model.TransactionSortOrder
import minicla03.cashmate.domain.repository.AccountRepository
import minicla03.cashmate.domain.repository.CategoryRepository
import minicla03.cashmate.domain.repository.TransactionRepository
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

class GetDashboardUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(recentLimit: Int = DEFAULT_RECENT_LIMIT): AppResult<DashboardSummary> =
        coroutineScope {
            val transactionsDeferred = async { transactionRepository.getTransactions() }
            val categoriesDeferred = async { categoryRepository.getCategories() }
            val accountsDeferred = async { accountRepository.getAccounts() }

            val transactionsResult = transactionsDeferred.await()
            val categoriesResult = categoriesDeferred.await()
            val accountsResult = accountsDeferred.await()

            if (transactionsResult is AppResult.Error) return@coroutineScope transactionsResult
            if (categoriesResult is AppResult.Error) return@coroutineScope categoriesResult
            if (accountsResult is AppResult.Error) return@coroutineScope accountsResult

            val transactions = (transactionsResult as AppResult.Success).data
            val categories = (categoriesResult as AppResult.Success).data
            val accounts = (accountsResult as AppResult.Success).data

            val categoriesById = categories.associateBy { it.id }
            val accountsById = accounts.associateBy { it.id }
            val currentMonth = YearMonth.now(clock)

            val monthlyIncome = FinancialCalculator.calculateIncome(transactions, currentMonth)
            val monthlyExpenses = FinancialCalculator.calculateExpenses(transactions, currentMonth)
            val recentTransactions = FinancialCalculator.filterAndSortTransactions(
                transactions = transactions,
                filter = TransactionFilter(sortOrder = TransactionSortOrder.DATE_DESC),
                categoriesById = categoriesById
            ).take(recentLimit)

            AppResult.Success(
                DashboardSummary(
                    currentBalance = FinancialCalculator.calculateBalance(transactions),
                    monthlyIncome = monthlyIncome,
                    monthlyExpenses = monthlyExpenses,
                    monthlySavings = FinancialCalculator.calculateSavings(monthlyIncome, monthlyExpenses),
                    recentTransactions = recentTransactions,
                    expenseDistribution = FinancialCalculator.calculateCategoryExpenses(
                        transactions = transactions,
                        categoriesById = categoriesById,
                        yearMonth = currentMonth
                    ),
                    categoriesById = categoriesById,
                    accountsById = accountsById
                )
            )
        }

    companion object {
        const val DEFAULT_RECENT_LIMIT = 5
    }
}

