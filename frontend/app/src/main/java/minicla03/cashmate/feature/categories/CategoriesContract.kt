package minicla03.cashmate.feature.categories

import androidx.annotation.StringRes
import minicla03.cashmate.core.ui.UiError
import minicla03.cashmate.domain.model.Category

data class CategoryEditorState(
    val editingCategoryId: String? = null,
    val name: String = "",
    val icon: String = "category",
    val color: String = "#5E35B1",
    @StringRes val nameErrorResId: Int? = null,
    val isSubmitting: Boolean = false
) {
    val isEditMode: Boolean get() = editingCategoryId != null
}

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val editor: CategoryEditorState? = null,
    val pendingDeleteCategory: Category? = null,
    val isDeleting: Boolean = false,
    val error: UiError? = null,
    val actionError: UiError? = null,
    @StringRes val snackbarMessageResId: Int? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && error == null && categories.isEmpty()
}

sealed interface CategoriesEvent {
    data object Refresh : CategoriesEvent
    data object OpenCreateDialog : CategoriesEvent
    data class OpenEditDialog(val category: Category) : CategoriesEvent
    data object DismissEditorDialog : CategoriesEvent
    data class EditorNameChanged(val name: String) : CategoriesEvent
    data class EditorIconSelected(val icon: String) : CategoriesEvent
    data class EditorColorSelected(val color: String) : CategoriesEvent
    data object SubmitEditor : CategoriesEvent
    data class RequestDelete(val category: Category) : CategoriesEvent
    data object DismissDeleteDialog : CategoriesEvent
    data object ConfirmDelete : CategoriesEvent
    data object DismissSnackbar : CategoriesEvent
}

