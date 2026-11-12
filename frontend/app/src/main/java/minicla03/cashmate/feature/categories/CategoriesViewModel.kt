package minicla03.cashmate.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import minicla03.cashmate.R
import minicla03.cashmate.core.common.AppResult
import minicla03.cashmate.core.error.AppError
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.ui.CategoryVisuals
import minicla03.cashmate.core.ui.toFieldMessageResIds
import minicla03.cashmate.core.ui.toUiError
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.usecase.CategoryValidator
import minicla03.cashmate.domain.usecase.CreateCategoryUseCase
import minicla03.cashmate.domain.usecase.DeleteCategoryUseCase
import minicla03.cashmate.domain.usecase.GetCategoriesUseCase
import minicla03.cashmate.domain.usecase.UpdateCategoryUseCase
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val categoryValidator: CategoryValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun onEvent(event: CategoriesEvent) {
        when (event) {
            CategoriesEvent.Refresh -> loadCategories()

            CategoriesEvent.OpenCreateDialog -> _uiState.update {
                it.copy(
                    editor = CategoryEditorState(
                        icon = CategoryVisuals.availableIcons.first().key,
                        color = CategoryVisuals.availableColors.first().hex
                    )
                )
            }

            is CategoriesEvent.OpenEditDialog -> _uiState.update {
                it.copy(
                    editor = CategoryEditorState(
                        editingCategoryId = event.category.id,
                        name = event.category.name,
                        icon = event.category.icon,
                        color = event.category.color
                    )
                )
            }

            CategoriesEvent.DismissEditorDialog -> _uiState.update {
                it.copy(editor = null)
            }

            is CategoriesEvent.EditorNameChanged -> _uiState.update { state ->
                state.copy(
                    editor = state.editor?.copy(name = event.name, nameErrorResId = null)
                )
            }

            is CategoriesEvent.EditorIconSelected -> _uiState.update { state ->
                state.copy(editor = state.editor?.copy(icon = event.icon))
            }

            is CategoriesEvent.EditorColorSelected -> _uiState.update { state ->
                state.copy(editor = state.editor?.copy(color = event.color))
            }

            CategoriesEvent.SubmitEditor -> submitEditor()

            is CategoriesEvent.RequestDelete -> _uiState.update {
                it.copy(pendingDeleteCategory = event.category)
            }

            CategoriesEvent.DismissDeleteDialog -> _uiState.update {
                it.copy(pendingDeleteCategory = null)
            }

            CategoriesEvent.ConfirmDelete -> confirmDelete()

            CategoriesEvent.DismissSnackbar -> _uiState.update {
                it.copy(actionError = null, snackbarMessageResId = null)
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.categories.isEmpty(), error = null) }
            when (val result = getCategoriesUseCase()) {
                is AppResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        categories = result.data,
                        error = null
                    )
                }

                is AppResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.error.toUiError()
                    )
                }
            }
        }
    }

    private fun submitEditor() {
        val editor = _uiState.value.editor ?: return
        if (editor.isSubmitting) return

        val candidate = Category(
            id = editor.editingCategoryId.orEmpty(),
            name = editor.name,
            icon = editor.icon,
            color = editor.color
        )

        val validationError = categoryValidator.validate(candidate)
        if (validationError != null) {
            val nameErr = validationError.toFieldMessageResIds()[ValidationField.CATEGORY_NAME]
            _uiState.update { it.copy(editor = editor.copy(nameErrorResId = nameErr)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(editor = editor.copy(isSubmitting = true)) }
            val result = if (editor.isEditMode) {
                updateCategoryUseCase(candidate)
            } else {
                createCategoryUseCase(candidate)
            }

            when (result) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            editor = null,
                            snackbarMessageResId = R.string.category_saved_message
                        )
                    }
                    loadCategories()
                }

                is AppResult.Error -> when (val err = result.error) {
                    is AppError.Validation -> {
                        val nameErr = err.toFieldMessageResIds()[ValidationField.CATEGORY_NAME]
                        _uiState.update {
                            it.copy(editor = editor.copy(isSubmitting = false, nameErrorResId = nameErr))
                        }
                    }

                    else -> _uiState.update {
                        it.copy(
                            editor = editor.copy(isSubmitting = false),
                            actionError = err.toUiError()
                        )
                    }
                }
            }
        }
    }

    private fun confirmDelete() {
        val target = _uiState.value.pendingDeleteCategory ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(pendingDeleteCategory = null, isDeleting = true) }
            when (val result = deleteCategoryUseCase(target.id)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            snackbarMessageResId = R.string.category_deleted_message
                        )
                    }
                    loadCategories()
                }

                is AppResult.Error -> _uiState.update {
                    it.copy(
                        isDeleting = false,
                        actionError = result.error.toUiError()
                    )
                }
            }
        }
    }
}

