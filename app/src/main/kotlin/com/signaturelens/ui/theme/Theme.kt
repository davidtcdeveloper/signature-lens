package com.signaturelens.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFD4A574), // Warm gold (rangefinder signature)
    secondary = androidx.compose.ui.graphics.Color(0xFF808080),
    background = androidx.compose.ui.graphics.Color(0xFF000000),
    surface = androidx.compose.ui.graphics.Color(0xFF1C1C1C),
)

/**
 * SignatureLens theme with minimal, photography-focused dark design.
 * Per spec §11: Material 3 with dark color scheme.
 */
@Composable
fun SignatureLensTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
