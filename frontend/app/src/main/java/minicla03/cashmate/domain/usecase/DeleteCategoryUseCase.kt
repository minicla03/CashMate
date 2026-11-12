package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.repository.CategoryRepository
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(id: String): AppResult<Unit> =
        categoryRepository.deleteCategory(id)
}

