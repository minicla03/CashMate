package minicla03.cashmate.data.repository

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.data.remote.datasource.AccountRemoteDataSource
import minicla03.cashmate.data.remote.mapper.toDomain
import minicla03.cashmate.domain.model.Account
import minicla03.cashmate.domain.repository.AccountRepository
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val remoteDataSource: AccountRemoteDataSource,
    private val safeRemoteCall: SafeRemoteCall
) : AccountRepository {

    override suspend fun getAccounts(): AppResult<List<Account>> =
        safeRemoteCall {
            remoteDataSource.getAccounts().map { it.toDomain() }
        }
}

