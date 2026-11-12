package minicla03.cashmate.feature.receipt.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import minicla03.cashmate.R
import minicla03.cashmate.core.common.MoneyParser
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMateOutlinedButton
import minicla03.cashmate.core.designsystem.CashMatePrimaryButton
import minicla03.cashmate.core.designsystem.CashMateTextField
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.core.ui.CategoryIconBadge
import minicla03.cashmate.core.ui.MoneyFormatter
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.ReceiptAdjustmentType
import minicla03.cashmate.feature.receipt.presentation.ReceiptAdjustmentDraft
import minicla03.cashmate.feature.receipt.presentation.ReceiptCategoryResolver
import minicla03.cashmate.feature.receipt.presentation.ReceiptItemDraft
import minicla03.cashmate.feature.receipt.presentation.ReceiptScanEvent
import minicla03.cashmate.feature.receipt.presentation.ReceiptScanState
import java.util.Currency

@Composable
fun ReceiptItemCard(
    item: ReceiptItemDraft,
    categories: List<Category>,
    currency: Currency,
    isInvalid: Boolean,
    enabled: Boolean,
    onEvent: (ReceiptScanEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    val semantic = CashMateTheme.semanticColors
    var expanded by rememberSaveable(item.id) { mutableStateOf(item.description.isBlank()) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val resolvedCategory = ReceiptCategoryResolver.resolve(item.category, categories)
    val parsedUnit = MoneyParser.parse(item.unitPriceInput)
    val parsedGross = MoneyParser.parse(item.grossAmountInput)
    val parsedDiscount = MoneyParser.parse(item.discountAmountInput)
    val parsedNet = MoneyParser.parse(item.netAmountInput)

    val unitDisplay = parsedUnit?.let { MoneyFormatter.format(it, currency) } ?: item.unitPriceInput
    val grossDisplay = parsedGross?.let { MoneyFormatter.format(it, currency) } ?: item.grossAmountInput
    val netDisplay = parsedNet?.let { MoneyFormatter.format(it, currency) } ?: item.netAmountInput
    val hasItemDiscount = parsedDiscount != null && parsedDiscount.isPositive
    val discountDisplay = parsedDiscount?.takeIf { it.isPositive }?.let {
        "-${MoneyFormatter.format(it, currency)}"
    }

    CashMateCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            CategoryIconBadge(
                iconKey = resolvedCategory?.icon ?: "category",
                colorHex = resolvedCategory?.color ?: "#546E7A",
                size = 38.dp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.description.ifBlank { stringResource(R.string.receipt_review_item_description) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.quantityInput} × $unitDisplay • ${resolvedCategory?.name ?: item.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (discountDisplay != null) {
                    Text(
                        text = "${stringResource(R.string.receipt_review_item_discount)}: $discountDisplay",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = semantic.income
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (hasItemDiscount || (parsedGross != null && parsedNet != null && parsedGross > parsedNet)) {
                    Text(
                        text = grossDisplay,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
                Text(
                    text = netDisplay,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(
                        onClick = { expanded = !expanded },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.cd_edit_receipt_item, item.description)
                        )
                    }
                    IconButton(
                        onClick = { onEvent(ReceiptScanEvent.RemoveItem(item.id)) },
                        enabled = enabled
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.cd_remove_receipt_item, item.description),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (expanded || isInvalid) {
            HorizontalDivider(modifier = Modifier.padding(vertical = spacing.sm))
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                CashMateTextField(
                    value = item.description,
                    onValueChange = { onEvent(ReceiptScanEvent.ItemDescriptionChanged(item.id, it)) },
                    label = stringResource(R.string.receipt_review_item_description),
                    errorMessage = if (isInvalid && item.description.isBlank()) {
                        stringResource(R.string.validation_receipt_item_description_required)
                    } else {
                        null
                    },
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
                        value = item.quantityInput,
                        onValueChange = { onEvent(ReceiptScanEvent.ItemQuantityChanged(item.id, it)) },
                        label = stringResource(R.string.receipt_review_item_quantity),
                        errorMessage = if (isInvalid && (item.quantityInput.toIntOrNull() ?: 0) <= 0) {
                            stringResource(R.string.validation_receipt_item_quantity_positive)
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(0.35f)
                    )
                    CashMateTextField(
                        value = item.unitPriceInput,
                        onValueChange = { onEvent(ReceiptScanEvent.ItemUnitPriceChanged(item.id, it)) },
                        label = stringResource(R.string.receipt_review_item_unit_price),
                        leadingIcon = { Text(currency.symbol) },
                        errorMessage = if (isInvalid && MoneyParser.parse(item.unitPriceInput) == null) {
                            stringResource(R.string.validation_receipt_item_price_non_negative)
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(0.65f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    CashMateTextField(
                        value = item.grossAmountInput,
                        onValueChange = { onEvent(ReceiptScanEvent.ItemGrossAmountChanged(item.id, it)) },
                        label = stringResource(R.string.receipt_review_item_gross),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    CashMateTextField(
                        value = item.discountAmountInput,
                        onValueChange = { onEvent(ReceiptScanEvent.ItemDiscountAmountChanged(item.id, it)) },
                        label = stringResource(R.string.receipt_review_item_discount),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    CashMateTextField(
                        value = item.netAmountInput,
                        onValueChange = { onEvent(ReceiptScanEvent.ItemNetAmountChanged(item.id, it)) },
                        label = stringResource(R.string.receipt_review_item_net),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = resolvedCategory?.name ?: item.category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.receipt_review_item_category)) },
                        trailingIcon = {
                            Icon(imageVector = Icons.Rounded.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(enabled = enabled && categories.isNotEmpty()) {
                                categoryExpanded = true
                            }
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
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
                                    categoryExpanded = false
                                    onEvent(ReceiptScanEvent.ItemCategoryChanged(item.id, category.name))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReceiptAdjustmentCard(
    adjustment: ReceiptAdjustmentDraft,
    currency: Currency,
    isInvalid: Boolean,
    enabled: Boolean,
    onEvent: (ReceiptScanEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    val semantic = CashMateTheme.semanticColors
    var expanded by rememberSaveable(adjustment.id) {
        mutableStateOf(adjustment.description.isBlank())
    }

    val parsedAmount = MoneyParser.parse(adjustment.amountInput)
    val amountDisplay = parsedAmount?.let { MoneyFormatter.format(it, currency) } ?: adjustment.amountInput
    val signedDisplay = when (adjustment.type) {
        ReceiptAdjustmentType.DISCOUNT -> "-$amountDisplay"
        ReceiptAdjustmentType.SURCHARGE,
        ReceiptAdjustmentType.TAX -> "+$amountDisplay"
        ReceiptAdjustmentType.OTHER -> amountDisplay
    }

    CashMateCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = adjustment.description.ifBlank {
                        stringResource(R.string.receipt_review_adjustment_description)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = adjustmentTypeLabel(adjustment.type),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = signedDisplay,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (adjustment.type == ReceiptAdjustmentType.DISCOUNT) {
                    semantic.income
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            IconButton(
                onClick = { expanded = !expanded },
                enabled = enabled
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.action_edit)
                )
            }
            IconButton(
                onClick = { onEvent(ReceiptScanEvent.RemoveAdjustment(adjustment.id)) },
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(
                        R.string.cd_remove_receipt_adjustment,
                        adjustment.description
                    ),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        if (expanded || isInvalid) {
            HorizontalDivider(modifier = Modifier.padding(vertical = spacing.sm))
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                CashMateTextField(
                    value = adjustment.description,
                    onValueChange = {
                        onEvent(ReceiptScanEvent.AdjustmentDescriptionChanged(adjustment.id, it))
                    },
                    label = stringResource(R.string.receipt_review_adjustment_description),
                    errorMessage = if (isInvalid && adjustment.description.isBlank()) {
                        stringResource(R.string.validation_receipt_adjustment_description_required)
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )

                CashMateTextField(
                    value = adjustment.amountInput,
                    onValueChange = {
                        onEvent(ReceiptScanEvent.AdjustmentAmountChanged(adjustment.id, it))
                    },
                    label = stringResource(R.string.receipt_review_adjustment_amount),
                    leadingIcon = { Text(currency.symbol) },
                    errorMessage = if (isInvalid && MoneyParser.parse(adjustment.amountInput) == null) {
                        stringResource(R.string.validation_receipt_adjustment_amount_non_negative)
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    )
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    ReceiptAdjustmentType.entries.forEach { type ->
                        FilterChip(
                            selected = adjustment.type == type,
                            onClick = {
                                if (enabled) {
                                    onEvent(ReceiptScanEvent.AdjustmentTypeChanged(adjustment.id, type))
                                }
                            },
                            label = { Text(adjustmentTypeLabel(type)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun adjustmentTypeLabel(type: ReceiptAdjustmentType): String = when (type) {
    ReceiptAdjustmentType.DISCOUNT -> stringResource(R.string.receipt_review_adjustment_type_discount)
    ReceiptAdjustmentType.SURCHARGE -> stringResource(R.string.receipt_review_adjustment_type_surcharge)
    ReceiptAdjustmentType.TAX -> stringResource(R.string.receipt_review_adjustment_type_tax)
    ReceiptAdjustmentType.OTHER -> stringResource(R.string.receipt_review_adjustment_type_other)
}

@Composable
fun ReceiptSavingsCard(
    subtotal: minicla03.cashmate.domain.model.Money?,
    savings: minicla03.cashmate.domain.model.Money,
    totalPaid: minicla03.cashmate.domain.model.Money?,
    currency: Currency,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    val semantic = CashMateTheme.semanticColors

    CashMateCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Surface(
                color = semantic.incomeContainer,
                contentColor = semantic.onIncomeContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.sm, vertical = spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Savings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.receipt_review_savings_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = MoneyFormatter.format(savings, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (subtotal != null && subtotal.isPositive && totalPaid != null) {
                ReceiptSummaryRow(
                    label = stringResource(R.string.receipt_review_original_value),
                    value = MoneyFormatter.format(subtotal, currency)
                )
                ReceiptSummaryRow(
                    label = stringResource(R.string.receipt_review_savings_label),
                    value = "-${MoneyFormatter.format(savings, currency)}",
                    valueColor = semantic.income
                )
                ReceiptSummaryRow(
                    label = stringResource(R.string.receipt_review_you_paid),
                    value = MoneyFormatter.format(totalPaid, currency),
                    bold = true
                )
            }
        }
    }
}

@Composable
fun ReceiptSummaryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
fun ReceiptConfirmedContent(
    state: ReceiptScanState.Success,
    onViewTransaction: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CashMateTheme.spacing
    val semantic = CashMateTheme.semanticColors

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.md),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CashMateCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(semantic.incomeContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = semantic.income,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.receipt_confirmed_title),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = state.merchant,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = MoneyFormatter.format(state.transaction.amount, state.currency),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.receipt_confirmed_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(spacing.xs))

                CashMatePrimaryButton(
                    text = stringResource(R.string.receipt_confirmed_view_transaction),
                    onClick = onViewTransaction,
                    modifier = Modifier.fillMaxWidth()
                )

                CashMateOutlinedButton(
                    text = stringResource(R.string.receipt_confirmed_done),
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

