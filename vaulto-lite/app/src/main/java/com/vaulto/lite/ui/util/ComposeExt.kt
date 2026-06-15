package com.vaulto.lite.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin wrapper so screens can call `.collectAsStateWithLifecycleCompat()` without
 * pulling in the lifecycle-runtime-compose artifact version mismatch concerns.
 * Backed by the standard Compose `collectAsState`.
 */
@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycleCompat(): State<T> = this.collectAsState()
