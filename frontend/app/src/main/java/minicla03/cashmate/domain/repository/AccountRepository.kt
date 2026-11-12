package minicla03.cashmate.domain.repository

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.Account

interface AccountRepository {

    suspend fun getAccounts(): AppResult<List<Account>>
}

