package com.prathik.fairshare.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.ExpenseItem
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsTopBar
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
import com.prathik.fairshare.util.MoneyUtils

/**
 * ItemAssignmentScreen — shown after receipt scan, before expense is saved.
 *
 * Flow:
 * 1. Loads items from GET /api/receipts/{receiptId}/items
 * 2. User taps a member chip under each item to assign them
 * 3. Empty assignment = split among all (backend default)
 * 4. "Done" calls onDone(assignments) which sets them in AddExpenseViewModel
 */
@Composable
fun ItemAssignmentScreen(
    receiptId       : String,
    members         : List<GroupMember>,
    currency        : String,
    onBack               : () -> Unit,
    onDone               : (Map<String, List<String>>) -> Unit,
    onNavigateToReview   : () -> Unit,
    viewModel       : ItemAssignmentViewModel = hiltViewModel(),
) {
    val items       by viewModel.items.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    val assignments by viewModel.assignments.collectAsState()

    // Only load items if not already loaded — prevents clearing assignments on back navigation
    LaunchedEffect(receiptId) {
        if (viewModel.items.value.isEmpty()) {
            viewModel.loadItems(receiptId)
        }
    }

    Scaffold(
        containerColor = Surface0,
        topBar = {
            FsTopBar(
                title   = "Assign items",
                onBack  = onBack,
                actions = {
                    Text(
                        text     = "Done",
                        color    = Green400,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                onDone(viewModel.buildAssignmentsMap())
                                onNavigateToReview()
                            }
                            .padding(horizontal = Spacing.md),
                    )
                },
            )
        },
    ) { paddingValues ->
        if (isLoading) {
            FsLoadingScreen()
            return@Scaffold
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🧾", fontSize = 48.sp)
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        "No items detected",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "Gemini couldn't extract items from this receipt.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    Button(
                        onClick = { onDone(emptyMap()); onNavigateToReview() },
                        colors = ButtonDefaults.buttonColors(containerColor = Green400),
                    ) {
                        Text("Continue without itemizing", color = Color.Black)
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Header hint
            Text(
                text     = "Tap members to assign each item. Leave blank to split equally.",
                fontSize = 13.sp,
                color    = TextTertiary,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )

            LazyColumn(
                contentPadding     = PaddingValues(
                    start  = Spacing.lg,
                    end    = Spacing.lg,
                    top    = Spacing.sm,
                    bottom = 100.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(items, key = { it.id }) { item ->
                    val itemAssigned = assignments[item.id] ?: emptySet()
                    ItemAssignmentCard(
                        item       = item,
                        currency   = currency,
                        members    = members,
                        assigned   = itemAssigned,
                        onToggle   = { userId -> viewModel.toggleAssignment(item.id, userId) },
                        onAssignAll = { viewModel.assignAll(item.id, members.map { it.userId }) },
                        onClear    = { viewModel.clearAssignment(item.id) },
                    )
                }
            }
        }
    }
}

// ── Item Card ──────────────────────────────────────────────────────────────────

@Composable
private fun ItemAssignmentCard(
    item       : ExpenseItem,
    currency   : String,
    members    : List<GroupMember>,
    assigned   : Set<String>,
    onToggle   : (String) -> Unit,
    onAssignAll: () -> Unit,
    onClear    : () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(Surface2)
            .padding(Spacing.md),
    ) {
        // Item name + price
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = item.name,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color      = TextPrimary,
                )
                if ((item.quantity ?: 1) > 1) {
                    Text(
                        text     = "×${item.quantity}",
                        fontSize = 12.sp,
                        color    = TextTertiary,
                    )
                }
            }
            Text(
                text       = MoneyUtils.format(item.totalPrice, currency),
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary,
            )
        }

        Spacer(Modifier.height(Spacing.sm))
        HorizontalDivider(color = Surface4, thickness = 0.5.dp)
        Spacer(Modifier.height(Spacing.sm))

        // Assignment label
        val assignLabel = when {
            assigned.isEmpty()          -> "Split equally among all"
            assigned.size == members.size -> "Everyone"
            else -> members.filter { it.userId in assigned }
                .joinToString(", ") { it.fullName.split(" ").first() }
        }
        Row(
            modifier             = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment    = Alignment.CenterVertically,
        ) {
            Text(
                text     = assignLabel,
                fontSize = 12.sp,
                color    = if (assigned.isEmpty()) TextTertiary else Green400,
            )
            // Quick actions
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (assigned.isNotEmpty()) {
                    Text(
                        text     = "Clear",
                        fontSize = 12.sp,
                        color    = TextTertiary,
                        modifier = Modifier.clickable { onClear() },
                    )
                }
                Text(
                    text     = "All",
                    fontSize = 12.sp,
                    color    = TextSecondary,
                    modifier = Modifier.clickable { onAssignAll() },
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        // Member chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier              = Modifier.fillMaxWidth(),
        ) {
            members.forEach { member ->
                val isAssigned = member.userId in assigned
                MemberChip(
                    member     = member,
                    isAssigned = isAssigned,
                    onClick    = { onToggle(member.userId) },
                )
            }
        }
    }
}

// ── Member Chip ────────────────────────────────────────────────────────────────

@Composable
private fun MemberChip(
    member    : GroupMember,
    isAssigned: Boolean,
    onClick   : () -> Unit,
) {
    val initials = member.fullName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

    Box(
        modifier          = Modifier.size(44.dp),
        contentAlignment  = Alignment.Center,
    ) {
        // Avatar circle — always shows initials
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isAssigned) Green400.copy(alpha = 0.2f) else Surface3)
                .border(
                    width = 2.dp,
                    color = if (isAssigned) Green400 else Surface4,
                    shape = CircleShape,
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = initials,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = if (isAssigned) Green400 else TextSecondary,
            )
        }
        // Small checkmark badge when selected
        if (isAssigned) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Green400),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Check,
                    contentDescription = null,
                    tint               = Color.Black,
                    modifier           = Modifier.size(10.dp),
                )
            }
        }
    }
}