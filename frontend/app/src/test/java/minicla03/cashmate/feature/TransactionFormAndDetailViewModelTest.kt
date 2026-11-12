package minicla03.cashmate.feature

import androidx.lifecycle.SavedStateHandle
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
import minicla03.cashmate.core.error.ValidationField
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
import minicla03.cashmate.domain.usecase.CreateTransactionUseCase
import minicla03.cashmate.domain.usecase.DeleteTransactionUseCase
import minicla03.cashmate.domain.usecase.GetAccountsUseCase
import minicla03.cashmate.domain.usecase.GetCategoriesUseCase
import minicla03.cashmate.domain.usecase.GetTransactionUseCase
import minicla03.cashmate.domain.usecase.TransactionValidator
import minicla03.cashmate.domain.usecase.UpdateTransactionUseCase
import minicla03.cashmate.feature.transactions.TransactionDetailEffect
import minicla03.cashmate.feature.transactions.TransactionDetailEvent
import minicla03.cashmate.feature.transactions.TransactionDetailViewModel
import minicla03.cashmate.feature.transactions.TransactionFormEffect
import minicla03.cashmate.feature.transactions.TransactionFormEvent
import minicla03.cashmate.feature.transactions.TransactionFormViewModel
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
import java.util.Currency

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionFormAndDetailViewModelTest {

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
    fun `form validates invalid fields and submits valid transaction`() = runTest(testDispatcher) {
        var createdTransaction: Transaction? = null
        val txRepo = FakeCrudTxRepo(onCreate = { createdTransaction = it })
        val formVm = createFormViewModel(transactionId = null, txRepo = txRepo)
        advanceUntilIdle()

        assertFalse(formVm.uiState.value.isEditMode)
        formVm.onEvent(TransactionFormEvent.Submit)

        val invalidState = formVm.uiState.value
        assertEquals(R.string.validation_amount_positive, invalidState.fieldErrors[ValidationField.AMOUNT])
        assertEquals(R.string.validation_description_required, invalidState.fieldErrors[ValidationField.DESCRIPTION])

        formVm.onEvent(TransactionFormEvent.AmountChanged("19,50"))
        formVm.onEvent(TransactionFormEvent.DescriptionChanged("Lunch"))
        formVm.onEvent(TransactionFormEvent.Submit)
        advanceUntilIdle()

        assertEquals(Money(1950L), createdTransaction?.amount)
        assertEquals("Lunch", createdTransaction?.description)
        assertEquals(TransactionFormEffect.NavigateBackSaved, formVm.effects.first())
    }

    @Test
    fun `edit mode preloads transaction and preserves inputs when submit fails`() = runTest(testDispatcher) {
        val existing = Transaction(
            id = "tx-9",
            amount = Money(4200L),
            type = TransactionType.EXPENSE,
            description = "Train",
            categoryId = "cat-food",
            accountId = "acc-main",
            date = LocalDate.of(2026, 11, 8)
        )
        val txRepo = FakeCrudTxRepo(
            existing = existing,
            updateResult = AppResult.Error(AppError.Network.ServerError)
        )
        val formVm = createFormViewModel(transactionId = "tx-9", txRepo = txRepo)
        advanceUntilIdle()

        val loaded = formVm.uiState.value
        assertTrue(loaded.isEditMode)
        assertEquals("42.00", loaded.amountInput)
        assertEquals("Train", loaded.description)

        formVm.onEvent(TransactionFormEvent.DescriptionChanged("High-speed Train"))
        formVm.onEvent(TransactionFormEvent.Submit)
        advanceUntilIdle()

        val afterError = formVm.uiState.value
        assertEquals(R.string.error_server, afterError.submitError?.messageResId)
        assertEquals("High-speed Train", afterError.description)
    }

    @Test
    fun `detail view model loads transaction and emits back effect after delete`() = runTest(testDispatcher) {
        val existing = Transaction(
            id = "tx-9",
            amount = Money(4200L),
            type = TransactionType.EXPENSE,
            description = "Train",
            categoryId = "cat-food",
            accountId = "acc-main",
            date = LocalDate.of(2026, 11, 8)
        )
        val txRepo = FakeCrudTxRepo(existing = existing)
        val detailVm = TransactionDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TransactionDetailViewModel.ARG_TRANSACTION_ID to "tx-9")),
            getTransactionUseCase = GetTransactionUseCase(txRepo),
            getCategoriesUseCase = GetCategoriesUseCase(fakeCatRepo()),
            getAccountsUseCase = GetAccountsUseCase(fakeAccountRepo()),
            deleteTransactionUseCase = DeleteTransactionUseCase(txRepo),
            userPreferencesRepository = InMemoryUserPreferencesRepository()
        )
        advanceUntilIdle()

        assertEquals("Train", detailVm.uiState.value.transaction?.description)
        assertNull(detailVm.uiState.value.error)

        detailVm.onEvent(TransactionDetailEvent.DeleteClick)
        assertTrue(detailVm.uiState.value.showDeleteDialog)

        detailVm.onEvent(TransactionDetailEvent.ConfirmDelete)
        advanceUntilIdle()

        assertEquals(TransactionDetailEffect.NavigateBackAfterDelete, detailVm.effects.first())
    }

    private fun createFormViewModel(
        transactionId: String?,
        txRepo: TransactionRepository
    ): TransactionFormViewModel {
        val savedStateHandle = if (transactionId != null) {
            SavedStateHandle(mapOf(TransactionFormViewModel.ARG_TRANSACTION_ID to transactionId))
        } else {
            SavedStateHandle()
        }
        val validator = TransactionValidator()
        return TransactionFormViewModel(
            savedStateHandle = savedStateHandle,
            getTransactionUseCase = GetTransactionUseCase(txRepo),
            getCategoriesUseCase = GetCategoriesUseCase(fakeCatRepo()),
            getAccountsUseCase = GetAccountsUseCase(fakeAccountRepo()),
            createTransactionUseCase = CreateTransactionUseCase(txRepo, validator),
            updateTransactionUseCase = UpdateTransactionUseCase(txRepo, validator),
            transactionValidator = validator,
            userPreferencesRepository = InMemoryUserPreferencesRepository(),
            clock = fixedClock
        )
    }

    private fun fakeCatRepo() = object : CategoryRepository {
        override suspend fun getCategories() = AppResult.Success(
            listOf(Category("cat-food", "Food", "restaurant", "#5E35B1"))
        )
        override suspend fun createCategory(category: Category) = AppResult.Success(category)
        override suspend fun updateCategory(category: Category) = AppResult.Success(category)
        override suspend fun deleteCategory(id: String) = AppResult.Success(Unit)
    }

    private fun fakeAccountRepo() = object : AccountRepository {
        override suspend fun getAccounts() = AppResult.Success(
            listOf(Account("acc-main", "Main Account", Currency.getInstance("EUR")))
        )
    }

    private class FakeCrudTxRepo(
        private val existing: Transaction? = null,
        private val updateResult: AppResult<Transaction>? = null,
        private val onCreate: (Transaction) -> Unit = {}
    ) : TransactionRepository {
        override suspend fun getTransactions(filter: TransactionFilter?) =
            AppResult.Success(listOfNotNull(existing))

        override suspend fun getTransaction(id: String): AppResult<Transaction> =
            existing?.takeIf { it.id == id }?.let { AppResult.Success(it) }
                ?: AppResult.Error(AppError.Network.NotFound)

        override suspend fun createTransaction(transaction: Transaction): AppResult<Transaction> {
            onCreate(transaction)
            return AppResult.Success(transaction.copy(id = "tx-new"))
        }

        override suspend fun updateTransaction(transaction: Transaction): AppResult<Transaction> =
            updateResult ?: AppResult.Success(transaction)

        override suspend fun deleteTransaction(id: String): AppResult<Unit> = AppResult.Success(Unit)
    }
}

