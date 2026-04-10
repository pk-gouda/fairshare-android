package com.prathik.fairshare.ui.groups

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.GroupType
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.AvatarColors
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
import com.prathik.fairshare.ui.theme.TileCoupleEnd
import com.prathik.fairshare.ui.theme.TileCoupleStart
import com.prathik.fairshare.ui.theme.TileEventEnd
import com.prathik.fairshare.ui.theme.TileEventStart
import com.prathik.fairshare.ui.theme.TileFriendsEnd
import com.prathik.fairshare.ui.theme.TileFriendsStart
import com.prathik.fairshare.ui.theme.TileHomeEnd
import com.prathik.fairshare.ui.theme.TileHomeStart
import com.prathik.fairshare.ui.theme.TileOfficeEnd
import com.prathik.fairshare.ui.theme.TileOfficeStart
import com.prathik.fairshare.ui.theme.TileOtherEnd
import com.prathik.fairshare.ui.theme.TileOtherStart
import com.prathik.fairshare.ui.theme.TileTripEnd
import com.prathik.fairshare.ui.theme.TileTripStart

// Primary group types shown in the collapsed grid (first 4)
private val primaryTypes = listOf(
    GroupType.TRIP,
    GroupType.HOME,
    GroupType.OFFICE,
    GroupType.EVENT,
)

// All 8 types shown when expanded
private val allTypes = listOf(
    GroupType.TRIP,
    GroupType.HOME,
    GroupType.OFFICE,
    GroupType.EVENT,
    GroupType.FRIENDS,
    GroupType.COUPLE,
    GroupType.APARTMENT,
    GroupType.OTHER,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onBack         : () -> Unit,
    onGroupCreated : (String) -> Unit,
    viewModel      : CreateGroupViewModel = hiltViewModel(),
) {
    val step              by viewModel.step.collectAsState()
    val name              by viewModel.name.collectAsState()
    val description       by viewModel.description.collectAsState()
    val selectedType      by viewModel.selectedType.collectAsState()
    val showAllTypes      by viewModel.showAllTypes.collectAsState()
    val nameError         by viewModel.nameError.collectAsState()
    val isLoading         by viewModel.isLoading.collectAsState()
    val actionState       by viewModel.actionState.collectAsState()
    val createdGroup      by viewModel.createdGroup.collectAsState()
    val friends           by viewModel.friends.collectAsState()
    val friendSearchQuery by viewModel.friendSearchQuery.collectAsState()
    val selectedFriendIds by viewModel.selectedFriendIds.collectAsState()
    val friendsLoading    by viewModel.friendsLoading.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is CreateGroupActionState.Success -> onGroupCreated(s.groupId)
            is CreateGroupActionState.Error   -> {
                snackbarHost.showSnackbar(s.message)
                viewModel.resetActionState()
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar = {
            if (step == 1) {
                FsTopBar(title = "New Group", onBack = onBack)
            } else {
                FsTopBar(
                    title = "Add Members",
                    onBack = { /* go back to step 1 — but group already created, just skip */ viewModel.skipMembers() },
                    actions = {
                        TextButton(onClick = { viewModel.skipMembers() }) {
                            Text("Skip", color = Color(0xFF4A6FE8), fontSize = 15.sp)
                        }
                    },
                )
            }
        },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Progress bar ──────────────────────────────────────────────────
            StepProgressBar(
                currentStep = step,
                totalSteps  = 2,
                modifier    = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            )

            if (step == 1) {
                Step1Content(
                    name          = name,
                    description   = description,
                    selectedType  = selectedType,
                    showAllTypes  = showAllTypes,
                    nameError     = nameError,
                    isLoading     = isLoading,
                    onNameChanged = { viewModel.onNameChanged(it) },
                    onDescChanged = { viewModel.onDescriptionChanged(it) },
                    onTypeSelected = { viewModel.onTypeSelected(it) },
                    onToggleTypes = { viewModel.toggleShowAllTypes() },
                    onContinue    = { viewModel.proceed() },
                )
            } else {
                Step2Content(
                    groupName         = createdGroup?.name ?: name,
                    memberCount       = selectedFriendIds.size,
                    friends           = viewModel.filteredFriends(),
                    searchQuery       = friendSearchQuery,
                    selectedFriendIds = selectedFriendIds,
                    friendsLoading    = friendsLoading,
                    isLoading         = isLoading,
                    onSearchChanged   = { viewModel.onFriendSearchChanged(it) },
                    onToggleFriend    = { viewModel.toggleFriend(it) },
                    onShareInvite     = {
                        val inviteCode = createdGroup?.inviteCode ?: return@Step2Content
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join my group on FairShare! Use invite code: $inviteCode"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share invite link"))
                    },
                    onFinish = { viewModel.finishWithMembers() },
                )
            }
        }
    }
}

// ── Step 1: Group Details ─────────────────────────────────────────────────────

@Composable
private fun Step1Content(
    name          : String,
    description   : String,
    selectedType  : GroupType,
    showAllTypes  : Boolean,
    nameError     : String?,
    isLoading     : Boolean,
    onNameChanged : (String) -> Unit,
    onDescChanged : (String) -> Unit,
    onTypeSelected: (GroupType) -> Unit,
    onToggleTypes : () -> Unit,
    onContinue    : () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg),
    ) {
        Text(
            text  = "Step 1 of 2: Group details",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = Spacing.xl),
        )

        // ── Photo picker ──────────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.CenterHorizontally)
                .drawBehind {
                    drawRoundRect(
                        color       = TextTertiary.copy(alpha = 0.4f),
                        style       = Stroke(
                            width      = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(10f, 8f), 0f
                            ),
                        ),
                        cornerRadius = CornerRadius(Radius.xl.toPx()),
                    )
                },
        ) {
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = "Add photo",
                tint   = Color(0xFF4A6FE8),
                modifier = Modifier.size(32.dp),
            )
            // + badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF4A6FE8)),
            ) {
                Text("+", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text     = "Add group photo",
            fontSize = 12.sp,
            color    = TextSecondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = Spacing.sm, bottom = Spacing.xl),
        )

        // ── Group name ────────────────────────────────────────────────────────
        CountedField(
            label       = "GROUP NAME",
            value       = name,
            maxLength   = 25,
            placeholder = "e.g. Goa Trip 2026",
            error       = nameError,
            singleLine  = true,
            imeAction   = ImeAction.Next,
            onValueChange = onNameChanged,
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        // ── Description ───────────────────────────────────────────────────────
        CountedField(
            label       = "DESCRIPTION",
            labelSuffix = " (optional)",
            value       = description,
            maxLength   = 100,
            placeholder = "What's this group for?",
            singleLine  = false,
            minHeight   = 90.dp,
            imeAction   = ImeAction.Default,
            onValueChange = onDescChanged,
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        // ── Group type ────────────────────────────────────────────────────────
        Text(
            text          = "GROUP TYPE",
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = TextTertiary,
            letterSpacing = 1.sp,
            modifier      = Modifier.padding(bottom = Spacing.sm),
        )

        val typesToShow = if (showAllTypes) allTypes else primaryTypes

        // 2-column grid
        typesToShow.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier              = Modifier.fillMaxWidth(),
            ) {
                row.forEach { type ->
                    GroupTypeCard(
                        type       = type,
                        isSelected = type == selectedType,
                        onClick    = { onTypeSelected(type) },
                        modifier   = Modifier.weight(1f),
                    )
                }
                // Pad last row if odd number
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        // Show more / less link
        TextButton(
            onClick  = onToggleTypes,
            modifier = Modifier.padding(top = 0.dp, bottom = Spacing.sm),
        ) {
            Text(
                text     = if (showAllTypes) "Show fewer types" else "Show more types",
                fontSize = 14.sp,
                color    = Green400,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        FsPrimaryButton(
            text      = "Continue",
            onClick   = onContinue,
            isLoading = isLoading,
            enabled   = name.isNotBlank(),
            modifier  = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Spacing.xxxl))
    }
}

// ── Step 2: Add Members ───────────────────────────────────────────────────────

@Composable
private fun Step2Content(
    groupName        : String,
    memberCount      : Int,
    friends          : List<Friend>,
    searchQuery      : String,
    selectedFriendIds: Set<String>,
    friendsLoading   : Boolean,
    isLoading        : Boolean,
    onSearchChanged  : (String) -> Unit,
    onToggleFriend   : (String) -> Unit,
    onShareInvite    : () -> Unit,
    onFinish         : () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {

        Text(
            text     = "Step 2 of 2: Add friends",
            fontSize = 13.sp,
            color    = TextSecondary,
            modifier = Modifier.padding(horizontal = Spacing.lg).padding(bottom = Spacing.md),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Group summary card ────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface2)
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Group initials avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(ComponentSize.avatarLg)
                        .clip(RoundedCornerShape(Radius.lg))
                        .background(groupInitialsColor(groupName)),
                ) {
                    Text(
                        text       = groupInitials(groupName),
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.md))
                Column {
                    Text(groupName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("$memberCount member${if (memberCount != 1) "s" else ""}", fontSize = 12.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Search bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface2)
                    .padding(horizontal = Spacing.md, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint     = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                BasicTextField(
                    value         = searchQuery,
                    onValueChange = onSearchChanged,
                    singleLine    = true,
                    textStyle     = TextStyle(fontSize = 15.sp, color = TextPrimary),
                    cursorBrush   = SolidColor(Green400),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text("Search friends or enter email", fontSize = 15.sp, color = TextTertiary)
                        }
                        inner()
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // ── Suggested label ───────────────────────────────────────────────
            if (friends.isNotEmpty() || friendsLoading) {
                Text(
                    text          = "SUGGESTED",
                    fontSize      = 11.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = TextTertiary,
                    letterSpacing = 1.sp,
                    modifier      = Modifier.padding(horizontal = Spacing.lg).padding(bottom = Spacing.sm),
                )
            }

            if (friendsLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xl),
                ) {
                    CircularProgressIndicator(color = Green400, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            } else {
                // Friend rows
                friends.forEach { friend ->
                    val isSelected = friend.id in selectedFriendIds
                    FriendSelectRow(
                        friend     = friend,
                        isSelected = isSelected,
                        onClick    = { onToggleFriend(friend.id) },
                    )
                    HorizontalDivider(
                        color     = Surface3,
                        thickness = 0.5.dp,
                        modifier  = Modifier.padding(start = Spacing.lg + ComponentSize.avatarLg + Spacing.md),
                    )
                }

                // Share invite link row
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onShareInvite)
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(ComponentSize.avatarLg)
                            .clip(RoundedCornerShape(Radius.lg))
                            .background(Color(0xFF4A6FE8).copy(alpha = 0.15f)),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            tint     = Color(0xFF4A6FE8),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column {
                        Text("Share invite link", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text("Anyone with the link can join", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // ── CTA ───────────────────────────────────────────────────────────────
        Column(
            modifier          = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FsPrimaryButton(
                text = when {
                    memberCount == 0 -> "Create group"
                    memberCount == 1 -> "Create group with 1 member"
                    else             -> "Create group with $memberCount members"
                },
                onClick   = onFinish,
                isLoading = isLoading,
                modifier  = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text     = "You can add more people later",
                fontSize = 12.sp,
                color    = TextTertiary,
            )
        }
    }
}

// ── Step progress bar ─────────────────────────────────────────────────────────

@Composable
private fun StepProgressBar(
    currentStep: Int,
    totalSteps : Int,
    modifier   : Modifier = Modifier,
) {
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        repeat(totalSteps) { index ->
            val filled = index < currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(Radius.full))
                    .background(if (filled) Green400 else Surface4),
            )
        }
    }
}

// ── Group type card (2-column grid style) ─────────────────────────────────────

@Composable
private fun GroupTypeCard(
    type      : GroupType,
    isSelected: Boolean,
    onClick   : () -> Unit,
    modifier  : Modifier = Modifier,
) {
    val gradient = groupTypeGradient(type)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .then(
                if (isSelected) Modifier.border(1.5.dp, Green400, RoundedCornerShape(Radius.xl))
                else Modifier.border(0.5.dp, Surface4, RoundedCornerShape(Radius.xl))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Gradient tile with emoji
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(gradient)
                ),
        ) {
            Text(groupTypeEmoji(type), fontSize = 18.sp)
        }
        Text(
            text       = groupTypeLabel(type),
            fontSize   = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (isSelected) Green400 else TextPrimary,
            modifier   = Modifier.weight(1f),
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint     = Green400,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ── Friend select row ─────────────────────────────────────────────────────────

@Composable
private fun FriendSelectRow(
    friend    : Friend,
    isSelected: Boolean,
    onClick   : () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FsAvatar(
            name   = friend.fullName,
            userId = friend.id,
            size   = ComponentSize.avatarLg,
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(friend.fullName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(friend.email, fontSize = 12.sp, color = TextSecondary)
        }
        // +/✓ toggle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) Green400 else Surface4
                ),
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Selected",
                    tint     = Surface0,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Text("+", fontSize = 18.sp, color = TextSecondary, fontWeight = FontWeight.Light)
            }
        }
    }
}

// ── Counted text field ────────────────────────────────────────────────────────

@Composable
private fun CountedField(
    label       : String,
    value       : String,
    maxLength   : Int,
    placeholder : String,
    onValueChange: (String) -> Unit,
    labelSuffix : String = "",
    error       : String? = null,
    singleLine  : Boolean = true,
    minHeight   : androidx.compose.ui.unit.Dp = 0.dp,
    imeAction   : ImeAction = ImeAction.Default,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
    ) {
        Row {
            Text(
                text       = label,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (error != null) androidx.compose.ui.graphics.Color(0xFFFF6B35) else TextTertiary,
                letterSpacing = 0.8.sp,
            )
            if (labelSuffix.isNotEmpty()) {
                Text(
                    text     = labelSuffix,
                    fontSize = 11.sp,
                    color    = TextTertiary,
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Box(modifier = Modifier.fillMaxWidth().then(if (minHeight > 0.dp) Modifier.height(minHeight) else Modifier)) {
            BasicTextField(
                value         = value,
                onValueChange = onValueChange,
                singleLine    = singleLine,
                textStyle     = TextStyle(fontSize = 16.sp, color = TextPrimary),
                cursorBrush   = SolidColor(Green400),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = imeAction,
                ),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 16.sp, color = TextTertiary)
                    }
                    inner()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (error != null) {
                Text(error, fontSize = 11.sp, color = androidx.compose.ui.graphics.Color(0xFFFF6B35), modifier = Modifier.weight(1f))
            }
            Text(
                text     = "${value.length}/$maxLength",
                fontSize = 11.sp,
                color    = if (value.length == maxLength) androidx.compose.ui.graphics.Color(0xFFFF6B35) else TextTertiary,
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun groupInitials(name: String): String {
    val words = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "G"
        words.size == 1 -> words[0].take(2).uppercase()
        else            -> "${words[0][0]}${words[1][0]}".uppercase()
    }
}

private fun groupInitialsColor(name: String): Color {
    return AvatarColors[name.hashCode().and(0x7FFFFFFF) % AvatarColors.size]
}

private fun groupTypeGradient(type: GroupType): List<Color> = when (type) {
    GroupType.HOME      -> listOf(TileHomeStart, TileHomeEnd)
    GroupType.TRIP      -> listOf(TileTripStart, TileTripEnd)
    GroupType.COUPLE    -> listOf(TileCoupleStart, TileCoupleEnd)
    GroupType.OFFICE    -> listOf(TileOfficeStart, TileOfficeEnd)
    GroupType.FRIENDS   -> listOf(TileFriendsStart, TileFriendsEnd)
    GroupType.EVENT     -> listOf(TileEventStart, TileEventEnd)
    GroupType.APARTMENT -> listOf(TileFriendsStart, TileFriendsEnd)
    GroupType.OTHER     -> listOf(TileOtherStart, TileOtherEnd)
}

private fun groupTypeEmoji(type: GroupType): String = when (type) {
    GroupType.TRIP      -> "✈️"
    GroupType.HOME      -> "🏠"
    GroupType.COUPLE    -> "💑"
    GroupType.APARTMENT -> "🏢"
    GroupType.OFFICE    -> "💼"
    GroupType.FRIENDS   -> "👥"
    GroupType.EVENT     -> "🎉"
    GroupType.OTHER     -> "📦"
}

private fun groupTypeLabel(type: GroupType): String = when (type) {
    GroupType.TRIP      -> "Trip"
    GroupType.HOME      -> "Home"
    GroupType.COUPLE    -> "Couple"
    GroupType.APARTMENT -> "Apartment"
    GroupType.OFFICE    -> "Office"
    GroupType.FRIENDS   -> "Friends"
    GroupType.EVENT     -> "Event"
    GroupType.OTHER     -> "Other"
}