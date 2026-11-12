package minicla03.cashmate.domain

import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionFilter
import minicla03.cashmate.domain.model.TransactionSortOrder
import minicla03.cashmate.domain.model.TransactionType
import minicla03.cashmate.domain.usecase.FinancialCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FinancialCalculatorTest {

    private val food = Category("cat-food", "Food", "restaurant", "#5E35B1")
    private val transport = Category("cat-transport", "Transport", "directions_car", "#00897B")
    private val salary = Category("cat-salary", "Salary", "payments", "#2E7D32")
    private val categoriesById = listOf(food, transport, salary).associateBy { it.id }

    private val sampleTransactions = listOf(
        Transaction("t1", Money(200_000L), TransactionType.INCOME, "October Salary", "cat-salary", "acc-main", LocalDate.of(2026, 10, 1)),
        Transaction("t2", Money(10_000L), TransactionType.EXPENSE, "Groceries", "cat-food", "acc-main", LocalDate.of(2026, 10, 10)),
        Transaction("t3", Money(300_000L), TransactionType.INCOME, "November Salary", "cat-salary", "acc-main", LocalDate.of(2026, 11, 1)),
        Transaction("t4", Money(30_000L), TransactionType.EXPENSE, "Supermarket", "cat-food", "acc-main", LocalDate.of(2026, 11, 5)),
        Transaction("t5", Money(10_000L), TransactionType.EXPENSE, "Train Ticket", "cat-transport", "acc-main", LocalDate.of(2026, 11, 8))
    )

    @Test
    fun `calculates balance income expenses and savings accurately`() {
        val balance = FinancialCalculator.calculateBalance(sampleTransactions)
        assertEquals(Money(450_000L), balance)

        val nov = YearMonth.of(2026, 11)
        val novIncome = FinancialCalculator.calculateIncome(sampleTransactions, nov)
        val novExpenses = FinancialCalculator.calculateExpenses(sampleTransactions, nov)
        val novSavings = FinancialCalculator.calculateSavings(novIncome, novExpenses)

        assertEquals(Money(300_000L), novIncome)
        assertEquals(Money(40_000L), novExpenses)
        assertEquals(Money(260_000L), novSavings)
    }

    @Test
    fun `calculates category expense stats sorted descending with basis points`() {
        val stats = FinancialCalculator.calculateCategoryExpenses(
            transactions = sampleTransactions,
            categoriesById = categoriesById,
            yearMonth = YearMonth.of(2026, 11)
        )

        assertEquals(2, stats.size)
        assertEquals("cat-food", stats[0].category.id)
        assertEquals(Money(30_000L), stats[0].amount)
        assertEquals(7500, stats[0].shareBasisPoints)

        assertEquals("cat-transport", stats[1].category.id)
        assertEquals(Money(10_000L), stats[1].amount)
        assertEquals(2500, stats[1].shareBasisPoints)
    }

    @Test
    fun `calculates monthly trend and month over month comparison`() {
        val nov = YearMonth.of(2026, 11)
        val trend = FinancialCalculator.calculateMonthlyTrend(
            transactions = sampleTransactions,
            referenceMonth = nov,
            monthCount = 3
        )

        assertEquals(3, trend.size)
        assertEquals(YearMonth.of(2026, 9), trend[0].yearMonth)
        assertEquals(Money.ZERO, trend[0].expenses)

        assertEquals(YearMonth.of(2026, 10), trend[1].yearMonth)
        assertEquals(Money(200_000L), trend[1].income)
        assertEquals(Money(10_000L), trend[1].expenses)
        assertEquals(Money(190_000L), trend[1].savings)

        val comparison = FinancialCalculator.calculateMonthComparison(sampleTransactions, nov)
        assertEquals(Money(40_000L), comparison.currentExpenses)
        assertEquals(Money(10_000L), comparison.previousExpenses)
        assertEquals(Money(30_000L), comparison.expenseDelta)
        assertEquals(30_000, comparison.changeBasisPoints)
    }

    @Test
    fun `month comparison returns null percentage when previous month has zero expenses`() {
        val oct = YearMonth.of(2026, 10)
        val comparison = FinancialCalculator.calculateMonthComparison(sampleTransactions, oct)
        assertEquals(Money(10_000L), comparison.currentExpenses)
        assertEquals(Money.ZERO, comparison.previousExpenses)
        assertNull(comparison.changeBasisPoints)
    }

    @Test
    fun `filters and sorts transactions by query category type date range and sort order`() {
        val byQueryOnCategory = FinancialCalculator.filterAndSortTransactions(
            transactions = sampleTransactions,
            filter = TransactionFilter(query = "food"),
            categoriesById = categoriesById
        )
        assertEquals(listOf("t4", "t2"), byQueryOnCategory.map { it.id })

        val byRangeAndTypeAsc = FinancialCalculator.filterAndSortTransactions(
            transactions = sampleTransactions,
            filter = TransactionFilter(
                type = TransactionType.EXPENSE,
                fromDate = LocalDate.of(2026, 11, 1),
                toDate = LocalDate.of(2026, 11, 8),
                sortOrder = TransactionSortOrder.DATE_ASC
            ),
            categoriesById = categoriesById
        )
        assertEquals(listOf("t4", "t5"), byRangeAndTypeAsc.map { it.id })
    }
}

