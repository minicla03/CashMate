package minicla03.cashmate.domain.repository

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionFilter

interface TransactionRepository {

    suspend fun getTransactions(
        filter: TransactionFilter? = null
    ): AppResult<List<Transaction>>

    suspend fun getTransaction(
        id: String
    ): AppResult<Transaction>

    suspend fun createTransaction(
        transaction: Transaction
    ): AppResult<Transaction>

    suspend fun updateTransaction(
        transaction: Transaction
    ): AppResult<Transaction>

    suspend fun updateTransactionWithReceipt(
        transaction: Transaction,
        receipt: Receipt
    ): AppResult<Transaction>

    suspend fun deleteTransaction(
        id: String
    ): AppResult<Unit>
}

