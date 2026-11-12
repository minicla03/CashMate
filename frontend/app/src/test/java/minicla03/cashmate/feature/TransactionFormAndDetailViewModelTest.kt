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
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.ReceiptAdjustment
import minicla03.cashmate.domain.model.ReceiptAdjustmentType
import minicla03.cashmate.domain.model.ReceiptImage
import minicla03.cashmate.domain.model.ReceiptItem
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionFilter
import minicla03.cashmate.domain.model.TransactionSource
import minicla03.cashmate.domain.model.TransactionType
import minicla03.cashmate.domain.repository.AccountRepository
import minicla03.cashmate.domain.repository.CategoryRepository
import minicla03.cashmate.domain.repository.ReceiptRepository
import minicla03.cashmate.domain.repository.TransactionRepository
import minicla03.cashmate.domain.usecase.CreateTransactionUseCase
import minicla03.cashmate.domain.usecase.DeleteTransactionUseCase
import minicla03.cashmate.domain.usecase.GetAccountsUseCase
import minicla03.cashmate.domain.usecase.GetCategoriesUseCase
import minicla03.cashmate.domain.usecase.GetTransactionReceiptUseCase
import minicla03.cashmate.domain.usecase.GetTransactionUseCase
import minicla03.cashmate.domain.usecase.ReceiptValidator
import minicla03.cashmate.domain.usecase.TransactionValidator
import minicla03.cashmate.domain.usecase.UpdateTransactionUseCase
import minicla03.cashmate.domain.usecase.UpdateTransactionWithReceiptUseCase
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
        assertFalse(loaded.isReceiptTransaction)
        assertNull(loaded.receipt)
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
            getTransactionReceiptUseCase = GetTransactionReceiptUseCase(fakeReceiptRepo()),
            getCategoriesUseCase = GetCategoriesUseCase(fakeCatRepo()),
            getAccountsUseCase = GetAccountsUseCase(fakeAccountRepo()),
            deleteTransactionUseCase = DeleteTransactionUseCase(txRepo),
            userPreferencesRepository = InMemoryUserPreferencesRepository()
        )
        advanceUntilIdle()

        assertEquals("Train", detailVm.uiState.value.transaction?.description)
        assertNull(detailVm.uiState.value.receipt)
        assertFalse(detailVm.uiState.value.receiptUnavailable)
        assertNull(detailVm.uiState.value.error)

        detailVm.onEvent(TransactionDetailEvent.DeleteClick)
        assertTrue(detailVm.uiState.value.showDeleteDialog)

        detailVm.onEvent(TransactionDetailEvent.ConfirmDelete)
        advanceUntilIdle()

        assertEquals(TransactionDetailEffect.NavigateBackAfterDelete, detailVm.effects.first())
    }

    @Test
    fun `detail view model loads receipt details for receipt-originated transaction`() =
        runTest(testDispatcher) {
            val receiptTx = Transaction(
                id = "tx-receipt",
                amount = Money(4280L),
                type = TransactionType.EXPENSE,
                description = "Esselunga",
                categoryId = "cat-food",
                accountId = "acc-main",
                date = LocalDate.of(2026, 11, 12),
                source = TransactionSource.RECEIPT,
                receiptId = "receipt-123"
            )
            val sampleReceipt = Receipt(
                id = "receipt-123",
                merchant = "Esselunga",
                date = LocalDate.of(2026, 11, 12),
                subtotal = Money(4780L),
                totalDiscount = Money(500L),
                total = Money(4280L),
                items = listOf(
                    ReceiptItem(
                        id = "item-1",
                        description = "Pasta Barilla",
                        quantity = 1,
                        unitPrice = Money(250L),
                        grossAmount = Money(250L),
                        discountAmount = Money(50L),
                        netAmount = Money(200L),
                        category = "FOOD"
                    )
                ),
                adjustments = listOf(
                    ReceiptAdjustment(
                        id = "adj-1",
                        description = "Fidaty coupon",
                        amount = Money(450L),
                        type = ReceiptAdjustmentType.DISCOUNT
                    )
                )
            )
            val txRepo = FakeCrudTxRepo(existing = receiptTx)
            val detailVm = TransactionDetailViewModel(
                savedStateHandle = SavedStateHandle(mapOf(TransactionDetailViewModel.ARG_TRANSACTION_ID to "tx-receipt")),
                getTransactionUseCase = GetTransactionUseCase(txRepo),
                getTransactionReceiptUseCase = GetTransactionReceiptUseCase(
                    fakeReceiptRepo(AppResult.Success(sampleReceipt))
                ),
                getCategoriesUseCase = GetCategoriesUseCase(fakeCatRepo()),
                getAccountsUseCase = GetAccountsUseCase(fakeAccountRepo()),
                deleteTransactionUseCase = DeleteTransactionUseCase(txRepo),
                userPreferencesRepository = InMemoryUserPreferencesRepository()
            )
            advanceUntilIdle()

            val state = detailVm.uiState.value
            assertEquals("Esselunga", state.transaction?.description)
            assertEquals(sampleReceipt, state.receipt)
            assertFalse(state.isReceiptLoading)
            assertFalse(state.receiptUnavailable)
            assertNull(state.error)
        }

    @Test
    fun `detail view model keeps transaction visible when receipt lookup fails`() =
        runTest(testDispatcher) {
            val receiptTx = Transaction(
                id = "tx-receipt",
                amount = Money(4280L),
                type = TransactionType.EXPENSE,
                description = "Esselunga",
                categoryId = "cat-food",
                accountId = "acc-main",
                date = LocalDate.of(2026, 11, 12),
                source = TransactionSource.RECEIPT,
                receiptId = "receipt-missing"
            )
            val txRepo = FakeCrudTxRepo(existing = receiptTx)
            val detailVm = TransactionDetailViewModel(
                savedStateHandle = SavedStateHandle(mapOf(TransactionDetailViewModel.ARG_TRANSACTION_ID to "tx-receipt")),
                getTransactionUseCase = GetTransactionUseCase(txRepo),
                getTransactionReceiptUseCase = GetTransactionReceiptUseCase(
                    fakeReceiptRepo(AppResult.Error(AppError.Network.NotFound))
                ),
                getCategoriesUseCase = GetCategoriesUseCase(fakeCatRepo()),
                getAccountsUseCase = GetAccountsUseCase(fakeAccountRepo()),
                deleteTransactionUseCase = DeleteTransactionUseCase(txRepo),
                userPreferencesRepository = InMemoryUserPreferencesRepository()
            )
            advanceUntilIdle()

            val state = detailVm.uiState.value
            assertEquals("Esselunga", state.transaction?.description)
            assertNull(state.receipt)
            assertFalse(state.isReceiptLoading)
            assertTrue(state.receiptUnavailable)
            assertNull(state.error)
        }

    @Test
    fun `receipt edit mode modifies removes and adds items recalculates totals and syncs transaction amount`() =
        runTest(testDispatcher) {
            val receiptTx = Transaction(
                id = "tx-receipt",
                amount = Money(1250L),
                type = TransactionType.EXPENSE,
                description = "Conad",
                categoryId = "cat-food",
                accountId = "acc-main",
                date = LocalDate.of(2026, 11, 12),
                source = TransactionSource.RECEIPT,
                receiptId = "receipt-1"
            )
            val initialReceipt = Receipt(
                id = "receipt-1",
                merchant = "Conad",
                date = LocalDate.of(2026, 11, 12),
                subtotal = Money(1700L),
                totalDiscount = Money(450L),
                total = Money(1250L),
                categoryId = "cat-food",
                accountId = "acc-main",
                items = listOf(
                    ReceiptItem(
                        id = "item-1",
                        description = "Pasta Barilla",
                        quantity = 2,
                        unitPrice = Money(150L),
                        grossAmount = Money(300L),
                        discountAmount = Money(50L),
                        netAmount = Money(250L),
                        category = "Food"
                    ),
                    ReceiptItem(
                        id = "item-2",
                        description = "Olio Extra Vergine",
                        quantity = 1,
                        unitPrice = Money(1400L),
                        grossAmount = Money(1400L),
                        discountAmount = Money(200L),
                        netAmount = Money(1200L),
                        category = "Food"
                    )
                ),
                adjustments = listOf(
                    ReceiptAdjustment(
                        id = "adj-1",
                        description = "Buono Fedelta",
                        amount = Money(200L),
                        type = ReceiptAdjustmentType.DISCOUNT
                    )
                )
            )

            var savedPair: Pair<Transaction, Receipt>? = null
            val txRepo = FakeCrudTxRepo(
                existing = receiptTx,
                onUpdateWithReceipt = { tx, receipt -> savedPair = tx to receipt }
            )
            val formVm = createFormViewModel(
                transactionId = "tx-receipt",
                txRepo = txRepo,
                receiptRepo = fakeReceiptRepo(AppResult.Success(initialReceipt))
            )
            advanceUntilIdle()

            val loaded = formVm.uiState.value
            assertTrue(loaded.isReceiptTransaction)
            assertEquals("12.50", loaded.amountInput)
            assertEquals(2, loaded.receipt?.items?.size)

            // 1. Modify item-1 quantity from 2 to 4 (4 * 150 = 600 gross, -50 disc = 550 net)
            formVm.onEvent(TransactionFormEvent.OpenEditReceiptItem(loaded.receipt!!.items.first()))
            formVm.onEvent(TransactionFormEvent.ItemEditorQuantityChanged("4"))
            formVm.onEvent(TransactionFormEvent.SubmitItemEditor)

            val afterEdit = formVm.uiState.value
            assertNull(afterEdit.itemEditor)
            assertEquals(4, afterEdit.receipt?.items?.first()?.quantity)
            assertEquals(Money(600L), afterEdit.receipt?.items?.first()?.grossAmount)
            assertEquals(Money(550L), afterEdit.receipt?.items?.first()?.netAmount)
            // Subtotal = 600 + 1400 = 2000; Discounts = 50 + 200 + 200 (adj) = 450; Total = 1550
            assertEquals(Money(2000L), afterEdit.receipt?.subtotal)
            assertEquals(Money(450L), afterEdit.receipt?.totalDiscount)
            assertEquals(Money(1550L), afterEdit.receipt?.total)
            assertEquals("15.50", afterEdit.amountInput)

            // 2. Remove item-2 after confirmation
            val itemToRemove = afterEdit.receipt!!.items[1]
            formVm.onEvent(TransactionFormEvent.RequestRemoveReceiptItem(itemToRemove))
            assertEquals(itemToRemove, formVm.uiState.value.pendingRemoveItem)
            formVm.onEvent(TransactionFormEvent.ConfirmRemoveReceiptItem)

            val afterRemove = formVm.uiState.value
            assertNull(afterRemove.pendingRemoveItem)
            assertEquals(1, afterRemove.receipt?.items?.size)
            // Remaining: Pasta (600 gross, 50 disc, 550 net) - 200 adj = 350 total; totalDiscount = 250
            assertEquals(Money(600L), afterRemove.receipt?.subtotal)
            assertEquals(Money(250L), afterRemove.receipt?.totalDiscount)
            assertEquals(Money(350L), afterRemove.receipt?.total)
            assertEquals("3.50", afterRemove.amountInput)

            // 3. Add a new product (first check validation on empty fields, then submit valid)
            formVm.onEvent(TransactionFormEvent.OpenAddReceiptItem)
            formVm.onEvent(TransactionFormEvent.SubmitItemEditor)
            val invalidEditor = formVm.uiState.value.itemEditor!!
            assertEquals(R.string.validation_receipt_item_description_required, invalidEditor.descriptionErrorResId)
            assertEquals(R.string.validation_receipt_item_price_non_negative, invalidEditor.unitPriceErrorResId)

            formVm.onEvent(TransactionFormEvent.ItemEditorDescriptionChanged("Latte Bio"))
            formVm.onEvent(TransactionFormEvent.ItemEditorQuantityChanged("2"))
            formVm.onEvent(TransactionFormEvent.ItemEditorUnitPriceChanged("1.80"))
            formVm.onEvent(TransactionFormEvent.ItemEditorDiscountChanged("0.30"))
            formVm.onEvent(TransactionFormEvent.ItemEditorCategorySelected("Food"))
            formVm.onEvent(TransactionFormEvent.SubmitItemEditor)

            val afterAdd = formVm.uiState.value
            assertNull(afterAdd.itemEditor)
            assertEquals(2, afterAdd.receipt?.items?.size)
            // Pasta (600 gross, 50 disc) + Latte (360 gross, 30 disc) = 960 gross; disc = 50 + 30 + 200 (adj) = 280; total = 680
            assertEquals(Money(960L), afterAdd.receipt?.subtotal)
            assertEquals(Money(280L), afterAdd.receipt?.totalDiscount)
            assertEquals(Money(680L), afterAdd.receipt?.total)
            assertEquals("6.80", afterAdd.amountInput)
            // Adjustment stayed separate
            assertEquals(1, afterAdd.receipt?.adjustments?.size)
            assertEquals(Money(200L), afterAdd.receipt?.adjustments?.first()?.amount)

            // 4. Submit and verify persisted Transaction & Receipt
            formVm.onEvent(TransactionFormEvent.Submit)
            advanceUntilIdle()

            assertEquals(TransactionFormEffect.NavigateBackSaved, formVm.effects.first())
            val (persistedTx, persistedReceipt) = savedPair!!
            assertEquals(persistedReceipt.total, persistedTx.amount)
            assertEquals(Money(680L), persistedTx.amount)
            assertEquals(2, persistedReceipt.items.size)
            assertEquals(4, persistedReceipt.items[0].quantity)
            assertEquals("Latte Bio", persistedReceipt.items[1].description)
        }

    @Test
    fun `removing all receipt items updates local summary without deleting transaction and error preserves edits`() =
        runTest(testDispatcher) {
            val receiptTx = Transaction(
                id = "tx-receipt-2",
                amount = Money(250L),
                type = TransactionType.EXPENSE,
                description = "Esselunga",
                categoryId = "cat-food",
                accountId = "acc-main",
                date = LocalDate.of(2026, 11, 12),
                source = TransactionSource.RECEIPT,
                receiptId = "receipt-2"
            )
            val singleItemReceipt = Receipt(
                id = "receipt-2",
                merchant = "Esselunga",
                date = LocalDate.of(2026, 11, 12),
                subtotal = Money(250L),
                totalDiscount = Money.ZERO,
                total = Money(250L),
                categoryId = "cat-food",
                accountId = "acc-main",
                items = listOf(
                    ReceiptItem(
                        id = "only-item",
                        description = "Bread",
                        quantity = 1,
                        unitPrice = Money(250L),
                        grossAmount = Money(250L),
                        discountAmount = Money.ZERO,
                        netAmount = Money(250L),
                        category = "Food"
                    )
                )
            )
            var deleteCalled = false
            val txRepo = FakeCrudTxRepo(
                existing = receiptTx,
                updateResult = AppResult.Error(AppError.Network.ServerError),
                onDelete = { deleteCalled = true }
            )
            val formVm = createFormViewModel(
                transactionId = "tx-receipt-2",
                txRepo = txRepo,
                receiptRepo = fakeReceiptRepo(AppResult.Success(singleItemReceipt))
            )
            advanceUntilIdle()

            // Remove the only product
            val onlyItem = formVm.uiState.value.receipt!!.items.single()
            formVm.onEvent(TransactionFormEvent.RequestRemoveReceiptItem(onlyItem))
            formVm.onEvent(TransactionFormEvent.ConfirmRemoveReceiptItem)

            assertFalse(deleteCalled)
            val emptiedState = formVm.uiState.value
            assertTrue(emptiedState.receipt!!.items.isEmpty())
            assertEquals(Money.ZERO, emptiedState.receipt!!.subtotal)
            assertEquals(Money.ZERO, emptiedState.receipt!!.total)
            assertEquals("0.00", emptiedState.amountInput)

            // Add another product and fail to save: edits must be preserved
            formVm.onEvent(TransactionFormEvent.OpenAddReceiptItem)
            formVm.onEvent(TransactionFormEvent.ItemEditorDescriptionChanged("Water"))
            formVm.onEvent(TransactionFormEvent.ItemEditorQuantityChanged("6"))
            formVm.onEvent(TransactionFormEvent.ItemEditorUnitPriceChanged("0.50"))
            formVm.onEvent(TransactionFormEvent.SubmitItemEditor)

            formVm.onEvent(TransactionFormEvent.Submit)
            advanceUntilIdle()

            val afterError = formVm.uiState.value
            assertEquals(R.string.error_server, afterError.submitError?.messageResId)
            assertEquals(1, afterError.receipt?.items?.size)
            assertEquals("Water", afterError.receipt?.items?.single()?.description)
            assertEquals("3.00", afterError.amountInput)
        }

    private fun createFormViewModel(
        transactionId: String?,
        txRepo: TransactionRepository,
        receiptRepo: ReceiptRepository = fakeReceiptRepo()
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
            getTransactionReceiptUseCase = GetTransactionReceiptUseCase(receiptRepo),
            getCategoriesUseCase = GetCategoriesUseCase(fakeCatRepo()),
            getAccountsUseCase = GetAccountsUseCase(fakeAccountRepo()),
            createTransactionUseCase = CreateTransactionUseCase(txRepo, validator),
            updateTransactionUseCase = UpdateTransactionUseCase(txRepo, validator),
            updateTransactionWithReceiptUseCase = UpdateTransactionWithReceiptUseCase(
                transactionRepository = txRepo,
                transactionValidator = validator,
                receiptValidator = ReceiptValidator()
            ),
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

    private fun fakeReceiptRepo(
        receiptResult: AppResult<Receipt?> = AppResult.Success(null)
    ) = object : ReceiptRepository {
        override suspend fun processReceipt(image: ReceiptImage) =
            AppResult.Error(AppError.Unknown)
        override suspend fun getReceiptDraft(receiptId: String) =
            AppResult.Error(AppError.Network.NotFound)
        override suspend fun getReceiptForTransaction(transactionId: String) = receiptResult
        override suspend fun confirmReceipt(receipt: Receipt) =
            AppResult.Error(AppError.Unknown)
        override suspend fun discardReceiptDraft(receiptId: String) = Unit
    }

    private class FakeCrudTxRepo(
        private val existing: Transaction? = null,
        private val updateResult: AppResult<Transaction>? = null,
        private val onCreate: (Transaction) -> Unit = {},
        private val onUpdateWithReceipt: (Transaction, Receipt) -> Unit = { _, _ -> },
        private val onDelete: () -> Unit = {}
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

        override suspend fun updateTransactionWithReceipt(
            transaction: Transaction,
            receipt: Receipt
        ): AppResult<Transaction> {
            updateResult?.let { return it }
            onUpdateWithReceipt(transaction, receipt)
            return AppResult.Success(transaction)
        }

        override suspend fun deleteTransaction(id: String): AppResult<Unit> {
            onDelete()
            return AppResult.Success(Unit)
        }
    }
}

