package com.prathik.fairshare.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface4

/**
 * Standard card used throughout the app.
 *
 * Rounded corners, subtle border, Surface2 background.
 * Optional [onClick] — adds ripple effect when provided.
 *
 * Usage:
 *
 * // Static card
 * FsCard {
 *     Text("Content here")
 * }
 *
 * // Clickable card
 * FsCard(onClick = { navController.navigate(...) }) {
 *     Text("Tap me")
 * }
 */
@Composable
fun FsCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    radius: Dp = Radius.xl,
    padding: Dp = Spacing.lg,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        onClick      = onClick ?: {},
        enabled      = onClick != null,
        modifier     = modifier.fillMaxWidth(),
        shape        = RoundedCornerShape(radius),
        color        = Surface2,
        border       = BorderStroke(1.dp, Surface4),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(padding),
            content  = content,
        )
    }
}