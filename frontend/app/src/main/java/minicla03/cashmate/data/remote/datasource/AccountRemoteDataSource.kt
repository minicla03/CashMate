package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.dto.AccountDto

interface AccountRemoteDataSource {

    suspend fun getAccounts(): List<AccountDto>
}

