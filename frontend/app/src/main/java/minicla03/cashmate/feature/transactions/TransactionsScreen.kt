package minicla03.cashmate.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.core.designsystem.EmptyStateView
import minicla03.cashmate.core.designsystem.ErrorStateView
import minicla03.cashmate.core.designsystem.LoadingStateView
import minicla03.cashmate.core.ui.DateFormatter
import minicla03.cashmate.core.ui.TransactionRow
import minicla03.cashmate.domain.model.TransactionSortOrder
import minicla03.cashmate.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun TransactionsRoute(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var skipInitialResume by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (skipInitialResume) {
            skipInitialResume = false
        } else {
            viewModel.onEvent(TransactionsEvent.Refresh)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TransactionsEffect.NavigateToAddTransaction -> onNavigateToAddTransaction()
                is TransactionsEffect.NavigateToTransactionDetail ->
                    onNavigateToTransactionDetail(effect.id)
            }
        }
    }

    TransactionsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onEvent: (TransactionsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    var showDateRangePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TransactionsFilterHeader(
            state = state,
            onEvent = onEvent,
            onOpenCustomDatePicker = { showDateRangePicker = true }
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> LoadingStateView()
                state.error != null -> ErrorStateView(
                    message = stringResource(state.error.messageResId),
                    onRetry = if (state.error.canRetry) {
                        { onEvent(TransactionsEvent.Refresh) }
                    } else {
                        null
                    }
                )

                state.isEmpty -> EmptyStateView(
                    title = stringResource(R.string.transactions_empty_title),
                    subtitle = stringResource(R.string.transactions_empty_subtitle),
                    actionLabel = if (state.hasActiveFilters) {
                        stringResource(R.string.transactions_clear_filters)
                    } else {
                        stringResource(R.string.dashboard_add_first)
                    },
                    onAction = {
                        if (state.hasActiveFilters) {
                            onEvent(TransactionsEvent.ClearFilters)
                        } else {
                            onEvent(TransactionsEvent.AddTransactionClick)
                        }
                    }
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = spacing.md,
                        end = spacing.md,
                        top = spacing.xs,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    items(
                        items = state.transactions,
                        key = { it.id }
                    ) { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            category = state.categoriesById[transaction.categoryId],
                            currency = state.currency,
                            onClick = { onEvent(TransactionsEvent.TransactionClick(transaction.id)) }
                        )
                    }
                }
            }
        }
    }

    if (showDateRangePicker) {
        CustomDateRangeDialog(
            initialFrom = state.customFromDate,
            initialTo = state.customToDate,
            onDismiss = { showDateRangePicker = false },
            onConfirm = { from, to ->
                showDateRangePicker = false
                onEvent(TransactionsEvent.CustomDateRangeSelected(from, to))
            }
        )
    }
}

@Composable
private fun TransactionsFilterHeader(
    state: TransactionsUiState,
    onEvent: (TransactionsEvent) -> Unit,
    onOpenCustomDatePicker: () -> Unit
) {
    val spacing = CashMateTheme.spacing
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var dateMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onEvent(TransactionsEvent.SearchQueryChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = stringResource(R.string.transactions_search_hint)) },
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onEvent(TransactionsEvent.SearchQueryChanged("")) }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_clear_search)
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.small
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item(key = "type_all") {
                FilterChip(
                    selected = state.selectedType == null,
                    onClick = { onEvent(TransactionsEvent.TypeFilterChanged(null)) },
                    label = { Text(stringResource(R.string.transaction_type_all)) }
                )
            }
            item(key = "type_expense") {
                FilterChip(
                    selected = state.selectedType == TransactionType.EXPENSE,
                    onClick = {
                        val next = if (state.selectedType == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                        onEvent(TransactionsEvent.TypeFilterChanged(next))
                    },
                    label = { Text(stringResource(R.string.transaction_type_expense)) }
                )
            }
            item(key = "type_income") {
                FilterChip(
                    selected = state.selectedType == TransactionType.INCOME,
                    onClick = {
                        val next = if (state.selectedType == TransactionType.INCOME) null else TransactionType.INCOME
                        onEvent(TransactionsEvent.TypeFilterChanged(next))
                    },
                    label = { Text(stringResource(R.string.transaction_type_income)) }
                )
            }

            item(key = "category_dropdown") {
                Box {
                    val selectedCategoryName = state.selectedCategoryId
                        ?.let { state.categoriesById[it]?.name }
                        ?: stringResource(R.string.transactions_filter_category_all)

                    FilterChip(
                        selected = state.selectedCategoryId != null,
                        onClick = { categoryMenuExpanded = true },
                        label = { Text(selectedCategoryName) },
                        trailingIcon = {
                            Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                    )
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.transactions_filter_category_all)) },
                            onClick = {
                                categoryMenuExpanded = false
                                onEvent(TransactionsEvent.CategoryFilterChanged(null))
                            }
                        )
                        state.categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    categoryMenuExpanded = false
                                    onEvent(TransactionsEvent.CategoryFilterChanged(category.id))
                                }
                            )
                        }
                    }
                }
            }

            item(key = "date_dropdown") {
                Box {
                    val dateLabel = when (state.datePreset) {
                        DateRangePreset.ALL -> stringResource(R.string.transactions_filter_date_all)
                        DateRangePreset.THIS_MONTH -> stringResource(R.string.transactions_filter_date_this_month)
                        DateRangePreset.LAST_MONTH -> stringResource(R.string.transactions_filter_date_last_month)
                        DateRangePreset.LAST_30_DAYS -> stringResource(R.string.transactions_filter_date_last_30_days)
                        DateRangePreset.CUSTOM -> {
                            val from = state.customFromDate?.let { DateFormatter.formatShort(it) } ?: "…"
                            val to = state.customToDate?.let { DateFormatter.formatShort(it) } ?: "…"
                            "$from – $to"
                        }
                    }

                    FilterChip(
                        selected = state.datePreset != DateRangePreset.ALL,
                        onClick = { dateMenuExpanded = true },
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.DateRange, contentDescription = null)
                        },
                        label = { Text(dateLabel) }
                    )
                    DropdownMenu(
                        expanded = dateMenuExpanded,
                        onDismissRequest = { dateMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.transactions_filter_date_all)) },
                            onClick = {
                                dateMenuExpanded = false
                                onEvent(TransactionsEvent.DatePresetSelected(DateRangePreset.ALL))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.transactions_filter_date_this_month)) },
                            onClick = {
                                dateMenuExpanded = false
                                onEvent(TransactionsEvent.DatePresetSelected(DateRangePreset.THIS_MONTH))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.transactions_filter_date_last_month)) },
                            onClick = {
                                dateMenuExpanded = false
                                onEvent(TransactionsEvent.DatePresetSelected(DateRangePreset.LAST_MONTH))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.transactions_filter_date_last_30_days)) },
                            onClick = {
                                dateMenuExpanded = false
                                onEvent(TransactionsEvent.DatePresetSelected(DateRangePreset.LAST_30_DAYS))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.transactions_filter_date_custom)) },
                            onClick = {
                                dateMenuExpanded = false
                                onOpenCustomDatePicker()
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.transactions_count_label,
                    state.transactions.size,
                    state.transactions.size
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.hasActiveFilters) {
                    TextButton(onClick = { onEvent(TransactionsEvent.ClearFilters) }) {
                        Text(text = stringResource(R.string.transactions_clear_filters))
                    }
                }
                TextButton(onClick = { onEvent(TransactionsEvent.ToggleSortOrder) }) {
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = stringResource(R.string.cd_sort_order),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = when (state.sortOrder) {
                            TransactionSortOrder.DATE_DESC -> stringResource(R.string.transactions_sort_newest)
                            TransactionSortOrder.DATE_ASC -> stringResource(R.string.transactions_sort_oldest)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangeDialog(
    initialFrom: LocalDate?,
    initialTo: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate?, LocalDate?) -> Unit
) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialFrom?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        initialSelectedEndDateMillis = initialTo?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val from = pickerState.selectedStartDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    val to = pickerState.selectedEndDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    onConfirm(from, to)
                }
            ) {
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    ) {
        DateRangePicker(
            state = pickerState,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

