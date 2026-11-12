package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.dto.AccountDto
import javax.inject.Inject

class FakeAccountRemoteDataSource @Inject constructor(
    private val store: FakeRemoteStore
) : AccountRemoteDataSource {

    override suspend fun getAccounts(): List<AccountDto> =
        store.getAccounts()
}

