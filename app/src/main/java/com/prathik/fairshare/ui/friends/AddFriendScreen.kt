package com.prathik.fairshare.ui.friends

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.ComponentSize
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddFriendScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: AddFriendViewModel = hiltViewModel(),
) {
    val screen by viewModel.screen.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val noResultFound by viewModel.noResultFound.collectAsState()
    val selectedPeople by viewModel.selectedPeople.collectAsState()
    val inviteName by viewModel.inviteName.collectAsState()
    val inviteContact by viewModel.inviteEmailOrPhone.collectAsState()
    val inviteMode by viewModel.inviteMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    var showAddNewSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is AddFriendActionState.Error -> {
                snackbarHost.showSnackbar(s.message); viewModel.resetActionState()
            }

            else -> Unit
        }
    }

    // "Add someone new" bottom sheet
    if (showAddNewSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddNewSheet = false },
            sheetState = sheetState,
            containerColor = Surface2,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.xxxl),
            ) {
                Text(
                    text = "Add someone new",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = Spacing.lg),
                )
                // Option 1 — Invite by email/phone
                SheetOption(
                    emoji = "✉️",
                    title = "Invite by phone or email",
                    subtitle = "Send them an invite link",
                    onClick = {
                        showAddNewSheet = false
                        viewModel.showInviteForm(mode = InviteMode.Invite)
                    },
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                // Option 2 — Placeholder
                SheetOption(
                    emoji = "👤",
                    title = "Placeholder only",
                    subtitle = "Add without an account — assign expenses to them",
                    onClick = {
                        showAddNewSheet = false
                        viewModel.showInviteForm(mode = InviteMode.Placeholder)
                    },
                )
            }
        }
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            FsTopBar(
                title = when (screen) {
                    AddFriendScreen.Search -> "Add friends"
                    AddFriendScreen.InviteForm -> if (inviteMode == InviteMode.Placeholder) "Add placeholder" else "Invite a friend"
                    AddFriendScreen.Review -> "Review"
                },
                onBack = when (screen) {
                    AddFriendScreen.Search -> onBack
                    AddFriendScreen.InviteForm -> {
                        { viewModel.cancelInviteForm() }
                    }

                    AddFriendScreen.Review -> {
                        { viewModel.backToSearch() }
                    }
                },
            )
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                } else {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                }
            },
            label = "add_friend_screen",
        ) { currentScreen ->
            when (currentScreen) {

                // ── Search screen ─────────────────────────────────────────────
                AddFriendScreen.Search -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        // Search bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                                .clip(RoundedCornerShape(Radius.lg))
                                .background(Surface2)
                                .padding(horizontal = Spacing.md, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = Green400,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchChanged(it) },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary),
                                cursorBrush = SolidColor(Green400),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Search,
                                ),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Enter email or phone #",
                                            fontSize = 15.sp,
                                            color = TextTertiary
                                        )
                                    }
                                    inner()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                            )
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Green400,
                                    strokeWidth = 2.dp,
                                )
                            }
                        }

                        // Selected people chips
                        if (selectedPeople.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.lg)
                                    .padding(bottom = Spacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                selectedPeople.forEach { person ->
                                    PersonChip(
                                        name = person.displayName,
                                        onRemove = { viewModel.removePerson(person.id) },
                                    )
                                }
                            }
                        }

                        // Search result
                        if (searchResult != null) {
                            val user = searchResult!!
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectExistingUser(user) }
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                FsAvatar(
                                    name = user.fullName,
                                    userId = user.id,
                                    size = ComponentSize.avatarLg
                                )
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.fullName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                    Text(text = user.email, fontSize = 12.sp, color = TextTertiary)
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(Radius.lg))
                                        .background(Green400)
                                        .padding(horizontal = Spacing.md, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = "Add",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Surface0
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = Surface3, thickness = 0.5.dp,
                                modifier = Modifier.padding(start = Spacing.lg + 56.dp)
                            )
                        }

                        // No result hint
                        if (noResultFound && searchQuery.isNotBlank()) {
                            Text(
                                text = "No account found for \"$searchQuery\"",
                                fontSize = 13.sp,
                                color = TextTertiary,
                                modifier = Modifier.padding(
                                    horizontal = Spacing.lg,
                                    vertical = Spacing.sm
                                ),
                            )
                        }

                        // Add someone new
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddNewSheet = true }
                                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(ComponentSize.avatarLg)
                                    .clip(CircleShape)
                                    .background(Surface2),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PersonAdd,
                                    contentDescription = null,
                                    tint = Green400,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                text = "Add someone new",
                                fontSize = 15.sp,
                                color = Green400,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Next button — only when someone is selected
                        if (selectedPeople.isNotEmpty()) {
                            FsPrimaryButton(
                                text = "Next",
                                onClick = { viewModel.goToReview() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.lg)
                                    .padding(bottom = Spacing.lg),
                            )
                        }
                    }
                }

                // ── Invite form ───────────────────────────────────────────────
                AddFriendScreen.InviteForm -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = Spacing.lg)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Spacer(modifier = Modifier.height(Spacing.lg))

                        FsTextField(
                            value = inviteName,
                            onValueChange = { viewModel.onInviteNameChanged(it) },
                            label = "Name",
                            placeholder = "Their name",
                            imeAction = if (inviteMode == InviteMode.Placeholder) ImeAction.Done else ImeAction.Next,
                            keyboardActions = KeyboardActions(onDone = { if (inviteMode == InviteMode.Placeholder) viewModel.confirmInvitePerson() }),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Placeholder disclaimer
                        if (inviteMode == InviteMode.Placeholder) {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(Radius.lg))
                                    .background(Surface2)
                                    .padding(Spacing.md),
                            ) {
                                Text(
                                    text = "About placeholders",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                                Text(
                                    text = "• They won't have an account or see any debts\n• You can assign expenses to them\n• If they join FairShare later, you can link their account\n• Useful for tracking what someone owes offline",
                                    fontSize = 12.sp,
                                    color = TextTertiary,
                                    lineHeight = 18.sp,
                                )
                                Spacer(modifier = Modifier.height(Spacing.sm))
                                androidx.compose.material3.TextButton(
                                    onClick = { viewModel.switchToInviteMode() },
                                    modifier = Modifier.align(Alignment.End),
                                ) {
                                    Text(
                                        text = "Invite instead →",
                                        fontSize = 12.sp,
                                        color = Green400
                                    )
                                }
                            }
                        }

                        // Email/phone — only for invite mode
                        if (inviteMode == InviteMode.Invite) {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            FsTextField(
                                value = inviteContact,
                                onValueChange = { viewModel.onInviteEmailOrPhoneChanged(it) },
                                label = "Email or phone number",
                                placeholder = "email@example.com",
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Done,
                                keyboardActions = KeyboardActions(onDone = { viewModel.confirmInvitePerson() }),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.height(Spacing.xxxl))

                        FsPrimaryButton(
                            text = "Next",
                            onClick = { viewModel.confirmInvitePerson() },
                            enabled = inviteName.isNotBlank() && (inviteMode == InviteMode.Placeholder || inviteContact.isNotBlank()),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(Spacing.lg))
                    }
                }

                // ── Review screen ─────────────────────────────────────────────
                AddFriendScreen.Review -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            selectedPeople.forEachIndexed { index, person ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    FsAvatar(
                                        name = person.displayName,
                                        userId = person.id,
                                        size = ComponentSize.avatarLg,
                                    )
                                    Spacer(modifier = Modifier.width(Spacing.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = person.displayName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = person.emailOrPhone,
                                            fontSize = 12.sp,
                                            color = TextTertiary
                                        )
                                    }
                                    androidx.compose.material3.TextButton(
                                        onClick = { viewModel.showInviteForm(person) },
                                    ) {
                                        Text(text = "Edit", fontSize = 13.sp, color = Green400)
                                    }
                                }
                                if (index < selectedPeople.lastIndex) {
                                    HorizontalDivider(
                                        color = Surface3, thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = Spacing.lg + 56.dp)
                                    )
                                }
                            }
                        }

                        FsPrimaryButton(
                            text = if (selectedPeople.size == 1) "Add friend" else "Add ${selectedPeople.size} friends",
                            onClick = { viewModel.submitAll { onDone() } },
                            isLoading = isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg)
                                .padding(bottom = Spacing.lg),
                        )
                    }
                }
            }
        }
    }
}

// ── Reusable components ───────────────────────────────────────────────────────

@Composable
private fun PersonChip(name: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Green400.copy(alpha = 0.15f))
            .padding(start = Spacing.sm, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = name, fontSize = 13.sp, color = Green400, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Green400.copy(alpha = 0.3f))
                .clickable(onClick = onRemove),
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove",
                tint = Green400,
                modifier = Modifier.size(10.dp),
            )
        }
    }
}

@Composable
private fun SheetOption(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface0)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = emoji, fontSize = 22.sp, modifier = Modifier
            .size(44.dp)
            .padding(Spacing.sm))
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(text = subtitle, fontSize = 12.sp, color = TextTertiary)
        }
        Text(text = "›", fontSize = 18.sp, color = TextTertiary)
    }
}