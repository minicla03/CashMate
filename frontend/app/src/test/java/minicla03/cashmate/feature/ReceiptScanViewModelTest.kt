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
import minicla03.cashmate.domain.model.TransactionType
import minicla03.cashmate.domain.repository.AccountRepository
import minicla03.cashmate.domain.repository.CategoryRepository
import minicla03.cashmate.domain.repository.ReceiptRepository
import minicla03.cashmate.domain.usecase.ConfirmReceiptUseCase
import minicla03.cashmate.domain.usecase.DiscardReceiptDraftUseCase
import minicla03.cashmate.domain.usecase.GetAccountsUseCase
import minicla03.cashmate.domain.usecase.GetCategoriesUseCase
import minicla03.cashmate.domain.usecase.GetReceiptDraftUseCase
import minicla03.cashmate.domain.usecase.ProcessReceiptUseCase
import minicla03.cashmate.domain.usecase.ReceiptValidator
import minicla03.cashmate.feature.receipt.presentation.ReceiptImageReader
import minicla03.cashmate.feature.receipt.presentation.ReceiptScanEffect
import minicla03.cashmate.feature.receipt.presentation.ReceiptScanEvent
import minicla03.cashmate.feature.receipt.presentation.ReceiptScanState
import minicla03.cashmate.feature.receipt.presentation.ReceiptScanViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.Currency

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptScanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `happy path transitions from idle to review editing confirmation and success`() =
        runTest(testDispatcher) {
            val repo = FakeReceiptRepo()
            val vm = createViewModel(repo = repo)

            assertEquals(ReceiptScanState.Idle, vm.uiState.value)

            vm.onEvent(ReceiptScanEvent.BeginImageSelection)
            assertEquals(ReceiptScanState.SelectingImage, vm.uiState.value)

            vm.onEvent(ReceiptScanEvent.ImageSelected("content://receipt/1"))
            advanceUntilIdle()

            val review = vm.uiState.value as ReceiptScanState.Review
            assertEquals("Esselunga", review.merchant)
            assertEquals("42.80", review.totalInput)
            assertEquals("cat-food", review.selectedCategoryId)
            assertEquals(2, review.items.size)
            assertEquals(ReceiptScanEffect.NavigateToReview("receipt-1"), vm.effects.first())

            // Edit receipt fields
            vm.onEvent(ReceiptScanEvent.MerchantChanged("Esselunga Centro"))
            vm.onEvent(ReceiptScanEvent.TotalChanged("40,00"))
            vm.onEvent(ReceiptScanEvent.ItemQuantityChanged("item-1", "2"))
            vm.onEvent(ReceiptScanEvent.ItemDiscountAmountChanged("item-1", "1.00"))
            vm.onEvent(ReceiptScanEvent.AddItem)
            val afterAdd = vm.uiState.value as ReceiptScanState.Review
            assertEquals(3, afterAdd.items.size)
            assertTrue(afterAdd.hasUnsavedChanges)

            // Remove newly added blank item before confirming
            val addedId = afterAdd.items.last().id
            vm.onEvent(ReceiptScanEvent.RemoveItem(addedId))

            vm.onEvent(ReceiptScanEvent.ConfirmClick)
            advanceUntilIdle()

            val success = vm.uiState.value as ReceiptScanState.Success
            assertEquals("Esselunga Centro", success.merchant)
            assertEquals(Money(4000L), success.transaction.amount)
            assertEquals(1, repo.confirmedReceipts.size)
            assertEquals(2, repo.confirmedReceipts.first().items.first().quantity)
            assertEquals(Money(100L), repo.confirmedReceipts.first().items.first().discountAmount)

            vm.onEvent(ReceiptScanEvent.ViewTransactionClick(success.transaction.id))
            assertEquals(
                ReceiptScanEffect.NavigateToTransactionDetail(success.transaction.id),
                vm.effects.first()
            )
        }

    @Test
    fun `processing failure transitions to error and retry recovers to review`() =
        runTest(testDispatcher) {
            val repo = FakeReceiptRepo(
                processResult = AppResult.Error(AppError.Receipt.ProcessingFailed)
            )
            val vm = createViewModel(repo = repo)

            vm.onEvent(ReceiptScanEvent.ImageSelected("content://receipt/1"))
            advanceUntilIdle()

            val errorState = vm.uiState.value as ReceiptScanState.Error
            assertEquals(R.string.error_receipt_processing_failed, errorState.error.messageResId)
            assertTrue(errorState.canRetryLastImage)

            repo.processResult = AppResult.Success(sampleReceipt())
            vm.onEvent(ReceiptScanEvent.RetryProcessing)
            advanceUntilIdle()

            assertTrue(vm.uiState.value is ReceiptScanState.Review)
        }

    @Test
    fun `unsupported image transitions to non-retryable error and allows choosing another`() =
        runTest(testDispatcher) {
            val imageReader = FakeImageReader(AppResult.Error(AppError.Receipt.UnsupportedImage))
            val vm = createViewModel(imageReader = imageReader)

            vm.onEvent(ReceiptScanEvent.ImageSelected("content://receipt/bad"))
            advanceUntilIdle()

            val errorState = vm.uiState.value as ReceiptScanState.Error
            assertEquals(R.string.error_receipt_unsupported_image, errorState.error.messageResId)
            assertFalse(errorState.canRetryLastImage)

            vm.onEvent(ReceiptScanEvent.ChooseAnotherImage)
            assertEquals(ReceiptScanState.Idle, vm.uiState.value)
        }

    @Test
    fun `cancel without edits discards draft immediately without creating transaction`() =
        runTest(testDispatcher) {
            val repo = FakeReceiptRepo()
            val vm = createViewModel(repo = repo)

            vm.onEvent(ReceiptScanEvent.ImageSelected("content://receipt/1"))
            advanceUntilIdle()
            assertEquals(ReceiptScanEffect.NavigateToReview("receipt-1"), vm.effects.first())

            vm.onEvent(ReceiptScanEvent.CancelClick)
            advanceUntilIdle()

            assertEquals(ReceiptScanState.Idle, vm.uiState.value)
            assertEquals(listOf("receipt-1"), repo.discardedIds)
            assertTrue(repo.confirmedReceipts.isEmpty())
            assertEquals(ReceiptScanEffect.ExitFlow, vm.effects.first())
        }

    @Test
    fun `cancel after edits prompts confirmation before discarding draft`() =
        runTest(testDispatcher) {
            val repo = FakeReceiptRepo()
            val vm = createViewModel(repo = repo)

            vm.onEvent(ReceiptScanEvent.ImageSelected("content://receipt/1"))
            advanceUntilIdle()
            assertEquals(ReceiptScanEffect.NavigateToReview("receipt-1"), vm.effects.first())

            vm.onEvent(ReceiptScanEvent.MerchantChanged("Updated Store"))
            vm.onEvent(ReceiptScanEvent.CancelClick)

            val promptState = vm.uiState.value as ReceiptScanState.Review
            assertTrue(promptState.showDiscardDialog)
            assertTrue(repo.discardedIds.isEmpty())

            vm.onEvent(ReceiptScanEvent.DismissDiscardDialog)
            assertFalse((vm.uiState.value as ReceiptScanState.Review).showDiscardDialog)

            vm.onEvent(ReceiptScanEvent.CancelClick)
            vm.onEvent(ReceiptScanEvent.ConfirmDiscard)
            advanceUntilIdle()

            assertEquals(ReceiptScanState.Idle, vm.uiState.value)
            assertEquals(listOf("receipt-1"), repo.discardedIds)
            assertTrue(repo.confirmedReceipts.isEmpty())
            assertEquals(ReceiptScanEffect.ExitFlow, vm.effects.first())
        }

    @Test
    fun `confirm validates invalid edits and preserves edits on backend error`() =
        runTest(testDispatcher) {
            val repo = FakeReceiptRepo(
                confirmResult = AppResult.Error(AppError.Network.ServerError)
            )
            val vm = createViewModel(repo = repo)

            vm.onEvent(ReceiptScanEvent.ImageSelected("content://receipt/1"))
            advanceUntilIdle()

            vm.onEvent(ReceiptScanEvent.MerchantChanged(""))
            vm.onEvent(ReceiptScanEvent.TotalChanged("0.00"))
            vm.onEvent(ReceiptScanEvent.ConfirmClick)

            val invalidState = vm.uiState.value as ReceiptScanState.Review
            assertEquals(
                R.string.validation_merchant_required,
                invalidState.fieldErrors[ValidationField.MERCHANT]
            )
            assertEquals(
                R.string.validation_receipt_total_positive,
                invalidState.fieldErrors[ValidationField.TOTAL]
            )
            assertTrue(repo.confirmedReceipts.isEmpty())

            vm.onEvent(ReceiptScanEvent.MerchantChanged("Esselunga Fixed"))
            vm.onEvent(ReceiptScanEvent.TotalChanged("42.80"))
            vm.onEvent(ReceiptScanEvent.ConfirmClick)
            advanceUntilIdle()

            val afterServerError = vm.uiState.value as ReceiptScanState.Review
            assertEquals("Esselunga Fixed", afterServerError.merchant)
            assertNotNull(afterServerError.submitError)
            assertEquals(R.string.error_server, afterServerError.submitError?.messageResId)
        }

    private fun createViewModel(
        imageReader: ReceiptImageReader = FakeImageReader(),
        repo: FakeReceiptRepo = FakeReceiptRepo()
    ): ReceiptScanViewModel = ReceiptScanViewModel(
        imageReader = imageReader,
        processReceiptUseCase = ProcessReceiptUseCase(repo),
        confirmReceiptUseCase = ConfirmReceiptUseCase(repo, ReceiptValidator()),
        getReceiptDraftUseCase = GetReceiptDraftUseCase(repo),
        discardReceiptDraftUseCase = DiscardReceiptDraftUseCase(repo),
        getCategoriesUseCase = GetCategoriesUseCase(fakeCatRepo()),
        getAccountsUseCase = GetAccountsUseCase(fakeAccountRepo()),
        receiptValidator = ReceiptValidator(),
        userPreferencesRepository = InMemoryUserPreferencesRepository()
    )

    private class FakeImageReader(
        private val result: AppResult<ReceiptImage> = AppResult.Success(
            ReceiptImage(byteArrayOf(1, 2, 3), "image/jpeg", "receipt.jpg")
        )
    ) : ReceiptImageReader {
        var cleanedUp = false
        override suspend fun read(uriString: String): AppResult<ReceiptImage> = result
        override fun cleanupTemporaryFiles() {
            cleanedUp = true
        }
    }

    private class FakeReceiptRepo(
        var processResult: AppResult<Receipt> = AppResult.Success(sampleReceipt()),
        var confirmResult: AppResult<Transaction>? = null
    ) : ReceiptRepository {
        val confirmedReceipts = mutableListOf<Receipt>()
        val discardedIds = mutableListOf<String>()

        override suspend fun processReceipt(image: ReceiptImage): AppResult<Receipt> = processResult

        override suspend fun getReceiptDraft(receiptId: String): AppResult<Receipt> =
            processResult

        override suspend fun getReceiptForTransaction(transactionId: String): AppResult<Receipt?> =
            AppResult.Success(null)

        override suspend fun confirmReceipt(receipt: Receipt): AppResult<Transaction> {
            confirmResult?.let { return it }
            confirmedReceipts.add(receipt)
            return AppResult.Success(
                Transaction(
                    id = "tx-created-1",
                    amount = receipt.total,
                    type = TransactionType.EXPENSE,
                    description = receipt.merchant,
                    categoryId = receipt.categoryId.orEmpty(),
                    accountId = receipt.accountId.orEmpty(),
                    date = receipt.date
                )
            )
        }

        override suspend fun discardReceiptDraft(receiptId: String) {
            discardedIds.add(receiptId)
        }
    }

    private fun fakeCatRepo() = object : CategoryRepository {
        override suspend fun getCategories() = AppResult.Success(
            listOf(
                Category("cat-food", "Food & Groceries", "restaurant", "#5E35B1"),
                Category("cat-other", "Other", "category", "#546E7A")
            )
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

    companion object {
        fun sampleReceipt(): Receipt = Receipt(
            id = "receipt-1",
            merchant = "Esselunga",
            date = LocalDate.of(2026, 11, 12),
            subtotal = Money(4780L),
            totalDiscount = Money(500L),
            total = Money(4280L),
            categoryId = "cat-food",
            accountId = "acc-main",
            items = listOf(
                ReceiptItem(
                    id = "item-1",
                    description = "Pasta",
                    quantity = 1,
                    unitPrice = Money(250L),
                    grossAmount = Money(250L),
                    discountAmount = Money(50L),
                    netAmount = Money(200L),
                    category = "FOOD"
                ),
                ReceiptItem(
                    id = "item-2",
                    description = "Milk",
                    quantity = 1,
                    unitPrice = Money(180L),
                    grossAmount = Money(180L),
                    discountAmount = Money.ZERO,
                    netAmount = Money(180L),
                    category = "FOOD"
                )
            ),
            adjustments = listOf(
                ReceiptAdjustment(
                    id = "adj-1",
                    description = "Coupon",
                    amount = Money(450L),
                    type = ReceiptAdjustmentType.DISCOUNT
                )
            )
        )
    }
}

