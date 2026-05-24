package com.prathik.fairshare.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
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

// ── Skeleton / shimmer placeholders ──────────────────────────────────────────

/**
 * A single animated shimmer placeholder block.
 * Use [width] as a fraction of max width (0f..1f), or leave null for full width.
 */
@Composable
fun FsSkeletonBlock(
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    widthFraction: Float = 1f,
    cornerRadius: androidx.compose.ui.unit.Dp = 6.dp,
) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue  = 0.65f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "shimmer_alpha",
    )
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(androidx.compose.ui.graphics.Color(0xFF2A2A2D).copy(alpha = alpha))
    )
}

/**
 * Skeleton placeholder for a detail screen — mimics the cover header, balance bar,
 * action row, and 3 timeline rows. Used in GroupDetail and FriendDetail when no
 * cached data is available yet.
 */
@Composable
fun FsDetailSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        // Cover header placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(androidx.compose.ui.graphics.Color(0xFF1A1A1C))
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FsSkeletonBlock(height = 22.dp, widthFraction = 0.45f)  // group/friend name
                FsSkeletonBlock(height = 14.dp, widthFraction = 0.25f)  // member count / status
            }
        }
        // Balance bar placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(androidx.compose.ui.graphics.Color(0xFF161618))
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            FsSkeletonBlock(height = 16.dp, widthFraction = 0.4f)
        }
        // Action row placeholder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Color(0xFF161618))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(3) {
                FsSkeletonBlock(
                    height = 36.dp,
                    modifier = Modifier.weight(1f),
                    cornerRadius = 8.dp,
                )
            }
        }
        // Timeline row placeholders
        repeat(4) {
            FsSkeletonTimelineRow()
        }
    }
}

/** Single skeleton timeline row — mimics an expense or settlement row. */
@Composable
fun FsSkeletonTimelineRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FsSkeletonBlock(height = 40.dp, widthFraction = 0.1f, cornerRadius = 20.dp) // avatar circle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FsSkeletonBlock(height = 14.dp, widthFraction = 0.6f)
            FsSkeletonBlock(height = 11.dp, widthFraction = 0.35f)
        }
        FsSkeletonBlock(height = 14.dp, widthFraction = 0.2f) // amount
    }
}