package minicla03.cashmate.data.remote.datasource

import minicla03.cashmate.data.remote.api.CashMateApi
import minicla03.cashmate.data.remote.dto.AccountDto
import javax.inject.Inject

class RetrofitAccountRemoteDataSource @Inject constructor(
    private val api: CashMateApi
) : AccountRemoteDataSource {

    override suspend fun getAccounts(): List<AccountDto> = api.getAccounts()
}

