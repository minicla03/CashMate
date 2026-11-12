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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMatePrimaryButton
import minicla03.cashmate.core.designsystem.CashMateTextField
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.core.designsystem.ErrorStateView
import minicla03.cashmate.core.designsystem.LoadingStateView
import minicla03.cashmate.core.error.ValidationField
import minicla03.cashmate.core.ui.CategoryIconBadge
import minicla03.cashmate.core.ui.DateFormatter
import minicla03.cashmate.domain.model.TransactionType
import java.time.Instant
import java.time.ZoneOffset

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

                    CashMateTextField(
                        value = state.amountInput,
                        onValueChange = { onEvent(TransactionFormEvent.AmountChanged(it)) },
                        label = stringResource(R.string.transaction_form_amount_label),
                        placeholder = stringResource(R.string.transaction_form_amount_hint),
                        errorMessage = state.fieldErrors[ValidationField.AMOUNT]?.let { stringResource(it) },
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
}

