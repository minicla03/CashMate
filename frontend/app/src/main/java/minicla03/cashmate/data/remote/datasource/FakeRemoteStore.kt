package minicla03.cashmate.data.remote.datasource

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import minicla03.cashmate.data.remote.dto.AccountDto
import minicla03.cashmate.data.remote.dto.CategoryDto
import minicla03.cashmate.data.remote.dto.CreateCategoryRequest
import minicla03.cashmate.data.remote.dto.CreateTransactionRequest
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.data.remote.dto.TransactionFilterDto
import minicla03.cashmate.data.remote.dto.UpdateCategoryRequest
import minicla03.cashmate.data.remote.dto.UpdateTransactionRequest
import minicla03.cashmate.data.remote.mapper.CategoryInUseException
import minicla03.cashmate.data.remote.mapper.DuplicateCategoryException
import minicla03.cashmate.data.remote.mapper.LastCategoryDeletionException
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

    private val transactions: MutableList<TransactionDto> = seedTransactions(LocalDate.now(clock)).toMutableList()

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
            val updated = TransactionDto(
                id = id,
                amount = request.amount,
                type = request.type,
                description = request.description,
                categoryId = request.categoryId,
                accountId = request.accountId,
                date = request.date
            )
            transactions[index] = updated
            updated
        }

    suspend fun deleteTransaction(id: String) {
        executeRemote {
            val removed = transactions.removeAll { it.id == id }
            if (!removed) throw notFoundException()
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
            TransactionDto("tx-107", 6_480L, "EXPENSE", "Organic Market", "cat-food", DEFAULT_ACCOUNT_ID, currentMonthDay(11)),
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

    companion object {
        const val DEFAULT_ACCOUNT_ID = "acc-main"
    }
}

