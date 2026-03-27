package com.prathik.fairshare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary

/**
 * Full screen loading indicator.
 * Used as initial load state for every list screen.
 */
@Composable
fun FsLoadingScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier.fillMaxSize(),
    ) {
        CircularProgressIndicator(
            color       = Green400,
            strokeWidth = 2.dp,
            modifier    = Modifier.size(40.dp),
        )
    }
}

/**
 * Inline loading indicator — smaller, used inside cards or rows.
 */
@Composable
fun FsLoadingIndicator(
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = modifier,
    ) {
        CircularProgressIndicator(
            color       = Green400,
            strokeWidth = 2.dp,
            modifier    = Modifier.size(24.dp),
        )
    }
}

/**
 * Full screen error state.
 * Shows an icon, message, and optional retry button.
 *
 * [message]   — user-facing error description
 * [onRetry]   — if not null, shows a "Try again" button below the message
 * [isNetwork] — if true, shows a wifi-off icon instead of error icon
 */
@Composable
fun FsErrorScreen(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    isNetwork: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier            = modifier
            .fillMaxSize()
            .padding(Spacing.xxxl),
    ) {
        Icon(
            imageVector        = if (isNetwork) Icons.Outlined.CloudOff
            else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint               = Negative,
            modifier           = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text      = if (isNetwork) "No internet connection" else "Something went wrong",
            color     = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize  = 16.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text      = message,
            color     = TextSecondary,
            fontSize  = 14.sp,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(Spacing.xl))
            FsSecondaryButton(
                text    = "Try again",
                onClick = onRetry,
            )
        }
    }
}

/**
 * Full screen empty state.
 * Used when a list has no items — no groups, no expenses, no friends, etc.
 *
 * [title]    — primary empty state message e.g. "No expenses yet"
 * [subtitle] — secondary hint e.g. "Add the first expense to get started"
 * [ctaText]  — if not null, shows a primary CTA button
 * [onCta]    — action for the CTA button
 */
@Composable
fun FsEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    ctaText: String? = null,
    onCta: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier            = modifier
            .fillMaxSize()
            .padding(Spacing.xxxl),
    ) {
        Icon(
            imageVector        = Icons.Outlined.Inbox,
            contentDescription = null,
            tint               = TextSecondary,
            modifier           = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text       = title,
            color      = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 16.sp,
            textAlign  = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text      = subtitle,
            color     = TextSecondary,
            fontSize  = 14.sp,
            textAlign = TextAlign.Center,
        )
        if (ctaText != null && onCta != null) {
            Spacer(modifier = Modifier.height(Spacing.xl))
            FsPrimaryButton(
                text    = ctaText,
                onClick = onCta,
            )
        }
    }
}