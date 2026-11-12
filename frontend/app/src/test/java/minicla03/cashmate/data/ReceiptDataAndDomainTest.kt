package minicla03.cashmate.data

import kotlinx.coroutines.test.runTest
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.error.ValidationReason
import minicla03.cashmate.data.remote.datasource.FakeReceiptRemoteDataSource
import minicla03.cashmate.data.remote.datasource.FakeRemoteConfig
import minicla03.cashmate.data.remote.datasource.FakeRemoteStore
import minicla03.cashmate.data.remote.datasource.FakeTransactionRemoteDataSource
import minicla03.cashmate.data.remote.dto.ReceiptAdjustmentDto
import minicla03.cashmate.data.remote.dto.ReceiptDraftDto
import minicla03.cashmate.data.remote.dto.ReceiptItemDto
import minicla03.cashmate.data.remote.mapper.InvalidReceiptException
import minicla03.cashmate.data.remote.mapper.NetworkErrorMapper
import minicla03.cashmate.data.remote.mapper.ReceiptProcessingFailedException
import minicla03.cashmate.data.remote.mapper.UnsupportedImageException
import minicla03.cashmate.data.remote.mapper.toConfirmRequest
import minicla03.cashmate.data.remote.mapper.toDomain
import minicla03.cashmate.data.repository.ReceiptRepositoryImpl
import minicla03.cashmate.data.repository.SafeRemoteCall
import minicla03.cashmate.data.repository.TransactionRepositoryImpl
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.ReceiptAdjustment
import minicla03.cashmate.domain.model.ReceiptAdjustmentType
import minicla03.cashmate.domain.model.ReceiptImage
import minicla03.cashmate.domain.model.ReceiptItem
import minicla03.cashmate.domain.model.TransactionType
import minicla03.cashmate.domain.usecase.ConfirmReceiptUseCase
import minicla03.cashmate.domain.usecase.ProcessReceiptUseCase
import minicla03.cashmate.domain.usecase.ReceiptCalculator
import minicla03.cashmate.domain.usecase.ReceiptValidator
import minicla03.cashmate.domain.usecase.TransactionValidator
import minicla03.cashmate.domain.usecase.UpdateTransactionUseCase
import minicla03.cashmate.domain.usecase.UpdateTransactionWithReceiptUseCase
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ReceiptDataAndDomainTest {

    private val errorMapper = NetworkErrorMapper()
    private val fixedClock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `maps receipt dto items adjustments and confirm request accurately`() {
        val dto = ReceiptDraftDto(
            id = "receipt-123",
            merchant = "  Esselunga ",
            date = "2026-11-12",
            subtotal = 4780L,
            totalDiscount = 500L,
            total = 4280L,
            categoryId = "cat-food",
            accountId = "acc-main",
            items = listOf(
                ReceiptItemDto(
                    id = "item-1",
                    description = "Pasta",
                    quantity = 2,
                    unitPrice = 250L,
                    grossAmount = 500L,
                    discountAmount = 50L,
                    netAmount = 450L,
                    category = "FOOD"
                )
            ),
            adjustments = listOf(
                ReceiptAdjustmentDto(
                    id = "adj-1",
                    description = "Fidaty coupon",
                    amount = 450L,
                    type = "DISCOUNT"
                )
            )
        )

        val receipt = dto.toDomain()
        assertEquals("receipt-123", receipt.id)
        assertEquals("Esselunga", receipt.merchant)
        assertEquals(LocalDate.of(2026, 11, 12), receipt.date)
        assertEquals(Money(4780L), receipt.subtotal)
        assertEquals(Money(500L), receipt.totalDiscount)
        assertEquals(Money(4280L), receipt.total)
        assertEquals(1, receipt.items.size)
        assertEquals(Money(50L), receipt.items.first().discountAmount)
        assertEquals(Money(450L), receipt.items.first().netAmount)
        assertEquals(ReceiptAdjustmentType.DISCOUNT, receipt.adjustments.first().type)
        assertEquals(Money(450L), receipt.adjustments.first().amount)

        val confirmRequest = receipt.toConfirmRequest()
        assertEquals("Esselunga", confirmRequest.merchant)
        assertEquals(4280L, confirmRequest.total)
        assertEquals(500L, confirmRequest.totalDiscount)
        assertEquals("cat-food", confirmRequest.categoryId)
        assertEquals(1, confirmRequest.items.size)
        assertEquals(1, confirmRequest.adjustments.size)
    }

    @Test(expected = InvalidReceiptException::class)
    fun `rejects receipt dto with invalid total`() {
        ReceiptDraftDto(
            id = "r-1",
            merchant = "Shop",
            date = "2026-11-12",
            subtotal = 100L,
            totalDiscount = 0L,
            total = 0L
        ).toDomain()
    }

    @Test(expected = InvalidReceiptException::class)
    fun `rejects receipt dto with malformed date`() {
        ReceiptDraftDto(
            id = "r-1",
            merchant = "Shop",
            date = "not-a-date",
            subtotal = 100L,
            totalDiscount = 0L,
            total = 100L
        ).toDomain()
    }

    @Test
    fun `maps receipt specific http and domain errors`() {
        assertEquals(AppError.Network.BadRequest, errorMapper.map(httpException(400)))
        assertEquals(AppError.Receipt.UnsupportedImage, errorMapper.map(httpException(415)))
        assertEquals(AppError.Receipt.InvalidReceipt, errorMapper.map(httpException(422)))
        assertEquals(AppError.Receipt.ProcessingFailed, errorMapper.map(ReceiptProcessingFailedException()))
        assertEquals(AppError.Receipt.InvalidReceipt, errorMapper.map(InvalidReceiptException()))
        assertEquals(AppError.Receipt.UnsupportedImage, errorMapper.map(UnsupportedImageException()))
    }

    @Test
    fun `fake receipt repository processes realistic receipt and creates single transaction on confirm`() =
        runTest {
            val config = FakeRemoteConfig().apply { simulatedDelayMs = 0L }
            val store = FakeRemoteStore(fixedClock, config)
            val txRemote = FakeTransactionRemoteDataSource(store)
            val receiptRemote = FakeReceiptRemoteDataSource(store)
            val repo = ReceiptRepositoryImpl(receiptRemote, SafeRemoteCall(errorMapper))
            val processUseCase = ProcessReceiptUseCase(repo)
            val confirmUseCase = ConfirmReceiptUseCase(repo, ReceiptValidator())

            val beforeCount = txRemote.getTransactions(null).size

            val validImage = ReceiptImage(
                bytes = byteArrayOf(1, 2, 3, 4),
                mimeType = "image/jpeg",
                fileName = "receipt.jpg"
            )
            // Ensure ReceiptImage.toString never prints raw byte contents
            assertFalse(validImage.toString().contains("[1, 2, 3, 4]"))

            val processed = processUseCase(validImage)
            assertTrue(processed is AppResult.Success)
            val draft = (processed as AppResult.Success).data
            assertEquals("Esselunga", draft.merchant)
            assertTrue(draft.items.size >= 2)
            assertTrue(draft.totalDiscount.isPositive)
            assertTrue(draft.adjustments.isNotEmpty())

            // Cached in-memory draft is retrievable
            assertEquals(AppResult.Success(draft), repo.getReceiptDraft(draft.id))

            // Simulate user correction before confirming
            val edited = draft.copy(
                merchant = "  Esselunga Milano  ",
                total = Money(1980L),
                categoryId = "cat-food",
                accountId = "acc-main"
            )
            val confirmed = confirmUseCase(edited)
            assertTrue(confirmed is AppResult.Success)
            val tx = (confirmed as AppResult.Success).data
            assertEquals("Esselunga Milano", tx.description)
            assertEquals(Money(1980L), tx.amount)
            assertEquals(TransactionType.EXPENSE, tx.type)
            assertEquals("cat-food", tx.categoryId)

            // Exactly 1 transaction created for the entire multi-item receipt
            val afterCount = txRemote.getTransactions(null).size
            assertEquals(beforeCount + 1, afterCount)

            // Confirmed receipt remains retrievable from the created transaction
            val loadedReceiptResult = repo.getReceiptForTransaction(tx.id)
            assertTrue(loadedReceiptResult is AppResult.Success)
            val loadedReceipt = (loadedReceiptResult as AppResult.Success).data!!
            assertEquals("Esselunga Milano", loadedReceipt.merchant)
            assertEquals(Money(1980L), loadedReceipt.total)
            assertEquals(draft.items.size, loadedReceipt.items.size)

            // Seeded receipt transaction also exposes full receipt details
            val seededReceipt = (repo.getReceiptForTransaction("tx-107") as AppResult.Success).data!!
            assertEquals("Organic Market", seededReceipt.merchant)
            assertEquals(Money(6480L), seededReceipt.total)
            assertTrue(seededReceipt.totalDiscount.isPositive)

            // Draft cleared after confirmation
            assertEquals(AppResult.Error(AppError.Network.NotFound), repo.getReceiptDraft(draft.id))
        }

    @Test
    fun `process use case rejects unsupported image and maps simulated remote failures`() = runTest {
        val config = FakeRemoteConfig().apply { simulatedDelayMs = 0L }
        val store = FakeRemoteStore(fixedClock, config)
        val repo = ReceiptRepositoryImpl(FakeReceiptRemoteDataSource(store), SafeRemoteCall(errorMapper))
        val processUseCase = ProcessReceiptUseCase(repo)

        val emptyImage = ReceiptImage(ByteArray(0), "image/jpeg", "empty.jpg")
        assertEquals(AppResult.Error(AppError.Receipt.UnsupportedImage), processUseCase(emptyImage))

        val pdfFile = ReceiptImage(byteArrayOf(1, 2), "application/pdf", "doc.pdf")
        assertEquals(AppResult.Error(AppError.Receipt.UnsupportedImage), processUseCase(pdfFile))

        config.simulatedErrorProvider = { SocketTimeoutException("timeout") }
        val validImage = ReceiptImage(byteArrayOf(1, 2), "image/png", "r.png")
        assertEquals(AppResult.Error(AppError.Network.Timeout), processUseCase(validImage))

        config.simulatedErrorProvider = { ReceiptProcessingFailedException() }
        assertEquals(AppResult.Error(AppError.Receipt.ProcessingFailed), processUseCase(validImage))
    }

    @Test
    fun `validator rejects invalid receipt fields`() {
        val validator = ReceiptValidator()
        val invalid = Receipt(
            id = "r-1",
            merchant = "  ",
            date = LocalDate.of(1960, 1, 1),
            subtotal = Money(100L),
            totalDiscount = Money(0L),
            total = Money.ZERO,
            categoryId = "",
            accountId = null,
            items = listOf(
                ReceiptItem(
                    id = "i-1",
                    description = "",
                    quantity = 0,
                    unitPrice = Money(-10L),
                    grossAmount = Money(100L),
                    discountAmount = Money(0L),
                    netAmount = Money(100L),
                    category = "FOOD"
                )
            ),
            adjustments = listOf(
                ReceiptAdjustment(
                    id = "a-1",
                    description = " ",
                    amount = Money(-5L),
                    type = ReceiptAdjustmentType.DISCOUNT
                )
            )
        )

        val err = validator.validate(invalid)!!
        assertEquals(ValidationReason.REQUIRED, err.fieldErrors[ValidationField.MERCHANT])
        assertEquals(ValidationReason.INVALID_RANGE, err.fieldErrors[ValidationField.DATE])
        assertEquals(ValidationReason.MUST_BE_POSITIVE, err.fieldErrors[ValidationField.TOTAL])
        assertEquals(ValidationReason.REQUIRED, err.fieldErrors[ValidationField.CATEGORY])
        assertEquals(ValidationReason.REQUIRED, err.fieldErrors[ValidationField.ACCOUNT])
        assertEquals(ValidationReason.REQUIRED, err.fieldErrors[ValidationField.ITEM_DESCRIPTION])
        assertEquals(ValidationReason.MUST_BE_POSITIVE, err.fieldErrors[ValidationField.ITEM_QUANTITY])
        assertEquals(ValidationReason.MUST_BE_NON_NEGATIVE, err.fieldErrors[ValidationField.ITEM_PRICE])
        assertEquals(ValidationReason.REQUIRED, err.fieldErrors[ValidationField.ADJUSTMENT_DESCRIPTION])
        assertEquals(ValidationReason.MUST_BE_NON_NEGATIVE, err.fieldErrors[ValidationField.ADJUSTMENT_AMOUNT])
    }

    @Test
    fun `updating receipt items persists changes atomically to transaction and receipt without creating extra transactions`() =
        runTest {
            val config = FakeRemoteConfig().apply { simulatedDelayMs = 0L }
            val store = FakeRemoteStore(fixedClock, config)
            val safeCall = SafeRemoteCall(errorMapper)
            val txRepo = TransactionRepositoryImpl(FakeTransactionRemoteDataSource(store), safeCall)
            val receiptRepo = ReceiptRepositoryImpl(FakeReceiptRemoteDataSource(store), safeCall)
            val updateWithReceipt = UpdateTransactionWithReceiptUseCase(
                transactionRepository = txRepo,
                transactionValidator = TransactionValidator(),
                receiptValidator = ReceiptValidator()
            )

            val initialCount = (txRepo.getTransactions() as AppResult.Success).data.size
            val seededTx = (txRepo.getTransaction("tx-107") as AppResult.Success).data
            val seededReceipt = (receiptRepo.getReceiptForTransaction("tx-107") as AppResult.Success).data!!

            // Seeded has 6 items, adjustment discount 510L, total 6480L
            assertEquals(Money(6_480L), seededTx.amount)
            assertEquals(seededReceipt.total, seededTx.amount)
            assertEquals(6, seededReceipt.items.size)

            // Modify first item quantity (2 -> 4 @ 250L, discount 100L -> gross 1000L, net 900L),
            // remove last item (1630L), and add a new item (qty 1 @ 500L, discount 50L -> net 450L)
            val editedFirst = ReceiptCalculator.calculateItem(
                id = seededReceipt.items.first().id,
                description = "Pasta Barilla Integrale 500g",
                quantity = 4,
                unitPrice = Money(250L),
                discountAmount = Money(100L),
                category = "FOOD"
            )
            val addedItem = ReceiptCalculator.calculateItem(
                id = "new-item-1",
                description = "Pane Artigianale",
                quantity = 1,
                unitPrice = Money(500L),
                discountAmount = Money(50L),
                category = "FOOD"
            )
            val modifiedItems = listOf(editedFirst) + seededReceipt.items.drop(1).dropLast(1) + addedItem
            val updatedReceiptInput = seededReceipt.copy(items = modifiedItems)

            val updateResult = updateWithReceipt(
                transaction = seededTx.copy(description = "Organic Market Updated"),
                receipt = updatedReceiptInput
            )
            assertTrue(updateResult is AppResult.Success)
            val updatedTx = (updateResult as AppResult.Success).data

            // Verify single transaction preserved
            assertEquals(initialCount, (txRepo.getTransactions() as AppResult.Success).data.size)

            // Reload both Transaction and Receipt from remote store
            val reloadedTx = (txRepo.getTransaction("tx-107") as AppResult.Success).data
            val reloadedReceipt = (receiptRepo.getReceiptForTransaction("tx-107") as AppResult.Success).data!!

            // Expected math:
            // items gross: 1000 + 570 + 1490 + 2500 + 890 + 500 = 6950
            // item discounts: 100 + 0 + 300 + 0 + 190 + 50 = 640
            // global adjustment discount kept separate: 510
            // totalDiscount: 640 + 510 = 1150
            // total: 6950 - 1150 = 5800
            assertEquals(Money(6_950L), reloadedReceipt.subtotal)
            assertEquals(Money(1_150L), reloadedReceipt.totalDiscount)
            assertEquals(Money(5_800L), reloadedReceipt.total)
            assertEquals(reloadedReceipt.total, reloadedTx.amount)
            assertEquals(reloadedReceipt.total, updatedTx.amount)
            assertEquals("Organic Market Updated", reloadedTx.description)
            assertEquals("Organic Market Updated", reloadedReceipt.merchant)
            assertEquals(1, reloadedReceipt.adjustments.size)
            assertEquals(Money(510L), reloadedReceipt.adjustments.first().amount)
            assertEquals(6, reloadedReceipt.items.size)
            assertEquals(4, reloadedReceipt.items.first().quantity)
            assertEquals("Pane Artigianale", reloadedReceipt.items.last().description)
        }

    @Test
    fun `updating manual transaction does not alter receipt state`() = runTest {
        val config = FakeRemoteConfig().apply { simulatedDelayMs = 0L }
        val store = FakeRemoteStore(fixedClock, config)
        val safeCall = SafeRemoteCall(errorMapper)
        val txRepo = TransactionRepositoryImpl(FakeTransactionRemoteDataSource(store), safeCall)
        val updateManual = UpdateTransactionUseCase(txRepo, TransactionValidator())

        val manualTx = (txRepo.getTransaction("tx-103") as AppResult.Success).data
        val updated = updateManual(manualTx.copy(description = "Manual Grocery", amount = Money(15_000L)))
        assertTrue(updated is AppResult.Success)
        val reloaded = (txRepo.getTransaction("tx-103") as AppResult.Success).data
        assertEquals("Manual Grocery", reloaded.description)
        assertEquals(Money(15_000L), reloaded.amount)
    }

    private fun httpException(code: Int): HttpException = HttpException(
        Response.error<Unit>(code, "".toResponseBody("application/json".toMediaType()))
    )
}

