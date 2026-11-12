package minicla03.cashmate.data.remote.api

import minicla03.cashmate.core.network.ApiEndpoints
import minicla03.cashmate.data.remote.dto.AccountDto
import minicla03.cashmate.data.remote.dto.CategoryDto
import minicla03.cashmate.data.remote.dto.ConfirmReceiptRequest
import minicla03.cashmate.data.remote.dto.CreateCategoryRequest
import minicla03.cashmate.data.remote.dto.CreateTransactionRequest
import minicla03.cashmate.data.remote.dto.DashboardDto
import minicla03.cashmate.data.remote.dto.ReceiptDraftDto
import minicla03.cashmate.data.remote.dto.StatisticsDto
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.data.remote.dto.UpdateCategoryRequest
import minicla03.cashmate.data.remote.dto.UpdateTransactionRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for the CashMate Spring Boot REST API (/api/v1/).
 */
interface CashMateApi {

    @GET(ApiEndpoints.TRANSACTIONS)
    suspend fun getTransactions(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("type") type: String? = null
    ): List<TransactionDto>

    @GET(ApiEndpoints.TRANSACTION_BY_ID)
    suspend fun getTransaction(
        @Path("id") id: String
    ): TransactionDto

    @POST(ApiEndpoints.TRANSACTIONS)
    suspend fun createTransaction(
        @Body request: CreateTransactionRequest
    ): TransactionDto

    @PUT(ApiEndpoints.TRANSACTION_BY_ID)
    suspend fun updateTransaction(
        @Path("id") id: String,
        @Body request: UpdateTransactionRequest
    ): TransactionDto

    @DELETE(ApiEndpoints.TRANSACTION_BY_ID)
    suspend fun deleteTransaction(
        @Path("id") id: String
    ): Response<Unit>

    @GET(ApiEndpoints.CATEGORIES)
    suspend fun getCategories(): List<CategoryDto>

    @POST(ApiEndpoints.CATEGORIES)
    suspend fun createCategory(
        @Body request: CreateCategoryRequest
    ): CategoryDto

    @PUT(ApiEndpoints.CATEGORY_BY_ID)
    suspend fun updateCategory(
        @Path("id") id: String,
        @Body request: UpdateCategoryRequest
    ): CategoryDto

    @DELETE(ApiEndpoints.CATEGORY_BY_ID)
    suspend fun deleteCategory(
        @Path("id") id: String
    ): Response<Unit>

    @GET(ApiEndpoints.ACCOUNTS)
    suspend fun getAccounts(): List<AccountDto>

    @GET(ApiEndpoints.DASHBOARD)
    suspend fun getDashboard(): DashboardDto

    @GET(ApiEndpoints.STATISTICS)
    suspend fun getStatistics(): StatisticsDto

    @Multipart
    @POST(ApiEndpoints.RECEIPT_PROCESS)
    suspend fun processReceipt(
        @Part image: MultipartBody.Part
    ): ReceiptDraftDto

    @POST(ApiEndpoints.RECEIPT_CONFIRM)
    suspend fun confirmReceipt(
        @Path("id") id: String,
        @Body request: ConfirmReceiptRequest
    ): TransactionDto
}

