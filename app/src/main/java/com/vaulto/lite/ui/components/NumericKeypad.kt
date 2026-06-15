package com.vaulto.lite.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * Custom numeric keypad for amount entry on the Add/Edit Expense screen.
 * Emits raw amount-string edits via [onValueChange]; the caller owns the
 * text state. A single decimal point is allowed and limited to 2 digits
 * after it.
 */
@Composable
fun NumericKeypad(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    fun appendDigit(digit: String) {
        if (digit == "." && value.contains(".")) return
        val newValue = value + digit
        val parts = newValue.split(".")
        if (parts.size == 2 && parts[1].length > 2) return
        onValueChange(newValue)
    }

    fun backspace() {
        if (value.isNotEmpty()) onValueChange(value.dropLast(1))
    }

    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    KeypadKey(
                        label = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            when (key) {
                                "⌫" -> backspace()
                                else -> appendDigit(key)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (label == "⌫") {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace")
            } else {
                Text(label, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
