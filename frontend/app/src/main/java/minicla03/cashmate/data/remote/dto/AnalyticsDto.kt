package minicla03.cashmate.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardDto(
    val currentBalance: Long,
    val monthlyIncome: Long,
    val monthlyExpenses: Long,
    val monthlySavings: Long,
    val recentTransactions: List<TransactionDto>,
    val expenseDistribution: List<CategoryExpenseDto>
)

@Serializable
data class CategoryExpenseDto(
    val categoryId: String,
    val categoryName: String,
    val amount: Long,
    val shareBasisPoints: Int
)

@Serializable
data class MonthlyTrendDto(
    val month: String,
    val income: Long,
    val expenses: Long,
    val savings: Long
)

@Serializable
data class MonthComparisonDto(
    val currentMonth: String,
    val previousMonth: String,
    val currentExpenses: Long,
    val previousExpenses: Long,
    val expenseDelta: Long,
    val changeBasisPoints: Int? = null
)

@Serializable
data class StatisticsDto(
    val totalExpenses: Long,
    val totalIncome: Long,
    val expensesByCategory: List<CategoryExpenseDto>,
    val monthlyTrend: List<MonthlyTrendDto>,
    val monthComparison: MonthComparisonDto
)

