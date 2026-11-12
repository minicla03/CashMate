package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.Account
import minicla03.cashmate.domain.repository.AccountRepository
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(): AppResult<List<Account>> =
        accountRepository.getAccounts()
}

