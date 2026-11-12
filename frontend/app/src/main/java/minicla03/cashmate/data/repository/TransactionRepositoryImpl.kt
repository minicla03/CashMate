package minicla03.cashmate.data.repository

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.data.remote.datasource.TransactionRemoteDataSource
import minicla03.cashmate.data.remote.mapper.toCreateRequest
import minicla03.cashmate.data.remote.mapper.toDomain
import minicla03.cashmate.data.remote.mapper.toDto
import minicla03.cashmate.data.remote.mapper.toUpdateRequest
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionFilter
import minicla03.cashmate.domain.repository.TransactionRepository
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val remoteDataSource: TransactionRemoteDataSource,
    private val safeRemoteCall: SafeRemoteCall
) : TransactionRepository {

    override suspend fun getTransactions(filter: TransactionFilter?): AppResult<List<Transaction>> =
        safeRemoteCall {
            remoteDataSource.getTransactions(filter?.toDto()).map { it.toDomain() }
        }

    override suspend fun getTransaction(id: String): AppResult<Transaction> =
        safeRemoteCall {
            remoteDataSource.getTransaction(id).toDomain()
        }

    override suspend fun createTransaction(transaction: Transaction): AppResult<Transaction> =
        safeRemoteCall {
            remoteDataSource.createTransaction(transaction.toCreateRequest()).toDomain()
        }

    override suspend fun updateTransaction(transaction: Transaction): AppResult<Transaction> =
        safeRemoteCall {
            remoteDataSource.updateTransaction(
                id = transaction.id,
                request = transaction.toUpdateRequest()
            ).toDomain()
        }

    override suspend fun updateTransactionWithReceipt(
        transaction: Transaction,
        receipt: Receipt
    ): AppResult<Transaction> =
        safeRemoteCall {
            remoteDataSource.updateTransaction(
                id = transaction.id,
                request = transaction.toUpdateRequest(receipt)
            ).toDomain()
        }

    override suspend fun deleteTransaction(id: String): AppResult<Unit> =
        safeRemoteCall {
            remoteDataSource.deleteTransaction(id)
        }
}

