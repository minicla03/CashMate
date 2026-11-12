package minicla03.cashmate.feature.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.common.MoneyParser
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMatePrimaryButton
import minicla03.cashmate.core.designsystem.CashMateTextField
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.core.designsystem.ErrorStateView
import minicla03.cashmate.core.designsystem.LoadingStateView
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.ui.CategoryIconBadge
import minicla03.cashmate.core.ui.DateFormatter
import minicla03.cashmate.core.ui.MoneyFormatter
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.Receipt
import minicla03.cashmate.domain.model.ReceiptAdjustment
import minicla03.cashmate.domain.model.ReceiptAdjustmentType
import minicla03.cashmate.domain.model.ReceiptItem
import minicla03.cashmate.domain.model.TransactionType
import minicla03.cashmate.domain.usecase.ReceiptCalculator
import minicla03.cashmate.feature.receipt.presentation.ReceiptCategoryResolver
import minicla03.cashmate.feature.receipt.presentation.components.ReceiptSavingsCard
import minicla03.cashmate.feature.receipt.presentation.components.ReceiptSummaryRow
import java.time.Instant
import java.time.ZoneOffset
import java.util.Currency

@Composable
fun TransactionFormRoute(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TransactionFormViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.submitError) {
        val err = state.submitError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(err.messageResId))
        viewModel.onEvent(TransactionFormEvent.DismissSubmitError)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TransactionFormEffect.NavigateBackSaved -> onSaved()
            }
        }
    }

    TransactionFormScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    state: TransactionFormUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onEvent: (TransactionFormEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val titleRes = if (state.isEditMode) {
        R.string.transaction_form_edit_title
    } else {
        R.string.transaction_form_add_title
    }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingStateView(modifier = Modifier.padding(padding))
            state.loadError != null -> ErrorStateView(
                message = stringResource(state.loadError.messageResId),
                onRetry = if (state.loadError.canRetry) {
                    { onEvent(TransactionFormEvent.RetryLoad) }
                } else {
                    null
                },
                modifier = Modifier.padding(padding)
            )

            else -> {
                val spacing = CashMateTheme.spacing
                var categoryDropdownExpanded by remember { mutableStateOf(false) }
                var accountDropdownExpanded by remember { mutableStateOf(false) }

                val selectedCategory = state.categories.firstOrNull { it.id == state.selectedCategoryId }
                val selectedAccount = state.accounts.firstOrNull { it.id == state.selectedAccountId }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = stringResource(R.string.transaction_form_type_label),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            FilterChip(
                                selected = state.type == TransactionType.EXPENSE,
                                onClick = { onEvent(TransactionFormEvent.TypeChanged(TransactionType.EXPENSE)) },
                                label = { Text(stringResource(R.string.transaction_type_expense)) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Rounded.ArrowUpward, contentDescription = null)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = state.type == TransactionType.INCOME,
                                onClick = { onEvent(TransactionFormEvent.TypeChanged(TransactionType.INCOME)) },
                                label = { Text(stringResource(R.string.transaction_type_income)) },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Rounded.ArrowDownward, contentDescription = null)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    val isDerivedFromReceiptItems = state.receipt != null && state.receipt.items.isNotEmpty()
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                        CashMateTextField(
                            value = state.amountInput,
                            onValueChange = { onEvent(TransactionFormEvent.AmountChanged(it)) },
                            label = stringResource(R.string.transaction_form_amount_label),
                            placeholder = stringResource(R.string.transaction_form_amount_hint),
                            errorMessage = state.fieldErrors[ValidationField.AMOUNT]?.let { stringResource(it) },
                            readOnly = isDerivedFromReceiptItems,
                            leadingIcon = {
                                Text(
                                    text = state.currency.symbol,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )
                        if (isDerivedFromReceiptItems && state.fieldErrors[ValidationField.AMOUNT] == null) {
                            Text(
                                text = stringResource(R.string.transaction_form_amount_derived_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = spacing.xs)
                            )
                        }
                    }

                    CashMateTextField(
                        value = state.description,
                        onValueChange = { onEvent(TransactionFormEvent.DescriptionChanged(it)) },
                        label = stringResource(R.string.transaction_form_description_label),
                        placeholder = stringResource(R.string.transaction_form_description_hint),
                        errorMessage = state.fieldErrors[ValidationField.DESCRIPTION]?.let { stringResource(it) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        )
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val categoryError = state.fieldErrors[ValidationField.CATEGORY]?.let { stringResource(it) }
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.transaction_form_category_label)) },
                            placeholder = { Text(stringResource(R.string.transaction_form_category_placeholder)) },
                            leadingIcon = selectedCategory?.let { cat ->
                                {
                                    CategoryIconBadge(
                                        iconKey = cat.icon,
                                        colorHex = cat.color
                                    )
                                }
                            },
                            trailingIcon = {
                                Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
                            },
                            isError = categoryError != null,
                            supportingText = categoryError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { categoryDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            state.categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    leadingIcon = {
                                        CategoryIconBadge(
                                            iconKey = category.icon,
                                            colorHex = category.color
                                        )
                                    },
                                    onClick = {
                                        categoryDropdownExpanded = false
                                        onEvent(TransactionFormEvent.CategorySelected(category.id))
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val dateError = state.fieldErrors[ValidationField.DATE]?.let { stringResource(it) }
                        OutlinedTextField(
                            value = DateFormatter.formatLong(state.date),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.transaction_form_date_label)) },
                            trailingIcon = {
                                Icon(imageVector = Icons.Rounded.CalendarToday, contentDescription = null)
                            },
                            isError = dateError != null,
                            supportingText = dateError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val accountError = state.fieldErrors[ValidationField.ACCOUNT]?.let { stringResource(it) }
                        OutlinedTextField(
                            value = selectedAccount?.let { "${it.name} (${it.currency.currencyCode})" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.transaction_form_account_label)) },
                            trailingIcon = {
                                Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
                            },
                            isError = accountError != null,
                            supportingText = accountError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable(enabled = state.accounts.size > 1) {
                                    accountDropdownExpanded = true
                                }
                        )
                        DropdownMenu(
                            expanded = accountDropdownExpanded,
                            onDismissRequest = { accountDropdownExpanded = false }
                        ) {
                            state.accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text("${account.name} (${account.currency.currencyCode})") },
                                    onClick = {
                                        accountDropdownExpanded = false
                                        onEvent(TransactionFormEvent.AccountSelected(account.id))
                                    }
                                )
                            }
                        }
                    }

                    if (state.receipt != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = spacing.xs))
                        ReceiptProductsEditSection(
                            receipt = state.receipt,
                            categories = state.categories,
                            currency = state.currency,
                            enabled = !state.isSubmitting,
                            onEditItem = { onEvent(TransactionFormEvent.OpenEditReceiptItem(it)) },
                            onRemoveItem = { onEvent(TransactionFormEvent.RequestRemoveReceiptItem(it)) },
                            onAddItem = { onEvent(TransactionFormEvent.OpenAddReceiptItem) }
                        )
                    } else if (state.receiptUnavailable) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = spacing.xs))
                        CashMateCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.transaction_detail_receipt_unavailable),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.xs))

                    CashMatePrimaryButton(
                        text = stringResource(
                            if (state.isEditMode) {
                                R.string.transaction_form_update
                            } else {
                                R.string.transaction_form_save
                            }
                        ),
                        onClick = { onEvent(TransactionFormEvent.Submit) },
                        isLoading = state.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onEvent(TransactionFormEvent.DateSelected(picked))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.action_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    state.itemEditor?.let { editor ->
        ReceiptItemEditorDialog(
            editor = editor,
            categories = state.categories,
            currency = state.currency,
            onDismiss = { onEvent(TransactionFormEvent.DismissItemEditor) },
            onDescriptionChange = { onEvent(TransactionFormEvent.ItemEditorDescriptionChanged(it)) },
            onQuantityChange = { onEvent(TransactionFormEvent.ItemEditorQuantityChanged(it)) },
            onUnitPriceChange = { onEvent(TransactionFormEvent.ItemEditorUnitPriceChanged(it)) },
            onDiscountChange = { onEvent(TransactionFormEvent.ItemEditorDiscountChanged(it)) },
            onCategorySelect = { onEvent(TransactionFormEvent.ItemEditorCategorySelected(it)) },
            onConfirm = { onEvent(TransactionFormEvent.SubmitItemEditor) }
        )
    }

    state.pendingRemoveItem?.let { pendingItem ->
        AlertDialog(
            onDismissRequest = { onEvent(TransactionFormEvent.DismissRemoveReceiptItem) },
            title = { Text(stringResource(R.string.transaction_form_remove_item_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.transaction_form_remove_item_message,
                        pendingItem.description
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(TransactionFormEvent.ConfirmRemoveReceiptItem) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.action_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(TransactionFormEvent.DismissRemoveReceiptItem) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ReceiptProductsEditSection(
    receipt: Receipt,
    categories: List<Category>,
    currency: Currency,
    enabled: Boolean,
    onEditItem: (ReceiptItem) -> Unit,
    onRemoveItem: (ReceiptItem) -> Unit,
    onAddItem: () -> Unit
) {
    val spacing = CashMateTheme.spacing
    val semantic = CashMateTheme.semanticColors

    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = stringResource(R.string.transaction_form_receipt_products_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )

        if (receipt.items.isEmpty()) {
            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.receipt_review_empty_items),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            receipt.items.forEach { item ->
                EditableReceiptItemCard(
                    item = item,
                    categories = categories,
                    currency = currency,
                    enabled = enabled,
                    onEdit = { onEditItem(item) },
                    onRemove = { onRemoveItem(item) }
                )
            }
        }

        OutlinedButton(
            onClick = onAddItem,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.minTouchTarget),
            shape = MaterialTheme.shapes.small
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(text = stringResource(R.string.receipt_review_add_item))
        }

        if (receipt.adjustments.isNotEmpty()) {
            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = stringResource(R.string.receipt_review_adjustments_section),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    receipt.adjustments.forEachIndexed { index, adjustment ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = spacing.xxs))
                        }
                        ReceiptFormAdjustmentRow(
                            adjustment = adjustment,
                            currency = currency
                        )
                    }
                }
            }
        }

        CashMateCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = stringResource(R.string.receipt_review_totals_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ReceiptSummaryRow(
                    label = stringResource(R.string.receipt_review_subtotal_label),
                    value = MoneyFormatter.format(receipt.subtotal, currency)
                )
                if (receipt.totalDiscount.isPositive) {
                    ReceiptSummaryRow(
                        label = stringResource(R.string.receipt_review_discounts_summary),
                        value = "-${MoneyFormatter.format(receipt.totalDiscount, currency)}",
                        valueColor = semantic.income
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = spacing.xxs))
                ReceiptSummaryRow(
                    label = stringResource(R.string.receipt_review_total_paid_label),
                    value = MoneyFormatter.format(receipt.total, currency),
                    valueColor = MaterialTheme.colorScheme.primary,
                    bold = true
                )
            }
        }

        if (receipt.totalDiscount.isPositive) {
            ReceiptSavingsCard(
                subtotal = receipt.subtotal,
                savings = receipt.totalDiscount,
                totalPaid = receipt.total,
                currency = currency
            )
        }
    }
}

@Composable
private fun EditableReceiptItemCard(
    item: ReceiptItem,
    categories: List<Category>,
    currency: Currency,
    enabled: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val spacing = CashMateTheme.spacing
    val semantic = CashMateTheme.semanticColors
    val resolvedCategory = ReceiptCategoryResolver.resolve(item.category, categories)
    val categoryLabel = resolvedCategory?.name ?: item.category.takeIf { it.isNotBlank() }
    val hasDiscount = item.discountAmount.isPositive || item.grossAmount > item.netAmount
    val qtyPrice = "${item.quantity} × ${MoneyFormatter.format(item.unitPrice, currency)}"
    val subtitle = if (categoryLabel != null) "$qtyPrice • $categoryLabel" else qtyPrice

    CashMateCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs)
            ) {
                CategoryIconBadge(
                    iconKey = resolvedCategory?.icon ?: "category",
                    colorHex = resolvedCategory?.color ?: "#546E7A",
                    size = 36.dp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.discountAmount.isPositive) {
                        Text(
                            text = "${stringResource(R.string.receipt_review_item_discount)}: -${
                                MoneyFormatter.format(item.discountAmount, currency)
                            }",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = semantic.income
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (hasDiscount) {
                        Text(
                            text = MoneyFormatter.format(item.grossAmount, currency),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    Text(
                        text = MoneyFormatter.format(item.netAmount, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEdit,
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.action_edit))
                }
                TextButton(
                    onClick = onRemove,
                    enabled = enabled,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.action_remove))
                }
            }
        }
    }
}

@Composable
private fun ReceiptFormAdjustmentRow(
    adjustment: ReceiptAdjustment,
    currency: Currency
) {
    val semantic = CashMateTheme.semanticColors
    val formatted = MoneyFormatter.format(adjustment.amount, currency)
    val signed = when (adjustment.type) {
        ReceiptAdjustmentType.DISCOUNT -> "-$formatted"
        ReceiptAdjustmentType.SURCHARGE,
        ReceiptAdjustmentType.TAX -> "+$formatted"
        ReceiptAdjustmentType.OTHER -> formatted
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = adjustment.description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = signed,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (adjustment.type == ReceiptAdjustmentType.DISCOUNT) {
                semantic.income
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun ReceiptItemEditorDialog(
    editor: ReceiptItemEditorState,
    categories: List<Category>,
    currency: Currency,
    onDismiss: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitPriceChange: (String) -> Unit,
    onDiscountChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onConfirm: () -> Unit
) {
    val spacing = CashMateTheme.spacing
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    val resolvedCategory = ReceiptCategoryResolver.resolve(editor.category, categories)

    val previewNet = run {
        val qty = editor.quantityInput.trim().toIntOrNull()
        val unitPrice = MoneyParser.parse(editor.unitPriceInput)
        val discount = if (editor.discountInput.isBlank()) Money.ZERO else MoneyParser.parse(editor.discountInput)
        if (qty != null && qty > 0 && unitPrice != null && discount != null) {
            ReceiptCalculator.calculateItem(
                id = editor.itemId.orEmpty(),
                description = editor.description,
                quantity = qty,
                unitPrice = unitPrice,
                discountAmount = discount,
                category = editor.category
            ).netAmount
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editor.isEditMode) {
                        R.string.transaction_form_edit_item_title
                    } else {
                        R.string.transaction_form_add_item_title
                    }
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                CashMateTextField(
                    value = editor.description,
                    onValueChange = onDescriptionChange,
                    label = stringResource(R.string.receipt_review_item_description),
                    errorMessage = editor.descriptionErrorResId?.let { stringResource(it) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    CashMateTextField(
                        value = editor.quantityInput,
                        onValueChange = onQuantityChange,
                        label = stringResource(R.string.receipt_review_item_quantity),
                        errorMessage = editor.quantityErrorResId?.let { stringResource(it) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(0.38f)
                    )
                    CashMateTextField(
                        value = editor.unitPriceInput,
                        onValueChange = onUnitPriceChange,
                        label = stringResource(R.string.receipt_review_item_unit_price),
                        placeholder = stringResource(R.string.transaction_form_amount_hint),
                        errorMessage = editor.unitPriceErrorResId?.let { stringResource(it) },
                        leadingIcon = { Text(currency.symbol) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(0.62f)
                    )
                }

                CashMateTextField(
                    value = editor.discountInput,
                    onValueChange = onDiscountChange,
                    label = stringResource(R.string.receipt_review_item_discount),
                    placeholder = stringResource(R.string.transaction_form_amount_hint),
                    errorMessage = editor.discountErrorResId?.let { stringResource(it) },
                    leadingIcon = { Text(currency.symbol) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    )
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = resolvedCategory?.name ?: editor.category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.receipt_review_item_category)) },
                        leadingIcon = resolvedCategory?.let { cat ->
                            {
                                CategoryIconBadge(
                                    iconKey = cat.icon,
                                    colorHex = cat.color
                                )
                            }
                        },
                        trailingIcon = {
                            Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(enabled = categories.isNotEmpty()) {
                                categoryDropdownExpanded = true
                            }
                    )
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                leadingIcon = {
                                    CategoryIconBadge(
                                        iconKey = category.icon,
                                        colorHex = category.color
                                    )
                                },
                                onClick = {
                                    categoryDropdownExpanded = false
                                    onCategorySelect(category.name)
                                }
                            )
                        }
                    }
                }

                if (previewNet != null) {
                    Text(
                        text = stringResource(
                            R.string.transaction_form_item_preview_label,
                            MoneyFormatter.format(previewNet, currency)
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
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

