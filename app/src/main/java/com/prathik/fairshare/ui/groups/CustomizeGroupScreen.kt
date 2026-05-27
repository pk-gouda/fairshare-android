package com.prathik.fairshare.ui.groups

import android.app.DatePickerDialog
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.prathik.fairshare.domain.model.GroupType
import com.prathik.fairshare.ui.components.FsDetailSkeleton
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.components.FsTopBar
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── CustomizeGroupScreen ──────────────────────────────────────────────────────
// Handles group name, notes, and trip dates (TRIP groups only).
// Group photo is read-only here (upload not yet implemented).

@Composable
fun CustomizeGroupScreen(
    onBack   : () -> Unit,
    viewModel: GroupSettingsViewModel = hiltViewModel(),
) {
    val group           by viewModel.group.collectAsState()
    val editName        by viewModel.editName.collectAsState()
    val editDescription by viewModel.editDescription.collectAsState()
    val tripStartDate   by viewModel.tripStartDate.collectAsState()
    val tripEndDate     by viewModel.tripEndDate.collectAsState()
    val actionState     by viewModel.actionState.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }

    // Surface action state feedback
    LaunchedEffect(actionState) {
        when (actionState) {
            is GroupSettingsActionState.Success ->
                snackbarHost.showSnackbar((actionState as GroupSettingsActionState.Success).message)
            is GroupSettingsActionState.Error ->
                snackbarHost.showSnackbar((actionState as GroupSettingsActionState.Error).message)
            else -> Unit
        }
        viewModel.resetActionState()
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar         = { FsTopBar(title = "Customize group", onBack = onBack) },
    ) { innerPadding ->

        if (isLoading && group == null) {
            FsDetailSkeleton()
            return@Scaffold
        }

        val g = group ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Group photo — read-only, upload coming soon ───────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface2)
                    .border(1.dp, Surface4, RoundedCornerShape(16.dp)),
            ) {
                if (!g.groupImage.isNullOrBlank()) {
                    var failed by remember(g.groupImage) { mutableStateOf(false) }
                    if (!failed) {
                        AsyncImage(
                            model              = g.groupImage,
                            contentDescription = g.name,
                            onError            = { failed = true },
                            modifier           = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(coverEmoji(g.type), fontSize = 32.sp)
                    }
                } else {
                    Text(coverEmoji(g.type), fontSize = 32.sp)
                }
            }
            Text(
                text     = "Photo · coming soon",
                fontSize = 11.sp,
                color    = TextTertiary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp, bottom = Spacing.lg),
            )

            // ── Group name ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = editName,
                onValueChange = { viewModel.onNameChanged(it) },
                label         = { Text("Group name", fontSize = 12.sp) },
                singleLine    = true,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                colors        = fieldColors(),
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            // ── Group notes ───────────────────────────────────────────────────
            OutlinedTextField(
                value         = editDescription,
                onValueChange = { if (it.length <= 100) viewModel.onDescriptionChanged(it) },
                label         = { Text("Group notes", fontSize = 12.sp) },
                placeholder   = { Text("Add notes, rules, or purpose…", fontSize = 14.sp, color = TextTertiary) },
                maxLines      = 4,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                colors        = fieldColors(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    "${editDescription.length}/100",
                    fontSize = 11.sp,
                    color    = if (editDescription.length >= 90) Negative else TextTertiary,
                )
            }

            // ── Trip dates (TRIP groups only) ─────────────────────────────────
            if (g.type == GroupType.TRIP) {
                Spacer(modifier = Modifier.height(Spacing.md))
                CustomizeTripDatesSection(
                    tripStartDate    = tripStartDate,
                    tripEndDate      = tripEndDate,
                    onStartChanged   = { viewModel.onTripStartDateChanged(it) },
                    onEndChanged     = { viewModel.onTripEndDateChanged(it) },
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // ── Save button ───────────────────────────────────────────────────
            FsPrimaryButton(
                text     = "Save changes",
                onClick  = { viewModel.saveGroupIdentity() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            )

            Spacer(modifier = Modifier.height(Spacing.xxxl))
        }
    }
}

// ── Trip date section for CustomizeGroup ──────────────────────────────────────

@Composable
private fun CustomizeTripDatesSection(
    tripStartDate : String?,
    tripEndDate   : String?,
    onStartChanged: (String?) -> Unit,
    onEndChanged  : (String?) -> Unit,
) {
    val context = LocalContext.current

    val endBeforeStart = tripStartDate != null && tripEndDate != null && runCatching {
        LocalDate.parse(tripEndDate) < LocalDate.parse(tripStartDate)
    }.getOrDefault(false)

    Column(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text("Trip dates", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        Text(
            "Dates are optional. End date cannot be before start date.",
            fontSize = 12.sp, color = TextTertiary, lineHeight = 16.sp,
        )

        CustomizeDateRow(
            label     = "Start date",
            display   = tripStartDate?.let { custFormatDate(it) } ?: "Not set",
            context   = context,
            current   = tripStartDate,
            maxDate   = tripEndDate,
            allowClear = false,
            onPicked  = onStartChanged,
        )
        CustomizeDateRow(
            label     = "End date",
            display   = tripEndDate?.let { custFormatDate(it) } ?: "Not set",
            context   = context,
            current   = tripEndDate,
            minDate   = tripStartDate,
            allowClear = true,
            onPicked  = onEndChanged,
        )

        if (endBeforeStart) {
            Text("End date must be on or after start date", fontSize = 12.sp, color = Negative)
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun CustomizeDateRow(
    label     : String,
    display   : String,
    context   : android.content.Context,
    current   : String?,
    minDate   : String? = null,
    maxDate   : String? = null,
    allowClear: Boolean = true,
    onPicked  : (String?) -> Unit,
) {
    val calendar = java.util.Calendar.getInstance()
    current?.let { s ->
        runCatching { LocalDate.parse(s) }.getOrNull()?.let {
            calendar.set(it.year, it.monthValue - 1, it.dayOfMonth)
        }
    }
    val dialog = DatePickerDialog(
        context,
        { _, y, m, d ->
            onPicked(LocalDate.of(y, m + 1, d).format(DateTimeFormatter.ISO_LOCAL_DATE))
        },
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH),
        calendar.get(java.util.Calendar.DAY_OF_MONTH),
    )
    minDate?.let { s -> runCatching { LocalDate.parse(s) }.getOrNull()?.let {
        val c = java.util.Calendar.getInstance(); c.set(it.year, it.monthValue - 1, it.dayOfMonth)
        dialog.datePicker.minDate = c.timeInMillis
    }}
    maxDate?.let { s -> runCatching { LocalDate.parse(s) }.getOrNull()?.let {
        val c = java.util.Calendar.getInstance(); c.set(it.year, it.monthValue - 1, it.dayOfMonth)
        dialog.datePicker.maxDate = c.timeInMillis
    }}

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.md))
            .background(Surface3)
            .clickable { dialog.show() }
            .padding(horizontal = Spacing.md, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(display, fontSize = 14.sp, color = TextPrimary)
            if (allowClear && current != null) {
                Text(
                    "Clear",
                    fontSize = 12.sp,
                    color    = TextTertiary,
                    modifier = Modifier.clickable { onPicked(null) },
                )
            }
        }
    }
}

private fun custFormatDate(isoDate: String): String = runCatching {
    val d = LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
    d.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
}.getOrDefault(isoDate)

private fun coverEmoji(type: GroupType): String = when (type) {
    GroupType.TRIP      -> "✈️"
    GroupType.HOME      -> "🏠"
    GroupType.OFFICE    -> "💼"
    GroupType.FRIENDS   -> "👫"
    GroupType.COUPLE    -> "💑"
    GroupType.EVENT     -> "🎉"
    GroupType.APARTMENT -> "🏢"
    GroupType.OTHER     -> "💰"
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = Green400,
    unfocusedBorderColor    = Surface4,
    focusedLabelColor       = Green400,
    unfocusedLabelColor     = TextTertiary,
    focusedTextColor        = TextPrimary,
    unfocusedTextColor      = TextPrimary,
    focusedContainerColor   = Surface2,
    unfocusedContainerColor = Surface2,
)