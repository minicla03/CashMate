package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.dto.CreateTransactionRequest
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.data.remote.dto.TransactionFilterDto
import minicla03.cashmate.data.remote.dto.UpdateTransactionRequest
import javax.inject.Inject

class FakeTransactionRemoteDataSource @Inject constructor(
    private val store: FakeRemoteStore
) : TransactionRemoteDataSource {

    override suspend fun getTransactions(filter: TransactionFilterDto?): List<TransactionDto> =
        store.getTransactions(filter)

    override suspend fun getTransaction(id: String): TransactionDto =
        store.getTransaction(id)

    override suspend fun createTransaction(request: CreateTransactionRequest): TransactionDto =
        store.createTransaction(request)

    override suspend fun updateTransaction(
        id: String,
        request: UpdateTransactionRequest
    ): TransactionDto = store.updateTransaction(id, request)

    override suspend fun deleteTransaction(id: String) =
        store.deleteTransaction(id)
}

