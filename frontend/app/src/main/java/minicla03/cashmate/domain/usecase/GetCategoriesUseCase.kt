package minicla03.cashmate.domain.usecase

import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.repository.CategoryRepository
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(): AppResult<List<Category>> =
        categoryRepository.getCategories()
}

