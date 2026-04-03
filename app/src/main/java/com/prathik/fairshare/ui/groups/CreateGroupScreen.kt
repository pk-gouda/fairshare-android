package com.prathik.fairshare.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.GroupType
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateGroupScreen(
    onBack           : () -> Unit,
    onGroupCreated   : (String) -> Unit,
    viewModel        : CreateGroupViewModel = hiltViewModel(),
) {
    val name         by viewModel.name.collectAsState()
    val description  by viewModel.description.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val actionState  by viewModel.actionState.collectAsState()
    val nameError    by viewModel.nameError.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

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
        topBar         = { FsTopBar(title = "New Group", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(Spacing.lg))

            // ── Group name ────────────────────────────────────────────────────
            FsTextField(
                value         = name,
                onValueChange = { viewModel.onNameChanged(it) },
                label         = "Group name",
                placeholder   = "e.g. Goa Trip, Apartment 4B",
                error         = nameError,
                imeAction     = ImeAction.Next,
                modifier      = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Description ───────────────────────────────────────────────────
            FsTextField(
                value         = description,
                onValueChange = { viewModel.onDescriptionChanged(it) },
                label         = "Description",
                placeholder   = "Optional",
                imeAction     = ImeAction.Done,
                singleLine    = false,
                maxLines      = 3,
                modifier      = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Group type ────────────────────────────────────────────────────
            Text(
                text          = "GROUP TYPE",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.SemiBold,
                color         = TextTertiary,
                letterSpacing = 1.sp,
                modifier      = Modifier.padding(start = 2.dp, bottom = Spacing.sm),
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement   = Arrangement.spacedBy(Spacing.sm),
                modifier              = Modifier.fillMaxWidth(),
            ) {
                GroupType.values().forEach { type ->
                    GroupTypeChip(
                        type       = type,
                        isSelected = type == selectedType,
                        onClick    = { viewModel.onTypeSelected(type) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxxl))

            // ── Create button ─────────────────────────────────────────────────
            FsPrimaryButton(
                text      = "Create group",
                onClick   = { viewModel.createGroup() },
                isLoading = isLoading,
                enabled   = name.isNotBlank(),
                modifier  = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }
    }
}

// ── Group Type Chip ───────────────────────────────────────────────────────────

@Composable
private fun GroupTypeChip(
    type      : GroupType,
    isSelected: Boolean,
    onClick   : () -> Unit,
) {
    val emoji = groupTypeEmoji(type)
    val label = groupTypeLabel(type)

    Row(
        modifier          = Modifier
            .clip(RoundedCornerShape(Radius.xl))
            .background(if (isSelected) Green400.copy(alpha = 0.15f) else Surface2)
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) Green400 else Surface3,
                shape = RoundedCornerShape(Radius.xl),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(text = emoji, fontSize = 16.sp)
        Text(
            text       = label,
            fontSize   = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (isSelected) Green400 else TextSecondary,
        )
    }
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