package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.dto.CreateTransactionRequest
import minicla03.cashmate.data.remote.dto.TransactionDto
import minicla03.cashmate.data.remote.dto.TransactionFilterDto
import minicla03.cashmate.data.remote.dto.UpdateTransactionRequest

interface TransactionRemoteDataSource {

    suspend fun getTransactions(
        filter: TransactionFilterDto? = null
    ): List<TransactionDto>

    suspend fun getTransaction(
        id: String
    ): TransactionDto

    suspend fun createTransaction(
        request: CreateTransactionRequest
    ): TransactionDto

    suspend fun updateTransaction(
        id: String,
        request: UpdateTransactionRequest
    ): TransactionDto

    suspend fun deleteTransaction(
        id: String
    )
}

