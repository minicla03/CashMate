package minicla03.cashmate.domain.usecase

import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.CategoryExpenseStat
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.MonthComparison
import minicla03.cashmate.domain.model.MonthlyTrendPoint
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionFilter
import minicla03.cashmate.domain.model.TransactionSortOrder
import minicla03.cashmate.domain.model.TransactionType
import java.time.YearMonth

/**
 * Pure domain calculations for balances, monthly summaries, category distributions, trends, and filtering.
 * Uses exact integer arithmetic via [Money] without floating-point calculations.
 */
object FinancialCalculator {

    private const val BASIS_POINTS_SCALE = 10_000L

    fun calculateBalance(transactions: List<Transaction>): Money {
        var netMinorUnits = 0L
        for (tx in transactions) {
            netMinorUnits += when (tx.type) {
                TransactionType.INCOME -> tx.amount.minorUnits
                TransactionType.EXPENSE -> -tx.amount.minorUnits
            }
        }
        return Money(netMinorUnits)
    }

    fun calculateIncome(
        transactions: List<Transaction>,
        yearMonth: YearMonth? = null
    ): Money = sumByType(transactions, TransactionType.INCOME, yearMonth)

    fun calculateExpenses(
        transactions: List<Transaction>,
        yearMonth: YearMonth? = null
    ): Money = sumByType(transactions, TransactionType.EXPENSE, yearMonth)

    fun calculateSavings(income: Money, expenses: Money): Money = income - expenses

    fun calculateCategoryExpenses(
        transactions: List<Transaction>,
        categoriesById: Map<String, Category>,
        yearMonth: YearMonth? = null
    ): List<CategoryExpenseStat> {
        val expenses = transactions.filter { tx ->
            tx.type == TransactionType.EXPENSE &&
                (yearMonth == null || YearMonth.from(tx.date) == yearMonth)
        }
        if (expenses.isEmpty()) return emptyList()

        val totalsByCategory = mutableMapOf<String, Long>()
        var totalMinorUnits = 0L
        for (tx in expenses) {
            totalsByCategory[tx.categoryId] =
                (totalsByCategory[tx.categoryId] ?: 0L) + tx.amount.minorUnits
            totalMinorUnits += tx.amount.minorUnits
        }
        if (totalMinorUnits <= 0L) return emptyList()

        return totalsByCategory.entries
            .sortedByDescending { it.value }
            .map { (categoryId, amountMinor) ->
                val category = categoriesById[categoryId] ?: Category(
                    id = categoryId,
                    name = categoryId,
                    icon = "category",
                    color = "#78909C"
                )
                val shareBps = ((amountMinor * BASIS_POINTS_SCALE) / totalMinorUnits).toInt()
                CategoryExpenseStat(
                    category = category,
                    amount = Money(amountMinor),
                    shareBasisPoints = shareBps
                )
            }
    }

    fun calculateMonthlyTrend(
        transactions: List<Transaction>,
        referenceMonth: YearMonth,
        monthCount: Int = 6
    ): List<MonthlyTrendPoint> {
        require(monthCount > 0) { "monthCount must be positive" }
        val startMonth = referenceMonth.minusMonths((monthCount - 1).toLong())
        return (0 until monthCount).map { offset ->
            val month = startMonth.plusMonths(offset.toLong())
            val income = calculateIncome(transactions, month)
            val expenses = calculateExpenses(transactions, month)
            MonthlyTrendPoint(
                yearMonth = month,
                income = income,
                expenses = expenses,
                savings = calculateSavings(income, expenses)
            )
        }
    }

    fun calculateMonthComparison(
        transactions: List<Transaction>,
        referenceMonth: YearMonth
    ): MonthComparison {
        val previousMonth = referenceMonth.minusMonths(1)
        val currentExpenses = calculateExpenses(transactions, referenceMonth)
        val previousExpenses = calculateExpenses(transactions, previousMonth)
        val expenseDelta = currentExpenses - previousExpenses
        val changeBasisPoints = if (previousExpenses.minorUnits > 0L) {
            ((expenseDelta.minorUnits * BASIS_POINTS_SCALE) / previousExpenses.minorUnits).toInt()
        } else {
            null
        }
        return MonthComparison(
            currentMonth = referenceMonth,
            previousMonth = previousMonth,
            currentExpenses = currentExpenses,
            previousExpenses = previousExpenses,
            expenseDelta = expenseDelta,
            changeBasisPoints = changeBasisPoints,
            currentIncome = calculateIncome(transactions, referenceMonth),
            previousIncome = calculateIncome(transactions, previousMonth)
        )
    }

    fun filterAndSortTransactions(
        transactions: List<Transaction>,
        filter: TransactionFilter,
        categoriesById: Map<String, Category> = emptyMap()
    ): List<Transaction> {
        val normalizedQuery = filter.query?.trim()?.takeIf { it.isNotEmpty() }

        val filtered = transactions.filter { tx ->
            val matchesQuery = normalizedQuery == null ||
                tx.description.contains(normalizedQuery, ignoreCase = true) ||
                (categoriesById[tx.categoryId]?.name?.contains(normalizedQuery, ignoreCase = true) == true)

            val matchesCategory = filter.categoryId == null || tx.categoryId == filter.categoryId
            val matchesType = filter.type == null || tx.type == filter.type
            val matchesFrom = filter.fromDate == null || !tx.date.isBefore(filter.fromDate)
            val matchesTo = filter.toDate == null || !tx.date.isAfter(filter.toDate)

            matchesQuery && matchesCategory && matchesType && matchesFrom && matchesTo
        }

        return when (filter.sortOrder) {
            TransactionSortOrder.DATE_DESC -> filtered.sortedWith(
                compareByDescending<Transaction> { it.date }.thenByDescending { it.id }
            )
            TransactionSortOrder.DATE_ASC -> filtered.sortedWith(
                compareBy<Transaction> { it.date }.thenBy { it.id }
            )
        }
    }

    private fun sumByType(
        transactions: List<Transaction>,
        type: TransactionType,
        yearMonth: YearMonth?
    ): Money {
        var totalMinor = 0L
        for (tx in transactions) {
            if (tx.type == type && (yearMonth == null || YearMonth.from(tx.date) == yearMonth)) {
                totalMinor += tx.amount.minorUnits
            }
        }
        return Money(totalMinor)
    }
}

