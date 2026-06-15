package com.vaulto.lite.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaulto.lite.data.local.entity.ExpenseEntity
import com.vaulto.lite.data.settings.Currency
import com.vaulto.lite.ui.MainViewModel
import com.vaulto.lite.ui.components.SwipeableExpenseRow
import com.vaulto.lite.ui.components.animatedCount
import com.vaulto.lite.ui.theme.StatusAmber
import com.vaulto.lite.ui.theme.StatusGreen
import com.vaulto.lite.ui.theme.StatusRed
import com.vaulto.lite.ui.util.collectAsStateWithLifecycleCompat
import com.vaulto.lite.ui.util.dateHeaderLabel
import com.vaulto.lite.ui.util.startOfDay
import java.text.NumberFormat
import java.util.Locale

/**
 * Home screen per spec:
 * - Top bar: app name + emoji, month label with chevrons
 * - Hero card: remaining amount (count-up), progress bar, daily average,
 *   or a "Set a budget" prompt when no budget exists for this month
 * - Pull-down reveals "Today's spend" chip
 * - Search/filter bar
 * - Expense list grouped by date with sticky headers, emoji circles,
 *   swipe-left for Edit/Delete with haptics
 * - Empty state with friendly emoji + prompt
 * - Extended saffron FAB "Add Expense"
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onSetBudget: () -> Unit = {}
) {
    val month by viewModel.month.collectAsStateWithLifecycleCompat()
    val year by viewModel.year.collectAsStateWithLifecycleCompat()
    val expenses by viewModel.expenses.collectAsStateWithLifecycleCompat()
    val remaining by viewModel.remaining.collectAsStateWithLifecycleCompat()
    val budget by viewModel.currentBudget.collectAsStateWithLifecycleCompat()
    val dailyAverage by viewModel.dailyAverage.collectAsStateWithLifecycleCompat()
    val todaysSpend by viewModel.todaysSpend.collectAsStateWithLifecycleCompat()
    val currency by viewModel.currency.collectAsStateWithLifecycleCompat()

    var searchQuery by remember { mutableStateOf("") }
    var showTodaysSpend by remember { mutableStateOf(false) }
    var pullOffset by remember { mutableStateOf(0f) }

    val filteredExpenses = remember(expenses, searchQuery) {
        if (searchQuery.isBlank()) expenses
        else expenses.filter {
            it.categoryName.contains(searchQuery, ignoreCase = true) ||
                (it.note?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    val groupedExpenses = remember(filteredExpenses) {
        filteredExpenses
            .groupBy { startOfDay(it.date) }
            .toSortedMap(compareByDescending { it })
    }

    Scaffold(
        topBar = {
            VaultoTopBar(
                month = month,
                year = year,
                onPrevious = viewModel::goToPreviousMonth,
                onNext = viewModel::goToNextMonth
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddExpense,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Expense") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Pull-down gesture: dragging down on the list reveals the
                // "Today's spend" chip once past a small threshold.
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        if (delta > 0f) {
                            pullOffset = (pullOffset + delta).coerceIn(0f, 160f)
                            if (pullOffset > 60f) showTodaysSpend = true
                        } else {
                            pullOffset = (pullOffset + delta).coerceAtLeast(0f)
                        }
                    },
                    onDragStopped = { pullOffset = 0f }
                )
        ) {
            AnimatedVisibility(
                visible = showTodaysSpend,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TodaysSpendChip(
                    amount = todaysSpend,
                    currency = currency,
                    onDismiss = { showTodaysSpend = false }
                )
            }

            if (expenses.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        HeroCard(
                            remaining = remaining,
                            budgetAmount = budget?.amount,
                            dailyAverage = dailyAverage,
                            currency = currency,
                            onSetBudget = onSetBudget
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item {
                        SearchBar(query = searchQuery, onQueryChange = { searchQuery = it })
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    groupedExpenses.forEach { (dayMillis, dayExpenses) ->
                        stickyHeader {
                            DateHeader(dayMillis)
                        }
                        items(dayExpenses, key = { it.id }) { expense ->
                            SwipeableExpenseRow(
                                onEdit = { onEditExpense(expense.id) },
                                onDelete = { viewModel.deleteExpense(expense) }
                            ) {
                                ExpenseRow(expense = expense, currency = currency)
                            }
                        }
                    }

                    if (groupedExpenses.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No expenses match \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun VaultoTopBar(month: Int, year: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💰 Vaulto Lite", style = MaterialTheme.typography.titleLarge)
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    text = monthLabel(month, year),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                }
            }
        }
    )
}

/**
 * Pull-down reveal chip showing today's total spend.
 */
@Composable
private fun TodaysSpendChip(amount: Double, currency: Currency, onDismiss: () -> Unit) {
    val animated = animatedCount(amount)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        AssistChip(
            onClick = onDismiss,
            label = { Text("Today's spend: ${formatCurrency(animated, currency)}") },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}

/**
 * Hero card: "Remaining" amount (animated count-up), progress bar
 * (green -> amber -> red), daily average. If [budgetAmount] is null,
 * shows a friendly "Set a budget" prompt instead of a silent zero bar.
 */
@Composable
private fun HeroCard(
    remaining: Double,
    budgetAmount: Double?,
    dailyAverage: Double,
    currency: Currency,
    onSetBudget: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (budgetAmount == null || budgetAmount <= 0.0) {
                // No budget set state
                Text(
                    "No budget set for this month",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Set a monthly budget to track your remaining balance and spending pace.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(onClick = onSetBudget) {
                    Text("Set a budget")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Daily avg: ${formatCurrency(dailyAverage, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val spent = budgetAmount - remaining
                val progress = (spent / budgetAmount).coerceIn(0.0, 1.0)
                val animatedRemaining = animatedCount(remaining)
                val animatedProgress by animateFloatAsState(
                    targetValue = progress.toFloat(),
                    label = "budgetProgress"
                )

                Text(
                    "Remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(formatCurrency(animatedRemaining, currency), style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = progressColor(progress),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Daily avg: ${formatCurrency(dailyAverage, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search by category or note") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Sticky date header for a group of expenses. */
@Composable
private fun DateHeader(dayMillis: Long) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = dateHeaderLabel(dayMillis),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Single expense row: emoji in a colored circle, category/note, amount. */
@Composable
private fun ExpenseRow(expense: ExpenseEntity, currency: Currency) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryEmojiCircle(emoji = expense.categoryEmoji)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(expense.categoryName, style = MaterialTheme.typography.bodyLarge)
            if (!expense.note.isNullOrBlank()) {
                Text(
                    expense.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(formatCurrency(expense.amount, currency), style = MaterialTheme.typography.bodyLarge)
    }
}

/** Category emoji rendered in a soft colored circle background. */
@Composable
private fun CategoryEmojiCircle(emoji: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun progressColor(progress: Double): Color = when {
    progress < 0.7 -> StatusGreen
    progress < 1.0 -> StatusAmber
    else -> StatusRed
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧾", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("No expenses yet this month", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tap \"Add Expense\" to log your first one",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun monthLabel(month: Int, year: Int): String {
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    return "${monthNames[month - 1]} $year"
}

/**
 * Formats [amount] using the given [currency]'s symbol. Falls back to a
 * generic "symbol + grouped number" format so the symbol always matches
 * the user's selected currency, independent of device locale.
 */
fun formatCurrency(amount: Double, currency: Currency = Currency.DEFAULT): String {
    val number = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }.format(amount)
    return "${currency.symbol}$number"
}
