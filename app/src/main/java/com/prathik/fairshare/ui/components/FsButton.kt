package com.prathik.fairshare.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prathik.fairshare.ui.theme.Danger
import com.prathik.fairshare.ui.theme.DangerContainer
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary

/**
 * Primary filled button — green background, used for main CTAs.
 * Shows CircularProgressIndicator when isLoading = true.
 */
@Composable
fun FsPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(Radius.lg),
        colors = ButtonDefaults.buttonColors(
            containerColor      = Green400,
            contentColor        = Surface2,
            disabledContainerColor = Surface3,
            disabledContentColor   = TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color    = Surface2,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = text, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Secondary outlined button — green border, transparent background.
 */
@Composable
fun FsSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(Radius.lg),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor         = Green400,
            disabledContentColor = TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        Text(text = text, fontWeight = FontWeight.Medium)
    }
}

/**
 * Text/ghost button — no background, no border.
 */
@Composable
fun FsTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor         = Green400,
            disabledContentColor = TextSecondary,
        ),
    ) {
        Text(text = text, fontWeight = FontWeight.Medium)
    }
}

/**
 * Danger button — orange tinted surface, used for destructive actions.
 */
@Composable
fun FsDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(Radius.lg),
        colors = ButtonDefaults.buttonColors(
            containerColor      = DangerContainer,
            contentColor        = Danger,
            disabledContainerColor = Surface3,
            disabledContentColor   = TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Icon button — Surface2 card with a centered icon.
 * Used for top bar actions and FAB-style buttons.
 */
@Composable
fun FsIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = TextPrimary,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = tint,
        ),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = contentDescription,
            tint               = tint,
        )
    }
}