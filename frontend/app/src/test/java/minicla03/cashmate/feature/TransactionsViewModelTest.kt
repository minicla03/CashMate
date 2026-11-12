package minicla03.cashmate.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import minicla03.cashmate.R
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.data.repository.InMemoryUserPreferencesRepository
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionFilter
import minicla03.cashmate.domain.model.TransactionSortOrder
import minicla03.cashmate.domain.model.TransactionType
import minicla03.cashmate.domain.repository.CategoryRepository
import minicla03.cashmate.domain.repository.TransactionRepository
import minicla03.cashmate.domain.usecase.GetCategoriesUseCase
import minicla03.cashmate.domain.usecase.GetTransactionsUseCase
import minicla03.cashmate.feature.transactions.DateRangePreset
import minicla03.cashmate.feature.transactions.TransactionsEvent
import minicla03.cashmate.feature.transactions.TransactionsViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedClock = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC)

    private val sampleTransactions = listOf(
        Transaction("t1", Money(250_000L), TransactionType.INCOME, "Salary", "cat-salary", "acc-main", LocalDate.of(2026, 11, 1)),
        Transaction("t2", Money(4500L), TransactionType.EXPENSE, "Supermarket", "cat-food", "acc-main", LocalDate.of(2026, 11, 5)),
        Transaction("t3", Money(1800L), TransactionType.EXPENSE, "Train pass", "cat-transport", "acc-main", LocalDate.of(2026, 10, 20))
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `filters by search query type category date preset and toggles sort order`() = runTest(testDispatcher) {
        val viewModel = createViewModel(AppResult.Success(sampleTransactions))
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()
        assertEquals(listOf("t2", "t1", "t3"), viewModel.uiState.value.transactions.map { it.id })

        viewModel.onEvent(TransactionsEvent.ToggleSortOrder)
        advanceUntilIdle()
        assertEquals(TransactionSortOrder.DATE_ASC, viewModel.uiState.value.sortOrder)
        assertEquals(listOf("t3", "t1", "t2"), viewModel.uiState.value.transactions.map { it.id })

        viewModel.onEvent(TransactionsEvent.TypeFilterChanged(TransactionType.EXPENSE))
        advanceUntilIdle()
        assertEquals(listOf("t3", "t2"), viewModel.uiState.value.transactions.map { it.id })

        viewModel.onEvent(TransactionsEvent.DatePresetSelected(DateRangePreset.THIS_MONTH))
        advanceUntilIdle()
        assertEquals(listOf("t2"), viewModel.uiState.value.transactions.map { it.id })

        viewModel.onEvent(TransactionsEvent.SearchQueryChanged("nonexistent"))
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isEmpty)

        viewModel.onEvent(TransactionsEvent.ClearFilters)
        advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.transactions.size)
        assertFalse(viewModel.uiState.value.hasActiveFilters)
    }

    @Test
    fun `emits error state on timeout`() = runTest(testDispatcher) {
        val viewModel = createViewModel(AppResult.Error(AppError.Network.Timeout))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(R.string.error_timeout, state.error?.messageResId)
        assertNull(state.transactions.firstOrNull())
    }

    private fun createViewModel(txResult: AppResult<List<Transaction>>): TransactionsViewModel {
        val txRepo = object : TransactionRepository {
            override suspend fun getTransactions(filter: TransactionFilter?) = txResult
            override suspend fun getTransaction(id: String) = AppResult.Error(AppError.Network.NotFound)
            override suspend fun createTransaction(transaction: Transaction) = AppResult.Success(transaction)
            override suspend fun updateTransaction(transaction: Transaction) = AppResult.Success(transaction)
            override suspend fun deleteTransaction(id: String) = AppResult.Success(Unit)
        }
        val catRepo = object : CategoryRepository {
            override suspend fun getCategories() = AppResult.Success(
                listOf(
                    Category("cat-salary", "Salary", "payments", "#2E7D32"),
                    Category("cat-food", "Food", "restaurant", "#5E35B1"),
                    Category("cat-transport", "Transport", "directions_car", "#00897B")
                )
            )
            override suspend fun createCategory(category: Category) = AppResult.Success(category)
            override suspend fun updateCategory(category: Category) = AppResult.Success(category)
            override suspend fun deleteCategory(id: String) = AppResult.Success(Unit)
        }
        return TransactionsViewModel(
            getTransactionsUseCase = GetTransactionsUseCase(txRepo),
            getCategoriesUseCase = GetCategoriesUseCase(catRepo),
            userPreferencesRepository = InMemoryUserPreferencesRepository(),
            clock = fixedClock
        )
    }
}

