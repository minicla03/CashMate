package minicla03.cashmate.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMateTextField
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.core.designsystem.EmptyStateView
import minicla03.cashmate.core.designsystem.ErrorStateView
import minicla03.cashmate.core.designsystem.LoadingStateView
import minicla03.cashmate.core.ui.CategoryIconBadge
import minicla03.cashmate.core.ui.CategoryVisuals
import minicla03.cashmate.domain.model.Category

@Composable
fun CategoriesRoute(
    onNavigateBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.actionError, state.snackbarMessageResId) {
        val messageRes = state.actionError?.messageResId ?: state.snackbarMessageResId ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(messageRes))
        viewModel.onEvent(CategoriesEvent.DismissSnackbar)
    }

    CategoriesScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onEvent: (CategoriesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(CategoriesEvent.OpenCreateDialog) }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.cd_add_category)
                )
            }
        }
    ) { padding ->
        val spacing = CashMateTheme.spacing
        when {
            state.isLoading -> LoadingStateView(modifier = Modifier.padding(padding))
            state.error != null && state.categories.isEmpty() -> ErrorStateView(
                message = stringResource(state.error.messageResId),
                onRetry = if (state.error.canRetry) {
                    { onEvent(CategoriesEvent.Refresh) }
                } else {
                    null
                },
                modifier = Modifier.padding(padding)
            )

            state.isEmpty -> EmptyStateView(
                title = stringResource(R.string.categories_empty_title),
                subtitle = stringResource(R.string.categories_empty_subtitle),
                actionLabel = stringResource(R.string.cd_add_category),
                onAction = { onEvent(CategoriesEvent.OpenCreateDialog) },
                modifier = Modifier.padding(padding)
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = spacing.md,
                    end = spacing.md,
                    top = spacing.sm,
                    bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                items(
                    items = state.categories,
                    key = { it.id }
                ) { category ->
                    CategoryRow(
                        category = category,
                        onEdit = { onEvent(CategoriesEvent.OpenEditDialog(category)) },
                        onDelete = { onEvent(CategoriesEvent.RequestDelete(category)) }
                    )
                }
            }
        }
    }

    state.editor?.let { editor ->
        CategoryEditorDialog(
            editor = editor,
            onDismiss = { onEvent(CategoriesEvent.DismissEditorDialog) },
            onNameChange = { onEvent(CategoriesEvent.EditorNameChanged(it)) },
            onIconSelected = { onEvent(CategoriesEvent.EditorIconSelected(it)) },
            onColorSelected = { onEvent(CategoriesEvent.EditorColorSelected(it)) },
            onConfirm = { onEvent(CategoriesEvent.SubmitEditor) }
        )
    }

    state.pendingDeleteCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { onEvent(CategoriesEvent.DismissDeleteDialog) },
            title = { Text(stringResource(R.string.categories_delete_title)) },
            text = { Text(stringResource(R.string.categories_delete_message, category.name)) },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(CategoriesEvent.ConfirmDelete) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(CategoriesEvent.DismissDeleteDialog) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    CashMateCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.sm)
        ) {
            CategoryIconBadge(
                iconKey = category.icon,
                colorHex = category.color,
                size = 42.dp
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.cd_edit_category, category.name)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.cd_delete_category, category.name),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    editor: CategoryEditorState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onIconSelected: (String) -> Unit,
    onColorSelected: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editor.isEditMode) R.string.categories_edit_title else R.string.categories_add_title
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.md)) {
                CashMateTextField(
                    value = editor.name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.categories_name_label),
                    errorMessage = editor.nameErrorResId?.let { stringResource(it) }
                )

                Column(verticalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.xs)) {
                    Text(
                        text = stringResource(R.string.categories_icon_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.xs)
                    ) {
                        CategoryVisuals.availableIcons.forEach { option ->
                            val isSelected = editor.icon == option.key
                            val activeColor = CategoryVisuals.parseColor(editor.color)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) activeColor.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) activeColor else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .semantics { selected = isSelected }
                                    .clickable(role = Role.RadioButton) {
                                        onIconSelected(option.key)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = stringResource(option.labelResId),
                                    tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.xs)) {
                    Text(
                        text = stringResource(R.string.categories_color_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.xs)
                    ) {
                        CategoryVisuals.availableColors.forEach { colorOption ->
                            val isSelected = editor.color.equals(colorOption.hex, ignoreCase = true)
                            val color = CategoryVisuals.parseColor(colorOption.hex)
                            val colorLabel = stringResource(colorOption.labelResId)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .semantics {
                                        selected = isSelected
                                        contentDescription = colorLabel
                                    }
                                    .clickable(role = Role.RadioButton) {
                                        onColorSelected(colorOption.hex)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !editor.isSubmitting
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

