package com.vaulto.lite.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaulto.lite.data.local.entity.BudgetEntity
import com.vaulto.lite.data.local.entity.CategoryEntity
import com.vaulto.lite.data.settings.Currency
import com.vaulto.lite.data.settings.ThemeMode
import com.vaulto.lite.ui.MainViewModel
import com.vaulto.lite.ui.components.SwipeableExpenseRow
import com.vaulto.lite.ui.screens.home.formatCurrency
import com.vaulto.lite.ui.theme.StatusAmber
import com.vaulto.lite.ui.theme.StatusGreen
import com.vaulto.lite.ui.theme.StatusRed
import com.vaulto.lite.ui.util.collectAsStateWithLifecycleCompat

/**
 * Settings screen with grouped sections per spec:
 * - Categories: list, add/edit (name+emoji+color), swipe-to-delete
 * - Budgets: overall + per-category, with mini progress bars
 * - Appearance: dark mode toggle + currency picker, both DataStore-backed
 * - Data: Backup/Restore JSON with confirmation dialogs
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val categories by viewModel.categories.collectAsStateWithLifecycleCompat()
    val budget by viewModel.currentBudget.collectAsStateWithLifecycleCompat()
    val categoryBudgets by viewModel.categoryBudgets.collectAsStateWithLifecycleCompat()
    val categorySpending by viewModel.categorySpending.collectAsStateWithLifecycleCompat()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycleCompat()
    val currency by viewModel.currency.collectAsStateWithLifecycleCompat()
    val month by viewModel.month.collectAsStateWithLifecycleCompat()
    val year by viewModel.year.collectAsStateWithLifecycleCompat()

    var categoryDialogTarget by remember { mutableStateOf<CategoryEditTarget?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var categoryPendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
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
                SettingsSection(title = "Categories") {
                    categories.forEach { category ->
                        SwipeableExpenseRow(
                            onEdit = { categoryDialogTarget = CategoryEditTarget.Edit(category) },
                            onDelete = { categoryPendingDelete = category }
                        ) {
                            ListItem(
                                headlineContent = { Text(category.name) },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(category.colorHex)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(category.emoji)
                                    }
                                }
                            )
                        }
                    }
                    TextButton(
                        onClick = { categoryDialogTarget = CategoryEditTarget.Add },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Category")
                    }
                }
            }

            item {
                SettingsSection(title = "Budgets") {
                    ListItem(
                        headlineContent = { Text("Overall monthly budget") },
                        trailingContent = {
                            Text(budget?.amount?.let { formatCurrency(it, currency) } ?: "Not set")
                        },
                        modifier = Modifier.clickable { showBudgetDialog = true }
                    )

                    if (categoryBudgets.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            categories.filter { categoryBudgets.containsKey(it.id) }.forEach { category ->
                                val allocated = categoryBudgets[category.id] ?: 0.0
                                val spent = categorySpending[category.id] ?: 0.0
                                val progress = if (allocated > 0) (spent / allocated).coerceIn(0.0, 1.0) else 0.0

                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${category.emoji} ${category.name}", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "${formatCurrency(spent, currency)} / ${formatCurrency(allocated, currency)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress.toFloat() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = budgetProgressColor(progress),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Appearance") {
                    ListItem(
                        headlineContent = { Text("Theme") },
                        trailingContent = {
                            SingleChoiceSegmentedButtonRow {
                                ThemeMode.entries.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        selected = themeMode == mode,
                                        onClick = { viewModel.setThemeMode(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                                    ) {
                                        Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                                    }
                                }
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Currency") },
                        trailingContent = { Text("${currency.code} (${currency.symbol})") },
                        modifier = Modifier.clickable { showCurrencyPicker = true }
                    )
                }
            }

            item {
                SettingsSection(title = "Data") {
                    BackupRestoreSection(viewModel = viewModel)
                }
            }
        }
    }

    // ---- Dialogs ----

    categoryDialogTarget?.let { target ->
        CategoryEditDialog(
            existing = (target as? CategoryEditTarget.Edit)?.category,
            onDismiss = { categoryDialogTarget = null },
            onSave = { name, emoji, colorHex ->
                when (target) {
                    is CategoryEditTarget.Add -> viewModel.addCategory(
                        CategoryEntity(name = name, emoji = emoji, colorHex = colorHex, isDefault = false)
                    )
                    is CategoryEditTarget.Edit -> viewModel.updateCategory(
                        target.category.copy(name = name, emoji = emoji, colorHex = colorHex)
                    )
                }
                categoryDialogTarget = null
            }
        )
    }

    categoryPendingDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryPendingDelete = null },
            title = { Text("Delete \"${category.name}\"?") },
            text = { Text("Existing expenses in this category will keep their category name but won't be reassigned.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(category)
                    categoryPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { categoryPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showBudgetDialog) {
        BudgetEditDialog(
            categories = categories,
            existingOverall = budget?.amount,
            existingCategoryBudgets = categoryBudgets,
            onDismiss = { showBudgetDialog = false },
            onSave = { overall, perCategory ->
                viewModel.setBudget(
                    BudgetEntity(
                        id = budget?.id ?: 0,
                        month = month,
                        year = year,
                        amount = overall,
                        categoryBudgets = perCategory
                    )
                )
                showBudgetDialog = false
            }
        )
    }

    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            selected = currency,
            onDismiss = { showCurrencyPicker = false },
            onSelect = {
                viewModel.setCurrency(it)
                showCurrencyPicker = false
            }
        )
    }
}

private sealed class CategoryEditTarget {
    data object Add : CategoryEditTarget()
    data class Edit(val category: CategoryEntity) : CategoryEditTarget()
}

@Composable
private fun CurrencyPickerDialog(
    selected: Currency,
    onDismiss: () -> Unit,
    onSelect: (Currency) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Currency") },
        text = {
            Column {
                Currency.entries.forEach { currency ->
                    ListItem(
                        headlineContent = { Text("${currency.displayName} (${currency.symbol})") },
                        trailingContent = {
                            RadioButton(selected = currency == selected, onClick = { onSelect(currency) })
                        },
                        modifier = Modifier.clickable { onSelect(currency) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Card(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
private fun budgetProgressColor(progress: Double): Color = when {
    progress < 0.7 -> StatusGreen
    progress < 1.0 -> StatusAmber
    else -> StatusRed
}

private fun parseHexColor(hex: String): Color =
    try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
