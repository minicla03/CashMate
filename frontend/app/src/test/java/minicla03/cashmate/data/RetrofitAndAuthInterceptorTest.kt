package minicla03.cashmate.data

import kotlinx.coroutines.test.runTest
import minicla03.cashmate.core.network.AuthInterceptor
import minicla03.cashmate.core.network.TokenProvider
import minicla03.cashmate.data.remote.api.CashMateApi
import minicla03.cashmate.data.remote.datasource.RetrofitAccountRemoteDataSource
import minicla03.cashmate.data.remote.datasource.RetrofitCategoryRemoteDataSource
import minicla03.cashmate.data.remote.datasource.RetrofitTransactionRemoteDataSource
import minicla03.cashmate.data.remote.dto.AccountDto
import minicla03.cashmate.data.remote.dto.CategoryDto
import minicla03.cashmate.data.remote.dto.CreateCategoryRequest
import minicla03.cashmate.data.remote.dto.CreateTransactionRequest
import minicla03.cashmate.data.remote.dto.DashboardDto
import minicla03.cashmate.data.remote.dto.MonthComparisonDto
import minicla03.cashmate.data.remote.dto.StatisticsDto
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.data.remote.dto.TransactionFilterDto
import minicla03.cashmate.data.remote.dto.UpdateCategoryRequest
import minicla03.cashmate.data.remote.dto.UpdateTransactionRequest
import okhttp3.MediaType.Companion.toMediaType
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
                TransactionDto(id, 100L, "EXPENSE", "Coffee", "c1", "a1", "2026-11-11")
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
        }

        val txRemote = RetrofitTransactionRemoteDataSource(stubApi)
        val catRemote = RetrofitCategoryRemoteDataSource(stubApi)
        val accRemote = RetrofitAccountRemoteDataSource(stubApi)

        assertEquals(1, txRemote.getTransactions(TransactionFilterDto()).size)
        assertEquals("c1", catRemote.getCategories().first().id)
        assertEquals("EUR", accRemote.getAccounts().first().currency)

        txRemote.deleteTransaction("t1")

        deleteResponseCode = 404
        val failure = runCatching { txRemote.deleteTransaction("t1") }.exceptionOrNull()
        assertEquals(404, (failure as HttpException).code())
    }
}

