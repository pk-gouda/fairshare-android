package com.prathik.fairshare.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsEmptyState
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.components.FsErrorDialog
import com.prathik.fairshare.ui.components.FsErrorDialogState
import com.prathik.fairshare.ui.components.apiErrorDialogState
import com.prathik.fairshare.ui.theme.ComponentSize
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary

/**
 * Add Member Screen.
 *
 * Shows the user's friends list with a search bar.
 * - Friends NOT in the group: tappable, shows + icon → adds to group on tap
 * - Friends ALREADY in the group: green checkmark, non-tappable
 * - Per-row loading spinner while adding
 *
 * If user has no friends, shows empty state with guidance.
 */
@Composable
fun AddMemberScreen(
    onBack: () -> Unit,
    viewModel: AddMemberViewModel = hiltViewModel(),
) {
    val isLoading    by viewModel.isLoading.collectAsState()
    val searchQuery  by viewModel.searchQuery.collectAsState()
    val actionState  by viewModel.actionState.collectAsState()
    val addingIds    by viewModel.addingIds.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var errorDialog by remember { mutableStateOf<FsErrorDialogState?>(null) }

    val filtered by remember { derivedStateOf { viewModel.filteredFriends() } }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is AddMemberActionState.Success -> {
                snackbarHost.showSnackbar("${s.name} added to group")
                viewModel.resetActionState()
            }
            is AddMemberActionState.Error -> {
                errorDialog = apiErrorDialogState(s.message)
                viewModel.resetActionState()
            }
            else -> Unit
        }
    }


    errorDialog?.let { err ->
        FsErrorDialog(
            title     = err.title,
            message   = err.message,
            onDismiss = { errorDialog = null },
        )
    }
    Scaffold(
        containerColor = Surface0,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = { FsTopBar(title = "Add members", onBack = onBack) },
    ) { innerPadding ->

        if (isLoading) {
            FsLoadingScreen()
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Search bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface2)
                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Search, null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchChanged(it) },
                    textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                    singleLine = true,
                    cursorBrush = SolidColor(Green400),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (searchQuery.isBlank()) {
                                Text("Search friends", fontSize = 15.sp, color = TextTertiary)
                            }
                            inner()
                        }
                    },
                )
            }

            HorizontalDivider(color = Surface3, thickness = 0.5.dp)

            // ── Friends list ──────────────────────────────────────────────────
            if (filtered.isEmpty()) {
                FsEmptyState(
                    title = if (searchQuery.isBlank()) "No friends yet"
                    else "No friends match \"$searchQuery\"",
                    subtitle = if (searchQuery.isBlank()) "Add friends first, then you can add them to this group"
                    else "Try a different search",
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    // Non-members header
                    val nonMembers = filtered.filter { !it.second }
                    val alreadyMembers = filtered.filter { it.second }

                    if (nonMembers.isNotEmpty()) {
                        item {
                            SectionHeader("Friends")
                        }
                        items(
                            items = nonMembers,
                            key = { it.first.id },
                        ) { (friend, _) ->
                            val isAdding = addingIds.contains(friend.id)
                            FriendRow(
                                friend = friend,
                                isInGroup = false,
                                isAdding = isAdding,
                                onClick = { if (!isAdding) viewModel.addMember(friend.id) },
                            )
                            HorizontalDivider(
                                color = Surface3, thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 72.dp),
                            )
                        }
                    }

                    if (alreadyMembers.isNotEmpty()) {
                        item {
                            SectionHeader("Already in group")
                        }
                        items(
                            items = alreadyMembers,
                            key = { it.first.id },
                        ) { (friend, _) ->
                            FriendRow(
                                friend = friend,
                                isInGroup = true,
                                isAdding = false,
                                onClick = {},
                            )
                            HorizontalDivider(
                                color = Surface3, thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 72.dp),
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(Spacing.xxxl)) }
                }
            }
        }
    }
}

// ── Section Header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextTertiary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(
            horizontal = Spacing.lg,
            vertical = Spacing.md,
        ),
    )
}

// ── Friend Row ────────────────────────────────────────────────────────────────

@Composable
private fun FriendRow(
    friend: Friend,
    isInGroup: Boolean,
    isAdding: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isInGroup && !isAdding) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FsAvatar(
            name = friend.fullName,
            userId = friend.id,
            size = ComponentSize.avatarMd,
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.fullName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isInGroup) TextSecondary else TextPrimary,
            )
            if (friend.email.isNotBlank()) {
                Text(
                    text = friend.email,
                    fontSize = 13.sp,
                    color = TextTertiary,
                )
            }
        }
        Spacer(modifier = Modifier.width(Spacing.md))

        when {
            isAdding -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Green400,
                    strokeWidth = 2.dp,
                )
            }
            isInGroup -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(Radius.full))
                        .background(Green400.copy(alpha = 0.15f)),
                ) {
                    Icon(
                        Icons.Outlined.Check, "Already added",
                        tint = Green400,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            else -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(Radius.full))
                        .background(Green400),
                ) {
                    Icon(
                        Icons.Outlined.PersonAdd, "Add",
                        tint = Surface0,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}