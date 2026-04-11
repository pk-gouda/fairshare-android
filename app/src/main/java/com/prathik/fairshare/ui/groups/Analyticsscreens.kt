package com.prathik.fairshare.ui.groups

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.network.api.AnalyticsApiService
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject

// ── Palette for pie slices ────────────────────────────────────────────────────

private val PIE_COLORS = listOf(
    Color(0xFF4FC3F7), // light blue
    Color(0xFFAED581), // light green
    Color(0xFFFFB74D), // orange
    Color(0xFFF06292), // pink
    Color(0xFF9575CD), // purple
    Color(0xFF4DB6AC), // teal
    Color(0xFFFF8A65), // deep orange
    Color(0xFF90A4AE), // blue grey
    Color(0xFFFFF176), // yellow
    Color(0xFF80CBC4), // teal light
    Color(0xFFCE93D8), // light purple
    Color(0xFFEF9A9A), // light red
)

// ── Fields to hide ────────────────────────────────────────────────────────────
private val HIDDEN_FIELDS = setOf("groupId", "userId", "isArchived", "groupName")

// ── UI state types ────────────────────────────────────────────────────────────

sealed class AnalyticsObjectState {
    object Loading : AnalyticsObjectState()
    data class Success(val data: JsonElement) : AnalyticsObjectState()
    data class Error(val message: String) : AnalyticsObjectState()
}

sealed class AnalyticsListState {
    object Loading : AnalyticsListState()
    data class Success(val data: JsonElement) : AnalyticsListState()
    data class Error(val message: String) : AnalyticsListState()
}

// ── Data class for a pie slice entry ─────────────────────────────────────────

data class PieEntry(val label: String, val amount: Double, val percentage: Float)

// ── GroupAnalyticsViewModel ───────────────────────────────────────────────────

@HiltViewModel
class GroupAnalyticsViewModel @Inject constructor(
    private val analyticsApiService: AnalyticsApiService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _summaryState  = MutableStateFlow<AnalyticsObjectState>(AnalyticsObjectState.Loading)
    val summaryState: StateFlow<AnalyticsObjectState> = _summaryState.asStateFlow()

    private val _categoryState = MutableStateFlow<AnalyticsListState>(AnalyticsListState.Loading)
    val categoryState: StateFlow<AnalyticsListState> = _categoryState.asStateFlow()

    private val _memberState   = MutableStateFlow<AnalyticsListState>(AnalyticsListState.Loading)
    val memberState: StateFlow<AnalyticsListState> = _memberState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val summaryDef  = async { safeApiCall { analyticsApiService.getGroupSummary(groupId) } }
            val categoryDef = async { safeApiCall { analyticsApiService.getGroupCategoryBreakdown(groupId) } }
            val memberDef   = async { safeApiCall { analyticsApiService.getGroupMemberBreakdown(groupId) } }

            _summaryState.value = when (val r = summaryDef.await()) {
                is ApiResult.Success      -> AnalyticsObjectState.Success(r.data)
                is ApiResult.NetworkError -> AnalyticsObjectState.Error("No internet connection.")
                else                      -> AnalyticsObjectState.Error("Failed to load analytics.")
            }
            _categoryState.value = when (val r = categoryDef.await()) {
                is ApiResult.Success -> AnalyticsListState.Success(r.data)
                else                 -> AnalyticsListState.Error("")
            }
            _memberState.value = when (val r = memberDef.await()) {
                is ApiResult.Success -> AnalyticsListState.Success(r.data)
                else                 -> AnalyticsListState.Error("")
            }
        }
    }
}

// ── GroupAnalyticsScreen ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupAnalyticsScreen(
    onBack   : () -> Unit,
    viewModel: GroupAnalyticsViewModel = hiltViewModel(),
) {
    val summaryState  by viewModel.summaryState.collectAsState()
    val categoryState by viewModel.categoryState.collectAsState()
    val memberState   by viewModel.memberState.collectAsState()

    Scaffold(
        containerColor = Surface0,
        topBar         = { FsTopBar(title = "Group analytics", onBack = onBack) },
    ) { innerPadding ->
        when (summaryState) {
            is AnalyticsObjectState.Loading -> FsLoadingScreen()
            is AnalyticsObjectState.Error   -> FsErrorScreen(
                message = (summaryState as AnalyticsObjectState.Error).message,
                onRetry = { viewModel.load() }
            )
            is AnalyticsObjectState.Success -> {
                val summary    = (summaryState as AnalyticsObjectState.Success).data as? JsonObject ?: JsonObject(emptyMap())
                val categories = ((categoryState as? AnalyticsListState.Success)?.data) as? JsonArray
                val members    = ((memberState   as? AnalyticsListState.Success)?.data) as? JsonArray

                // Build pie entries from category data
                val categoryEntries: List<PieEntry> = categories?.mapNotNull { item ->
                    val obj = item as? JsonObject ?: return@mapNotNull null
                    val label  = obj["category"]?.toDisplayString()?.toReadable() ?: return@mapNotNull null
                    val amount = obj["amount"]?.toDisplayString()?.toDoubleOrNull() ?: return@mapNotNull null
                    val pct    = obj["percentage"]?.toDisplayString()?.toFloatOrNull() ?: 0f
                    PieEntry(label, amount, pct)
                }?.sortedByDescending { it.amount } ?: emptyList()

                // Build pie entries from member data
                val memberEntries: List<PieEntry> = members?.mapNotNull { item ->
                    val obj = item as? JsonObject ?: return@mapNotNull null
                    val label  = obj["fullName"]?.toDisplayString() ?: return@mapNotNull null
                    val amount = obj["amountPaid"]?.toDisplayString()?.toDoubleOrNull() ?: return@mapNotNull null
                    val pct    = obj["percentage"]?.toDisplayString()?.toFloatOrNull() ?: 0f
                    PieEntry(label, amount, pct)
                }?.sortedByDescending { it.amount } ?: emptyList()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(Modifier.height(Spacing.md))

                    // Overview card
                    val overviewEntries = summary.entries
                        .filter { it.key !in HIDDEN_FIELDS }
                        .sortedBy { it.key }
                    if (overviewEntries.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                            SectionLabel("OVERVIEW")
                            Spacer(Modifier.height(Spacing.sm))
                            AnalyticsCard {
                                overviewEntries.forEachIndexed { i, (key, value) ->
                                    AnalyticsRow(label = key.toReadable(), value = value.toDisplayString())
                                    if (i < overviewEntries.lastIndex)
                                        HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                                            modifier = Modifier.padding(start = Spacing.md))
                                }
                            }
                        }
                        Spacer(Modifier.height(Spacing.xl))
                    }

                    // Category pie chart
                    if (categoryEntries.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                            SectionLabel("SPENDING BY CATEGORY")
                        }
                        Spacer(Modifier.height(Spacing.md))
                        PieChart(
                            entries   = categoryEntries,
                            modifier  = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.xxxl),
                        )
                        Spacer(Modifier.height(Spacing.lg))
                        PieLegend(entries = categoryEntries, modifier = Modifier.padding(horizontal = Spacing.lg))
                        Spacer(Modifier.height(Spacing.xl))
                    }

                    // Member pie chart
                    if (memberEntries.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                            SectionLabel("SPENDING BY MEMBER")
                        }
                        Spacer(Modifier.height(Spacing.md))
                        PieChart(
                            entries   = memberEntries,
                            colorOffset = PIE_COLORS.size / 2,
                            modifier  = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.xxxl),
                        )
                        Spacer(Modifier.height(Spacing.lg))
                        PieLegend(entries = memberEntries, colorOffset = PIE_COLORS.size / 2,
                            modifier = Modifier.padding(horizontal = Spacing.lg))
                    }

                    Spacer(Modifier.height(Spacing.xxxl))
                }
            }
        }
    }
}

// ── MyAnalyticsViewModel ──────────────────────────────────────────────────────

@HiltViewModel
class MyAnalyticsViewModel @Inject constructor(
    private val analyticsApiService: AnalyticsApiService,
) : ViewModel() {

    private val _summaryState = MutableStateFlow<AnalyticsObjectState>(AnalyticsObjectState.Loading)
    val summaryState: StateFlow<AnalyticsObjectState> = _summaryState.asStateFlow()

    private val _statsState   = MutableStateFlow<AnalyticsObjectState>(AnalyticsObjectState.Loading)
    val statsState: StateFlow<AnalyticsObjectState> = _statsState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val summaryDef = async { safeApiCall { analyticsApiService.getPersonalSummary() } }
            val statsDef   = async { safeApiCall { analyticsApiService.getPersonalStats() } }
            _summaryState.value = when (val r = summaryDef.await()) {
                is ApiResult.Success      -> AnalyticsObjectState.Success(r.data)
                is ApiResult.NetworkError -> AnalyticsObjectState.Error("No internet connection.")
                else                      -> AnalyticsObjectState.Error("Failed to load analytics.")
            }
            _statsState.value = when (val r = statsDef.await()) {
                is ApiResult.Success -> AnalyticsObjectState.Success(r.data)
                else                 -> AnalyticsObjectState.Error("")
            }
        }
    }
}

// ── MyAnalyticsScreen ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAnalyticsScreen(
    onBack   : () -> Unit,
    viewModel: MyAnalyticsViewModel = hiltViewModel(),
) {
    val summaryState by viewModel.summaryState.collectAsState()
    val statsState   by viewModel.statsState.collectAsState()

    Scaffold(
        containerColor = Surface0,
        topBar         = { FsTopBar(title = "My analytics", onBack = onBack) },
    ) { innerPadding ->
        when (summaryState) {
            is AnalyticsObjectState.Loading -> FsLoadingScreen()
            is AnalyticsObjectState.Error   -> FsErrorScreen(
                message = (summaryState as AnalyticsObjectState.Error).message,
                onRetry = { viewModel.load() }
            )
            is AnalyticsObjectState.Success -> {
                val summary = ((summaryState as AnalyticsObjectState.Success).data) as? JsonObject ?: JsonObject(emptyMap())
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = Spacing.lg)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(Modifier.height(Spacing.md))
                    val entries = summary.entries.filter { it.key !in HIDDEN_FIELDS }.sortedBy { it.key }
                    if (entries.isNotEmpty()) {
                        SectionLabel("SPENDING SUMMARY")
                        Spacer(Modifier.height(Spacing.sm))
                        AnalyticsCard {
                            entries.forEachIndexed { i, (key, value) ->
                                AnalyticsRow(label = key.toReadable(), value = value.toDisplayString())
                                if (i < entries.lastIndex)
                                    HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = Spacing.md))
                            }
                        }
                        Spacer(Modifier.height(Spacing.xl))
                    }
                    val statsEntries = ((statsState as? AnalyticsObjectState.Success)?.data as? JsonObject)
                        ?.entries?.filter { it.key !in HIDDEN_FIELDS }?.sortedBy { it.key }
                    if (!statsEntries.isNullOrEmpty()) {
                        SectionLabel("STATS")
                        Spacer(Modifier.height(Spacing.sm))
                        AnalyticsCard {
                            statsEntries.forEachIndexed { i, (key, value) ->
                                AnalyticsRow(label = key.toReadable(), value = value.toDisplayString())
                                if (i < statsEntries.lastIndex)
                                    HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = Spacing.md))
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.xxxl))
                }
            }
        }
    }
}

// ── FriendAnalyticsViewModel ──────────────────────────────────────────────────

@HiltViewModel
class FriendAnalyticsViewModel @Inject constructor(
    private val analyticsApiService: AnalyticsApiService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val friendId: String = checkNotNull(savedStateHandle["friendId"])

    private val _trendsState = MutableStateFlow<AnalyticsListState>(AnalyticsListState.Loading)
    val trendsState: StateFlow<AnalyticsListState> = _trendsState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _trendsState.value = when (val r = safeApiCall { analyticsApiService.getFriendTrends(friendId) }) {
                is ApiResult.Success      -> AnalyticsListState.Success(r.data)
                is ApiResult.NetworkError -> AnalyticsListState.Error("No internet connection.")
                else                      -> AnalyticsListState.Error("Failed to load trends.")
            }
        }
    }
}

// ── FriendAnalyticsScreen ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendAnalyticsScreen(
    onBack   : () -> Unit,
    viewModel: FriendAnalyticsViewModel = hiltViewModel(),
) {
    val trendsState by viewModel.trendsState.collectAsState()

    Scaffold(
        containerColor = Surface0,
        topBar         = { FsTopBar(title = "Spending trends", onBack = onBack) },
    ) { innerPadding ->
        when (trendsState) {
            is AnalyticsListState.Loading -> FsLoadingScreen()
            is AnalyticsListState.Error   -> FsErrorScreen(
                message = (trendsState as AnalyticsListState.Error).message,
                onRetry = { viewModel.load() }
            )
            is AnalyticsListState.Success -> {
                val trends = ((trendsState as AnalyticsListState.Success).data) as? JsonArray ?: JsonArray(emptyList())
                if (trends.isEmpty()) {
                    FsErrorScreen(message = "No shared spending found.", onRetry = {})
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = Spacing.lg)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Spacer(Modifier.height(Spacing.md))
                        SectionLabel("MONTHLY TRENDS")
                        Spacer(Modifier.height(Spacing.sm))
                        AnalyticsCard {
                            trends.forEachIndexed { i, item ->
                                val obj    = item as? JsonObject ?: return@forEachIndexed
                                val year   = obj["year"]?.toDisplayString() ?: ""
                                val month  = obj["month"]?.toDisplayString()?.toIntOrNull()
                                    ?.let { monthName(it) } ?: ""
                                val amount = obj["amount"]?.toDisplayString() ?: "-"
                                AnalyticsRow(label = "$month $year", value = "$$amount")
                                if (i < trends.size - 1)
                                    HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = Spacing.md))
                            }
                        }
                        Spacer(Modifier.height(Spacing.xxxl))
                    }
                }
            }
        }
    }
}

// ── Pie chart composables ─────────────────────────────────────────────────────

@Composable
private fun PieChart(
    entries    : List<PieEntry>,
    modifier   : Modifier = Modifier,
    colorOffset: Int = 0,
) {
    val total = entries.sumOf { it.amount }.toFloat().coerceAtLeast(0.001f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            var startAngle = -90f
            entries.forEachIndexed { i, entry ->
                val sweep = (entry.amount.toFloat() / total) * 360f
                drawArc(
                    color      = PIE_COLORS[(i + colorOffset) % PIE_COLORS.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter  = true,
                )
                // White separator
                drawArc(
                    color      = Color.Black.copy(alpha = 0.15f),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter  = true,
                    style      = Stroke(width = 2f),
                )
                startAngle += sweep
            }
        }
    }
}

@Composable
private fun PieLegend(
    entries    : List<PieEntry>,
    modifier   : Modifier = Modifier,
    colorOffset: Int = 0,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2),
    ) {
        // Header
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(28.dp))
            Text(text = "Category", fontSize = 12.sp, color = TextTertiary,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(text = "Total", fontSize = 12.sp, color = TextTertiary,
                fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(color = Surface3, thickness = 0.5.dp)

        entries.forEachIndexed { i, entry ->
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Color swatch
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(PIE_COLORS[(i + colorOffset) % PIE_COLORS.size])
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text     = entry.label,
                    fontSize = 14.sp,
                    color    = TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text       = "$${String.format("%.2f", entry.amount)}",
                    fontSize   = 14.sp,
                    color      = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (i < entries.lastIndex)
                HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                    modifier = Modifier.padding(start = Spacing.md))
        }
    }
}

// ── Shared UI components ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 11.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = TextTertiary,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun AnalyticsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2),
    ) { content() }
}

@Composable
private fun AnalyticsRow(label: String, value: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = 14.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(text = value, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun kotlinx.serialization.json.JsonElement.toDisplayString(): String = when (this) {
    is JsonPrimitive -> contentOrNull ?: toString()
    is JsonArray     -> joinToString(", ") { it.toDisplayString() }
    is JsonNull      -> "-"
    else             -> toString().trim('"')
}

private fun String.toReadable(): String =
    replace('_', ' ')
        .replace(Regex("([A-Z])")) { " ${it.value}" }
        .trim()
        .replaceFirstChar { it.uppercase() }

private fun monthName(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
    5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
    9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
    else -> ""
}