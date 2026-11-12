package minicla03.cashmate.data.remote.datasource

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import minicla03.cashmate.data.remote.dto.AccountDto
import minicla03.cashmate.data.remote.dto.AuthResponseDto
import minicla03.cashmate.data.remote.dto.AuthTokensDto
import minicla03.cashmate.data.remote.dto.CategoryDto
import minicla03.cashmate.data.remote.dto.ChangePasswordRequest
import minicla03.cashmate.data.remote.dto.ConfirmReceiptRequest
import minicla03.cashmate.data.remote.dto.ConsentItemDto
import minicla03.cashmate.data.remote.dto.CreateCategoryRequest
import minicla03.cashmate.data.remote.dto.CreateTransactionRequest
import minicla03.cashmate.data.remote.dto.DeleteAccountRequest
import minicla03.cashmate.data.remote.dto.EmailVerificationResponseDto
import minicla03.cashmate.data.remote.dto.LoginRequest
import minicla03.cashmate.data.remote.dto.LogoutRequest
import minicla03.cashmate.data.remote.dto.PasswordResetRequestDto
import minicla03.cashmate.data.remote.dto.PrivacyDataCategoryDto
import minicla03.cashmate.data.remote.dto.PrivacyInfoDto
import minicla03.cashmate.data.remote.dto.ReceiptAdjustmentDto
import minicla03.cashmate.data.remote.dto.ReceiptDraftDto
import minicla03.cashmate.data.remote.dto.ReceiptItemDto
import minicla03.cashmate.data.remote.dto.RefreshTokenRequest
import minicla03.cashmate.data.remote.dto.RegisterRequest
import minicla03.cashmate.data.remote.dto.RegisterResponseDto
import minicla03.cashmate.data.remote.dto.ResendVerificationRequest
import minicla03.cashmate.data.remote.dto.ResetPasswordRequest
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.data.remote.dto.TransactionFilterDto
import minicla03.cashmate.data.remote.dto.UpdateCategoryRequest
import minicla03.cashmate.data.remote.dto.UpdateConsentRequest
import minicla03.cashmate.data.remote.dto.UpdateProfileRequest
import minicla03.cashmate.data.remote.dto.UpdateTransactionRequest
import minicla03.cashmate.data.remote.dto.UserDataExportDto
import minicla03.cashmate.data.remote.dto.UserDto
import minicla03.cashmate.data.remote.dto.UserPreferencesDto
import minicla03.cashmate.data.remote.dto.VerifyEmailRequest
import minicla03.cashmate.data.remote.mapper.CategoryInUseException
import minicla03.cashmate.data.remote.mapper.DuplicateCategoryException
import minicla03.cashmate.data.remote.mapper.EmailAlreadyExistsException
import minicla03.cashmate.data.remote.mapper.EmailNotVerifiedException
import minicla03.cashmate.data.remote.mapper.InvalidCredentialsException
import minicla03.cashmate.data.remote.mapper.InvalidCurrentPasswordException
import minicla03.cashmate.data.remote.mapper.InvalidResetTokenException
import minicla03.cashmate.data.remote.mapper.InvalidVerificationTokenException
import minicla03.cashmate.data.remote.mapper.LastCategoryDeletionException
import minicla03.cashmate.data.remote.mapper.RequiredConsentCannotBeRevokedException
import minicla03.cashmate.data.remote.mapper.UnprocessableEntityException
import minicla03.cashmate.data.remote.mapper.UnsupportedImageException
import minicla03.cashmate.domain.model.ReceiptImage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime behavior controls for fake remote data sources (latency & optional error simulation).
 */
@Singleton
class FakeRemoteConfig @Inject constructor() {
    @Volatile
    var simulatedDelayMs: Long = 150L

    @Volatile
    var simulatedErrorProvider: (() -> Throwable?)? = null
}

/**
 * Thread-safe in-memory store backing the fake remote data sources during frontend development.
 * Emulates REST server CRUD semantics, HTTP status failures, and realistic initial data.
 */
@Singleton
class FakeRemoteStore @Inject constructor(
    private val clock: Clock,
    private val config: FakeRemoteConfig
) {
    private val mutex = Mutex()
    private val idSequence = AtomicLong(1000L)

    private val accounts = mutableListOf(
        AccountDto(
            id = DEFAULT_ACCOUNT_ID,
            name = "Main Account",
            currency = "EUR"
        )
    )

    private val categories = mutableListOf(
        CategoryDto(id = "cat-salary", name = "Salary", icon = "payments", color = "#2E7D32"),
        CategoryDto(id = "cat-food", name = "Food & Groceries", icon = "restaurant", color = "#5E35B1"),
        CategoryDto(id = "cat-transport", name = "Transport", icon = "directions_car", color = "#00897B"),
        CategoryDto(id = "cat-housing", name = "Housing", icon = "home", color = "#1E88E5"),
        CategoryDto(id = "cat-entertainment", name = "Entertainment", icon = "movie", color = "#FB8C00"),
        CategoryDto(id = "cat-health", name = "Health", icon = "medical_services", color = "#E53935"),
        CategoryDto(id = "cat-shopping", name = "Shopping", icon = "shopping_bag", color = "#8E24AA"),
        CategoryDto(id = "cat-other", name = "Other", icon = "category", color = "#546E7A")
    )

    private val receiptDrafts = mutableMapOf<String, ReceiptDraftDto>()
    private val confirmedReceipts = seedConfirmedReceipts(LocalDate.now(clock)).toMutableMap()
    private val transactions: MutableList<TransactionDto> = seedTransactions(LocalDate.now(clock)).toMutableList()

    private data class FakeUserRecord(
        val id: String,
        val name: String,
        val email: String,
        val password: String,
        val emailVerified: Boolean,
        val createdAt: String,
        val preferences: UserPreferencesDto = UserPreferencesDto(),
        val consents: List<ConsentItemDto> = defaultConsents()
    )

    private val users = mutableListOf(
        FakeUserRecord(
            id = SEEDED_USER_ID,
            name = "cla",
            email = SEEDED_USER_EMAIL,
            password = SEEDED_USER_PASSWORD,
            emailVerified = true,
            createdAt = "2026-01-15T10:00:00Z"
        ),
        FakeUserRecord(
            id = "user-unverified",
            name = "Pending User",
            email = SEEDED_UNVERIFIED_EMAIL,
            password = SEEDED_USER_PASSWORD,
            emailVerified = false,
            createdAt = "2026-10-01T09:00:00Z"
        )
    )
    private val verificationTokens = mutableMapOf(
        SEEDED_UNVERIFIED_EMAIL to DEMO_VERIFICATION_CODE
    )
    private val resetTokens = mutableMapOf<String, String>()
    private val refreshTokenOwner = mutableMapOf<String, String>()
    private var activeUserId: String? = null
    private val exportJson = Json { prettyPrint = true; encodeDefaults = true }

    suspend fun login(request: LoginRequest): AuthResponseDto = executeRemote {
        val email = request.email.trim().lowercase()
        val user = users.firstOrNull { it.email.equals(email, ignoreCase = true) }
            ?: throw InvalidCredentialsException()
        if (user.password != request.password) {
            throw InvalidCredentialsException()
        }
        if (!user.emailVerified) {
            throw EmailNotVerifiedException()
        }
        activeUserId = user.id
        val tokens = issueTokens(user.id)
        AuthResponseDto(user = user.toDto(), tokens = tokens)
    }

    suspend fun register(request: RegisterRequest): RegisterResponseDto = executeRemote {
        val email = request.email.trim().lowercase()
        if (users.any { it.email.equals(email, ignoreCase = true) }) {
            throw EmailAlreadyExistsException()
        }
        val created = FakeUserRecord(
            id = "user-${idSequence.incrementAndGet()}",
            name = request.name.trim(),
            email = email,
            password = request.password,
            emailVerified = false,
            createdAt = clock.instant().toString()
        )
        users.add(created)
        verificationTokens[email] = DEMO_VERIFICATION_CODE
        RegisterResponseDto(
            user = created.toDto(),
            requiresEmailVerification = true,
            tokens = null
        )
    }

    suspend fun logout(request: LogoutRequest) {
        executeRemote {
            request.refreshToken?.let { refreshTokenOwner.remove(it) }
            activeUserId = null
        }
    }

    suspend fun refreshToken(request: RefreshTokenRequest): AuthTokensDto = executeRemote {
        val userId = refreshTokenOwner.remove(request.refreshToken) ?: throw unauthorizedException()
        if (users.none { it.id == userId }) {
            throw unauthorizedException()
        }
        activeUserId = userId
        issueTokens(userId)
    }

    suspend fun verifyEmail(request: VerifyEmailRequest): EmailVerificationResponseDto = executeRemote {
        val email = request.email.trim().lowercase()
        val index = users.indexOfFirst { it.email.equals(email, ignoreCase = true) }
        if (index < 0) throw InvalidVerificationTokenException()
        val expectedToken = verificationTokens[email]
        if (expectedToken == null || request.token.trim() != expectedToken) {
            throw InvalidVerificationTokenException()
        }
        verificationTokens.remove(email)
        users[index] = users[index].copy(emailVerified = true)
        EmailVerificationResponseDto(email = users[index].email, verified = true)
    }

    suspend fun resendVerificationEmail(request: ResendVerificationRequest) {
        executeRemote {
            val email = request.email.trim().lowercase()
            if (users.any { it.email.equals(email, ignoreCase = true) }) {
                verificationTokens[email] = DEMO_VERIFICATION_CODE
            }
        }
    }

    suspend fun requestPasswordReset(request: PasswordResetRequestDto) {
        executeRemote {
            val email = request.email.trim().lowercase()
            if (users.any { it.email.equals(email, ignoreCase = true) }) {
                resetTokens[email] = DEMO_RESET_CODE
            }
        }
    }

    suspend fun resetPassword(request: ResetPasswordRequest) {
        executeRemote {
            val email = request.email.trim().lowercase()
            val index = users.indexOfFirst { it.email.equals(email, ignoreCase = true) }
            val expectedToken = resetTokens[email]
            if (index < 0 || expectedToken == null || request.token.trim() != expectedToken) {
                throw InvalidResetTokenException()
            }
            resetTokens.remove(email)
            users[index] = users[index].copy(password = request.newPassword)
            refreshTokenOwner.entries.removeAll { it.value == users[index].id }
        }
    }

    suspend fun getCurrentUser(): UserDto = executeRemote {
        requireActiveUser().toDto()
    }

    suspend fun updateProfile(request: UpdateProfileRequest): UserDto = executeRemote {
        val active = requireActiveUser()
        val newEmail = request.email.trim().lowercase()
        val duplicate = users.any { it.id != active.id && it.email.equals(newEmail, ignoreCase = true) }
        if (duplicate) throw EmailAlreadyExistsException()
        val index = users.indexOfFirst { it.id == active.id }
        val updated = active.copy(
            name = request.name.trim(),
            email = newEmail
        )
        users[index] = updated
        updated.toDto()
    }

    suspend fun changePassword(request: ChangePasswordRequest) {
        executeRemote {
            val active = requireActiveUser()
            if (active.password != request.currentPassword) {
                throw InvalidCurrentPasswordException()
            }
            val index = users.indexOfFirst { it.id == active.id }
            users[index] = active.copy(password = request.newPassword)
        }
    }

    suspend fun getPreferences(): UserPreferencesDto = executeRemote {
        requireActiveUser().preferences
    }

    suspend fun updatePreferences(request: UserPreferencesDto): UserPreferencesDto = executeRemote {
        val active = requireActiveUser()
        val index = users.indexOfFirst { it.id == active.id }
        val updated = active.copy(preferences = request)
        users[index] = updated
        updated.preferences
    }

    suspend fun getPrivacyInfo(): PrivacyInfoDto = executeRemote {
        val active = requireActiveUser()
        PrivacyInfoDto(
            summary = "CashMate stores only the account, preference, and financial data required to provide expense tracking and receipt parsing via the backend.",
            dataCategories = listOf(
                PrivacyDataCategoryDto(
                    id = "data-account",
                    title = "Account & profile",
                    description = "Your display name, email address, verification status, and account creation timestamp.",
                    retentionSummary = "Retained while your account remains active."
                ),
                PrivacyDataCategoryDto(
                    id = "data-preferences",
                    title = "Application preferences",
                    description = "Your selected theme mode and display currency.",
                    retentionSummary = "Retained with your account preferences."
                ),
                PrivacyDataCategoryDto(
                    id = "data-finance",
                    title = "Transactions, categories & accounts",
                    description = "Expense and income entries, category labels, and confirmed receipt line items.",
                    retentionSummary = "Retained until modified or deleted by you, or upon account deletion."
                ),
                PrivacyDataCategoryDto(
                    id = "data-receipts",
                    title = "Receipt uploads",
                    description = "Images uploaded for server-side OCR processing and structured item extraction.",
                    retentionSummary = "Receipt image lifecycle is governed by backend retention policies."
                )
            ),
            consents = active.consents,
            policyUpdatedAt = "2026-11-01"
        )
    }

    suspend fun updateConsent(consentId: String, request: UpdateConsentRequest): List<ConsentItemDto> =
        executeRemote {
            val active = requireActiveUser()
            val consentIndex = active.consents.indexOfFirst { it.id == consentId }
            if (consentIndex < 0) throw notFoundException()
            val target = active.consents[consentIndex]
            if (target.required && !request.granted) {
                throw RequiredConsentCannotBeRevokedException()
            }
            val updatedConsents = active.consents.toMutableList()
            updatedConsents[consentIndex] = target.copy(
                granted = request.granted,
                updatedAt = clock.instant().toString()
            )
            val userIndex = users.indexOfFirst { it.id == active.id }
            users[userIndex] = active.copy(consents = updatedConsents)
            updatedConsents
        }

    suspend fun exportUserData(): UserDataExportDto = executeRemote {
        val active = requireActiveUser()
        val payload = FakeExportPayload(
            exportedAt = clock.instant().toString(),
            user = active.toDto(),
            preferences = active.preferences,
            consents = active.consents,
            accounts = accounts.toList(),
            categories = categories.toList(),
            transactions = transactions.toList()
        )
        val jsonBytes = exportJson.encodeToString(payload).toByteArray(Charsets.UTF_8)
        UserDataExportDto(
            fileName = "cashmate-export-${LocalDate.now(clock)}.json",
            mimeType = "application/json",
            contentBytes = jsonBytes,
            generatedAt = payload.exportedAt
        )
    }

    suspend fun deleteAccount(request: DeleteAccountRequest) {
        executeRemote {
            val active = requireActiveUser()
            if (request.confirmation.trim() != "DELETE") {
                throw UnprocessableEntityException()
            }
            users.removeAll { it.id == active.id }
            refreshTokenOwner.entries.removeAll { it.value == active.id }
            activeUserId = null
        }
    }

    private fun requireActiveUser(): FakeUserRecord {
        val id = activeUserId ?: throw unauthorizedException()
        return users.firstOrNull { it.id == id } ?: throw unauthorizedException()
    }

    private fun issueTokens(userId: String): AuthTokensDto {
        val seq = idSequence.incrementAndGet()
        val accessToken = "fake-access-$userId-$seq"
        val refreshToken = "fake-refresh-$userId-$seq"
        refreshTokenOwner[refreshToken] = userId
        return AuthTokensDto(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresInSeconds = 3600L
        )
    }

    private fun FakeUserRecord.toDto(): UserDto = UserDto(
        id = id,
        name = name,
        email = email,
        emailVerified = emailVerified,
        createdAt = createdAt
    )

    @Serializable
    private data class FakeExportPayload(
        val exportedAt: String,
        val user: UserDto,
        val preferences: UserPreferencesDto,
        val consents: List<ConsentItemDto>,
        val accounts: List<AccountDto>,
        val categories: List<CategoryDto>,
        val transactions: List<TransactionDto>
    )

    suspend fun getTransactions(filter: TransactionFilterDto?): List<TransactionDto> = executeRemote {
        val fromDate = filter?.from?.let(LocalDate::parse)
        val toDate = filter?.to?.let(LocalDate::parse)
        transactions.filter { dto ->
            val txDate = LocalDate.parse(dto.date)
            val matchesFrom = fromDate == null || !txDate.isBefore(fromDate)
            val matchesTo = toDate == null || !txDate.isAfter(toDate)
            val matchesCategory = filter?.categoryId == null || dto.categoryId == filter.categoryId
            val matchesType = filter?.type == null || dto.type.equals(filter.type, ignoreCase = true)
            matchesFrom && matchesTo && matchesCategory && matchesType
        }
    }

    suspend fun getTransaction(id: String): TransactionDto = executeRemote {
        transactions.firstOrNull { it.id == id } ?: throw notFoundException()
    }

    suspend fun createTransaction(request: CreateTransactionRequest): TransactionDto = executeRemote {
        val created = TransactionDto(
            id = "tx-${idSequence.incrementAndGet()}",
            amount = request.amount,
            type = request.type,
            description = request.description,
            categoryId = request.categoryId,
            accountId = request.accountId,
            date = request.date
        )
        transactions.add(created)
        created
    }

    suspend fun updateTransaction(id: String, request: UpdateTransactionRequest): TransactionDto =
        executeRemote {
            val index = transactions.indexOfFirst { it.id == id }
            if (index < 0) throw notFoundException()
            val existing = transactions[index]
            val receiptPayload = request.receipt
            if (receiptPayload == null) {
                val updated = TransactionDto(
                    id = id,
                    amount = request.amount,
                    type = request.type,
                    description = request.description,
                    categoryId = request.categoryId,
                    accountId = request.accountId,
                    date = request.date,
                    source = existing.source,
                    receiptId = existing.receiptId,
                    receipt = existing.receipt
                )
                transactions[index] = updated
                return@executeRemote updated
            }

            val normalizedItems = receiptPayload.items.map { item ->
                val itemId = item.id.ifBlank { "item-${idSequence.incrementAndGet()}" }
                val gross = item.unitPrice * item.quantity.toLong()
                val net = (gross - item.discountAmount).coerceAtLeast(0L)
                item.copy(
                    id = itemId,
                    description = item.description.trim(),
                    grossAmount = gross,
                    netAmount = net,
                    category = item.category.trim()
                )
            }
            val normalizedAdjustments = receiptPayload.adjustments.map { adj ->
                adj.copy(description = adj.description.trim())
            }
            val subtotal = normalizedItems.sumOf { it.grossAmount }
            val itemDiscounts = normalizedItems.sumOf { it.discountAmount }
            val itemsNet = normalizedItems.sumOf { it.netAmount }
            val adjDiscounts = normalizedAdjustments
                .filter { it.type.equals("DISCOUNT", ignoreCase = true) }
                .sumOf { it.amount }
            val adjAdditions = normalizedAdjustments
                .filter {
                    it.type.equals("SURCHARGE", ignoreCase = true) ||
                        it.type.equals("TAX", ignoreCase = true)
                }
                .sumOf { it.amount }
            val totalDiscount = itemDiscounts + adjDiscounts
            val total = if (normalizedItems.isNotEmpty()) {
                (itemsNet - adjDiscounts + adjAdditions).coerceAtLeast(0L)
            } else {
                request.amount
            }
            val resolvedReceiptId = existing.receiptId ?: receiptPayload.id
            val updatedReceipt = ReceiptDraftDto(
                id = resolvedReceiptId,
                merchant = request.description.trim().ifBlank { receiptPayload.merchant.trim() },
                date = request.date,
                subtotal = subtotal,
                totalDiscount = totalDiscount,
                total = total,
                categoryId = request.categoryId,
                accountId = request.accountId,
                items = normalizedItems,
                adjustments = normalizedAdjustments
            )
            confirmedReceipts[resolvedReceiptId] = updatedReceipt

            val updated = TransactionDto(
                id = id,
                amount = total,
                type = request.type,
                description = request.description.trim(),
                categoryId = request.categoryId,
                accountId = request.accountId,
                date = request.date,
                source = existing.source,
                receiptId = resolvedReceiptId,
                receipt = updatedReceipt
            )
            transactions[index] = updated
            updated
        }

    suspend fun deleteTransaction(id: String) {
        executeRemote {
            val existing = transactions.firstOrNull { it.id == id } ?: throw notFoundException()
            transactions.removeAll { it.id == id }
            existing.receiptId?.let { confirmedReceipts.remove(it) }
        }
    }

    suspend fun getCategories(): List<CategoryDto> = executeRemote {
        categories.toList()
    }

    suspend fun createCategory(request: CreateCategoryRequest): CategoryDto = executeRemote {
        if (categories.any { it.name.equals(request.name.trim(), ignoreCase = true) }) {
            throw DuplicateCategoryException()
        }
        val created = CategoryDto(
            id = "cat-${idSequence.incrementAndGet()}",
            name = request.name.trim(),
            icon = request.icon,
            color = request.color
        )
        categories.add(created)
        created
    }

    suspend fun updateCategory(id: String, request: UpdateCategoryRequest): CategoryDto = executeRemote {
        val index = categories.indexOfFirst { it.id == id }
        if (index < 0) throw notFoundException()
        val duplicate = categories.any {
            it.id != id && it.name.equals(request.name.trim(), ignoreCase = true)
        }
        if (duplicate) throw DuplicateCategoryException()
        val updated = CategoryDto(
            id = id,
            name = request.name.trim(),
            icon = request.icon,
            color = request.color
        )
        categories[index] = updated
        updated
    }

    suspend fun deleteCategory(id: String) {
        executeRemote {
            if (categories.none { it.id == id }) throw notFoundException()
            if (categories.size <= 1) throw LastCategoryDeletionException()
            if (transactions.any { it.categoryId == id }) throw CategoryInUseException()
            categories.removeAll { it.id == id }
        }
    }

    suspend fun getAccounts(): List<AccountDto> = executeRemote {
        accounts.toList()
    }

    suspend fun processReceipt(image: ReceiptImage): ReceiptDraftDto = executeRemote {
        if (image.bytes.isEmpty() || !image.mimeType.startsWith("image/", ignoreCase = true)) {
            throw UnsupportedImageException()
        }
        val draftId = "receipt-${idSequence.incrementAndGet()}"
        val today = LocalDate.now(clock).toString()
        val draft = ReceiptDraftDto(
            id = draftId,
            merchant = "Esselunga",
            date = today,
            subtotal = 2_380L,
            totalDiscount = 500L,
            total = 1_880L,
            categoryId = "cat-food",
            accountId = DEFAULT_ACCOUNT_ID,
            items = listOf(
                ReceiptItemDto(
                    id = "item-1",
                    description = "Pasta di Semola 500g",
                    quantity = 2,
                    unitPrice = 125L,
                    grossAmount = 250L,
                    discountAmount = 50L,
                    netAmount = 200L,
                    category = "FOOD"
                ),
                ReceiptItemDto(
                    id = "item-2",
                    description = "Latte Fresco Intero 1L",
                    quantity = 2,
                    unitPrice = 180L,
                    grossAmount = 360L,
                    discountAmount = 0L,
                    netAmount = 360L,
                    category = "FOOD"
                ),
                ReceiptItemDto(
                    id = "item-3",
                    description = "Olio Extra Vergine 1L",
                    quantity = 1,
                    unitPrice = 990L,
                    grossAmount = 990L,
                    discountAmount = 200L,
                    netAmount = 790L,
                    category = "FOOD"
                ),
                ReceiptItemDto(
                    id = "item-4",
                    description = "Detersivo Piatti Eco",
                    quantity = 1,
                    unitPrice = 340L,
                    grossAmount = 340L,
                    discountAmount = 0L,
                    netAmount = 340L,
                    category = "SHOPPING"
                ),
                ReceiptItemDto(
                    id = "item-5",
                    description = "Biglietto Trasporto Urbano",
                    quantity = 2,
                    unitPrice = 220L,
                    grossAmount = 440L,
                    discountAmount = 0L,
                    netAmount = 440L,
                    category = "TRANSPORT"
                )
            ),
            adjustments = listOf(
                ReceiptAdjustmentDto(
                    id = "adj-1",
                    description = "Carta Fidaty - Buono Sconto",
                    amount = 250L,
                    type = "DISCOUNT"
                )
            )
        )
        receiptDrafts[draftId] = draft
        draft
    }

    suspend fun confirmReceipt(id: String, request: ConfirmReceiptRequest): TransactionDto =
        executeRemote {
            if (!receiptDrafts.containsKey(id)) {
                throw notFoundException()
            }
            receiptDrafts.remove(id)
            val confirmedReceipt = ReceiptDraftDto(
                id = id,
                merchant = request.merchant.trim(),
                date = request.date,
                subtotal = request.subtotal,
                totalDiscount = request.totalDiscount,
                total = request.total,
                categoryId = request.categoryId,
                accountId = request.accountId,
                items = request.items,
                adjustments = request.adjustments
            )
            confirmedReceipts[id] = confirmedReceipt
            val created = TransactionDto(
                id = "tx-${idSequence.incrementAndGet()}",
                amount = request.total,
                type = "EXPENSE",
                description = request.merchant.trim(),
                categoryId = request.categoryId,
                accountId = request.accountId,
                date = request.date,
                source = "RECEIPT",
                receiptId = id,
                receipt = confirmedReceipt
            )
            transactions.add(created)
            created
        }

    suspend fun getReceiptForTransaction(transactionId: String): ReceiptDraftDto? = executeRemote {
        val tx = transactions.firstOrNull { it.id == transactionId } ?: throw notFoundException()
        val receiptId = tx.receiptId ?: return@executeRemote tx.receipt
        confirmedReceipts[receiptId] ?: tx.receipt
    }

    private suspend fun <T> executeRemote(block: () -> T): T {
        val delayMs = config.simulatedDelayMs
        if (delayMs > 0L) {
            delay(delayMs)
        }
        config.simulatedErrorProvider?.invoke()?.let { throw it }
        return mutex.withLock { block() }
    }

    private fun notFoundException(): HttpException = HttpException(
        Response.error<Unit>(404, "".toResponseBody("application/json".toMediaType()))
    )

    private fun unauthorizedException(): HttpException = HttpException(
        Response.error<Unit>(401, "".toResponseBody("application/json".toMediaType()))
    )

    private fun seedTransactions(today: LocalDate): List<TransactionDto> {
        val currentMonth = YearMonth.from(today)
        fun currentMonthDay(day: Int): String =
            currentMonth.atDay(day.coerceIn(1, today.dayOfMonth)).toString()

        fun pastMonthDay(monthsAgo: Long, day: Int): String {
            val month = currentMonth.minusMonths(monthsAgo)
            return month.atDay(day.coerceIn(1, month.lengthOfMonth())).toString()
        }

        return listOf(
            // Current month
            TransactionDto("tx-101", 265_000L, "INCOME", "Monthly Salary", "cat-salary", DEFAULT_ACCOUNT_ID, currentMonthDay(1)),
            TransactionDto("tx-102", 85_000L, "EXPENSE", "Apartment Rent & Utilities", "cat-housing", DEFAULT_ACCOUNT_ID, currentMonthDay(2)),
            TransactionDto("tx-103", 14_250L, "EXPENSE", "Supermarket Weekly Shopping", "cat-food", DEFAULT_ACCOUNT_ID, currentMonthDay(4)),
            TransactionDto("tx-104", 4_900L, "EXPENSE", "Metro & Train Pass", "cat-transport", DEFAULT_ACCOUNT_ID, currentMonthDay(6)),
            TransactionDto("tx-105", 3_850L, "EXPENSE", "Cinema & Dinner", "cat-entertainment", DEFAULT_ACCOUNT_ID, currentMonthDay(8)),
            TransactionDto("tx-106", 2_490L, "EXPENSE", "Pharmacy", "cat-health", DEFAULT_ACCOUNT_ID, currentMonthDay(9)),
            TransactionDto(
                id = "tx-107",
                amount = 6_480L,
                type = "EXPENSE",
                description = "Organic Market",
                categoryId = "cat-food",
                accountId = DEFAULT_ACCOUNT_ID,
                date = currentMonthDay(11),
                source = "RECEIPT",
                receiptId = SEEDED_RECEIPT_ID
            ),
            TransactionDto("tx-108", 1_850L, "EXPENSE", "Specialty Coffee & Bakery", "cat-food", DEFAULT_ACCOUNT_ID, currentMonthDay(12)),

            // 1 month ago
            TransactionDto("tx-201", 260_000L, "INCOME", "Monthly Salary", "cat-salary", DEFAULT_ACCOUNT_ID, pastMonthDay(1, 1)),
            TransactionDto("tx-202", 85_000L, "EXPENSE", "Apartment Rent & Utilities", "cat-housing", DEFAULT_ACCOUNT_ID, pastMonthDay(1, 3)),
            TransactionDto("tx-203", 21_400L, "EXPENSE", "Groceries & Pantry", "cat-food", DEFAULT_ACCOUNT_ID, pastMonthDay(1, 9)),
            TransactionDto("tx-204", 9_200L, "EXPENSE", "Running Shoes", "cat-shopping", DEFAULT_ACCOUNT_ID, pastMonthDay(1, 14)),
            TransactionDto("tx-205", 6_500L, "EXPENSE", "Fuel & Highway Toll", "cat-transport", DEFAULT_ACCOUNT_ID, pastMonthDay(1, 19)),
            TransactionDto("tx-206", 5_400L, "EXPENSE", "Concert Tickets", "cat-entertainment", DEFAULT_ACCOUNT_ID, pastMonthDay(1, 24)),

            // 2 months ago
            TransactionDto("tx-301", 260_000L, "INCOME", "Monthly Salary", "cat-salary", DEFAULT_ACCOUNT_ID, pastMonthDay(2, 1)),
            TransactionDto("tx-302", 84_000L, "EXPENSE", "Apartment Rent & Utilities", "cat-housing", DEFAULT_ACCOUNT_ID, pastMonthDay(2, 3)),
            TransactionDto("tx-303", 19_800L, "EXPENSE", "Groceries", "cat-food", DEFAULT_ACCOUNT_ID, pastMonthDay(2, 11)),
            TransactionDto("tx-304", 12_000L, "EXPENSE", "Dental Checkup", "cat-health", DEFAULT_ACCOUNT_ID, pastMonthDay(2, 18)),
            TransactionDto("tx-305", 4_800L, "EXPENSE", "Transit Pass", "cat-transport", DEFAULT_ACCOUNT_ID, pastMonthDay(2, 22)),

            // 3 months ago
            TransactionDto("tx-401", 255_000L, "INCOME", "Monthly Salary", "cat-salary", DEFAULT_ACCOUNT_ID, pastMonthDay(3, 1)),
            TransactionDto("tx-402", 84_000L, "EXPENSE", "Apartment Rent", "cat-housing", DEFAULT_ACCOUNT_ID, pastMonthDay(3, 2)),
            TransactionDto("tx-403", 22_500L, "EXPENSE", "Groceries & Dining", "cat-food", DEFAULT_ACCOUNT_ID, pastMonthDay(3, 12)),
            TransactionDto("tx-404", 15_600L, "EXPENSE", "Summer Wardrobe", "cat-shopping", DEFAULT_ACCOUNT_ID, pastMonthDay(3, 20)),

            // 4 months ago
            TransactionDto("tx-501", 255_000L, "INCOME", "Monthly Salary", "cat-salary", DEFAULT_ACCOUNT_ID, pastMonthDay(4, 1)),
            TransactionDto("tx-502", 84_000L, "EXPENSE", "Apartment Rent", "cat-housing", DEFAULT_ACCOUNT_ID, pastMonthDay(4, 2)),
            TransactionDto("tx-503", 18_700L, "EXPENSE", "Groceries", "cat-food", DEFAULT_ACCOUNT_ID, pastMonthDay(4, 15)),
            TransactionDto("tx-504", 7_200L, "EXPENSE", "Weekend Trip", "cat-entertainment", DEFAULT_ACCOUNT_ID, pastMonthDay(4, 23)),

            // 5 months ago
            TransactionDto("tx-601", 255_000L, "INCOME", "Monthly Salary", "cat-salary", DEFAULT_ACCOUNT_ID, pastMonthDay(5, 1)),
            TransactionDto("tx-602", 84_000L, "EXPENSE", "Apartment Rent", "cat-housing", DEFAULT_ACCOUNT_ID, pastMonthDay(5, 2)),
            TransactionDto("tx-603", 17_900L, "EXPENSE", "Groceries", "cat-food", DEFAULT_ACCOUNT_ID, pastMonthDay(5, 14)),
            TransactionDto("tx-604", 5_100L, "EXPENSE", "Public Transport", "cat-transport", DEFAULT_ACCOUNT_ID, pastMonthDay(5, 21))
        )
    }

    private fun seedConfirmedReceipts(today: LocalDate): Map<String, ReceiptDraftDto> {
        val currentMonth = YearMonth.from(today)
        val seededDate = currentMonth.atDay(11.coerceIn(1, today.dayOfMonth)).toString()
        val seededReceipt = ReceiptDraftDto(
            id = SEEDED_RECEIPT_ID,
            merchant = "Organic Market",
            date = seededDate,
            subtotal = 7_580L,
            totalDiscount = 1_100L,
            total = 6_480L,
            categoryId = "cat-food",
            accountId = DEFAULT_ACCOUNT_ID,
            items = listOf(
                ReceiptItemDto(
                    id = "seed-item-1",
                    description = "Pasta Barilla Integrale 500g",
                    quantity = 2,
                    unitPrice = 250L,
                    grossAmount = 500L,
                    discountAmount = 100L,
                    netAmount = 400L,
                    category = "FOOD"
                ),
                ReceiptItemDto(
                    id = "seed-item-2",
                    description = "Latte Fresco Bio 1L",
                    quantity = 3,
                    unitPrice = 190L,
                    grossAmount = 570L,
                    discountAmount = 0L,
                    netAmount = 570L,
                    category = "FOOD"
                ),
                ReceiptItemDto(
                    id = "seed-item-3",
                    description = "Olio Extra Vergine DOP 1L",
                    quantity = 1,
                    unitPrice = 1_490L,
                    grossAmount = 1_490L,
                    discountAmount = 300L,
                    netAmount = 1_190L,
                    category = "FOOD"
                ),
                ReceiptItemDto(
                    id = "seed-item-4",
                    description = "Filetto di Salmone Fresco",
                    quantity = 2,
                    unitPrice = 1_250L,
                    grossAmount = 2_500L,
                    discountAmount = 0L,
                    netAmount = 2_500L,
                    category = "FOOD"
                ),
                ReceiptItemDto(
                    id = "seed-item-5",
                    description = "Detersivo Lavatrice Eco",
                    quantity = 1,
                    unitPrice = 890L,
                    grossAmount = 890L,
                    discountAmount = 190L,
                    netAmount = 700L,
                    category = "SHOPPING"
                ),
                ReceiptItemDto(
                    id = "seed-item-6",
                    description = "Frutta e Verdura di Stagione",
                    quantity = 1,
                    unitPrice = 1_630L,
                    grossAmount = 1_630L,
                    discountAmount = 0L,
                    netAmount = 1_630L,
                    category = "FOOD"
                )
            ),
            adjustments = listOf(
                ReceiptAdjustmentDto(
                    id = "seed-adj-1",
                    description = "Member Weekend Discount",
                    amount = 510L,
                    type = "DISCOUNT"
                )
            )
        )
        return mapOf(SEEDED_RECEIPT_ID to seededReceipt)
    }

    companion object {
        const val DEFAULT_ACCOUNT_ID = "acc-main"
        const val SEEDED_RECEIPT_ID = "receipt-seed-107"
        const val SEEDED_USER_ID = "user-1"
        const val SEEDED_USER_EMAIL = "user@example.com"
        const val SEEDED_UNVERIFIED_EMAIL = "pending@example.com"
        const val SEEDED_USER_PASSWORD = "Password123!"
        const val DEMO_VERIFICATION_CODE = "123456"
        const val DEMO_RESET_CODE = "123456"

        private fun defaultConsents(): List<ConsentItemDto> = listOf(
            ConsentItemDto(
                id = "consent-essential",
                title = "Core account & financial storage",
                description = "Required to authenticate your account and store your transactions, categories, and preferences.",
                required = true,
                granted = true,
                updatedAt = "2026-01-15T10:00:00Z"
            ),
            ConsentItemDto(
                id = "consent-receipt-ocr",
                title = "Receipt categorization improvements",
                description = "Optional processing to improve automated category matching on uploaded receipts.",
                required = false,
                granted = false,
                updatedAt = "2026-01-15T10:00:00Z"
            ),
            ConsentItemDto(
                id = "consent-product-updates",
                title = "Account & feature announcements",
                description = "Optional email updates about new CashMate capabilities.",
                required = false,
                granted = false,
                updatedAt = "2026-01-15T10:00:00Z"
            )
        )
    }
}

