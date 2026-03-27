package com.prathik.fairshare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prathik.fairshare.ui.theme.AvatarColors
import com.prathik.fairshare.ui.theme.ComponentSize
import com.prathik.fairshare.ui.theme.TextPrimary

/**
 * Displays a circular avatar.
 *
 * If [imageUrl] is not null, loads the image via Coil.
 * Falls back to a colored circle with the user's initials.
 * Color is deterministic — same userId always gets the same color.
 *
 * Sizes:
 * - avatarSm = 28dp — compact lists
 * - avatarMd = 36dp — expense rows, notification rows
 * - avatarLg = 48dp — member lists, friend rows
 * - avatarXl = 52dp — profile screens, friend detail
 */
@Composable
fun FsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    userId: String = name,
    size: Dp = ComponentSize.avatarMd,
) {
    val initials = remember(name) { getInitials(name) }
    val color    = remember(userId) { AvatarColors[userId.hashCode().and(0x7FFFFFFF) % AvatarColors.size] }
    val fontSize = remember(size) {
        when {
            size <= ComponentSize.avatarSm -> 10.sp
            size <= ComponentSize.avatarMd -> 13.sp
            size <= ComponentSize.avatarLg -> 16.sp
            else                           -> 18.sp
        }
    }

    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model              = imageUrl,
            contentDescription = name,
            contentScale       = ContentScale.Crop,
            modifier           = modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
        ) {
            Text(
                text       = initials,
                color      = TextPrimary,
                fontSize   = fontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
            )
        }
    }
}

/**
 * Extracts initials from a full name.
 * "Prathik Gowda" → "PG"
 * "Prathik"       → "P"
 * ""              → "?"
 */
private fun getInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty()  -> "?"
        parts.size == 1  -> parts[0].take(1).uppercase()
        else             -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}