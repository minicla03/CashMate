package minicla03.cashmate.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.BusinessErrorReason
import minicla03.cashmate.data.remote.datasource.FakeRemoteConfig
import minicla03.cashmate.data.remote.datasource.FakeRemoteStore
import minicla03.cashmate.data.remote.datasource.FakeTransactionRemoteDataSource
import minicla03.cashmate.data.remote.dto.AccountDto
import minicla03.cashmate.data.remote.dto.CategoryDto
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.data.remote.mapper.NetworkErrorMapper
import minicla03.cashmate.data.remote.mapper.toCreateRequest
import minicla03.cashmate.data.remote.mapper.toDomain
import minicla03.cashmate.data.remote.mapper.toDto
import minicla03.cashmate.data.remote.mapper.toUpdateRequest
import minicla03.cashmate.data.repository.SafeRemoteCall
import minicla03.cashmate.data.repository.TransactionRepositoryImpl
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionFilter
import minicla03.cashmate.domain.model.TransactionType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Currency

class MappersAndErrorMapperTest {

    private val errorMapper = NetworkErrorMapper()

    @Test
    fun `maps transaction dto and domain models bidirectionally`() {
        val dto = TransactionDto(
            id = "tx-1",
            amount = 4250L,
            type = "EXPENSE",
            description = "Dinner",
            categoryId = "cat-food",
            accountId = "acc-main",
            date = "2026-11-11"
        )

        val domain = dto.toDomain()
        assertEquals(Money(4250L), domain.amount)
        assertEquals(TransactionType.EXPENSE, domain.type)
        assertEquals(LocalDate.of(2026, 11, 11), domain.date)

        val createRequest = domain.toCreateRequest()
        assertEquals(4250L, createRequest.amount)
        assertEquals("EXPENSE", createRequest.type)
        assertEquals("2026-11-11", createRequest.date)

        val updateRequest = domain.toUpdateRequest()
        assertEquals("Dinner", updateRequest.description)

        val filterDto = TransactionFilter(
            fromDate = LocalDate.of(2026, 11, 1),
            toDate = LocalDate.of(2026, 11, 30),
            categoryId = "cat-food",
            type = TransactionType.INCOME
        ).toDto()
        assertEquals("2026-11-01", filterDto.from)
        assertEquals("2026-11-30", filterDto.to)
        assertEquals("INCOME", filterDto.type)
    }

    @Test
    fun `maps category and account dtos to domain`() {
        val category = CategoryDto("cat-1", "Travel", "flight", "#1E88E5").toDomain()
        assertEquals(Category("cat-1", "Travel", "flight", "#1E88E5"), category)
        assertEquals("Travel", category.toCreateRequest().name)
        assertEquals("#1E88E5", category.toUpdateRequest().color)

        val account = AccountDto("acc-main", "Main Account", "EUR").toDomain()
        assertEquals(Currency.getInstance("EUR"), account.currency)
    }

    @Test
    fun `maps network exceptions and http errors to typed app errors`() {
        assertEquals(AppError.Network.NoInternet, errorMapper.map(UnknownHostException()))
        assertEquals(AppError.Network.NoInternet, errorMapper.map(ConnectException()))
        assertEquals(AppError.Network.Timeout, errorMapper.map(SocketTimeoutException()))
        assertEquals(AppError.Network.Unauthorized, errorMapper.map(httpException(401)))
        assertEquals(AppError.Network.Forbidden, errorMapper.map(httpException(403)))
        assertEquals(AppError.Network.NotFound, errorMapper.map(httpException(404)))
        assertEquals(
            AppError.BusinessRule(BusinessErrorReason.CATEGORY_IN_USE),
            errorMapper.map(httpException(409))
        )
        assertEquals(AppError.Network.ServerError, errorMapper.map(httpException(503)))
        assertEquals(AppError.Network.Unknown, errorMapper.map(SerializationException("bad json")))
    }

    @Test(expected = CancellationException::class)
    fun `network error mapper rethrows cancellation exception`() {
        errorMapper.map(CancellationException("cancelled"))
    }

    @Test
    fun `transaction repository works seamlessly with fake remote data source and simulated error`() = runTest {
        val clock = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC)
        val config = FakeRemoteConfig().apply { simulatedDelayMs = 0L }
        val store = FakeRemoteStore(clock, config)
        val remote = FakeTransactionRemoteDataSource(store)
        val repo = TransactionRepositoryImpl(remote, SafeRemoteCall(errorMapper))

        val created = repo.createTransaction(
            Transaction(
                id = "",
                amount = Money(1999L),
                type = TransactionType.EXPENSE,
                description = "Bookstore",
                categoryId = "cat-other",
                accountId = "acc-main",
                date = LocalDate.of(2026, 11, 11)
            )
        )
        assertTrue(created is AppResult.Success)
        val createdTx = (created as AppResult.Success).data

        val fetched = repo.getTransaction(createdTx.id)
        assertEquals(AppResult.Success(createdTx), fetched)

        config.simulatedErrorProvider = { SocketTimeoutException("timeout") }
        val failed = repo.getTransactions()
        assertEquals(AppResult.Error(AppError.Network.Timeout), failed)

        config.simulatedErrorProvider = null
        val deleted = repo.deleteTransaction(createdTx.id)
        assertTrue(deleted is AppResult.Success)

        val afterDelete = repo.getTransaction(createdTx.id)
        assertEquals(AppResult.Error(AppError.Network.NotFound), afterDelete)
    }

    private fun httpException(code: Int): HttpException = HttpException(
        Response.error<Unit>(code, "".toResponseBody("application/json".toMediaType()))
    )
}

