package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.api.CashMateApi
import minicla03.cashmate.data.remote.dto.CreateTransactionRequest
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.data.remote.dto.TransactionFilterDto
import minicla03.cashmate.data.remote.dto.UpdateTransactionRequest
import retrofit2.HttpException
import javax.inject.Inject

class RetrofitTransactionRemoteDataSource @Inject constructor(
    private val api: CashMateApi
) : TransactionRemoteDataSource {

    override suspend fun getTransactions(filter: TransactionFilterDto?): List<TransactionDto> =
        api.getTransactions(
            from = filter?.from,
            to = filter?.to,
            categoryId = filter?.categoryId,
            type = filter?.type
        )

    override suspend fun getTransaction(id: String): TransactionDto =
        api.getTransaction(id)

    override suspend fun createTransaction(request: CreateTransactionRequest): TransactionDto =
        api.createTransaction(request)

    override suspend fun updateTransaction(
        id: String,
        request: UpdateTransactionRequest
    ): TransactionDto = api.updateTransaction(id, request)

    override suspend fun deleteTransaction(id: String) {
        val response = api.deleteTransaction(id)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }
}

