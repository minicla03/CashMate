package minicla03.cashmate.data.remote.api

import minicla03.cashmate.core.network.ApiEndpoints
import minicla03.cashmate.data.remote.dto.AccountDto
import minicla03.cashmate.data.remote.dto.AuthResponseDto
import minicla03.cashmate.data.remote.dto.AuthTokensDto
import minicla03.cashmate.data.remote.dto.CategoryDto
import minicla03.cashmate.data.remote.dto.ChangePasswordRequest
import minicla03.cashmate.data.remote.dto.ConfirmReceiptRequest
import minicla03.cashmate.data.remote.dto.ConsentItemDto
import minicla03.cashmate.data.remote.dto.CreateCategoryRequest
import minicla03.cashmate.data.remote.dto.CreateTransactionRequest
import minicla03.cashmate.data.remote.dto.DashboardDto
import minicla03.cashmate.data.remote.dto.DeleteAccountRequest
import minicla03.cashmate.data.remote.dto.EmailVerificationResponseDto
import minicla03.cashmate.data.remote.dto.LoginRequest
import minicla03.cashmate.data.remote.dto.LogoutRequest
import minicla03.cashmate.data.remote.dto.PasswordResetRequestDto
import minicla03.cashmate.data.remote.dto.PrivacyInfoDto
import minicla03.cashmate.data.remote.dto.ReceiptDraftDto
import minicla03.cashmate.data.remote.dto.RefreshTokenRequest
import minicla03.cashmate.data.remote.dto.RegisterRequest
import minicla03.cashmate.data.remote.dto.RegisterResponseDto
import minicla03.cashmate.data.remote.dto.ResendVerificationRequest
import minicla03.cashmate.data.remote.dto.ResetPasswordRequest
import minicla03.cashmate.data.remote.dto.StatisticsDto
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.data.remote.dto.UpdateCategoryRequest
import minicla03.cashmate.data.remote.dto.UpdateConsentRequest
import minicla03.cashmate.data.remote.dto.UpdateProfileRequest
import minicla03.cashmate.data.remote.dto.UpdateTransactionRequest
import minicla03.cashmate.data.remote.dto.UserDto
import minicla03.cashmate.data.remote.dto.UserPreferencesDto
import minicla03.cashmate.data.remote.dto.VerifyEmailRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for the CashMate REST API (/api/v1/).
 */
interface CashMateApi {

    @POST(ApiEndpoints.AUTH_LOGIN)
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponseDto

    @POST(ApiEndpoints.AUTH_REGISTER)
    suspend fun register(
        @Body request: RegisterRequest
    ): RegisterResponseDto

    @POST(ApiEndpoints.AUTH_LOGOUT)
    suspend fun logout(
        @Body request: LogoutRequest
    ): Response<Unit>

    @POST(ApiEndpoints.AUTH_REFRESH)
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): AuthTokensDto

    @POST(ApiEndpoints.AUTH_VERIFY_EMAIL)
    suspend fun verifyEmail(
        @Body request: VerifyEmailRequest
    ): EmailVerificationResponseDto

    @POST(ApiEndpoints.AUTH_RESEND_VERIFICATION)
    suspend fun resendVerificationEmail(
        @Body request: ResendVerificationRequest
    ): Response<Unit>

    @POST(ApiEndpoints.AUTH_PASSWORD_RESET_REQUEST)
    suspend fun requestPasswordReset(
        @Body request: PasswordResetRequestDto
    ): Response<Unit>

    @POST(ApiEndpoints.AUTH_PASSWORD_RESET_CONFIRM)
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<Unit>

    @GET(ApiEndpoints.ME)
    suspend fun getCurrentUser(): UserDto

    @PUT(ApiEndpoints.ME)
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): UserDto

    @PUT(ApiEndpoints.ME_PASSWORD)
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<Unit>

    @GET(ApiEndpoints.ME_PREFERENCES)
    suspend fun getPreferences(): UserPreferencesDto

    @PUT(ApiEndpoints.ME_PREFERENCES)
    suspend fun updatePreferences(
        @Body request: UserPreferencesDto
    ): UserPreferencesDto

    @GET(ApiEndpoints.ME_PRIVACY)
    suspend fun getPrivacyInfo(): PrivacyInfoDto

    @PATCH(ApiEndpoints.ME_CONSENT_BY_ID)
    suspend fun updateConsent(
        @Path("id") consentId: String,
        @Body request: UpdateConsentRequest
    ): List<ConsentItemDto>

    @GET(ApiEndpoints.ME_EXPORT)
    suspend fun exportUserData(): Response<ResponseBody>

    @HTTP(method = "DELETE", path = ApiEndpoints.ME, hasBody = true)
    suspend fun deleteAccount(
        @Body request: DeleteAccountRequest
    ): Response<Unit>

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

