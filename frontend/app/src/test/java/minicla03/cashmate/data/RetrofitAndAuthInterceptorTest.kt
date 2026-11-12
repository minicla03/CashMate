package minicla03.cashmate.data

import kotlinx.coroutines.test.runTest
import minicla03.cashmate.core.network.AuthInterceptor
import minicla03.cashmate.core.network.InMemoryTokenStorage
import minicla03.cashmate.core.network.RefreshedTokenPair
import minicla03.cashmate.core.network.SessionExpiryNotifier
import minicla03.cashmate.core.network.TokenProvider
import minicla03.cashmate.core.network.TokenRefreshAuthenticator
import minicla03.cashmate.core.network.TokenRefresher
import minicla03.cashmate.data.remote.api.CashMateApi
import minicla03.cashmate.data.remote.datasource.RetrofitAccountRemoteDataSource
import minicla03.cashmate.data.remote.datasource.RetrofitAuthRemoteDataSource
import minicla03.cashmate.data.remote.datasource.RetrofitCategoryRemoteDataSource
import minicla03.cashmate.data.remote.datasource.RetrofitReceiptRemoteDataSource
import minicla03.cashmate.data.remote.datasource.RetrofitTransactionRemoteDataSource
import minicla03.cashmate.data.remote.datasource.RetrofitUserRemoteDataSource
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
import minicla03.cashmate.data.remote.dto.MonthComparisonDto
import minicla03.cashmate.data.remote.dto.PasswordResetRequestDto
import minicla03.cashmate.data.remote.dto.PrivacyInfoDto
import minicla03.cashmate.data.remote.dto.ReceiptDraftDto
import minicla03.cashmate.data.remote.dto.ReceiptItemDto
import minicla03.cashmate.data.remote.dto.RefreshTokenRequest
import minicla03.cashmate.data.remote.dto.RegisterRequest
import minicla03.cashmate.data.remote.dto.RegisterResponseDto
import minicla03.cashmate.data.remote.dto.ResendVerificationRequest
import minicla03.cashmate.data.remote.dto.ResetPasswordRequest
import minicla03.cashmate.data.remote.dto.StatisticsDto
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.data.remote.dto.TransactionFilterDto
import minicla03.cashmate.data.remote.dto.UpdateCategoryRequest
import minicla03.cashmate.data.remote.dto.UpdateConsentRequest
import minicla03.cashmate.data.remote.dto.UpdateProfileRequest
import minicla03.cashmate.data.remote.dto.UpdateTransactionRequest
import minicla03.cashmate.data.remote.dto.UserDto
import minicla03.cashmate.data.remote.dto.UserPreferencesDto
import minicla03.cashmate.data.remote.dto.VerifyEmailRequest
import minicla03.cashmate.domain.model.ReceiptImage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

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
            override suspend fun login(request: LoginRequest) = AuthResponseDto(
                UserDto("u1", "cla", request.email, emailVerified = true),
                AuthTokensDto("acc-1", "ref-1")
            )
            override suspend fun register(request: RegisterRequest) = RegisterResponseDto(
                UserDto("u1", request.name, request.email, emailVerified = false),
                requiresEmailVerification = true
            )
            override suspend fun logout(request: LogoutRequest): Response<Unit> = Response.success(Unit)
            override suspend fun refreshToken(request: RefreshTokenRequest) = AuthTokensDto("acc-2", "ref-2")
            override suspend fun verifyEmail(request: VerifyEmailRequest) = EmailVerificationResponseDto(request.email, true)
            override suspend fun resendVerificationEmail(request: ResendVerificationRequest): Response<Unit> = Response.success(Unit)
            override suspend fun requestPasswordReset(request: PasswordResetRequestDto): Response<Unit> = Response.success(Unit)
            override suspend fun resetPassword(request: ResetPasswordRequest): Response<Unit> = Response.success(Unit)
            override suspend fun getCurrentUser() = UserDto("u1", "cla", "user@example.com", emailVerified = true)
            override suspend fun updateProfile(request: UpdateProfileRequest) = UserDto("u1", request.name, request.email, emailVerified = true)
            override suspend fun changePassword(request: ChangePasswordRequest): Response<Unit> = Response.success(Unit)
            override suspend fun getPreferences() = UserPreferencesDto("DARK", "USD")
            override suspend fun updatePreferences(request: UserPreferencesDto) = request
            override suspend fun getPrivacyInfo() = PrivacyInfoDto("Summary", emptyList(), emptyList())
            override suspend fun updateConsent(consentId: String, request: UpdateConsentRequest): List<ConsentItemDto> = emptyList()
            override suspend fun exportUserData(): Response<ResponseBody> = Response.success(
                "{}".toResponseBody("application/json".toMediaType())
            )
            override suspend fun deleteAccount(request: DeleteAccountRequest): Response<Unit> = Response.success(Unit)
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
        val authRemote = RetrofitAuthRemoteDataSource(stubApi)
        val userRemote = RetrofitUserRemoteDataSource(stubApi)

        assertEquals("acc-1", authRemote.login(LoginRequest("user@example.com", "pw")).tokens.accessToken)
        assertEquals("cla", userRemote.getCurrentUser().name)
        assertEquals("DARK", userRemote.getPreferences().themeMode)

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

    @Test
    fun `token refresh authenticator refreshes on 401 and avoids infinite loops on failure`() {
        val tokenStorage = InMemoryTokenStorage().apply {
            saveTokens("expired-access", "valid-refresh")
        }
        val notifier = SessionExpiryNotifier()
        var refreshCalls = 0
        var shouldRefreshSucceed = true

        val authenticator = TokenRefreshAuthenticator(
            tokenStorage = tokenStorage,
            tokenRefresher = object : TokenRefresher {
                override fun refreshTokensSync(refreshToken: String): RefreshedTokenPair? {
                    refreshCalls++
                    return if (shouldRefreshSucceed && refreshToken == "valid-refresh") {
                        RefreshedTokenPair("fresh-access", "fresh-refresh")
                    } else {
                        null
                    }
                }
            },
            sessionExpiryNotifier = notifier
        )

        val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        val serverThread = thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: break
                socket.use { s ->
                    val reader = s.getInputStream().bufferedReader()
                    var authHeader: String? = null
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                        if (line.startsWith("Authorization:", ignoreCase = true)) {
                            authHeader = line.substringAfter(":").trim()
                        }
                    }
                    val statusLine = if (authHeader == "Bearer fresh-access") {
                        "HTTP/1.1 200 OK"
                    } else {
                        "HTTP/1.1 401 Unauthorized"
                    }
                    val body = "{}"
                    val httpResponse = "$statusLine\r\nContent-Type: application/json\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body"
                    s.getOutputStream().write(httpResponse.toByteArray())
                    s.getOutputStream().flush()
                }
            }
        }

        try {
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(object : TokenProvider {
                    override fun getAccessToken(): String? = tokenStorage.getAccessToken()
                }))
                .authenticator(authenticator)
                .build()

            val url = "http://127.0.0.1:${server.localPort}/api/v1/me"

            // 1. Expired token triggers refresh and retries with new access token
            val successResponse = client.newCall(Request.Builder().url(url).build()).execute()
            assertEquals(200, successResponse.code)
            assertEquals(1, refreshCalls)
            assertEquals("fresh-access", tokenStorage.getAccessToken())
            assertEquals("fresh-refresh", tokenStorage.getRefreshToken())

            // 2. Failing refresh clears tokens and terminates without infinite retries
            tokenStorage.saveTokens("bad-access", "revoked-refresh")
            shouldRefreshSucceed = false
            val failedResponse = client.newCall(Request.Builder().url(url).build()).execute()
            assertEquals(401, failedResponse.code)
            assertEquals(2, refreshCalls)
            assertNull(tokenStorage.getAccessToken())
            assertNull(tokenStorage.getRefreshToken())
            assertTrue(refreshCalls == 2)
        } finally {
            server.close()
            serverThread.join(1000)
        }
    }
}

