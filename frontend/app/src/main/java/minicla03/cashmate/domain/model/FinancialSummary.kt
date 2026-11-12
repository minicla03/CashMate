package minicla03.cashmate.domain.model

import java.time.YearMonth

data class CategoryExpenseStat(
    val category: Category,
    val amount: Money,
    val shareBasisPoints: Int
)

data class MonthlyTrendPoint(
    val yearMonth: YearMonth,
    val income: Money,
    val expenses: Money,
    val savings: Money
)

data class MonthComparison(
    val currentMonth: YearMonth,
    val previousMonth: YearMonth,
    val currentExpenses: Money,
    val previousExpenses: Money,
    val expenseDelta: Money,
    val changeBasisPoints: Int?,
    val currentIncome: Money,
    val previousIncome: Money
)

data class DashboardSummary(
    val currentBalance: Money,
    val monthlyIncome: Money,
    val monthlyExpenses: Money,
    val monthlySavings: Money,
    val recentTransactions: List<Transaction>,
    val expenseDistribution: List<CategoryExpenseStat>,
    val categoriesById: Map<String, Category>,
    val accountsById: Map<String, Account>
)

data class StatisticsSummary(
    val totalExpenses: Money,
    val totalIncome: Money,
    val expensesByCategory: List<CategoryExpenseStat>,
    val monthlyTrend: List<MonthlyTrendPoint>,
    val monthComparison: MonthComparison
)

