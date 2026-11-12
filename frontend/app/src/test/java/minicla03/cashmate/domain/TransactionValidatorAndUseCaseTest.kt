package minicla03.cashmate.domain

import kotlinx.coroutines.test.runTest
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.error.ValidationReason
import minicla03.cashmate.domain.model.Account
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionFilter
import minicla03.cashmate.domain.model.TransactionType
import minicla03.cashmate.domain.repository.AccountRepository
import minicla03.cashmate.domain.repository.CategoryRepository
import minicla03.cashmate.domain.repository.TransactionRepository
import minicla03.cashmate.domain.usecase.CategoryValidator
import minicla03.cashmate.domain.usecase.CreateCategoryUseCase
import minicla03.cashmate.domain.usecase.CreateTransactionUseCase
import minicla03.cashmate.domain.usecase.GetDashboardUseCase
import minicla03.cashmate.domain.usecase.GetStatisticsUseCase
import minicla03.cashmate.domain.usecase.GetTransactionsUseCase
import minicla03.cashmate.domain.usecase.TransactionValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Currency

class TransactionValidatorAndUseCaseTest {

    private val fixedClock = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC)
    private val validator = TransactionValidator()

    @Test
    fun `transaction validator flags non-positive amount blank description and missing ids`() {
        val invalid = Transaction(
            id = "",
            amount = Money.ZERO,
            type = TransactionType.EXPENSE,
            description = "   ",
            categoryId = "",
            accountId = "",
            date = LocalDate.of(2026, 11, 11)
        )

        val result = validator.validate(invalid)
        assertEquals(
            mapOf(
                ValidationField.AMOUNT to ValidationReason.MUST_BE_POSITIVE,
                ValidationField.DESCRIPTION to ValidationReason.REQUIRED,
                ValidationField.CATEGORY to ValidationReason.REQUIRED,
                ValidationField.ACCOUNT to ValidationReason.REQUIRED
            ),
            result?.fieldErrors
        )
    }

    @Test
    fun `create transaction trims description before calling repository`() = runTest {
        var saved: Transaction? = null
        val repo = object : StubTransactionRepository() {
            override suspend fun createTransaction(transaction: Transaction): AppResult<Transaction> {
                saved = transaction
                return AppResult.Success(transaction.copy(id = "tx-1"))
            }
        }
        val useCase = CreateTransactionUseCase(repo, validator)

        val result = useCase(
            Transaction(
                id = "",
                amount = Money(2500L),
                type = TransactionType.EXPENSE,
                description = "  Coffee  ",
                categoryId = "cat-food",
                accountId = "acc-main",
                date = LocalDate.of(2026, 11, 11)
            )
        )

        assertTrue(result is AppResult.Success)
        assertEquals("Coffee", saved?.description)
    }

    @Test
    fun `get transactions rejects inverted date range`() = runTest {
        val useCase = GetTransactionsUseCase(StubTransactionRepository())
        val result = useCase(
            TransactionFilter(
                fromDate = LocalDate.of(2026, 11, 15),
                toDate = LocalDate.of(2026, 11, 10)
            )
        )

        assertTrue(result is AppResult.Error)
        val err = (result as AppResult.Error).error as AppError.Validation
        assertEquals(ValidationReason.INVALID_RANGE, err.fieldErrors[ValidationField.DATE])
    }

    @Test
    fun `create category validates empty and overly long names`() = runTest {
        val categoryRepo = object : CategoryRepository {
            override suspend fun getCategories() = AppResult.Success(emptyList<Category>())
            override suspend fun createCategory(category: Category) = AppResult.Success(category)
            override suspend fun updateCategory(category: Category) = AppResult.Success(category)
            override suspend fun deleteCategory(id: String) = AppResult.Success(Unit)
        }
        val useCase = CreateCategoryUseCase(categoryRepo, CategoryValidator())

        val emptyRes = useCase(Category("", "   ", "category", "#000000"))
        assertTrue(emptyRes is AppResult.Error)

        val longRes = useCase(Category("", "a".repeat(41), "category", "#000000"))
        val err = (longRes as AppResult.Error).error as AppError.Validation
        assertEquals(ValidationReason.TOO_LONG, err.fieldErrors[ValidationField.CATEGORY_NAME])
    }

    @Test
    fun `get dashboard and statistics aggregate data and propagate network errors`() = runTest {
        val tx = Transaction(
            id = "tx-1",
            amount = Money(5000L),
            type = TransactionType.EXPENSE,
            description = "Lunch",
            categoryId = "cat-food",
            accountId = "acc-main",
            date = LocalDate.of(2026, 11, 11)
        )
        val txRepo = object : StubTransactionRepository() {
            override suspend fun getTransactions(filter: TransactionFilter?) = AppResult.Success(listOf(tx))
        }
        val catRepo = object : CategoryRepository {
            override suspend fun getCategories() = AppResult.Success(
                listOf(Category("cat-food", "Food", "restaurant", "#5E35B1"))
            )
            override suspend fun createCategory(category: Category) = AppResult.Success(category)
            override suspend fun updateCategory(category: Category) = AppResult.Success(category)
            override suspend fun deleteCategory(id: String) = AppResult.Success(Unit)
        }
        val accRepo = object : AccountRepository {
            override suspend fun getAccounts() = AppResult.Success(
                listOf(Account("acc-main", "Main", Currency.getInstance("EUR")))
            )
        }

        val dashboard = GetDashboardUseCase(txRepo, catRepo, accRepo, fixedClock)()
        assertTrue(dashboard is AppResult.Success)
        val summary = (dashboard as AppResult.Success).data
        assertEquals(Money(-5000L), summary.currentBalance)
        assertEquals(Money(5000L), summary.monthlyExpenses)

        val stats = GetStatisticsUseCase(txRepo, catRepo, fixedClock)()
        assertTrue(stats is AppResult.Success)
        assertEquals(Money(5000L), (stats as AppResult.Success).data.totalExpenses)

        val failingCatRepo = object : CategoryRepository by catRepo {
            override suspend fun getCategories(): AppResult<List<Category>> =
                AppResult.Error(AppError.Network.NoInternet)
        }
        val failedDashboard = GetDashboardUseCase(txRepo, failingCatRepo, accRepo, fixedClock)()
        assertEquals(AppResult.Error(AppError.Network.NoInternet), failedDashboard)
    }

    private open class StubTransactionRepository : TransactionRepository {
        override suspend fun getTransactions(filter: TransactionFilter?): AppResult<List<Transaction>> =
            AppResult.Success(emptyList())
        override suspend fun getTransaction(id: String): AppResult<Transaction> =
            AppResult.Error(AppError.Network.NotFound)
        override suspend fun createTransaction(transaction: Transaction): AppResult<Transaction> =
            AppResult.Success(transaction)
        override suspend fun updateTransaction(transaction: Transaction): AppResult<Transaction> =
            AppResult.Success(transaction)
        override suspend fun deleteTransaction(id: String): AppResult<Unit> =
            AppResult.Success(Unit)
    }
}

