package minicla03.cashmate.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import minicla03.cashmate.R
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.data.repository.InMemoryUserPreferencesRepository
import minicla03.cashmate.domain.model.Account
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionFilter
import minicla03.cashmate.domain.model.TransactionType
import minicla03.cashmate.domain.repository.AccountRepository
import minicla03.cashmate.domain.repository.CategoryRepository
import minicla03.cashmate.domain.repository.TransactionRepository
import minicla03.cashmate.domain.usecase.GetDashboardUseCase
import minicla03.cashmate.feature.dashboard.DashboardEffect
import minicla03.cashmate.feature.dashboard.DashboardEvent
import minicla03.cashmate.feature.dashboard.DashboardViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Currency

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedClock = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits loading then success state and navigation effects`() = runTest(testDispatcher) {
        val transactions = listOf(
            Transaction("t1", Money(100_000L), TransactionType.INCOME, "Salary", "cat-salary", "acc-main", LocalDate.of(2026, 11, 1)),
            Transaction("t2", Money(25_000L), TransactionType.EXPENSE, "Groceries", "cat-food", "acc-main", LocalDate.of(2026, 11, 10))
        )
        val viewModel = createViewModel(txResult = AppResult.Success(transactions))

        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEmpty)
        assertNull(state.error)
        assertNotNull(state.summary)
        assertEquals(Money(75_000L), state.summary?.currentBalance)

        viewModel.onEvent(DashboardEvent.AddTransactionClick)
        assertEquals(DashboardEffect.NavigateToAddTransaction, viewModel.effects.first())

        viewModel.onEvent(DashboardEvent.TransactionClick("t2"))
        assertEquals(DashboardEffect.NavigateToTransactionDetail("t2"), viewModel.effects.first())
    }

    @Test
    fun `emits empty state when backend returns no transactions`() = runTest(testDispatcher) {
        val viewModel = createViewModel(txResult = AppResult.Success(emptyList()))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertNull(state.error)
    }

    @Test
    fun `emits error state on failure and recovers on retry`() = runTest(testDispatcher) {
        var result: AppResult<List<Transaction>> = AppResult.Error(AppError.Network.NoInternet)
        val txRepo = object : TransactionRepository {
            override suspend fun getTransactions(filter: TransactionFilter?) = result
            override suspend fun getTransaction(id: String) = AppResult.Error(AppError.Network.NotFound)
            override suspend fun createTransaction(transaction: Transaction) = AppResult.Success(transaction)
            override suspend fun updateTransaction(transaction: Transaction) = AppResult.Success(transaction)
            override suspend fun deleteTransaction(id: String) = AppResult.Success(Unit)
        }
        val viewModel = createViewModel(txRepo = txRepo)
        advanceUntilIdle()

        val errorState = viewModel.uiState.value
        assertFalse(errorState.isLoading)
        assertEquals(R.string.error_no_internet, errorState.error?.messageResId)

        result = AppResult.Success(
            listOf(
                Transaction("t1", Money(5000L), TransactionType.INCOME, "Bonus", "cat-salary", "acc-main", LocalDate.of(2026, 11, 11))
            )
        )
        viewModel.onEvent(DashboardEvent.Refresh)
        advanceUntilIdle()

        val recovered = viewModel.uiState.value
        assertNull(recovered.error)
        assertEquals(Money(5000L), recovered.summary?.currentBalance)
    }

    private fun createViewModel(
        txResult: AppResult<List<Transaction>> = AppResult.Success(emptyList()),
        txRepo: TransactionRepository? = null
    ): DashboardViewModel {
        val effectiveTxRepo = txRepo ?: object : TransactionRepository {
            override suspend fun getTransactions(filter: TransactionFilter?) = txResult
            override suspend fun getTransaction(id: String) = AppResult.Error(AppError.Network.NotFound)
            override suspend fun createTransaction(transaction: Transaction) = AppResult.Success(transaction)
            override suspend fun updateTransaction(transaction: Transaction) = AppResult.Success(transaction)
            override suspend fun deleteTransaction(id: String) = AppResult.Success(Unit)
        }
        val categoryRepo = object : CategoryRepository {
            override suspend fun getCategories() = AppResult.Success(
                listOf(
                    Category("cat-salary", "Salary", "payments", "#2E7D32"),
                    Category("cat-food", "Food", "restaurant", "#5E35B1")
                )
            )
            override suspend fun createCategory(category: Category) = AppResult.Success(category)
            override suspend fun updateCategory(category: Category) = AppResult.Success(category)
            override suspend fun deleteCategory(id: String) = AppResult.Success(Unit)
        }
        val accountRepo = object : AccountRepository {
            override suspend fun getAccounts() = AppResult.Success(
                listOf(Account("acc-main", "Main", Currency.getInstance("EUR")))
            )
        }
        return DashboardViewModel(
            getDashboardUseCase = GetDashboardUseCase(effectiveTxRepo, categoryRepo, accountRepo, fixedClock),
            userPreferencesRepository = InMemoryUserPreferencesRepository()
        )
    }
}

