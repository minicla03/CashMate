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
import minicla03.cashmate.core.error.BusinessErrorReason
import minicla03.cashmate.data.repository.InMemoryUserPreferencesRepository
import minicla03.cashmate.domain.model.AppThemeMode
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionFilter
import minicla03.cashmate.domain.model.TransactionType
import minicla03.cashmate.domain.repository.CategoryRepository
import minicla03.cashmate.domain.repository.TransactionRepository
import minicla03.cashmate.domain.usecase.CategoryValidator
import minicla03.cashmate.domain.usecase.CreateCategoryUseCase
import minicla03.cashmate.domain.usecase.DeleteCategoryUseCase
import minicla03.cashmate.domain.usecase.GetCategoriesUseCase
import minicla03.cashmate.domain.usecase.GetStatisticsUseCase
import minicla03.cashmate.domain.usecase.UpdateCategoryUseCase
import minicla03.cashmate.feature.categories.CategoriesEvent
import minicla03.cashmate.feature.categories.CategoriesViewModel
import minicla03.cashmate.feature.settings.SettingsEffect
import minicla03.cashmate.feature.settings.SettingsEvent
import minicla03.cashmate.feature.settings.SettingsViewModel
import minicla03.cashmate.feature.statistics.StatisticsViewModel
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
class CategoriesStatisticsSettingsViewModelTest {

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
    fun `categories view model creates category and surfaces delete business rule error`() = runTest(testDispatcher) {
        val categories = mutableListOf(Category("cat-food", "Food", "restaurant", "#5E35B1"))
        val repo = object : CategoryRepository {
            override suspend fun getCategories() = AppResult.Success(categories.toList())
            override suspend fun createCategory(category: Category): AppResult<Category> {
                val created = category.copy(id = "cat-new")
                categories.add(created)
                return AppResult.Success(created)
            }
            override suspend fun updateCategory(category: Category) = AppResult.Success(category)
            override suspend fun deleteCategory(id: String): AppResult<Unit> =
                AppResult.Error(AppError.BusinessRule(BusinessErrorReason.CATEGORY_IN_USE))
        }
        val validator = CategoryValidator()
        val vm = CategoriesViewModel(
            getCategoriesUseCase = GetCategoriesUseCase(repo),
            createCategoryUseCase = CreateCategoryUseCase(repo, validator),
            updateCategoryUseCase = UpdateCategoryUseCase(repo, validator),
            deleteCategoryUseCase = DeleteCategoryUseCase(repo),
            categoryValidator = validator
        )
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.categories.size)

        vm.onEvent(CategoriesEvent.OpenCreateDialog)
        vm.onEvent(CategoriesEvent.EditorNameChanged("Travel"))
        vm.onEvent(CategoriesEvent.SubmitEditor)
        advanceUntilIdle()

        assertNull(vm.uiState.value.editor)
        assertEquals(2, vm.uiState.value.categories.size)
        assertEquals(R.string.category_saved_message, vm.uiState.value.snackbarMessageResId)

        vm.onEvent(CategoriesEvent.RequestDelete(categories.first()))
        vm.onEvent(CategoriesEvent.ConfirmDelete)
        advanceUntilIdle()

        assertEquals(R.string.error_category_in_use, vm.uiState.value.actionError?.messageResId)
    }

    @Test
    fun `statistics view model handles success empty and error states`() = runTest(testDispatcher) {
        val txRepo = object : TransactionRepository {
            override suspend fun getTransactions(filter: TransactionFilter?) = AppResult.Success(
                listOf(
                    Transaction("t1", Money(8000L), TransactionType.EXPENSE, "Dinner", "cat-food", "acc-main", LocalDate.of(2026, 11, 10))
                )
            )
            override suspend fun getTransaction(id: String) = AppResult.Error(AppError.Network.NotFound)
            override suspend fun createTransaction(transaction: Transaction) = AppResult.Success(transaction)
            override suspend fun updateTransaction(transaction: Transaction) = AppResult.Success(transaction)
            override suspend fun deleteTransaction(id: String) = AppResult.Success(Unit)
        }
        val catRepo = object : CategoryRepository {
            override suspend fun getCategories() = AppResult.Success(
                listOf(Category("cat-food", "Food", "restaurant", "#5E35B1"))
            )
            override suspend fun createCategory(category: Category) = AppResult.Success(category)
            override suspend fun updateCategory(category: Category) = AppResult.Success(category)
            override suspend fun deleteCategory(id: String) = AppResult.Success(Unit)
        }

        val vm = StatisticsViewModel(
            getStatisticsUseCase = GetStatisticsUseCase(txRepo, catRepo, fixedClock),
            userPreferencesRepository = InMemoryUserPreferencesRepository()
        )
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertNotNull(vm.uiState.value.summary)
        assertEquals(Money(8000L), vm.uiState.value.summary?.totalExpenses)
    }

    @Test
    fun `settings view model updates theme currency and emits category navigation`() = runTest(testDispatcher) {
        val prefsRepo = InMemoryUserPreferencesRepository()
        val vm = SettingsViewModel(prefsRepo)

        vm.onEvent(SettingsEvent.ThemeSelected(AppThemeMode.DARK))
        vm.onEvent(SettingsEvent.CurrencySelected(Currency.getInstance("USD")))
        advanceUntilIdle()

        assertEquals(AppThemeMode.DARK, vm.uiState.value.themeMode)
        assertEquals(Currency.getInstance("USD"), vm.uiState.value.selectedCurrency)

        vm.onEvent(SettingsEvent.ManageCategoriesClick)
        assertEquals(SettingsEffect.NavigateToCategories, vm.effects.first())
    }
}

