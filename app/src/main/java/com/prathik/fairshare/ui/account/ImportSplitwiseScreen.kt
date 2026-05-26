package com.prathik.fairshare.ui.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsSecondaryButton
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.ComponentSize
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import androidx.compose.material3.Icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSplitwiseScreen(
    onBack          : () -> Unit,
    onGroupImported : (groupId: String) -> Unit,
    viewModel       : ImportSplitwiseViewModel = hiltViewModel(),
) {
    val context         = LocalContext.current
    val uiState         by viewModel.uiState.collectAsState()
    val friends         by viewModel.friends.collectAsState()
    val claimState      by viewModel.claimState.collectAsState()
    val snackbarHost    = remember { SnackbarHostState() }

    // Import type selection
    var importType          by remember { mutableStateOf<ImportType?>(null) }
    var groupName           by remember { mutableStateOf("") }
    var showGroupNameDialog by remember { mutableStateOf(false) }
    var showGroupTypeDialog       by remember { mutableStateOf(false) }
    var selectedGroupType         by remember { mutableStateOf("OTHER") }
    var showWhoAreYouDialog       by remember { mutableStateOf(false) }
    var showFriendWhoAreYouDialog by remember { mutableStateOf(false) }
    var pendingCsv                by remember { mutableStateOf<String?>(null) }
    var pendingFriendCsv          by remember { mutableStateOf<String?>(null) }
    var showLinkFriendSheet       by remember { mutableStateOf(false) }
    var linkPlaceholderUserId     by remember { mutableStateOf<String?>(null) }
    var linkCsvName               by remember { mutableStateOf<String?>(null) }
    val csvMemberNames      by viewModel.csvMemberNames.collectAsState()
    var csvMismatch         by remember { mutableStateOf<CsvMismatch?>(null) }

    // ── Confirmation dialog state ─────────────────────────────────────────────
    // Set to the selected CSV name (or "" for "none of these") when the user
    // taps a name in "Which one is you?". The confirm dialog appears on top of
    // the still-open bottom sheet. Cancelling only closes the dialog — the sheet
    // stays visible so the user can choose a different name.
    var pendingGroupImporterNameForConfirm  by remember { mutableStateOf<String?>(null) }
    var showGroupConfirmDialog              by remember { mutableStateOf(false) }
    var pendingFriendImporterNameForConfirm by remember { mutableStateOf<String?>(null) }
    var showFriendConfirmDialog             by remember { mutableStateOf(false) }

    // Resets all group import draft state. Call on any cancel/dismiss in the import flow.
    val resetGroupImportDraft = {
        pendingCsv = null
        groupName = ""
        selectedGroupType = "OTHER"
        showGroupNameDialog = false
        showGroupTypeDialog = false
        showWhoAreYouDialog = false
        csvMismatch = null
        viewModel.clearCsvNames()
    }

    // Assign sheet
    var showAssignSheet by remember { mutableStateOf(false) }
    var selectedMember  by remember { mutableStateOf<GroupMember?>(null) }

    // File pickers — read CSV immediately while URI permission is still valid
    val groupFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val csv = try {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()?.replace("\r\n", "\n")?.replace("\r", "\n")
            } catch (e: Exception) { null }
            if (csv != null) {
                pendingCsv = csv
                viewModel.parseCsvNames(csv)
                val personCount = viewModel.csvMemberNames.value.size
                if (personCount in 1..2) {
                    csvMismatch = CsvMismatch(
                        title       = "This looks like a friend CSV",
                        message     = "This file only has $personCount ${if (personCount == 1) "person" else "people"} — it looks like a direct friend export, not a group. Did you mean to import friend expenses instead?",
                        switchLabel = "Import as friend expenses",
                        onSwitch    = {
                            csvMismatch = null
                            pendingFriendCsv = csv
                            pendingCsv = null
                            showFriendWhoAreYouDialog = true
                        },
                        onContinue  = {
                            csvMismatch = null
                            showGroupNameDialog = true
                        },
                    )
                } else {
                    showGroupNameDialog = true
                }
            } else {
                viewModel.setError("Could not read CSV file. Try a different file manager app.")
            }
        }
    }
    val friendFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val csv = try {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()?.replace("\r\n", "\n")?.replace("\r", "\n")
            } catch (e: Exception) { null }
            if (csv != null) {
                pendingFriendCsv = csv
                viewModel.parseCsvNames(csv)
                val personCount = viewModel.csvMemberNames.value.size
                if (personCount >= 3) {
                    csvMismatch = CsvMismatch(
                        title       = "This looks like a group CSV",
                        message     = "This file has $personCount people — it looks like a group export. Friend imports only support 2-person exports and will produce incorrect results with this file.",
                        switchLabel = "Import as a group instead",
                        onSwitch    = {
                            csvMismatch = null
                            pendingCsv = csv
                            pendingFriendCsv = null
                            showGroupNameDialog = true
                        },
                        onContinue  = { csvMismatch = null },
                        isHardBlock = true,
                    )
                } else {
                    showFriendWhoAreYouDialog = true
                }
            } else {
                viewModel.setError("Could not read CSV file. Try a different file manager app.")
            }
        }
    }

    // ── CSV type mismatch dialog ──────────────────────────────────────────────
    csvMismatch?.let { mismatch ->
        AlertDialog(
            onDismissRequest = {
                csvMismatch = null
                pendingCsv = null
                pendingFriendCsv = null
                viewModel.clearCsvNames()
            },
            title = { Text(mismatch.title) },
            text  = { Text(mismatch.message, fontSize = 14.sp, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = mismatch.onSwitch) {
                    Text(mismatch.switchLabel, color = Green400)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = if (mismatch.isHardBlock) {
                        { csvMismatch = null; pendingFriendCsv = null; viewModel.clearCsvNames() }
                    } else {
                        mismatch.onContinue
                    }
                ) {
                    Text(
                        text  = if (mismatch.isHardBlock) "Cancel" else "Continue anyway",
                        color = TextSecondary,
                    )
                }
            },
        )
    }

    // ── Group import confirmation dialog ─────────────────────────────────────────
    // Shown after user taps a name in "Which one is you?" — sits on top of the
    // still-open bottom sheet. Cancel returns user to the sheet; Confirm fires import.
    if (showGroupConfirmDialog) {
        val selectedName = pendingGroupImporterNameForConfirm
        val displayName = if (selectedName.isNullOrBlank()) {
            "without claiming a name"
        } else {
            "as \"$selectedName\""
        }
        AlertDialog(
            onDismissRequest = {
                // Close dialog only — sheet stays open for re-selection
                showGroupConfirmDialog = false
                pendingGroupImporterNameForConfirm = null
            },
            containerColor = Surface2,
            title = {
                Text(
                    "Confirm Splitwise Import",
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        "This will import your Splitwise history into FairShare $displayName.",
                        fontSize = 14.sp,
                        color = TextPrimary,
                    )
                    if (groupName.isNotBlank()) {
                        Text(
                            "Group: $groupName",
                            fontSize = 13.sp,
                            color = TextSecondary,
                        )
                    }
                    Text(
                        "Expenses, payments, and members will be created. This cannot be undone.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showGroupConfirmDialog = false
                    val csv        = pendingCsv
                    val gName      = groupName
                    val gType      = selectedGroupType
                    val importerName = pendingGroupImporterNameForConfirm?.ifBlank { null }
                    pendingGroupImporterNameForConfirm = null
                    resetGroupImportDraft()
                    csv?.let { viewModel.importGroup(it, gName, gType, importerName) }
                }) {
                    Text("Import", color = Green400, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGroupConfirmDialog = false
                    pendingGroupImporterNameForConfirm = null
                    // Bottom sheet remains visible — user can pick a different name
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    // ── Friend import confirmation dialog ─────────────────────────────────────
    if (showFriendConfirmDialog) {
        val selectedName = pendingFriendImporterNameForConfirm
        val displayName = if (selectedName.isNullOrBlank()) {
            "without claiming a name"
        } else {
            "as \"$selectedName\""
        }
        AlertDialog(
            onDismissRequest = {
                showFriendConfirmDialog = false
                pendingFriendImporterNameForConfirm = null
            },
            containerColor = Surface2,
            title = {
                Text(
                    "Confirm Friend Import",
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        "This will import your direct Splitwise expenses and payments $displayName.",
                        fontSize = 14.sp,
                        color = TextPrimary,
                    )
                    Text(
                        "Expenses and payments will be created between you and the other person. This cannot be undone.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showFriendConfirmDialog = false
                    val csv          = pendingFriendCsv
                    val importerName = pendingFriendImporterNameForConfirm?.ifBlank { null }
                    pendingFriendImporterNameForConfirm = null
                    showFriendWhoAreYouDialog = false
                    viewModel.clearCsvNames()
                    pendingFriendCsv = null
                    csv?.let { viewModel.importFriend(it, importerName) }
                }) {
                    Text("Import", color = Green400, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFriendConfirmDialog = false
                    pendingFriendImporterNameForConfirm = null
                    // Bottom sheet remains visible — user can pick a different name
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    LaunchedEffect(claimState) {
        when (val s = claimState) {
            is ClaimState.Done  -> { snackbarHost.showSnackbar("Member linked ✓"); viewModel.resetClaimState() }
            is ClaimState.Error -> { snackbarHost.showSnackbar(s.message); viewModel.resetClaimState() }
            else -> Unit
        }
    }

    // Group name dialog
    if (showGroupNameDialog) {
        AlertDialog(
            onDismissRequest = { resetGroupImportDraft() },
            title   = { Text("Name your group") },
            text    = {
                FsTextField(
                    value         = groupName,
                    onValueChange = { groupName = it },
                    label         = "Group name (e.g. Hawaii Trip)",
                    modifier      = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (groupName.isNotBlank()) {
                        showGroupNameDialog = false
                        showGroupTypeDialog = true
                    }
                }) { Text("Next →", color = Green400) }
            },
            dismissButton = {
                TextButton(onClick = { resetGroupImportDraft() }) { Text("Cancel") }
            },
        )
    }

    // ── Group type picker ─────────────────────────────────────────────────────
    if (showGroupTypeDialog) {
        AlertDialog(
            onDismissRequest = { resetGroupImportDraft() },
            containerColor   = Surface2,
            title = { Text("What type of group is this?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    val types = listOf(
                        Triple("TRIP",      "✈️", "Trip"),
                        Triple("HOME",      "🏠", "Home"),
                        Triple("APARTMENT", "🏢", "Apartment"),
                        Triple("OFFICE",    "💼", "Office"),
                        Triple("FRIENDS",   "👫", "Friends"),
                        Triple("COUPLE",    "💑", "Couple"),
                        Triple("EVENT",     "🎉", "Event"),
                        Triple("OTHER",     "💰", "Other"),
                    )
                    // 2-column grid
                    types.chunked(2).forEach { row ->
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            row.forEach { (value, emoji, label) ->
                                val selected = selectedGroupType == value
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier         = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(Radius.lg))
                                        .background(if (selected) Green400.copy(alpha = 0.15f) else Surface3)
                                        .border(
                                            width = 1.5.dp,
                                            color = if (selected) Green400 else Surface4,
                                            shape = RoundedCornerShape(Radius.lg),
                                        )
                                        .clickable { selectedGroupType = value }
                                        .padding(vertical = 12.dp),
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(emoji, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text      = label,
                                            fontSize  = 12.sp,
                                            color     = if (selected) Green400 else TextSecondary,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showGroupTypeDialog = false
                    showWhoAreYouDialog = true
                }) { Text("Next →", color = Green400) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGroupTypeDialog = false
                    showGroupNameDialog = true
                }) { Text("Back", color = TextSecondary) }
            },
        )
    }

    // ── "Which one is you?" sheet — shown after group type is picked ──────────
    if (showWhoAreYouDialog) {
        ModalBottomSheet(
            onDismissRequest = {
                resetGroupImportDraft()
            },
            sheetState     = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = com.prathik.fairshare.ui.theme.Surface2,
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text       = "Which one is you?",
                    fontSize   = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color      = com.prathik.fairshare.ui.theme.TextPrimary,
                    modifier   = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                )
                // Show the logged-in account name so user knows what to look for
                val myName = viewModel.currentUserFullName
                if (myName.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.sm)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(com.prathik.fairshare.ui.theme.Radius.lg))
                            .background(Green400.copy(alpha = 0.08f))
                            .padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("👤 ", fontSize = 14.sp)
                        Text(
                            text = "You are signed in as ",
                            fontSize = 13.sp,
                            color = com.prathik.fairshare.ui.theme.TextSecondary,
                        )
                        Text(
                            text = myName,
                            fontSize = 13.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = Green400,
                        )
                    }
                }
                Text(
                    text     = "Your Splitwise name may be different. Tap whichever CSV name is you.",
                    fontSize = 13.sp,
                    color    = com.prathik.fairshare.ui.theme.TextSecondary,
                    modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md),
                )
                androidx.compose.material3.HorizontalDivider(
                    color = com.prathik.fairshare.ui.theme.Surface4, thickness = 0.5.dp)

                csvMemberNames.forEach { name ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Store selected name and show confirmation dialog.
                                // Do NOT reset draft or call import yet — the sheet stays
                                // visible so cancel returns the user to their choice.
                                pendingGroupImporterNameForConfirm = name
                                showGroupConfirmDialog = true
                            }
                            .padding(horizontal = Spacing.lg, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(ComponentSize.avatarMd)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(com.prathik.fairshare.ui.theme.Surface4),
                        ) {
                            Text(
                                name.firstOrNull()?.uppercase() ?: "?",
                                fontSize = 16.sp,
                                color    = com.prathik.fairshare.ui.theme.TextPrimary,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(name, fontSize = 15.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                            color = com.prathik.fairshare.ui.theme.TextPrimary,
                            modifier = Modifier.weight(1f))
                        Text("→", fontSize = 18.sp,
                            color = com.prathik.fairshare.ui.theme.TextTertiary)
                    }
                    androidx.compose.material3.HorizontalDivider(
                        color = com.prathik.fairshare.ui.theme.Surface3, thickness = 0.5.dp)
                }

                // None of these is me
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Empty string = "none of these" sentinel.
                            // importerCsvName will be null after ifBlank{null} in confirm.
                            pendingGroupImporterNameForConfirm = ""
                            showGroupConfirmDialog = true
                        }
                        .padding(horizontal = Spacing.lg, vertical = 16.dp),
                ) {
                    Text("None of these is me — skip",
                        fontSize = 14.sp,
                        color = com.prathik.fairshare.ui.theme.TextTertiary)
                }
            }
        }
    }

    // Friend "Which one is you?" sheet
    if (showFriendWhoAreYouDialog && csvMemberNames.isNotEmpty()) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = {
                showFriendWhoAreYouDialog = false
                viewModel.clearCsvNames()
                pendingFriendCsv = null
            },
            sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Surface2,
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                // Header
                val myName = viewModel.myDisplayName()
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text       = "Which one is you?",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary,
                        modifier   = Modifier.weight(1f),
                    )
                }
                if (myName.isNotBlank()) {
                    Row(
                        modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("You are signed in as ", fontSize = 13.sp, color = com.prathik.fairshare.ui.theme.TextSecondary)
                        Text(myName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Green400)
                    }
                }
                Text(
                    text     = "Tap your name from the CSV so your expenses appear correctly.",
                    fontSize = 13.sp,
                    color    = com.prathik.fairshare.ui.theme.TextSecondary,
                    modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md),
                )
                androidx.compose.material3.HorizontalDivider(
                    color = com.prathik.fairshare.ui.theme.Surface4, thickness = 0.5.dp)

                csvMemberNames.forEach { name ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Store selected name and show confirmation dialog.
                                // Sheet stays visible underneath so cancel returns user here.
                                pendingFriendImporterNameForConfirm = name
                                showFriendConfirmDialog = true
                            }
                            .padding(horizontal = Spacing.lg, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(ComponentSize.avatarMd)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(com.prathik.fairshare.ui.theme.Surface4),
                        ) {
                            Text(
                                name.firstOrNull()?.uppercase() ?: "?",
                                fontSize   = 16.sp,
                                color      = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    }
                    androidx.compose.material3.HorizontalDivider(
                        color = com.prathik.fairshare.ui.theme.Surface4, thickness = 0.5.dp)
                }

                // Skip option
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable {
                            pendingFriendImporterNameForConfirm = ""
                            showFriendConfirmDialog = true
                        }
                        .padding(horizontal = Spacing.lg, vertical = 16.dp),
                ) {
                    Text("None of these is me — skip", fontSize = 14.sp,
                        color = com.prathik.fairshare.ui.theme.TextTertiary)
                }
            }
        }
    }

    // ── Link friend placeholder sheet ─────────────────────────────────────────
    if (showLinkFriendSheet && !linkPlaceholderUserId.isNullOrBlank()) {
        ModalBottomSheet(
            onDismissRequest = { showLinkFriendSheet = false },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = Surface2,
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text       = "Who is ${linkCsvName ?: "this person"} on FairShare?",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                )
                Text(
                    text     = "Link their expenses to a FairShare friend, or keep as placeholder.",
                    fontSize = 13.sp,
                    color    = TextSecondary,
                    modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md),
                )
                HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                if (friends.isEmpty()) {
                    Text(
                        text     = "No FairShare friends found.",
                        fontSize = 14.sp,
                        color    = TextTertiary,
                        modifier = Modifier.padding(Spacing.lg),
                    )
                } else {
                    friends.forEach { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    linkPlaceholderUserId?.let { pid ->
                                        viewModel.assignFriendPlaceholder(pid, friend.id)
                                    }
                                    showLinkFriendSheet = false
                                }
                                .padding(horizontal = Spacing.lg, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FsAvatar(name = friend.fullName, userId = friend.id,
                                imageUrl = friend.profilePictureUrl,
                                size = ComponentSize.avatarMd)
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(friend.fullName, fontSize = 15.sp,
                                fontWeight = FontWeight.Medium, color = TextPrimary)
                        }
                        HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLinkFriendSheet = false }
                        .padding(horizontal = Spacing.lg, vertical = 16.dp),
                ) {
                    Text("Keep as placeholder for now", fontSize = 14.sp, color = TextTertiary)
                }
            }
        }
    }

    // Assign sheet
    if (showAssignSheet && selectedMember != null) {
        val member = selectedMember!!
        val groupId = (uiState as? ImportUiState.GroupSuccess)?.groupId
        ModalBottomSheet(
            onDismissRequest = { showAssignSheet = false },
            sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor   = Surface2,
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text       = "Who is ${member.fullName}?",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    modifier   = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                )
                Text(
                    text     = "Link this Splitwise member to a friend in FairShare",
                    fontSize = 13.sp,
                    color    = TextSecondary,
                    modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md),
                )
                HorizontalDivider(color = Surface4, thickness = 0.5.dp)

                // "That's me" row
                if (groupId != null) {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAssignSheet = false
                                viewModel.claimIdentity(groupId, member.userId)
                            }
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(ComponentSize.avatarMd)
                                .clip(CircleShape)
                                .background(Green400.copy(alpha = 0.15f)),
                        ) {
                            Text("👤", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("That's me", fontSize = 15.sp,
                                fontWeight = FontWeight.Medium, color = Green400)
                            Text("Claim this as your identity", fontSize = 12.sp, color = TextTertiary)
                        }
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                }

                // Friends list
                friends.forEach { friend ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAssignSheet = false
                                if (groupId != null) {
                                    viewModel.assignPlaceholder(groupId, member.userId, friend.id)
                                }
                            }
                            .padding(horizontal = Spacing.lg, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(name = friend.fullName, userId = friend.id,
                            imageUrl = friend.profilePictureUrl,
                            size = ComponentSize.avatarMd)
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(friend.fullName, fontSize = 15.sp,
                            fontWeight = FontWeight.Medium, color = TextPrimary,
                            modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                }

                // Skip
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { showAssignSheet = false }
                        .padding(horizontal = Spacing.lg, vertical = 16.dp),
                ) {
                    Text("Skip for now", fontSize = 14.sp, color = TextTertiary)
                }
            }
        }
    }

    Scaffold(
        containerColor = Surface0,
        topBar         = { FsTopBar(title = "Import from Splitwise", onBack = onBack) },
        snackbarHost   = { SnackbarHost(snackbarHost) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {

                // ── Idle: pick import type ────────────────────────────────────
                is ImportUiState.Idle -> {
                    Column(
                        modifier            = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(Spacing.xl))

                        Text("📥", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text(
                            text       = "Import your Splitwise history",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary,
                            textAlign  = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text      = "Export a CSV from Splitwise, then choose what to import.",
                            fontSize  = 14.sp,
                            color     = TextSecondary,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(Spacing.xxxl))

                        // Group import card
                        ImportOptionCard(
                            emoji    = "👥",
                            title    = "Import a group",
                            subtitle = "Creates a new group with all expenses and members from your Splitwise export.",
                            onClick  = { groupFilePicker.launch(arrayOf("text/*", "text/csv", "*/*")) },
                        )

                        Spacer(modifier = Modifier.height(Spacing.md))

                        // Friend import card
                        ImportOptionCard(
                            emoji    = "👤",
                            title    = "Import friend expenses",
                            subtitle = "Imports direct (non-group) expenses with a friend from Splitwise.",
                            onClick  = { friendFilePicker.launch(arrayOf("text/*", "text/csv", "*/*")) },
                        )

                        Spacer(modifier = Modifier.height(Spacing.xxxl))

                        // How to export instructions
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.xl))
                                .background(Surface2)
                                .padding(Spacing.md),
                        ) {
                            Text("How to export from Splitwise",
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            listOf(
                                "Open Splitwise on web or mobile",
                                "Go to the group or friend you want to export",
                                "Tap ⚙️ Settings → Export to CSV",
                                "Save the file and import it here",
                            ).forEachIndexed { i, step ->
                                Row(modifier = Modifier.padding(vertical = 3.dp)) {
                                    Text("${i + 1}.", fontSize = 13.sp, color = Green400,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.width(20.dp))
                                    Text(step, fontSize = 13.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                // ── Loading ───────────────────────────────────────────────────
                is ImportUiState.Loading -> {
                    Column(
                        modifier            = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        FsLoadingScreen()
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text(state.message, fontSize = 14.sp, color = TextSecondary)
                    }
                }

                // ── Group success ─────────────────────────────────────────────
                is ImportUiState.GroupSuccess -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(Spacing.xl))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Green400.copy(alpha = 0.12f)),
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null,
                                tint = Green400, modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text("Import complete!", fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(state.groupName, fontSize = 15.sp, color = TextSecondary)

                        Spacer(modifier = Modifier.height(Spacing.xl))

                        // Stats
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            StatCard("Expenses", state.expensesCreated.toString(),
                                Modifier.weight(1f))
                            StatCard("Payments", state.settlementsCreated.toString(),
                                Modifier.weight(1f))
                        }

                        // Warnings
                        if (state.warnings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(Radius.xl))
                                    .background(Surface2)
                                    .padding(Spacing.md),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Warning, contentDescription = null,
                                        tint = Negative, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(Spacing.sm))
                                    Text("${state.warnings.size} rows skipped",
                                        fontSize = 13.sp, color = Negative,
                                        fontWeight = FontWeight.Medium)
                                }
                                state.warnings.take(3).forEach { w ->
                                    Text("• $w", fontSize = 12.sp, color = TextTertiary,
                                        modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }

                        // Unclaimed members section
                        if (state.unclaimedMembers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(Spacing.xl))
                            Text(
                                text       = "Link members to FairShare friends",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = TextPrimary,
                                modifier   = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text     = "These people were in your Splitwise group. Tap to link them to existing FairShare users.",
                                fontSize = 13.sp,
                                color    = TextSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = Spacing.md),
                            )

                            val currentUnclaimed by viewModel.unclaimedMembers.collectAsState()
                            currentUnclaimed.forEach { member ->
                                Row(
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(Radius.xl))
                                        .background(Surface2)
                                        .clickable {
                                            selectedMember = member
                                            showAssignSheet = true
                                        }
                                        .padding(horizontal = Spacing.md, vertical = Spacing.md),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    FsAvatar(name = member.fullName, userId = member.userId,
                                        imageUrl = member.profilePictureUrl,
                                        size = ComponentSize.avatarMd)
                                    Spacer(modifier = Modifier.width(Spacing.md))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(member.fullName, fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium, color = TextPrimary)
                                        Text("Tap to link", fontSize = 12.sp, color = TextTertiary)
                                    }
                                    Text("→", fontSize = 18.sp, color = TextTertiary)
                                }
                                Spacer(modifier = Modifier.height(Spacing.sm))
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.xl))

                        if (state.groupId != null) {
                            FsPrimaryButton(
                                text     = "Go to ${state.groupName}",
                                onClick  = { onGroupImported(state.groupId) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                        }
                        FsSecondaryButton(
                            text     = "Import another",
                            onClick  = { viewModel.reset() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // ── Friend success ────────────────────────────────────────────
                is ImportUiState.FriendSuccess -> {
                    Column(
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Green400.copy(alpha = 0.12f)),
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null,
                                tint = Green400, modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text("Import complete!", fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(Spacing.xl))

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            StatCard("Expenses", state.expensesCreated.toString(),
                                Modifier.weight(1f))
                            StatCard("Payments", state.settlementsCreated.toString(),
                                Modifier.weight(1f))
                        }

                        if (!state.otherPlaceholderUserId.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(Spacing.xl))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(Radius.xl))
                                    .background(Surface2)
                                    .clickable {
                                        linkPlaceholderUserId = state.otherPlaceholderUserId
                                        linkCsvName = state.otherCsvName
                                        showLinkFriendSheet = true
                                    }
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier         = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Surface3),
                                ) {
                                    Text(
                                        state.otherCsvName?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        fontSize = 16.sp, color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        state.otherCsvName ?: "Other person",
                                        fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                    )
                                    Text(
                                        "Tap to link to a FairShare friend",
                                        fontSize = 12.sp, color = TextSecondary,
                                    )
                                }
                                Text("Link →", fontSize = 13.sp, color = Green400,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(Spacing.xxxl))
                        FsPrimaryButton(
                            text     = "Done",
                            onClick  = onBack,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        FsSecondaryButton(
                            text     = "Import another",
                            onClick  = { viewModel.reset() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // ── Error ─────────────────────────────────────────────────────
                is ImportUiState.Error -> {
                    Column(
                        modifier            = Modifier
                            .fillMaxSize()
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("❌", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text("Import failed", fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(state.message, fontSize = 14.sp,
                            color = TextSecondary, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(Spacing.xl))
                        FsPrimaryButton(
                            text     = "Try again",
                            onClick  = { viewModel.reset() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportOptionCard(
    emoji   : String,
    title   : String,
    subtitle: String,
    onClick : () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .clickable(onClick = onClick)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface4),
        ) { Text(emoji, fontSize = 22.sp) }

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(subtitle, fontSize = 13.sp, color = TextSecondary)
        }

        Text("→", fontSize = 18.sp, color = TextTertiary)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Green400)
        Text(label, fontSize = 13.sp, color = TextSecondary)
    }
}

private enum class ImportType { GROUP, FRIEND }
/** Describes a detected mismatch between the chosen import type and the CSV content. */
private data class CsvMismatch(
    val title      : String,
    val message    : String,
    val switchLabel: String,
    val onSwitch   : () -> Unit,
    val onContinue : () -> Unit,
    val isHardBlock: Boolean = false,  // if true, no "Continue anyway" option is shown
)