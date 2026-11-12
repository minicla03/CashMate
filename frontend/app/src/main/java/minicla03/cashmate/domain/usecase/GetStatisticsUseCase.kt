package minicla03.cashmate.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.StatisticsSummary
import minicla03.cashmate.domain.repository.CategoryRepository
import minicla03.cashmate.domain.repository.TransactionRepository
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

class GetStatisticsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(trendMonths: Int = DEFAULT_TREND_MONTHS): AppResult<StatisticsSummary> =
        coroutineScope {
            val transactionsDeferred = async { transactionRepository.getTransactions() }
            val categoriesDeferred = async { categoryRepository.getCategories() }

            val transactionsResult = transactionsDeferred.await()
            val categoriesResult = categoriesDeferred.await()

            if (transactionsResult is AppResult.Error) return@coroutineScope transactionsResult
            if (categoriesResult is AppResult.Error) return@coroutineScope categoriesResult

            val transactions = (transactionsResult as AppResult.Success).data
            val categories = (categoriesResult as AppResult.Success).data
            val categoriesById = categories.associateBy { it.id }
            val currentMonth = YearMonth.now(clock)

            AppResult.Success(
                StatisticsSummary(
                    totalExpenses = FinancialCalculator.calculateExpenses(transactions),
                    totalIncome = FinancialCalculator.calculateIncome(transactions),
                    expensesByCategory = FinancialCalculator.calculateCategoryExpenses(
                        transactions = transactions,
                        categoriesById = categoriesById,
                        yearMonth = null
                    ),
                    monthlyTrend = FinancialCalculator.calculateMonthlyTrend(
                        transactions = transactions,
                        referenceMonth = currentMonth,
                        monthCount = trendMonths
                    ),
                    monthComparison = FinancialCalculator.calculateMonthComparison(
                        transactions = transactions,
                        referenceMonth = currentMonth
                    )
                )
            )
        }

    companion object {
        const val DEFAULT_TREND_MONTHS = 6
    }
}

