package com.prathik.fairshare.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.model.request.ItemAssignmentRequest
import com.prathik.fairshare.data.network.api.ExpenseApiService
import com.prathik.fairshare.data.network.api.ReceiptApiService
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.ExpenseItem
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.util.MoneyUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.absoluteValue

enum class SplitMode { EQUAL, AMOUNT, PERCENT, SHARES }

data class ShareUiState(
    val itemId    : String,
    val shareIdx  : Int,
    val amount    : Double,
    val splitMode : SplitMode           = SplitMode.EQUAL,
    val selected  : List<String>        = emptyList(),
    val amounts   : Map<String, String> = emptyMap(),
    val percents  : Map<String, String> = emptyMap(),
    val shares    : Map<String, String> = emptyMap(),
    val isAdvanced: Boolean             = false,
) {
    val key get() = "$itemId-$shareIdx"

    fun assignedAmount(): Double {
        return when (splitMode) {
            SplitMode.EQUAL   -> if (selected.isEmpty()) 0.0 else amount
            SplitMode.AMOUNT  -> amounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }.coerceAtMost(amount)
            SplitMode.PERCENT -> amount * percents.values.sumOf { it.toDoubleOrNull() ?: 0.0 }.coerceAtMost(100.0) / 100.0
            SplitMode.SHARES  -> if (shares.values.sumOf { it.toDoubleOrNull() ?: 0.0 } > 0.0) amount else 0.0
        }
    }

    fun myAmount(userId: String): Double {
        return when (splitMode) {
            SplitMode.EQUAL   -> if (userId in selected) amount / selected.size else 0.0
            SplitMode.AMOUNT  -> amounts[userId]?.toDoubleOrNull() ?: 0.0
            SplitMode.PERCENT -> amount * ((percents[userId]?.toDoubleOrNull() ?: 0.0) / 100.0)
            SplitMode.SHARES  -> {
                val total = shares.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
                if (total > 0.0) amount * ((shares[userId]?.toDoubleOrNull() ?: 0.0) / total) else 0.0
            }
        }
    }

    fun isComplete(): Boolean {
        return when (splitMode) {
            SplitMode.EQUAL   -> selected.isNotEmpty()
            SplitMode.AMOUNT  -> (amounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 } - amount).absoluteValue < 0.01
            SplitMode.PERCENT -> (percents.values.sumOf { it.toDoubleOrNull() ?: 0.0 } - 100.0).absoluteValue < 0.1
            SplitMode.SHARES  -> shares.values.sumOf { it.toDoubleOrNull() ?: 0.0 } > 0.0
        }
    }

    fun summaryText(members: List<GroupMember>, currency: String): String {
        return when (splitMode) {
            SplitMode.EQUAL -> {
                if (selected.isEmpty()) "Unassigned"
                else {
                    val per   = amount / selected.size
                    val names = selected.take(3).mapNotNull { uid ->
                        members.find { it.userId == uid }?.fullName?.split(" ")?.first()
                    }
                    val extra = if (selected.size > 3) " +${selected.size - 3}" else ""
                    if (selected.size == 1) "${names.firstOrNull() ?: ""} only • ${MoneyUtils.format(per, currency)}"
                    else "${names.joinToString(", ")}$extra • ${MoneyUtils.format(per, currency)} each"
                }
            }
            SplitMode.AMOUNT -> {
                val entries = amounts.entries.filter { (it.value.toDoubleOrNull() ?: 0.0) > 0 }
                if (entries.isEmpty()) "Unassigned"
                else entries.take(3).joinToString(", ") { (uid, v) ->
                    "${members.find { it.userId == uid }?.fullName?.initials() ?: uid.take(2)} ${MoneyUtils.format(v.toDoubleOrNull() ?: 0.0, currency)}"
                } + " • Amount"
            }
            SplitMode.PERCENT -> {
                val entries = percents.entries.filter { (it.value.toDoubleOrNull() ?: 0.0) > 0 }
                if (entries.isEmpty()) "Unassigned"
                else entries.take(3).joinToString(", ") { (uid, v) ->
                    "${members.find { it.userId == uid }?.fullName?.initials() ?: uid.take(2)} $v%"
                } + " • Percent"
            }
            SplitMode.SHARES -> {
                val entries = shares.entries.filter { (it.value.toDoubleOrNull() ?: 0.0) > 0 }
                if (entries.isEmpty()) "Unassigned"
                else entries.take(3).joinToString(", ") { (uid, v) ->
                    "${members.find { it.userId == uid }?.fullName?.initials() ?: uid.take(2)} ${v}x"
                } + " • Shares"
            }
        }
    }

    fun calcText(currency: String): String {
        if (selected.isEmpty()) return "Select people to split equally"
        val per = amount / selected.size
        return if (selected.size == 1) "${MoneyUtils.format(per, currency)} • 1 person"
        else "${MoneyUtils.format(per, currency)} each • ${selected.size} people"
    }

    fun advStatusText(currency: String): String {
        return when (splitMode) {
            SplitMode.EQUAL   -> calcText(currency)
            SplitMode.AMOUNT  -> {
                val tot = amounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
                val rem = amount - tot
                if (rem.absoluteValue < 0.01)
                    "Assigned: ${MoneyUtils.format(tot, currency)} / ${MoneyUtils.format(amount, currency)} ✓"
                else
                    "Assigned: ${MoneyUtils.format(tot, currency)} / ${MoneyUtils.format(amount, currency)} • ${MoneyUtils.format(rem.absoluteValue, currency)} ${if (rem > 0) "left" else "over"}"
            }
            SplitMode.PERCENT -> {
                val tot = percents.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
                val rem = 100.0 - tot
                if (rem.absoluteValue < 0.1) "Assigned: ${"%.1f".format(tot)}% / 100% ✓"
                else "Assigned: ${"%.1f".format(tot)}% / 100% • ${"%.1f".format(rem.absoluteValue)}% ${if (rem > 0) "left" else "over"}"
            }
            SplitMode.SHARES  -> {
                val tot = shares.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
                if (tot == 0.0) "Enter share counts for each person"
                else "${MoneyUtils.format(amount / tot, currency)} per share • ${tot.toInt()} total shares"
            }
        }
    }

    fun advStatusIsError(): Boolean {
        return when (splitMode) {
            SplitMode.AMOUNT  -> amounts.values.sumOf { it.toDoubleOrNull() ?: 0.0 } > amount + 0.01
            SplitMode.PERCENT -> percents.values.sumOf { it.toDoubleOrNull() ?: 0.0 } > 100.1
            else              -> false
        }
    }

    fun advStatusIsWarn(): Boolean = !isComplete() && !advStatusIsError()
}

private fun String.initials() = split(" ").filter { it.isNotBlank() }.take(2)
    .joinToString("") { it.first().uppercase() }

@HiltViewModel
class ItemAssignmentViewModel @Inject constructor(
    private val receiptApiService : ReceiptApiService,
    private val expenseApiService : ExpenseApiService,
    private val tokenStore        : EncryptedTokenStore,
) : ViewModel() {

    val currentUserId: String? = tokenStore.getUserId()

    private val _items         = MutableStateFlow<List<ExpenseItem>>(emptyList())
    val items: StateFlow<List<ExpenseItem>> = _items.asStateFlow()

    private val _isLoading     = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error         = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveState     = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _shareStates   = MutableStateFlow<Map<String, ShareUiState>>(emptyMap())
    val shareStates: StateFlow<Map<String, ShareUiState>> = _shareStates.asStateFlow()

    private val _separateItems = MutableStateFlow<Set<String>>(emptySet())
    val separateItems: StateFlow<Set<String>> = _separateItems.asStateFlow()

    private val _expandedKey   = MutableStateFlow<String?>(null)
    val expandedKey: StateFlow<String?> = _expandedKey.asStateFlow()

    // ── Reactive totals — recompute whenever items or share state changes ──────

    val receiptTotal: StateFlow<Double> = _items
        .map { list -> list.sumOf { it.totalPrice } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val totalAssigned: StateFlow<Double> =
        combine(_items, _shareStates, _separateItems) { items, states, sep ->
            items.flatMap { activeKeysFor(it, sep) }
                .mapNotNull { states[it]?.assignedAmount() }
                .sum()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val myTotal: StateFlow<Double> =
        combine(_items, _shareStates, _separateItems) { items, states, sep ->
            val uid = currentUserId ?: return@combine 0.0
            items.flatMap { activeKeysFor(it, sep) }
                .mapNotNull { states[it]?.myAmount(uid) }
                .sum()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadItems(receiptId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = safeApiCall { receiptApiService.getReceiptItems(receiptId) }) {
                is ApiResult.Success -> {
                    val items = result.data.map { it.toDomain() }
                    _items.value = items
                    initShareStates(items)
                }
                else -> _error.value = "Failed to load receipt items"
            }
            _isLoading.value = false
        }
    }

    fun loadItemsForExpense(expenseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = safeApiCall { expenseApiService.getExpenseItems(expenseId) }) {
                is ApiResult.Success -> {
                    val items = result.data.map { it.toDomain() }
                    _items.value = items
                    initShareStates(items)
                    val current = _shareStates.value.toMutableMap()
                    items.forEach { item ->
                        val assigned = item.assignedTo.map { it.userId }
                        if (assigned.isNotEmpty()) {
                            current["${item.id}-0"]?.let { s ->
                                current["${item.id}-0"] = s.copy(selected = assigned)
                            }
                        }
                    }
                    _shareStates.value = current
                }
                else -> _error.value = "Failed to load expense items"
            }
            _isLoading.value = false
        }
    }

    private fun initShareStates(items: List<ExpenseItem>) {
        _shareStates.value = buildMap {
            items.forEach { item ->
                put("${item.id}-0", ShareUiState(item.id, 0, item.totalPrice))
                if ((item.quantity ?: 1) > 1)
                    put("${item.id}-1", ShareUiState(item.id, 1, item.price))
            }
        }
        _separateItems.value = emptySet()
        _expandedKey.value   = null
    }

    // ── Key helpers ───────────────────────────────────────────────────────────

    private fun activeKeysFor(item: ExpenseItem, sep: Set<String>): List<String> {
        val qty = item.quantity ?: 1
        return if (item.id in sep && qty > 1)
            (0 until qty).map { "${item.id}-$it" }
        else
            listOf("${item.id}-0")
    }

    fun shareKeysForItem(itemId: String): List<String> {
        val item = _items.value.find { it.id == itemId } ?: return listOf("$itemId-0")
        return activeKeysFor(item, _separateItems.value)
    }

    // ── Expand ────────────────────────────────────────────────────────────────

    fun toggleExpand(key: String) {
        _expandedKey.value = if (_expandedKey.value == key) null else key
    }

    // ── Normal / Advanced ────────────────────────────────────────────────────

    fun showAdvanced(key: String) = updateShare(key) { it.copy(isAdvanced = true) }
    fun showNormal(key: String)   = updateShare(key) { it.copy(isAdvanced = false) }

    // ── Split mode ────────────────────────────────────────────────────────────

    fun setSplitMode(key: String, mode: SplitMode) =
        updateShare(key) { it.copy(splitMode = mode) }

    // ── Per-person selection ──────────────────────────────────────────────────

    fun togglePerson(key: String, userId: String) = updateShare(key) { share ->
        val list = share.selected.toMutableList()
        if (userId in list) list.remove(userId) else list.add(userId)
        share.copy(selected = list)
    }

    /** Select all members for this share */
    fun selectAll(key: String, members: List<GroupMember>) =
        updateShare(key) { it.copy(splitMode = SplitMode.EQUAL, selected = members.map { m -> m.userId }) }

    /** Clear all assignments for this share */
    fun clearShare(key: String) = updateShare(key) {
        it.copy(selected = emptyList(), amounts = emptyMap(), percents = emptyMap(), shares = emptyMap())
    }

    // ── Input updates ─────────────────────────────────────────────────────────

    fun setAmount(key: String, userId: String, raw: String) =
        updateShare(key) { it.copy(amounts = it.amounts + (userId to raw)) }

    fun setPercent(key: String, userId: String, raw: String) =
        updateShare(key) { it.copy(percents = it.percents + (userId to raw)) }

    fun setShares(key: String, userId: String, raw: String) =
        updateShare(key) { it.copy(shares = it.shares + (userId to raw)) }

    // ── Qty separate ─────────────────────────────────────────────────────────

    fun enableSeparate(itemId: String) {
        val item = _items.value.find { it.id == itemId } ?: return
        val qty = item.quantity ?: 1
        _separateItems.value = _separateItems.value + itemId
        val current = _shareStates.value.toMutableMap()
        // Create one share per unit, each with unit price
        for (i in 0 until qty) {
            current["$itemId-$i"] = ShareUiState(itemId, i, item.price)
        }
        _shareStates.value = current
        if (_expandedKey.value?.startsWith(itemId) == true) _expandedKey.value = null
    }

    fun disableSeparate(itemId: String) {
        val item = _items.value.find { it.id == itemId } ?: return
        val qty = item.quantity ?: 1
        _separateItems.value = _separateItems.value - itemId
        val current = _shareStates.value.toMutableMap()
        // Remove all individual share keys, reset to single combined share
        for (i in 1 until qty) current.remove("$itemId-$i")
        current["$itemId-0"] = ShareUiState(itemId, 0, item.totalPrice)
        _shareStates.value = current
        // Collapse expanded key if it was any of the now-removed shares
        if (_expandedKey.value?.let { it.startsWith(itemId) && it != "$itemId-0" } == true) {
            _expandedKey.value = null
        }
    }

    /** Returns true if any share beyond the first has assignments (used to confirm merge) */
    fun share1HasAssignments(itemId: String): Boolean {
        val item = _items.value.find { it.id == itemId } ?: return false
        val qty = item.quantity ?: 1
        return (1 until qty).any { i ->
            val s = _shareStates.value["$itemId-$i"] ?: return@any false
            s.selected.isNotEmpty() ||
                    s.amounts.values.any { (it.toDoubleOrNull() ?: 0.0) > 0 } ||
                    s.percents.values.any { (it.toDoubleOrNull() ?: 0.0) > 0 } ||
                    s.shares.values.any { (it.toDoubleOrNull() ?: 0.0) > 0 }
        }
    }

    // ── Bulk actions ──────────────────────────────────────────────────────────

    /** Assign all currently unassigned shares to the current user */
    fun assignRemainingToMe() {
        val uid = currentUserId ?: return
        val current = _shareStates.value.toMutableMap()
        _items.value.forEach { item ->
            shareKeysForItem(item.id).forEach { key ->
                val share = current[key] ?: return@forEach
                if (share.assignedAmount() == 0.0) {
                    current[key] = share.copy(splitMode = SplitMode.EQUAL, selected = listOf(uid))
                }
            }
        }
        _shareStates.value = current
        _expandedKey.value = null
    }

    /** Split all currently unassigned shares equally among all members */
    fun splitRemainingEqually(members: List<GroupMember>) {
        val allIds = members.map { it.userId }
        val current = _shareStates.value.toMutableMap()
        _items.value.forEach { item ->
            shareKeysForItem(item.id).forEach { key ->
                val share = current[key] ?: return@forEach
                if (share.assignedAmount() == 0.0) {
                    current[key] = share.copy(splitMode = SplitMode.EQUAL, selected = allIds)
                }
            }
        }
        _shareStates.value = current
        _expandedKey.value = null
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    fun saveAssignments(expenseId: String) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            _saveState.value = when (val r = safeApiCall {
                expenseApiService.assignItems(expenseId, ItemAssignmentRequest(buildAssignmentsMap()))
            }) {
                is ApiResult.Success -> SaveState.Success
                else                 -> SaveState.Error("Failed to save assignments")
            }
        }
    }

    fun resetSaveState() { _saveState.value = SaveState.Idle }

    /**
     * Compute accurate per-person dollar totals across ALL shares.
     *
     * This is the authoritative calculation used for:
     *   - ReviewSubmitScreen display
     *   - splitData sent to the backend (bypasses broken backend re-computation)
     *
     * For each active share key we call shareState.myAmount(userId) which
     * already handles EQUAL/AMOUNT/PERCENT/SHARES correctly per share.
     * We accumulate across all shares of all items.
     *
     * Members who end up with $0.00 are excluded — they are not participants.
     *
     * Returns: Map<userId, amountOwed>  (only members with amount > 0)
     */
    fun buildSplitData(): Map<String, Double> {
        val totals = mutableMapOf<String, Double>()

        _items.value.forEach { item ->
            shareKeysForItem(item.id).forEach { key ->
                val share = _shareStates.value[key] ?: return@forEach
                // Collect all userIds who have any assignment in this share
                val participantIds: List<String> = when (share.splitMode) {
                    SplitMode.EQUAL   -> share.selected
                    SplitMode.AMOUNT  -> share.amounts.filter { (_, v) -> (v.toDoubleOrNull() ?: 0.0) > 0 }.keys.toList()
                    SplitMode.PERCENT -> share.percents.filter { (_, v) -> (v.toDoubleOrNull() ?: 0.0) > 0 }.keys.toList()
                    SplitMode.SHARES  -> share.shares.filter { (_, v) -> (v.toDoubleOrNull() ?: 0.0) > 0 }.keys.toList()
                }
                participantIds.forEach { uid ->
                    val amount = share.myAmount(uid)
                    if (amount > 0.0) {
                        totals[uid] = (totals[uid] ?: 0.0) + amount
                    }
                }
            }
        }

        // Only return members who actually owe something
        return totals.filter { (_, v) -> v > 0.001 }
    }

    /**
     * Build item-level assignment map for the backend's assignItems() API.
     * Used only in the edit-expense flow.
     *
     * NOTE: This unions assignees across all shares of an item — intentionally,
     * because the backend stores one assignment list per item (not per share).
     * Dollar amounts come from buildSplitData() instead.
     */
    fun buildAssignmentsMap(): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableSet<String>>()
        _items.value.forEach { item ->
            shareKeysForItem(item.id).forEach { key ->
                val share = _shareStates.value[key] ?: return@forEach
                val assignees = when (share.splitMode) {
                    SplitMode.EQUAL   -> share.selected
                    SplitMode.AMOUNT  -> share.amounts.filter { (_, v) -> (v.toDoubleOrNull() ?: 0.0) > 0 }.keys.toList()
                    SplitMode.PERCENT -> share.percents.filter { (_, v) -> (v.toDoubleOrNull() ?: 0.0) > 0 }.keys.toList()
                    SplitMode.SHARES  -> share.shares.filter { (_, v) -> (v.toDoubleOrNull() ?: 0.0) > 0 }.keys.toList()
                }
                if (assignees.isNotEmpty()) result.getOrPut(item.id) { mutableSetOf() }.addAll(assignees)
            }
        }
        return result.mapValues { it.value.toList() }
    }

    private fun updateShare(key: String, update: (ShareUiState) -> ShareUiState) {
        val current = _shareStates.value.toMutableMap()
        current[key]?.let { current[key] = update(it) }
        _shareStates.value = current
    }
}

sealed class SaveState {
    object Idle    : SaveState()
    object Saving  : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}