package com.prathik.fairshare.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.SyneFontFamily
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary

/**
 * Standard top bar used across all screens.
 *
 * [title]          — screen title, Syne SemiBold, truncated if too long
 * [onBack]         — if not null, shows back arrow on the left
 * [actions]        — optional trailing content slot (icon buttons, overflow menu)
 *
 * Usage examples:
 *
 * // Back arrow only
 * FsTopBar(title = "Group Detail", onBack = { navController.popBackStack() })
 *
 * // Back arrow + edit icon
 * FsTopBar(title = "Edit Expense", onBack = { navController.popBackStack() }) {
 *     FsIconButton(icon = Icons.Default.Edit, contentDescription = "Edit", onClick = { })
 * }
 *
 * // No back arrow, with actions
 * FsTopBar(title = "FairShare") {
 *     FsIconButton(icon = Icons.Default.Search, contentDescription = "Search", onClick = { })
 *     FsIconButton(icon = Icons.Default.Add, contentDescription = "Add", onClick = { })
 * }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FsTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text       = title,
                fontFamily = SyneFontFamily,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                color      = TextPrimary,
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = TextPrimary,
                    )
                }
            }
        },
        actions = {
            actions?.invoke()
        },
        modifier = modifier,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor        = Surface0,
            titleContentColor     = TextPrimary,
            navigationIconContentColor = TextPrimary,
            actionIconContentColor     = TextSecondary,
        ),
    )
}