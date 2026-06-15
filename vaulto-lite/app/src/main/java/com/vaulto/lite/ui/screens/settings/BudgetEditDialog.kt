package com.vaulto.lite.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaulto.lite.data.local.entity.CategoryEntity

/**
 * Dialog for setting the overall monthly budget and optional per-category
 * allocations. Per-category fields are pre-filled from [existingCategoryBudgets]
 * and only non-blank, valid entries are included in [onSave]'s map.
 */
@Composable
fun BudgetEditDialog(
    categories: List<CategoryEntity>,
    existingOverall: Double?,
    existingCategoryBudgets: Map<Long, Double>,
    onDismiss: () -> Unit,
    onSave: (overall: Double, categoryBudgets: Map<Long, Double>) -> Unit
) {
    var overallText by remember {
        mutableStateOf(existingOverall?.let { formatForEdit(it) } ?: "")
    }
    val categoryTexts = remember {
        mutableStateMapOf<Long, String>().apply {
            categories.forEach { category ->
                existingCategoryBudgets[category.id]?.let { put(category.id, formatForEdit(it)) }
            }
        }
    }

    val overallAmount = overallText.toDoubleOrNull()
    val isValid = overallAmount != null && overallAmount > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = overallText,
                    onValueChange = { input -> if (input.all { it.isDigit() || it == '.' }) overallText = input },
                    label = { Text("Overall monthly budget") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Per-category (optional)", style = MaterialTheme.typography.labelLarge)

                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        OutlinedTextField(
                            value = categoryTexts[category.id] ?: "",
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' }) {
                                    categoryTexts[category.id] = input
                                }
                            },
                            label = { Text("${category.emoji} ${category.name}") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    val categoryBudgets = categoryTexts
                        .mapNotNull { (id, text) -> text.toDoubleOrNull()?.takeIf { it > 0 }?.let { id to it } }
                        .toMap()
                    onSave(overallAmount!!, categoryBudgets)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatForEdit(amount: Double): String =
    if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()
