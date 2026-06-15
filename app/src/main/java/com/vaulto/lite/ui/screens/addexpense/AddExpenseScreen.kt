package com.vaulto.lite.ui.screens.addexpense

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaulto.lite.data.local.entity.ExpenseEntity
import com.vaulto.lite.data.local.entity.RecurrenceType
import com.vaulto.lite.ui.MainViewModel
import com.vaulto.lite.ui.components.AnimatedCategoryChip
import com.vaulto.lite.ui.components.NumericKeypad
import com.vaulto.lite.ui.screens.home.formatCurrency
import com.vaulto.lite.ui.util.collectAsStateWithLifecycleCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Add/Edit Expense screen per spec:
 * - Custom numeric keypad for amount (replaces system keyboard)
 * - Horizontal scrollable category chips with selection scale animation
 * - Optional note, date picker, recurring toggle with animated sub-options
 * - Full-width Save button, disabled until amount > 0
 *
 * When [expenseId] is non-null, loads the existing expense and pre-fills
 * all fields (including date), so edits persist the original date unless
 * changed via the date picker.
 */
@Composable
fun AddExpenseScreen(
    viewModel: MainViewModel,
    expenseId: Long?,
    onDone: () -> Unit
) {
    val categories by viewModel.categories.collectAsStateWithLifecycleCompat()
    val currency by viewModel.currency.collectAsStateWithLifecycleCompat()

    var amountText by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var recurrenceType by remember { mutableStateOf(RecurrenceType.NONE) }
    var dateMillis by remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    var existingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var loaded by remember { mutableStateOf(expenseId == null) }

    // Load existing expense for edit mode
    LaunchedEffect(expenseId) {
        if (expenseId != null) {
            val expense = viewModel.getExpenseById(expenseId)
            if (expense != null) {
                existingExpense = expense
                amountText = formatAmountForEdit(expense.amount)
                selectedCategoryId = expense.categoryId
                note = expense.note ?: ""
                isRecurring = expense.isRecurring
                recurrenceType = expense.recurrenceType
                dateMillis = expense.date
            }
            loaded = true
        }
    }

    // Default category selection once categories load (Add mode only)
    LaunchedEffect(categories) {
        if (expenseId == null && selectedCategoryId == null && categories.isNotEmpty()) {
            selectedCategoryId = categories.first().id
        }
    }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isSaveEnabled = amount > 0.0 && selectedCategoryId != null

    if (showDatePicker) {
        ExpenseDatePickerDialog(
            initialMillis = dateMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                dateMillis = millis
                showDatePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (expenseId == null) "Add Expense" else "Edit Expense") })
        }
    ) { innerPadding ->
        if (!loaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Amount display
            Text(
                text = formatCurrency(amount, currency),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Category chips
            Text("Category", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    AnimatedCategoryChip(
                        label = "${category.emoji} ${category.name}",
                        selected = selectedCategoryId == category.id,
                        onClick = { selectedCategoryId = category.id }
                    )
                }
            }

            // Date picker trigger
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Date: ${formatDate(dateMillis)}")
            }

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Recurring toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Recurring expense", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
            }

            // Animated recurring sub-options
            AnimatedVisibility(
                visible = isRecurring,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(RecurrenceType.DAILY, RecurrenceType.WEEKLY, RecurrenceType.MONTHLY, RecurrenceType.YEARLY)
                        .forEach { type ->
                            FilterChip(
                                selected = recurrenceType == type,
                                onClick = { recurrenceType = type },
                                label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Custom numeric keypad
            NumericKeypad(
                value = amountText,
                onValueChange = { amountText = it }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val category = categories.find { it.id == selectedCategoryId } ?: return@Button
                    val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                    val savedExpense = ExpenseEntity(
                        id = existingExpense?.id ?: 0,
                        categoryId = category.id,
                        categoryName = category.name,
                        categoryEmoji = category.emoji,
                        amount = amount,
                        note = note.ifBlank { null },
                        month = cal.get(Calendar.MONTH) + 1,
                        year = cal.get(Calendar.YEAR),
                        date = dateMillis,
                        isRecurring = isRecurring,
                        recurrenceType = if (isRecurring) recurrenceType else RecurrenceType.NONE
                    )
                    if (existingExpense != null) {
                        viewModel.updateExpense(savedExpense)
                    } else {
                        viewModel.addExpense(savedExpense)
                    }
                    onDone()
                },
                enabled = isSaveEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Save")
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDatePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let(onConfirm)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = state)
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(java.util.Date(millis))

/** Formats a stored Double amount back into a keypad-editable string (no trailing .0). */
private fun formatAmountForEdit(amount: Double): String =
    if (amount == amount.toLong().toDouble()) amount.toLong().toString()
    else amount.toString()
