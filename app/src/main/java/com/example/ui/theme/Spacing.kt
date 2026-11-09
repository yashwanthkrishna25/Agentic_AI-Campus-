package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard 8.dp Material 3 Spacing System for AGENTRA AI.
 * Enforces unified density, consistent rhythm, and responsive layouts across all modules.
 */
@Immutable
object Spacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
    val xxxl: Dp = 64.dp

    // Touch targets and elevations
    val minTouchTarget: Dp = 48.dp
    val cornerSmall: Dp = 8.dp
    val cornerMedium: Dp = 16.dp
    val cornerLarge: Dp = 24.dp
    val cornerFull: Dp = 999.dp
}
