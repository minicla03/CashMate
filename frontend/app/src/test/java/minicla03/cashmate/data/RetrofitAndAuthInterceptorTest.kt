package minicla03.cashmate.data

import kotlinx.coroutines.test.runTest
import minicla03.cashmate.core.network.AuthInterceptor
import minicla03.cashmate.core.network.TokenProvider
import minicla03.cashmate.data.remote.api.CashMateApi
import minicla03.cashmate.data.remote.datasource.RetrofitAccountRemoteDataSource
import minicla03.cashmate.data.remote.datasource.RetrofitCategoryRemoteDataSource
import minicla03.cashmate.data.remote.datasource.RetrofitReceiptRemoteDataSource
import minicla03.cashmate.data.remote.datasource.RetrofitTransactionRemoteDataSource
import minicla03.cashmate.data.remote.dto.AccountDto
import minicla03.cashmate.data.remote.dto.CategoryDto
import minicla03.cashmate.data.remote.dto.ConfirmReceiptRequest
import minicla03.cashmate.data.remote.dto.CreateCategoryRequest
import minicla03.cashmate.data.remote.dto.CreateTransactionRequest
import minicla03.cashmate.data.remote.dto.DashboardDto
import minicla03.cashmate.data.remote.dto.MonthComparisonDto
import minicla03.cashmate.data.remote.dto.ReceiptDraftDto
import minicla03.cashmate.data.remote.dto.ReceiptItemDto
import minicla03.cashmate.data.remote.dto.StatisticsDto
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.data.remote.dto.TransactionFilterDto
import minicla03.cashmate.data.remote.dto.UpdateCategoryRequest
import minicla03.cashmate.data.remote.dto.UpdateTransactionRequest
import minicla03.cashmate.domain.model.ReceiptImage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RetrofitAndAuthInterceptorTest {

    @Test
    fun `auth interceptor adds bearer token only when token is present`() {
        var currentToken: String? = null
        var capturedHeader: String? = null

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(object : TokenProvider {
                override fun getAccessToken(): String? = currentToken
            }))
            .addInterceptor { chain ->
                capturedHeader = chain.request().header(AuthInterceptor.HEADER_AUTHORIZATION)
                okhttp3.Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        client.newCall(Request.Builder().url("https://api.cashmate.example/test").build()).execute()
        assertNull(capturedHeader)

        currentToken = "jwt-sample-token"
        client.newCall(Request.Builder().url("https://api.cashmate.example/test").build()).execute()
        assertEquals("Bearer jwt-sample-token", capturedHeader)
    }

    @Test
    fun `retrofit remote data sources delegate to api and throw HttpException on failed delete`() = runTest {
        var deleteResponseCode = 204
        val stubApi = object : CashMateApi {
            override suspend fun getTransactions(from: String?, to: String?, categoryId: String?, type: String?) =
                listOf(TransactionDto("t1", 100L, "EXPENSE", "Coffee", "c1", "a1", "2026-11-11"))
            override suspend fun getTransaction(id: String) =
                TransactionDto(
                    id = id,
                    amount = 200L,
                    type = "EXPENSE",
                    description = "Esselunga",
                    categoryId = "c1",
                    accountId = "a1",
                    date = "2026-11-11",
                    source = "RECEIPT",
                    receiptId = "r-1",
                    receipt = ReceiptDraftDto(
                        id = "r-1",
                        merchant = "Esselunga",
                        date = "2026-11-11",
                        subtotal = 250L,
                        totalDiscount = 50L,
                        total = 200L,
                        items = listOf(ReceiptItemDto("i-1", "Pasta", 1, 250L, 250L, 50L, 200L, "FOOD"))
                    )
                )
            override suspend fun createTransaction(request: CreateTransactionRequest) =
                TransactionDto("t2", request.amount, request.type, request.description, request.categoryId, request.accountId, request.date)
            override suspend fun updateTransaction(id: String, request: UpdateTransactionRequest) =
                TransactionDto(id, request.amount, request.type, request.description, request.categoryId, request.accountId, request.date)
            override suspend fun deleteTransaction(id: String): Response<Unit> =
                if (deleteResponseCode in 200..299) {
                    Response.success(Unit)
                } else {
                    Response.error(deleteResponseCode, "".toResponseBody("application/json".toMediaType()))
                }
            override suspend fun getCategories() = listOf(CategoryDto("c1", "Food", "restaurant", "#5E35B1"))
            override suspend fun createCategory(request: CreateCategoryRequest) =
                CategoryDto("c2", request.name, request.icon, request.color)
            override suspend fun updateCategory(id: String, request: UpdateCategoryRequest) =
                CategoryDto(id, request.name, request.icon, request.color)
            override suspend fun deleteCategory(id: String): Response<Unit> = Response.success(Unit)
            override suspend fun getAccounts() = listOf(AccountDto("a1", "Main", "EUR"))
            override suspend fun getDashboard() = DashboardDto(0, 0, 0, 0, emptyList(), emptyList())
            override suspend fun getStatistics() = StatisticsDto(0, 0, emptyList(), emptyList(), MonthComparisonDto("2026-11", "2026-10", 0, 0, 0, null))
            override suspend fun processReceipt(image: MultipartBody.Part) = ReceiptDraftDto(
                id = "r-1",
                merchant = "Esselunga",
                date = "2026-11-12",
                subtotal = 250L,
                totalDiscount = 50L,
                total = 200L,
                items = listOf(ReceiptItemDto("i-1", "Pasta", 1, 250L, 250L, 50L, 200L, "FOOD"))
            )
            override suspend fun confirmReceipt(id: String, request: ConfirmReceiptRequest) =
                TransactionDto("tx-from-receipt", request.total, "EXPENSE", request.merchant, request.categoryId, request.accountId, request.date)
        }

        val txRemote = RetrofitTransactionRemoteDataSource(stubApi)
        val catRemote = RetrofitCategoryRemoteDataSource(stubApi)
        val accRemote = RetrofitAccountRemoteDataSource(stubApi)
        val receiptRemote = RetrofitReceiptRemoteDataSource(stubApi)

        assertEquals(1, txRemote.getTransactions(TransactionFilterDto()).size)
        assertEquals("c1", catRemote.getCategories().first().id)
        assertEquals("EUR", accRemote.getAccounts().first().currency)

        val draft = receiptRemote.processReceipt(
            ReceiptImage(bytes = byteArrayOf(1, 2, 3), mimeType = "image/jpeg", fileName = "receipt.jpg")
        )
        assertEquals("Esselunga", draft.merchant)
        val confirmedTx = receiptRemote.confirmReceipt(
            draft.id,
            ConfirmReceiptRequest(
                merchant = draft.merchant,
                date = draft.date,
                subtotal = draft.subtotal,
                totalDiscount = draft.totalDiscount,
                total = draft.total,
                categoryId = "cat-food",
                accountId = "acc-main",
                items = draft.items,
                adjustments = draft.adjustments
            )
        )
        assertEquals("tx-from-receipt", confirmedTx.id)

        val fetchedReceipt = receiptRemote.getReceiptForTransaction("tx-from-receipt")
        assertEquals("Esselunga", fetchedReceipt?.merchant)
        assertEquals(1, fetchedReceipt?.items?.size)

        txRemote.deleteTransaction("t1")

        deleteResponseCode = 404
        val failure = runCatching { txRemote.deleteTransaction("t1") }.exceptionOrNull()
        assertEquals(404, (failure as HttpException).code())
    }
}

