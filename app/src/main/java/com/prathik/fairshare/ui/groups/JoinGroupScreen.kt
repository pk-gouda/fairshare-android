package com.prathik.fairshare.ui.groups

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupScreen(
    onBack   : () -> Unit,
    onSuccess: () -> Unit,          // navigates to Groups tab
    viewModel: JoinGroupViewModel = hiltViewModel(),
) {
    val inviteCode by viewModel.inviteCode.collectAsState()
    val uiState    by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val isLoading  = uiState is JoinGroupUiState.Loading

    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is JoinGroupUiState.Success       -> { onSuccess(); viewModel.resetUiState() }
            is JoinGroupUiState.AlreadyMember -> {
                snackbarHost.showSnackbar("You're already a member of this group.")
                viewModel.resetUiState()
            }
            is JoinGroupUiState.Error -> {
                snackbarHost.showSnackbar(s.message)
                viewModel.resetUiState()
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = Surface0,
        topBar         = { FsTopBar(title = "Join a group", onBack = onBack) },
        snackbarHost   = { SnackbarHost(snackbarHost) },
    ) { innerPadding ->
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(Spacing.xxxl))

            // ── Icon ──────────────────────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Green400.copy(alpha = 0.12f)),
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Groups,
                    contentDescription = null,
                    tint               = Green400,
                    modifier           = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            Text(
                text       = "Enter invite code",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text      = "Ask the group admin to share their invite code or link.",
                fontSize  = 14.sp,
                color     = TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))

            // ── Code input ────────────────────────────────────────────────────
            FsTextField(
                value         = inviteCode,
                onValueChange = { viewModel.onInviteCodeChanged(it) },
                label         = "Invite code",
                placeholder   = "e.g. A3F9B2C1",
                imeAction     = ImeAction.Go,
                keyboardActions = KeyboardActions(onGo = { viewModel.joinGroup() }),
                modifier      = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text     = "Codes are not case-sensitive",
                fontSize = 12.sp,
                color    = TextTertiary,
                modifier = Modifier.align(Alignment.Start),
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))

            // ── Join button ───────────────────────────────────────────────────
            FsPrimaryButton(
                text      = "Join group",
                onClick   = { viewModel.joinGroup() },
                enabled   = inviteCode.isNotBlank(),
                isLoading = isLoading,
                modifier  = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))

            // ── How it works card ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface2)
                    .padding(Spacing.md),
            ) {
                Text(
                    text       = "How to get an invite code",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                listOf(
                    "Ask the group admin to open Group Settings",
                    "They'll find the invite link under \"Invite Link\"",
                    "They can copy the code or share the link directly",
                ).forEachIndexed { i, step ->
                    Row(
                        modifier              = Modifier.padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment     = Alignment.Top,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Surface4),
                        ) {
                            Text(
                                text     = "${i + 1}",
                                fontSize = 10.sp,
                                color    = TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text(step, fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }
    }
}