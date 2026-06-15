package com.vaulto.lite.ui.screens.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaulto.lite.ui.MainViewModel
import com.vaulto.lite.ui.util.collectAsStateWithLifecycleCompat

/** Time period filter for the Analytics screen. */
enum class AnalyticsPeriod(val label: String) {
    THIS_MONTH("This Month"),
    LAST_3_MONTHS("Last 3 Months"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}

/**
 * Analytics screen scaffold - "the masterpiece".
 *
 * Sections to be implemented:
 * - Segmented control (period selector) with cross-fade chart transitions
 * - Sticky summary strip (total spent, vs previous period delta, avg daily/monthly)
 * - Donut chart (spending breakdown) with interactive slices + legend
 * - Trend chart (line/bar) with budget reference line
 * - Insights carousel
 * - Category comparison stacked bar
 * - Recurring expenses summary
 * - Export report button (PDF/CSV)
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: MainViewModel) {
    var selectedPeriod by remember { mutableStateOf(AnalyticsPeriod.THIS_MONTH) }

    val totalSpent by viewModel.totalSpent.collectAsStateWithLifecycleCompat()
    val previousMonthTotal by viewModel.previousMonthTotal.collectAsStateWithLifecycleCompat()
    val dailyAverage by viewModel.dailyAverage.collectAsStateWithLifecycleCompat()
    val currency by viewModel.currency.collectAsStateWithLifecycleCompat()
    val insights by viewModel.insights.collectAsStateWithLifecycleCompat()
    val recurringExpenses by viewModel.recurringExpenses.collectAsStateWithLifecycleCompat()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Analytics") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                PeriodSegmentedControl(
                    selected = selectedPeriod,
                    onSelect = { selectedPeriod = it }
                )
            }

            item {
                SummaryStrip(
                    totalSpent = totalSpent,
                    previousTotal = previousMonthTotal,
                    dailyAverage = dailyAverage,
                    currency = currency
                )
            }

            item {
                PlaceholderSection(title = "Spending Breakdown (Donut Chart)")
            }

            item {
                PlaceholderSection(title = "Trend Chart")
            }

            item {
                InsightsCarousel(insights = insights)
            }

            item {
                PlaceholderSection(title = "Category Comparison (Stacked Bar)")
            }

            item {
                RecurringSummaryCard(recurringExpenses = recurringExpenses, currency = currency)
            }

            item {
                OutlinedButton(
                    onClick = { /* TODO (step 4): export PDF/CSV report */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Report")
                }
            }
        }
    }
}

@Composable
private fun PeriodSegmentedControl(selected: AnalyticsPeriod, onSelect: (AnalyticsPeriod) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        AnalyticsPeriod.entries.forEachIndexed { index, period ->
            SegmentedButton(
                selected = selected == period,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = AnalyticsPeriod.entries.size)
            ) {
                Text(period.label)
            }
        }
    }
}

@Composable
private fun SummaryStrip(
    totalSpent: Double,
    previousTotal: Double,
    dailyAverage: Double,
    currency: com.vaulto.lite.data.settings.Currency
) {
    val animatedTotal = com.vaulto.lite.ui.components.animatedCount(totalSpent)

    val deltaPct = if (previousTotal > 0) ((totalSpent - previousTotal) / previousTotal) * 100 else null
    val spendingMore = deltaPct != null && deltaPct > 0
    val deltaColor = when {
        deltaPct == null -> MaterialTheme.colorScheme.onSurfaceVariant
        spendingMore -> com.vaulto.lite.ui.theme.DeltaPositiveBad
        else -> com.vaulto.lite.ui.theme.DeltaNegativeGood
    }

    Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Total Spent", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                com.vaulto.lite.ui.screens.home.formatCurrency(animatedTotal, currency),
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                if (deltaPct != null) {
                    val arrow = if (spendingMore) "↑" else "↓"
                    Text(
                        "$arrow ${kotlin.math.abs(deltaPct).toInt()}% vs last period",
                        style = MaterialTheme.typography.bodySmall,
                        color = deltaColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    "Avg daily: ${com.vaulto.lite.ui.screens.home.formatCurrency(dailyAverage, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InsightsCarousel(insights: List<com.vaulto.lite.domain.insights.Insight>) {
    Column {
        Text("Insights", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(insights) { insight ->
                Card(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.width(220.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(insight.icon, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(insight.text, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringSummaryCard(
    recurringExpenses: List<com.vaulto.lite.data.local.entity.ExpenseEntity>,
    currency: com.vaulto.lite.data.settings.Currency
) {
    val monthlyTotal = recurringExpenses.sumOf { it.amount }

    Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Recurring Expenses", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            if (recurringExpenses.isEmpty()) {
                Text(
                    "No recurring expenses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "${recurringExpenses.size} active · ${com.vaulto.lite.ui.screens.home.formatCurrency(monthlyTotal, currency)}/mo committed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                recurringExpenses.forEach { expense ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${expense.categoryEmoji} ${expense.categoryName}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            com.vaulto.lite.ui.screens.home.formatCurrency(expense.amount, currency),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderSection(title: String) {
    Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(16.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
