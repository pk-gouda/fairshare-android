package com.prathik.fairshare.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.ui.components.FsSkeletonBlock
import com.prathik.fairshare.ui.components.FsSkeletonTimelineRow
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import com.prathik.fairshare.util.MoneyUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun GlobalSearchScreen(
    onBack: () -> Unit,
    onNavigateToExpense: (String) -> Unit,
    viewModel: GlobalSearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val selectedGroupId by viewModel.selectedGroupId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val focusRequester = remember { FocusRequester() }

    // Auto-focus search bar when screen opens
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        containerColor = Surface0,
        topBar = {
            Column {
                // Search bar row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // Back arrow
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextSecondary,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Radius.lg))
                            .background(Surface2)
                            .padding(horizontal = Spacing.md, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = Green400,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        BasicTextField(
                            value = query,
                            onValueChange = { viewModel.onQueryChanged(it) },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary),
                            cursorBrush = SolidColor(Green400),
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text(
                                        "Search an expense...",
                                        fontSize = 14.sp,
                                        color = TextTertiary
                                    )
                                }
                                inner()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                        )
                        // Clear button — only visible when query is not empty
                        if (query.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(TextTertiary.copy(alpha = 0.3f))
                                    .clickable { viewModel.onQueryChanged("") },
                            ) {
                                Text(
                                    text = "×",
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 13.sp,
                                )
                            }
                        }
                    }
                }

                // Filter chips — group + more filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.lg)
                        .padding(bottom = Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // All chip
                    FilterChip(
                        label = "All",
                        isSelected = selectedGroupId == null,
                        onClick = { viewModel.onGroupFilterSelected(null) },
                    )
                    // Group chips
                    groups.forEach { group ->
                        FilterChip(
                            label = group.name,
                            isSelected = selectedGroupId == group.id,
                            onClick = { viewModel.onGroupFilterSelected(group.id) },
                        )
                    }
                }

                HorizontalDivider(color = Surface3, thickness = 0.5.dp)
            }
        },
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {

            // Show skeleton rows during initial data load (before any results exist)
            if (isLoading && results.isEmpty()) {
                item {
                    FsSkeletonBlock(
                        height = 14.dp,
                        widthFraction = 0.35f,
                        cornerRadius = 4.dp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
                items(6) { FsSkeletonTimelineRow() }
                return@LazyColumn
            }

            // Results count header
            item {
                Text(
                    text = if (query.isBlank()) "ALL EXPENSES · ${results.size}"
                    else "${results.size} RESULT${if (results.size != 1) "S" else ""} FOR \"${query.uppercase()}\"",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextTertiary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(
                        horizontal = Spacing.lg,
                        vertical = Spacing.md,
                    ),
                )
            }

            if (results.isEmpty() && !isLoading && query.isNotBlank()) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                    ) {
                        Text(text = "🔍", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text(
                            text = "No results for \"$query\"",
                            fontSize = 15.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Try a different search term",
                            fontSize = 12.sp,
                            color = TextTertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            } else {
                items(
                    items = results,
                    key = { it.id },
                ) { expense ->
                    SearchResultRow(
                        expense = expense,
                        query = query,
                        onClick = { onNavigateToExpense(expense.id) },
                    )
                    HorizontalDivider(
                        color = Surface3,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = Spacing.lg + 50.dp),
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

// ── Filter Chip ───────────────────────────────────────────────────────────────

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(if (isSelected) Surface2 else Surface2)
            .then(
                if (isSelected) Modifier.background(
                    androidx.compose.ui.graphics.Color(0xFF1A3A1A),
                    RoundedCornerShape(Radius.full)
                ) else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = Spacing.md, vertical = 5.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) Green400 else TextSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ── Search Result Row ─────────────────────────────────────────────────────────

@Composable
private fun SearchResultRow(
    expense: Expense,
    query: String,
    onClick: () -> Unit,
) {
    val youLent = expense.yourBalance > 0
    val youOwe = expense.yourBalance < 0

    val balanceLabel = when {
        youLent -> "you lent"; youOwe -> "you owe"; else -> "settled"
    }
    val balanceColor = when {
        youLent -> Green400; youOwe -> Negative; else -> TextTertiary
    }
    val balanceText = when {
        youLent -> MoneyUtils.format(expense.yourBalance, expense.currency)
        youOwe -> MoneyUtils.format(-expense.yourBalance, expense.currency)
        else -> ""
    }

    val dateText = try {
        val dt = LocalDateTime.parse(expense.expenseDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        dt.format(DateTimeFormatter.ofPattern("MMM d"))
    } catch (e: Exception) {
        ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Category emoji box
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(Radius.md))
                .background(categoryBgColor(expense.category)),
        ) {
            Text(text = categoryEmoji(expense.category), fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            // Highlight matching text in green
            val annotated = buildAnnotatedString {
                val desc = expense.description
                val lower = desc.lowercase()
                val q = query.trim().lowercase()
                if (q.isBlank() || !lower.contains(q)) {
                    append(desc)
                } else {
                    val start = lower.indexOf(q)
                    val end = start + q.length
                    append(desc.substring(0, start))
                    withStyle(SpanStyle(color = Green400, fontWeight = FontWeight.SemiBold)) {
                        append(desc.substring(start, end))
                    }
                    append(desc.substring(end))
                }
            }
            Text(
                text = annotated,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${expense.groupName ?: "Direct"} · $dateText",
                fontSize = 11.sp,
                color = TextTertiary,
            )
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = MoneyUtils.format(expense.totalAmount, expense.currency),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            if (expense.yourBalance != 0.0) {
                Text(text = balanceLabel, fontSize = 10.sp, color = balanceColor)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun categoryEmoji(category: ExpenseCategory?): String = when (category) {
    ExpenseCategory.DINING_OUT -> "🍽️"
    ExpenseCategory.GROCERIES -> "🛒"
    ExpenseCategory.CAR -> "🚗"
    ExpenseCategory.TAXI -> "🚕"
    ExpenseCategory.PLANE -> "✈️"
    ExpenseCategory.HOTEL -> "🏨"
    ExpenseCategory.RENT -> "🏠"
    ExpenseCategory.ELECTRICITY -> "⚡"
    ExpenseCategory.MOVIES -> "🎬"
    ExpenseCategory.GAMES -> "🎮"
    ExpenseCategory.MUSIC -> "🎵"
    ExpenseCategory.SPORTS -> "⚽"
    ExpenseCategory.MEDICAL -> "💊"
    ExpenseCategory.EDUCATION -> "📚"
    ExpenseCategory.GIFTS -> "🎁"
    ExpenseCategory.LIQUOR -> "🍺"
    ExpenseCategory.PETS -> "🐾"
    ExpenseCategory.CLOTHING -> "👕"
    ExpenseCategory.BUS_TRAIN -> "🚌"
    else -> "💰"
}

private fun categoryBgColor(category: ExpenseCategory?): androidx.compose.ui.graphics.Color {
    return when (category) {
        ExpenseCategory.DINING_OUT, ExpenseCategory.GROCERIES,
        ExpenseCategory.LIQUOR -> androidx.compose.ui.graphics.Color(0xFF1A2A1A)

        ExpenseCategory.RENT -> androidx.compose.ui.graphics.Color(0xFF1A2A3A)
        ExpenseCategory.TAXI, ExpenseCategory.CAR,
        ExpenseCategory.BUS_TRAIN, ExpenseCategory.PLANE -> androidx.compose.ui.graphics.Color(
            0xFF2A1A0A
        )

        ExpenseCategory.ELECTRICITY -> androidx.compose.ui.graphics.Color(0xFF1A1A3A)
        ExpenseCategory.MOVIES, ExpenseCategory.GAMES,
        ExpenseCategory.MUSIC, ExpenseCategory.SPORTS -> androidx.compose.ui.graphics.Color(
            0xFF2A1A2A
        )

        ExpenseCategory.MEDICAL -> androidx.compose.ui.graphics.Color(0xFF2A1A1A)
        else -> androidx.compose.ui.graphics.Color(0xFF1E1E20)
    }
}