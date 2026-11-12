package minicla03.cashmate.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import minicla03.cashmate.R
import minicla03.cashmate.core.designsystem.CashMateCard
import minicla03.cashmate.core.designsystem.CashMateTheme
import minicla03.cashmate.domain.model.Category
import minicla03.cashmate.domain.model.CategoryExpenseStat
import minicla03.cashmate.domain.model.Money
import minicla03.cashmate.domain.model.MonthlyTrendPoint
import minicla03.cashmate.domain.model.Transaction
import minicla03.cashmate.domain.model.TransactionType
import java.util.Currency

@Composable
fun CategoryIconBadge(
    iconKey: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val tint = CategoryVisuals.parseColor(colorHex)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = CategoryVisuals.iconForKey(iconKey),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    category: Category?,
    currency: Currency,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = CashMateTheme.semanticColors
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) semantic.income else semantic.expense
    val typeLabel = if (isIncome) {
        stringResource(R.string.cd_income_indicator)
    } else {
        stringResource(R.string.cd_expense_indicator)
    }
    val typeIcon = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward
    val formattedAmount = MoneyFormatter.formatSigned(transaction.amount, transaction.type, currency)
    val formattedDate = DateFormatter.formatShort(transaction.date)
    val categoryName = category?.name ?: transaction.categoryId

    CashMateCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "${transaction.description}, $categoryName, $typeLabel, $formattedAmount, $formattedDate"
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.sm)
        ) {
            CategoryIconBadge(
                iconKey = category?.icon ?: "category",
                colorHex = category?.color ?: "#546E7A",
                size = 44.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$categoryName • $formattedDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = typeLabel,
                    tint = amountColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    items: List<CategoryExpenseStat>,
    totalAmount: Money,
    currency: Currency,
    modifier: Modifier = Modifier
) {
    val totalFormatted = MoneyFormatter.format(totalAmount, currency)
    val chartSummary = items.joinToString { stat ->
        "${stat.category.name}: ${MoneyFormatter.formatBasisPointsPercent(stat.shareBasisPoints)}"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.md)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$totalFormatted. $chartSummary"
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 28.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                var startAngle = -90f
                val gapDegrees = if (items.size > 1) 2f else 0f

                items.forEach { stat ->
                    val sweep = (stat.shareBasisPoints / 10_000f) * 360f
                    val drawSweep = (sweep - gapDegrees).coerceAtLeast(1f)
                    drawArc(
                        color = CategoryVisuals.parseColor(stat.category.color),
                        startAngle = startAngle,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.statistics_total_expenses),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = totalFormatted,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.xs)) {
            items.forEach { stat ->
                CategoryDistributionLegendRow(stat = stat, currency = currency)
            }
        }
    }
}

@Composable
fun CategoryDistributionLegendRow(
    stat: CategoryExpenseStat,
    currency: Currency,
    modifier: Modifier = Modifier
) {
    val color = CategoryVisuals.parseColor(stat.category.color)
    val percent = MoneyFormatter.formatBasisPointsPercent(stat.shareBasisPoints)
    val amount = MoneyFormatter.format(stat.amount, currency)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = stat.category.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = percent,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(CashMateTheme.spacing.xs))
        Text(
            text = amount,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun MonthlyTrendBarChart(
    points: List<MonthlyTrendPoint>,
    currency: Currency,
    modifier: Modifier = Modifier
) {
    val semantic = CashMateTheme.semanticColors
    val maxMinor = points.maxOfOrNull { maxOf(it.income.minorUnits, it.expenses.minorUnits) }
        ?.coerceAtLeast(1L) ?: 1L
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.sm)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(CashMateTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChartLegendItem(
                color = semantic.income,
                label = stringResource(R.string.transaction_type_income)
            )
            ChartLegendItem(
                color = semantic.expense,
                label = stringResource(R.string.transaction_type_expense)
            )
        }

        val summaryDesc = points.joinToString("; ") { point ->
            val monthName = DateFormatter.formatMonthShort(point.yearMonth)
            "$monthName: ${MoneyFormatter.format(point.income, currency)} / ${MoneyFormatter.format(point.expenses, currency)}"
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .semantics { contentDescription = summaryDesc }
        ) {
            if (points.isEmpty()) return@Canvas
            val groupCount = points.size
            val groupWidth = size.width / groupCount
            val barWidth = (groupWidth * 0.28f).coerceAtMost(24.dp.toPx())
            val barGap = (barWidth * 0.25f).coerceAtLeast(3.dp.toPx())
            val chartHeight = size.height

            // Subtle baseline and mid-line
            drawLine(
                color = gridColor,
                start = Offset(0f, chartHeight),
                end = Offset(size.width, chartHeight),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = gridColor.copy(alpha = 0.5f),
                start = Offset(0f, chartHeight / 2f),
                end = Offset(size.width, chartHeight / 2f),
                strokeWidth = 1.dp.toPx()
            )

            points.forEachIndexed { index, point ->
                val centerX = groupWidth * index + groupWidth / 2f
                val incomeHeight = ((point.income.minorUnits.toFloat() / maxMinor.toFloat()) * chartHeight)
                    .coerceAtLeast(if (point.income.isPositive) 4.dp.toPx() else 0f)
                val expenseHeight = ((point.expenses.minorUnits.toFloat() / maxMinor.toFloat()) * chartHeight)
                    .coerceAtLeast(if (point.expenses.isPositive) 4.dp.toPx() else 0f)

                if (incomeHeight > 0f) {
                    drawRoundRect(
                        color = semantic.income,
                        topLeft = Offset(centerX - barWidth - barGap / 2f, chartHeight - incomeHeight),
                        size = Size(barWidth, incomeHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
                if (expenseHeight > 0f) {
                    drawRoundRect(
                        color = semantic.expense,
                        topLeft = Offset(centerX + barGap / 2f, chartHeight - expenseHeight),
                        size = Size(barWidth, expenseHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { point ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = DateFormatter.formatMonthShort(point.yearMonth),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

