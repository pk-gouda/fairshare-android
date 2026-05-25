package com.prathik.fairshare.ui.groups

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.prathik.fairshare.ui.components.FsSkeletonBlock
import com.prathik.fairshare.ui.components.FsSkeletonTimelineRow
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.util.MoneyUtils
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.sqrt

// ── Palette ───────────────────────────────────────────────────────────────────

private val PIE_COLORS = listOf(
    Color(0xFF4FC3F7), Color(0xFFAED581), Color(0xFFFFB74D), Color(0xFFF06292),
    Color(0xFF9575CD), Color(0xFF4DB6AC), Color(0xFFFF8A65), Color(0xFF90A4AE),
    Color(0xFFFFF176), Color(0xFF80CBC4), Color(0xFFCE93D8), Color(0xFFEF9A9A),
)

private val HIDDEN_FIELDS = setOf("groupId", "userId", "isArchived", "groupName")

// ── UI state types ─────────────────────────────────────────────────────────────

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

data class PieEntry(val label: String, val amount: Double, val percentage: Float)
data class MemberChip(val userId: String?, val fullName: String)
data class MonthChip(val year: Int?, val month: Int?, val label: String)

sealed class GroupChartState {
    object Loading : GroupChartState()
    data class Success(
        val members       : List<MemberChip>,
        val categories    : List<PieEntry>,
        val totalPaid     : Double,
        val totalOwes     : Double,
        val totalBalance  : Double,
        val selectedMember: MemberChip,
        val currency      : String = "USD",
    ) : GroupChartState()
    data class Error(val message: String) : GroupChartState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class GroupAnalyticsViewModel @Inject constructor(
    private val analyticsApiService: AnalyticsApiService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _chartState = MutableStateFlow<GroupChartState>(GroupChartState.Loading)
    val chartState: StateFlow<GroupChartState> = _chartState.asStateFlow()

    private val _selectedMember = MutableStateFlow(MemberChip(null, "All members"))
    val selectedMember: StateFlow<MemberChip> = _selectedMember.asStateFlow()

    private val _selectedMonth = MutableStateFlow(MonthChip(null, null, "All time"))
    val selectedMonth: StateFlow<MonthChip> = _selectedMonth.asStateFlow()

    private val _months = MutableStateFlow<List<MonthChip>>(buildMonthChips())
    val months: StateFlow<List<MonthChip>> = _months.asStateFlow()

    init { load() }

    fun selectMember(member: MemberChip) { _selectedMember.value = member; load() }
    fun selectMonth(month: MonthChip)   { _selectedMonth.value = month;   load() }

    fun load() {
        viewModelScope.launch {
            _chartState.value = GroupChartState.Loading
            val member    = _selectedMember.value
            val monthChip = _selectedMonth.value

            val startDate = monthChip.year?.let {
                "${it}-${monthChip.month!!.toString().padStart(2,'0')}-01T00:00:00"
            }
            val endDate = monthChip.year?.let {
                val last = LocalDate.of(it, monthChip.month!!, 1).plusMonths(1).minusDays(1)
                "${last}T23:59:59"
            }

            when (val r = safeApiCall {
                analyticsApiService.getGroupChartData(
                    groupId      = groupId,
                    memberUserId = member.userId,
                    startDate    = startDate,
                    endDate      = endDate,
                )
            }) {
                is ApiResult.Success -> {
                    val obj = r.data as? JsonObject ?: run {
                        _chartState.value = GroupChartState.Error("Invalid response")
                        return@launch
                    }

                    val currentMembers = (_chartState.value as? GroupChartState.Success)?.members
                    val members = currentMembers ?: run {
                        val raw = (obj["members"] as? JsonArray)?.mapNotNull { m ->
                            val mo = m as? JsonObject ?: return@mapNotNull null
                            MemberChip(
                                userId   = mo["userId"]?.toStr() ?: return@mapNotNull null,
                                fullName = mo["fullName"]?.toStr() ?: return@mapNotNull null,
                            )
                        } ?: emptyList()
                        listOf(MemberChip(null, "All members")) + raw
                    }

                    val cats = (obj["categories"] as? JsonArray)?.mapNotNull { item ->
                        val co = item as? JsonObject ?: return@mapNotNull null
                        val label  = co["category"]?.toStr()?.categoryLabel() ?: return@mapNotNull null
                        val amount = if (member.userId == null)
                            co["categoryTotal"]?.toStr()?.toDoubleOrNull() ?: 0.0
                        else
                            co["userOwes"]?.toStr()?.toDoubleOrNull() ?: 0.0
                        if (amount < 0.01) null else PieEntry(label, amount, 0f)
                    }?.sortedByDescending { it.amount } ?: emptyList()

                    val total = cats.sumOf { it.amount }.coerceAtLeast(0.001)
                    val catsWithPct = cats.map { it.copy(percentage = ((it.amount / total) * 100).toFloat()) }

                    val totalPaid = if (member.userId == null) 0.0 else
                        (obj["categories"] as? JsonArray)?.sumOf {
                            (it as? JsonObject)?.get("userPaid")?.toStr()?.toDoubleOrNull() ?: 0.0
                        } ?: 0.0
                    val totalOwes    = cats.sumOf { it.amount }
                    val totalBalance = totalPaid - totalOwes

                    val currency = obj["currency"]?.toStr()?.takeIf { it.isNotBlank() } ?: "USD"

                    _chartState.value = GroupChartState.Success(
                        members        = members,
                        categories     = catsWithPct,
                        totalPaid      = totalPaid,
                        totalOwes      = totalOwes,
                        totalBalance   = totalBalance,
                        selectedMember = member,
                        currency       = currency,
                    )
                }
                is ApiResult.NetworkError -> _chartState.value = GroupChartState.Error("No internet connection.")
                else -> _chartState.value = GroupChartState.Error("Failed to load analytics.")
            }
        }
    }

    private fun buildMonthChips(): List<MonthChip> {
        val now = LocalDate.now()
        return listOf(MonthChip(null, null, "All time")) + (0..11).map { i ->
            val d = now.minusMonths(i.toLong())
            MonthChip(d.year, d.monthValue, "${monthName(d.monthValue)} ${d.year}")
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupAnalyticsScreen(
    onBack   : () -> Unit,
    viewModel: GroupAnalyticsViewModel = hiltViewModel(),
) {
    val chartState     by viewModel.chartState.collectAsState()
    val selectedMember by viewModel.selectedMember.collectAsState()
    val selectedMonth  by viewModel.selectedMonth.collectAsState()
    val months         by viewModel.months.collectAsState()

    Scaffold(
        containerColor = Surface0,
        topBar = { FsTopBar(title = "Analytics", onBack = onBack) },
    ) { innerPadding ->
        when (val state = chartState) {
            is GroupChartState.Loading -> AnalyticsSkeleton(Modifier.padding(innerPadding))
            is GroupChartState.Error   -> FsErrorScreen(message = state.message, onRetry = { viewModel.load() })
            is GroupChartState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(Modifier.height(Spacing.md))

                    // ── Dropdowns row ─────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        // Member dropdown
                        if (state.members.size > 1) {
                            AnalyticsDropdown(
                                label    = selectedMember.fullName,
                                options  = state.members.map { it.fullName },
                                onSelect = { idx -> viewModel.selectMember(state.members[idx]) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Month dropdown
                        AnalyticsDropdown(
                            label    = selectedMonth.label,
                            options  = months.map { it.label },
                            onSelect = { idx -> viewModel.selectMonth(months[idx]) },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(Modifier.height(Spacing.lg))

                    // ── Summary card (specific member only) ───────────────────
                    if (selectedMember.userId != null && (state.totalPaid > 0 || state.totalOwes > 0)) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = Spacing.lg)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.xl))
                                .background(Surface2)
                                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            SummaryCol("Paid",  MoneyUtils.format(state.totalPaid, state.currency),  TextPrimary)
                            SummaryCol("Share", MoneyUtils.format(state.totalOwes, state.currency),  TextSecondary)
                            val netColor = if (state.totalBalance >= 0) Green400 else Negative
                            val netSign  = if (state.totalBalance >= 0) "+" else "-"
                            val netText  = "$netSign${MoneyUtils.format(Math.abs(state.totalBalance), state.currency)}"
                            SummaryCol("Net", netText, netColor)
                        }
                        Spacer(Modifier.height(Spacing.xl))
                    }

                    // ── Donut + legend ────────────────────────────────────────
                    if (state.categories.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                            SectionLabel("SPENDING BY CATEGORY")
                        }
                        Spacer(Modifier.height(Spacing.md))

                        val catTotal = state.categories.sumOf { it.amount }
                        var tappedIndex by remember { mutableStateOf<Int?>(null) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 56.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            DonutChart(
                                entries       = state.categories,
                                selectedIndex = tappedIndex,
                                onSliceTapped = { idx -> tappedIndex = if (tappedIndex == idx) null else idx },
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val tapped = tappedIndex?.let { state.categories.getOrNull(it) }
                                if (tapped != null) {
                                    Text(
                                        text       = tapped.label,
                                        fontSize   = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = TextPrimary,
                                    )
                                    Text(
                                        text     = "${MoneyUtils.format(tapped.amount, state.currency)} · ${String.format("%.0f", tapped.percentage)}%",
                                        fontSize = 12.sp,
                                        color    = TextSecondary,
                                    )
                                } else {
                                    Text(
                                        text       = MoneyUtils.format(catTotal, state.currency),
                                        fontSize   = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = TextPrimary,
                                    )
                                    Text(
                                        text     = if (selectedMember.userId == null) "group total" else "${selectedMember.fullName.split(" ").first()}'s share",
                                        fontSize = 11.sp,
                                        color    = TextTertiary,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(Spacing.lg))

                        // Legend
                        Column(
                            modifier = Modifier
                                .padding(horizontal = Spacing.lg)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.xl))
                                .background(Surface2),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                            ) {
                                Spacer(Modifier.width(22.dp))
                                Text("Category", fontSize = 12.sp, color = TextTertiary,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text("Amount", fontSize = 12.sp, color = TextTertiary,
                                    fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(Spacing.sm))
                                Text("%", fontSize = 12.sp, color = TextTertiary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
                            }
                            HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                            state.categories.forEachIndexed { i, entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.md, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(PIE_COLORS[i % PIE_COLORS.size])
                                    )
                                    Spacer(Modifier.width(Spacing.sm))
                                    Text(entry.label, fontSize = 13.sp, color = TextSecondary,
                                        modifier = Modifier.weight(1f))
                                    Text(MoneyUtils.format(entry.amount, state.currency), fontSize = 13.sp,
                                        color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.width(Spacing.sm))
                                    Text("${String.format("%.0f", entry.percentage)}%",
                                        fontSize = 12.sp, color = TextTertiary,
                                        modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
                                }
                                if (i < state.categories.lastIndex)
                                    HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 38.dp))
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xxxl),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📊", fontSize = 32.sp)
                                Spacer(Modifier.height(Spacing.md))
                                Text("No spending data for this selection",
                                    fontSize = 14.sp, color = TextTertiary, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}

// ── Dropdown component ────────────────────────────────────────────────────────

@Composable
private fun AnalyticsDropdown(
    label    : String,
    options  : List<String>,
    onSelect : (Int) -> Unit,
    modifier : Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.lg))
                .background(Surface2)
                .border(1.dp, Surface4, RoundedCornerShape(Radius.lg))
                .clickable { expanded = true }
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text       = label,
                fontSize   = 13.sp,
                color      = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines   = 1,
                modifier   = Modifier.weight(1f),
            )
            Icon(
                imageVector        = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint               = TextSecondary,
                modifier           = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false },
            containerColor   = Surface2,
        ) {
            options.forEachIndexed { idx, option ->
                DropdownMenuItem(
                    text    = { Text(option, fontSize = 14.sp, color = if (label == option) Green400 else TextPrimary) },
                    onClick = { onSelect(idx); expanded = false },
                    colors  = MenuDefaults.itemColors(textColor = TextPrimary),
                )
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun SummaryCol(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(3.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun DonutChart(
    entries       : List<PieEntry>,
    selectedIndex : Int? = null,
    onSliceTapped : (Int) -> Unit = {},
) {
    val total = entries.sumOf { it.amount }.toFloat().coerceAtLeast(0.001f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(entries) {
                detectTapGestures { tap ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = tap.x - cx
                    val dy = tap.y - cy
                    val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    val strokePx = 44.dp.toPx()
                    val minDim = minOf(size.width, size.height).toFloat()
                    val outerR = (minDim - strokePx) / 2f + strokePx / 2
                    val innerR = (minDim - strokePx) / 2f - strokePx / 2
                    if (dist < innerR || dist > outerR) return@detectTapGestures
                    var tapAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                    if (tapAngle < 0) tapAngle += 360f
                    var start = 0f
                    entries.forEachIndexed { i, entry ->
                        val sweep = (entry.amount.toFloat() / total) * 360f
                        if (tapAngle in start..(start + sweep)) {
                            onSliceTapped(i)
                            return@detectTapGestures
                        }
                        start += sweep
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke   = 44.dp.toPx()
            val diameter = size.minDimension - stroke
            val tl       = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            var start    = -90f
            entries.forEachIndexed { i, entry ->
                val sweep     = (entry.amount.toFloat() / total) * 360f
                val isSelected = i == selectedIndex
                drawArc(
                    color      = PIE_COLORS[i % PIE_COLORS.size].let {
                        if (selectedIndex != null && !isSelected) it.copy(alpha = 0.4f) else it
                    },
                    startAngle = start,
                    sweepAngle = sweep - 1.5f,
                    useCenter  = false,
                    topLeft    = Offset(tl.x, tl.y),
                    size       = Size(diameter, diameter),
                    style      = Stroke(width = if (isSelected) stroke * 1.25f else stroke),
                )
                start += sweep
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        color = TextTertiary, letterSpacing = 0.8.sp)
}

@Composable
private fun AnalyticsCard(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.xl)).background(Surface2)) {
        content()
    }
}

@Composable
private fun AnalyticsRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun JsonElement.toStr(): String = when (this) {
    is JsonPrimitive -> contentOrNull ?: toString()
    is JsonNull      -> ""
    else             -> toString().trim('"')
}

private fun JsonElement.toDisplayString(): String = when (this) {
    is JsonPrimitive -> contentOrNull ?: toString()
    is JsonArray     -> joinToString(", ") { it.toDisplayString() }
    is JsonNull      -> "-"
    else             -> toString().trim('"')
}

private fun String.toReadable(): String =
    replace('_', ' ').replace(Regex("([A-Z])")) { " ${it.value}" }
        .trim().replaceFirstChar { it.uppercase() }

private fun String.categoryLabel(): String =
    replace('_', ' ').lowercase()
        .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        .replace("Tv Phone Internet", "TV/Phone/Internet")
        .replace("Bus Train", "Bus/Train")
        .replace("Heat Gas", "Heat & Gas")

private fun fmt(v: Double): String = String.format("%.2f", v)

private fun monthName(month: Int): String = when (month) {
    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"
    5 -> "May"; 6 -> "Jun"; 7 -> "Jul"; 8 -> "Aug"
    9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
    else -> ""
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
            is AnalyticsObjectState.Loading -> AnalyticsSkeleton(Modifier.padding(innerPadding))
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

data class FriendMonthEntry(val label: String, val amount: Double)

sealed class FriendChartState {
    object Loading : FriendChartState()
    data class Success(
        val friendName   : String,
        val categories   : List<PieEntry>,
        val months       : List<FriendMonthEntry>,
        val totalPaid    : Double,
        val totalOwes    : Double,
        val totalBalance : Double,
        val currency     : String = "USD",
    ) : FriendChartState()
    data class Error(val message: String) : FriendChartState()
}

@HiltViewModel
class FriendAnalyticsViewModel @Inject constructor(
    private val analyticsApiService: AnalyticsApiService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val friendId: String = checkNotNull(savedStateHandle["friendId"])

    private val _chartState = MutableStateFlow<FriendChartState>(FriendChartState.Loading)
    val chartState: StateFlow<FriendChartState> = _chartState.asStateFlow()

    private val _selectedMonth = MutableStateFlow(MonthChip(null, null, "All time"))
    val selectedMonth: StateFlow<MonthChip> = _selectedMonth.asStateFlow()

    private val _months = MutableStateFlow<List<MonthChip>>(buildMonthChips())
    val months: StateFlow<List<MonthChip>> = _months.asStateFlow()

    private var allTrends: List<FriendMonthEntry> = emptyList()
    private var cachedBreakdown: JsonObject? = null

    init { load() }

    fun selectMonth(month: MonthChip) {
        _selectedMonth.value = month
        applyFilter()
    }

    fun load() {
        viewModelScope.launch {
            _chartState.value = FriendChartState.Loading
            val trendsDeferred    = async { safeApiCall { analyticsApiService.getFriendTrends(friendId) } }
            val breakdownDeferred = async { safeApiCall { analyticsApiService.getFriendBreakdown(friendId) } }

            val trendsResult    = trendsDeferred.await()
            val breakdownResult = breakdownDeferred.await()

            if (trendsResult is ApiResult.NetworkError) {
                _chartState.value = FriendChartState.Error("No internet connection.")
                return@launch
            }

            // Parse trends
            allTrends = ((trendsResult as? ApiResult.Success)?.data as? JsonArray)?.mapNotNull { item ->
                val obj    = item as? JsonObject ?: return@mapNotNull null
                val year   = obj["year"]?.toStr()?.toIntOrNull() ?: return@mapNotNull null
                val month  = obj["month"]?.toStr()?.toIntOrNull() ?: return@mapNotNull null
                val amount = obj["amount"]?.toStr()?.toDoubleOrNull() ?: 0.0
                FriendMonthEntry("${monthName(month)} $year", amount)
            } ?: emptyList()

            // Cache breakdown
            cachedBreakdown = (breakdownResult as? ApiResult.Success)?.data as? JsonObject

            applyFilter()
        }
    }

    private fun applyFilter() {
        val month = _selectedMonth.value
        val filtered = if (month.year == null) allTrends
        else allTrends.filter { it.label == "${monthName(month.month!!)} ${month.year}" }

        val breakdown = cachedBreakdown
        val friendName = breakdown?.get("otherUserName")?.toStr() ?: "Friend"
        val totalPaid  = breakdown?.get("totalPaid")?.toStr()?.toDoubleOrNull() ?: 0.0
        val totalOwes  = breakdown?.get("totalOwes")?.toStr()?.toDoubleOrNull() ?: 0.0

        val cats = (breakdown?.get("categories") as? JsonArray)?.mapNotNull { item ->
            val co = item as? JsonObject ?: return@mapNotNull null
            val label  = co["category"]?.toStr()?.categoryLabel() ?: return@mapNotNull null
            val amount = co["userOwes"]?.toStr()?.toDoubleOrNull() ?: 0.0
            val pct    = co["percentage"]?.toStr()?.toFloatOrNull() ?: 0f
            if (amount < 0.01) null else PieEntry(label, amount, pct)
        }?.sortedByDescending { it.amount } ?: emptyList()

        val currency = breakdown?.get("currency")?.toStr()?.takeIf { it.isNotBlank() } ?: "USD"

        _chartState.value = FriendChartState.Success(
            friendName   = friendName,
            categories   = cats,
            months       = filtered,
            totalPaid    = totalPaid,
            totalOwes    = totalOwes,
            totalBalance = totalPaid - totalOwes,
            currency     = currency,
        )
    }

    private fun buildMonthChips(): List<MonthChip> {
        val now = LocalDate.now()
        return listOf(MonthChip(null, null, "All time")) + (0..11).map { i ->
            val d = now.minusMonths(i.toLong())
            MonthChip(d.year, d.monthValue, "${monthName(d.monthValue)} ${d.year}")
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
    val chartState    by viewModel.chartState.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val months        by viewModel.months.collectAsState()

    Scaffold(
        containerColor = Surface0,
        topBar         = { FsTopBar(title = "Spending trends", onBack = onBack) },
    ) { innerPadding ->
        when (val state = chartState) {
            is FriendChartState.Loading -> AnalyticsSkeleton(Modifier.padding(innerPadding))
            is FriendChartState.Error   -> FsErrorScreen(message = state.message, onRetry = { viewModel.load() })
            is FriendChartState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(Modifier.height(Spacing.md))

                    // Month dropdown
                    AnalyticsDropdown(
                        label    = selectedMonth.label,
                        options  = months.map { it.label },
                        onSelect = { idx -> viewModel.selectMonth(months[idx]) },
                        modifier = Modifier.padding(horizontal = Spacing.lg).fillMaxWidth(),
                    )

                    Spacer(Modifier.height(Spacing.lg))

                    // Summary card
                    if (state.totalPaid > 0 || state.totalOwes > 0) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = Spacing.lg)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.xl))
                                .background(Surface2)
                                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            SummaryCol("Paid", MoneyUtils.format(state.totalPaid, state.currency), TextPrimary)
                            SummaryCol("Owes", MoneyUtils.format(state.totalOwes, state.currency), TextSecondary)
                            val netColor = if (state.totalBalance >= 0) Green400 else Negative
                            val netSign  = if (state.totalBalance >= 0) "+" else "-"
                            val netText  = "$netSign${MoneyUtils.format(Math.abs(state.totalBalance), state.currency)}"
                            SummaryCol("Net", netText, netColor)
                        }
                        Spacer(Modifier.height(Spacing.xl))
                    }

                    // Donut + legend (category breakdown)
                    if (state.categories.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                            SectionLabel("SPENDING BY CATEGORY")
                        }
                        Spacer(Modifier.height(Spacing.md))

                        val catTotal = state.categories.sumOf { it.amount }
                        var tappedIndex by remember { mutableStateOf<Int?>(null) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 56.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            DonutChart(
                                entries       = state.categories,
                                selectedIndex = tappedIndex,
                                onSliceTapped = { idx -> tappedIndex = if (tappedIndex == idx) null else idx },
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val tapped = tappedIndex?.let { state.categories.getOrNull(it) }
                                if (tapped != null) {
                                    Text(tapped.label, fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text("${MoneyUtils.format(tapped.amount, state.currency)} · ${String.format("%.0f", tapped.percentage)}%",
                                        fontSize = 12.sp, color = TextSecondary)
                                } else {
                                    Text(MoneyUtils.format(catTotal, state.currency), fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("your share", fontSize = 11.sp, color = TextTertiary)
                                }
                            }
                        }

                        Spacer(Modifier.height(Spacing.lg))

                        Column(
                            modifier = Modifier
                                .padding(horizontal = Spacing.lg)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.xl))
                                .background(Surface2),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
                                Spacer(Modifier.width(22.dp))
                                Text("Category", fontSize = 12.sp, color = TextTertiary,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text("Amount", fontSize = 12.sp, color = TextTertiary,
                                    fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(Spacing.sm))
                                Text("%", fontSize = 12.sp, color = TextTertiary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
                            }
                            HorizontalDivider(color = Surface3, thickness = 0.5.dp)
                            state.categories.forEachIndexed { i, entry ->
                                Row(modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = Spacing.md, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(14.dp).clip(RoundedCornerShape(3.dp))
                                        .background(PIE_COLORS[i % PIE_COLORS.size]))
                                    Spacer(Modifier.width(Spacing.sm))
                                    Text(entry.label, fontSize = 13.sp, color = TextSecondary,
                                        modifier = Modifier.weight(1f))
                                    Text(MoneyUtils.format(entry.amount, state.currency), fontSize = 13.sp,
                                        color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.width(Spacing.sm))
                                    Text("${String.format("%.0f", entry.percentage)}%",
                                        fontSize = 12.sp, color = TextTertiary,
                                        modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
                                }
                                if (i < state.categories.lastIndex)
                                    HorizontalDivider(color = Surface3, thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 38.dp))
                            }
                        }

                        Spacer(Modifier.height(Spacing.xl))
                    }

                    // Monthly bar chart
                    if (state.months.isNotEmpty()) {
                        Column(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                            SectionLabel("MONTHLY BREAKDOWN")
                            Spacer(Modifier.height(Spacing.sm))
                        }
                        val maxAmt = state.months.maxOf { it.amount }.coerceAtLeast(0.001)
                        Column(
                            modifier = Modifier
                                .padding(horizontal = Spacing.lg)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.xl))
                                .background(Surface2)
                                .padding(Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            state.months.forEachIndexed { i, entry ->
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()) {
                                    Text(entry.label, fontSize = 12.sp, color = TextSecondary,
                                        modifier = Modifier.width(72.dp))
                                    Spacer(Modifier.width(Spacing.sm))
                                    Box(modifier = Modifier.weight(1f).height(22.dp)
                                        .clip(RoundedCornerShape(4.dp)).background(Surface3)) {
                                        Box(modifier = Modifier.height(22.dp)
                                            .fillMaxWidth((entry.amount / maxAmt).toFloat())
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(PIE_COLORS[i % PIE_COLORS.size]))
                                    }
                                    Spacer(Modifier.width(Spacing.sm))
                                    Text(MoneyUtils.format(entry.amount, state.currency), fontSize = 12.sp,
                                        color = TextPrimary, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.width(72.dp), textAlign = TextAlign.End)
                                }
                            }
                        }
                    } else if (state.categories.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xxxl),
                            contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📊", fontSize = 32.sp)
                                Spacer(Modifier.height(Spacing.md))
                                Text("No shared spending found", fontSize = 14.sp, color = TextTertiary)
                            }
                        }
                    }

                    Spacer(Modifier.height(100.dp))
                }
            }
        }
    }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
private fun AnalyticsChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    small: Boolean = false,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.full))
            .background(if (isSelected) Green400 else Surface2)
            .border(1.dp, if (isSelected) Green400 else Surface4, RoundedCornerShape(Radius.full))
            .clickable(onClick = onClick)
            .padding(horizontal = if (small) Spacing.sm else Spacing.md, vertical = if (small) 5.dp else 7.dp),
    ) {
        Text(
            text       = label,
            fontSize   = if (small) 11.sp else 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (isSelected) Surface0 else TextSecondary,
        )
    }
}

// ── Analytics skeleton ────────────────────────────────────────────────────────

@androidx.compose.runtime.Composable
private fun AnalyticsSkeleton(modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
    ) {
        com.prathik.fairshare.ui.components.FsSkeletonBlock(height = 200.dp, widthFraction = 1f, cornerRadius = 12.dp)
        repeat(3) {
            com.prathik.fairshare.ui.components.FsSkeletonBlock(height = 60.dp, widthFraction = 1f, cornerRadius = 10.dp)
        }
    }
}