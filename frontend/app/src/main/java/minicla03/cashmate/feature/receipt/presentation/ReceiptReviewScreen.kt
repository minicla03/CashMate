package minicla03.cashmate.feature.receipt.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.common.MoneyParser
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMateOutlinedButton
import minicla03.cashmate.core.designsystem.CashMatePrimaryButton
import minicla03.cashmate.core.designsystem.CashMateTextField
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.core.designsystem.LoadingStateView
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.ui.CategoryIconBadge
import minicla03.cashmate.core.ui.DateFormatter
import minicla03.cashmate.core.ui.MoneyFormatter
import minicla03.cashmate.feature.receipt.presentation.components.ReceiptAdjustmentCard
import minicla03.cashmate.feature.receipt.presentation.components.ReceiptConfirmedContent
import minicla03.cashmate.feature.receipt.presentation.components.ReceiptItemCard
import minicla03.cashmate.feature.receipt.presentation.components.ReceiptSavingsCard
import minicla03.cashmate.feature.receipt.presentation.components.ReceiptSummaryRow
import java.time.Instant
import java.time.ZoneOffset

@Composable
fun ReceiptReviewRoute(
    receiptId: String,
    viewModel: ReceiptScanViewModel,
    onExitFlow: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(receiptId) {
        viewModel.onEvent(ReceiptScanEvent.EnsureDraftLoaded(receiptId))
    }

    val reviewError = (state as? ReceiptScanState.Review)?.submitError
    LaunchedEffect(reviewError) {
        val err = reviewError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(err.messageResId))
        viewModel.onEvent(ReceiptScanEvent.DismissSubmitError)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReceiptScanEffect.NavigateToTransactionDetail ->
                    onNavigateToTransactionDetail(effect.transactionId)
                ReceiptScanEffect.ExitFlow -> onExitFlow()
                is ReceiptScanEffect.NavigateToReview -> Unit
            }
        }
    }

    BackHandler(enabled = state !is ReceiptScanState.Confirming) {
        if (state is ReceiptScanState.Success) {
            viewModel.onEvent(ReceiptScanEvent.DoneClick)
        } else {
            viewModel.onEvent(ReceiptScanEvent.CancelClick)
        }
    }

    ReceiptReviewScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptReviewScreen(
    state: ReceiptScanState,
    snackbarHostState: SnackbarHostState,
    onEvent: (ReceiptScanEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val isConfirming = state is ReceiptScanState.Confirming
    val reviewState = when (state) {
        is ReceiptScanState.Review -> state
        is ReceiptScanState.Confirming -> state.review
        else -> null
    }

    val topBarTitle = if (state is ReceiptScanState.Success) {
        stringResource(R.string.receipt_confirmed_title)
    } else {
        stringResource(R.string.receipt_review_title)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle) },
                navigationIcon = {
                    if (!isConfirming && state !is ReceiptScanState.Success) {
                        IconButton(onClick = { onEvent(ReceiptScanEvent.CancelClick) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_navigate_back)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state is ReceiptScanState.Success -> ReceiptConfirmedContent(
                state = state,
                onViewTransaction = {
                    onEvent(ReceiptScanEvent.ViewTransactionClick(state.transaction.id))
                },
                onDone = { onEvent(ReceiptScanEvent.DoneClick) },
                modifier = Modifier.padding(padding)
            )

            reviewState != null -> ReceiptReviewContent(
                review = reviewState,
                isConfirming = isConfirming,
                onEvent = onEvent,
                modifier = Modifier.padding(padding)
            )

            else -> LoadingStateView(
                modifier = Modifier.padding(padding),
                message = stringResource(R.string.receipt_processing_title)
            )
        }
    }

    if (reviewState?.showDiscardDialog == true) {
        AlertDialog(
            onDismissRequest = { onEvent(ReceiptScanEvent.DismissDiscardDialog) },
            title = { Text(stringResource(R.string.receipt_review_discard_title)) },
            text = { Text(stringResource(R.string.receipt_review_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(ReceiptScanEvent.ConfirmDiscard) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.receipt_review_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ReceiptScanEvent.DismissDiscardDialog) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptReviewContent(
    review: ReceiptScanState.Review,
    isConfirming: Boolean,
    onEvent: (ReceiptScanEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    val editable = !isConfirming
    var showDatePicker by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var editingTotals by remember { mutableStateOf(false) }

    val semantic = CashMateTheme.semanticColors
    val selectedCategory = review.categories.firstOrNull { it.id == review.selectedCategoryId }
    val selectedAccount = review.accounts.firstOrNull { it.id == review.selectedAccountId }
    val parsedTotal = MoneyParser.parse(review.totalInput)
    val parsedSubtotal = MoneyParser.parse(review.subtotalInput)
    val parsedTotalDiscount = MoneyParser.parse(review.totalDiscountInput)
    val positiveDiscount = parsedTotalDiscount?.takeIf { it.isPositive }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        item(key = "found_banner") {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.receipt_review_found_banner),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(spacing.md)
                )
            }
        }

        item(key = "receipt_header_card") {
            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Text(
                        text = review.merchant.ifBlank {
                            stringResource(R.string.receipt_review_merchant_label)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = DateFormatter.formatLong(review.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = spacing.xxs))

                    Text(
                        text = stringResource(R.string.receipt_review_total_paid_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = parsedTotal?.let { MoneyFormatter.format(it, review.currency) }
                            ?: "${review.currency.symbol} ${review.totalInput}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.receipt_review_transaction_amount_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = spacing.xxs))

                    CashMateTextField(
                        value = review.merchant,
                        onValueChange = { if (editable) onEvent(ReceiptScanEvent.MerchantChanged(it)) },
                        label = stringResource(R.string.receipt_review_merchant_label),
                        placeholder = stringResource(R.string.receipt_review_merchant_hint),
                        errorMessage = review.fieldErrors[ValidationField.MERCHANT]?.let {
                            stringResource(it)
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val dateError = review.fieldErrors[ValidationField.DATE]?.let {
                            stringResource(it)
                        }
                        OutlinedTextField(
                            value = DateFormatter.formatLong(review.date),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.receipt_review_date_label)) },
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
                                .clickable(enabled = editable) { showDatePicker = true }
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val categoryError = review.fieldErrors[ValidationField.CATEGORY]?.let {
                            stringResource(it)
                        }
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.transaction_form_category_label)) },
                            placeholder = {
                                Text(stringResource(R.string.transaction_form_category_placeholder))
                            },
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
                                .clickable(enabled = editable && review.categories.isNotEmpty()) {
                                    categoryDropdownExpanded = true
                                }
                        )
                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            review.categories.forEach { category ->
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
                                        onEvent(ReceiptScanEvent.CategorySelected(category.id))
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val accountError = review.fieldErrors[ValidationField.ACCOUNT]?.let {
                            stringResource(it)
                        }
                        OutlinedTextField(
                            value = selectedAccount?.let { "${it.name} (${it.currency.currencyCode})" }
                                ?: "",
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
                                .clickable(enabled = editable && review.accounts.size > 1) {
                                    accountDropdownExpanded = true
                                }
                        )
                        DropdownMenu(
                            expanded = accountDropdownExpanded,
                            onDismissRequest = { accountDropdownExpanded = false }
                        ) {
                            review.accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text("${account.name} (${account.currency.currencyCode})") },
                                    onClick = {
                                        accountDropdownExpanded = false
                                        onEvent(ReceiptScanEvent.AccountSelected(account.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "products_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.receipt_review_products_section),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() }
                )
                TextButton(
                    onClick = { onEvent(ReceiptScanEvent.AddItem) },
                    enabled = editable
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.receipt_review_add_item))
                }
            }
        }

        if (review.items.isEmpty()) {
            item(key = "products_empty") {
                CashMateCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.receipt_review_empty_items),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(
                items = review.items,
                key = { it.id }
            ) { item ->
                ReceiptItemCard(
                    item = item,
                    categories = review.categories,
                    currency = review.currency,
                    isInvalid = item.id in review.invalidItemIds,
                    enabled = editable,
                    onEvent = onEvent
                )
            }
        }

        item(key = "adjustments_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.receipt_review_adjustments_section),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() }
                )
                TextButton(
                    onClick = { onEvent(ReceiptScanEvent.AddAdjustment) },
                    enabled = editable
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.receipt_review_add_adjustment))
                }
            }
        }

        items(
            items = review.adjustments,
            key = { it.id }
        ) { adjustment ->
            ReceiptAdjustmentCard(
                adjustment = adjustment,
                currency = review.currency,
                isInvalid = adjustment.id in review.invalidAdjustmentIds,
                enabled = editable,
                onEvent = onEvent
            )
        }

        if (positiveDiscount != null) {
            item(key = "savings_card") {
                ReceiptSavingsCard(
                    subtotal = parsedSubtotal,
                    savings = positiveDiscount,
                    totalPaid = parsedTotal,
                    currency = review.currency
                )
            }
        }

        item(key = "totals_card") {
            val showTotalEditors = editingTotals || review.fieldErrors.containsKey(ValidationField.TOTAL)
            CashMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.receipt_review_totals_section),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() }
                        )
                        TextButton(
                            onClick = { editingTotals = !editingTotals },
                            enabled = editable
                        ) {
                            Text(
                                text = stringResource(
                                    if (showTotalEditors) {
                                        R.string.receipt_review_done_editing_totals
                                    } else {
                                        R.string.receipt_review_edit_totals
                                    }
                                )
                            )
                        }
                    }

                    ReceiptSummaryRow(
                        label = stringResource(R.string.receipt_review_subtotal_label),
                        value = parsedSubtotal?.let { MoneyFormatter.format(it, review.currency) }
                            ?: review.subtotalInput
                    )

                    if (positiveDiscount != null) {
                        ReceiptSummaryRow(
                            label = stringResource(R.string.receipt_review_discounts_summary),
                            value = "-${MoneyFormatter.format(positiveDiscount, review.currency)}",
                            valueColor = semantic.income
                        )
                    }

                    HorizontalDivider()

                    ReceiptSummaryRow(
                        label = stringResource(R.string.receipt_review_total_paid_label),
                        value = parsedTotal?.let { MoneyFormatter.format(it, review.currency) }
                            ?: review.totalInput,
                        valueColor = MaterialTheme.colorScheme.primary,
                        bold = true
                    )

                    if (showTotalEditors) {
                        HorizontalDivider(modifier = Modifier.padding(top = spacing.xxs))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                        ) {
                            CashMateTextField(
                                value = review.subtotalInput,
                                onValueChange = { if (editable) onEvent(ReceiptScanEvent.SubtotalChanged(it)) },
                                label = stringResource(R.string.receipt_review_subtotal_label),
                                leadingIcon = { Text(review.currency.symbol) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            CashMateTextField(
                                value = review.totalDiscountInput,
                                onValueChange = {
                                    if (editable) onEvent(ReceiptScanEvent.TotalDiscountChanged(it))
                                },
                                label = stringResource(R.string.receipt_review_total_discount_label),
                                leadingIcon = { Text(review.currency.symbol) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        CashMateTextField(
                            value = review.totalInput,
                            onValueChange = { if (editable) onEvent(ReceiptScanEvent.TotalChanged(it)) },
                            label = stringResource(R.string.receipt_review_total_paid_label),
                            leadingIcon = { Text(review.currency.symbol) },
                            errorMessage = review.fieldErrors[ValidationField.TOTAL]?.let {
                                stringResource(it)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            )
                        )
                    }
                }
            }
        }

        item(key = "review_actions") {
            Spacer(modifier = Modifier.height(spacing.xs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                CashMateOutlinedButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = { onEvent(ReceiptScanEvent.CancelClick) },
                    enabled = editable,
                    modifier = Modifier.weight(1f)
                )
                CashMatePrimaryButton(
                    text = stringResource(R.string.receipt_review_confirm),
                    onClick = { onEvent(ReceiptScanEvent.ConfirmClick) },
                    isLoading = isConfirming,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = review.date.atStartOfDay(ZoneOffset.UTC).toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            onEvent(ReceiptScanEvent.DateChanged(picked))
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
}

