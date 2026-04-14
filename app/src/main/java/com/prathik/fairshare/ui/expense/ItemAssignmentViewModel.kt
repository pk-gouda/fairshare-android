package com.prathik.fairshare.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.model.request.ItemAssignmentRequest
import com.prathik.fairshare.data.network.api.ExpenseApiService
import com.prathik.fairshare.data.network.api.ReceiptApiService
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.ExpenseItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemAssignmentViewModel @Inject constructor(
    private val receiptApiService: ReceiptApiService,
    private val expenseApiService: ExpenseApiService,
) : ViewModel() {

    private val _items       = MutableStateFlow<List<ExpenseItem>>(emptyList())
    val items: StateFlow<List<ExpenseItem>> = _items.asStateFlow()

    private val _isLoading   = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error       = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveState   = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // itemId → set of userIds assigned
    private val _assignments = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val assignments: StateFlow<Map<String, Set<String>>> = _assignments.asStateFlow()

    // ── Load for Add flow (from receipt, before expense is saved) ─────────────

    fun loadItems(receiptId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = safeApiCall { receiptApiService.getReceiptItems(receiptId) }
            when (result) {
                is ApiResult.Success -> {
                    _items.value = result.data.map { it.toDomain() }
                    _assignments.value = emptyMap()
                }
                else -> _error.value = "Failed to load receipt items"
            }
            _isLoading.value = false
        }
    }

    // ── Load for Edit flow (from saved expense) ───────────────────────────────

    fun loadItemsForExpense(expenseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = safeApiCall { expenseApiService.getExpenseItems(expenseId) }
            when (result) {
                is ApiResult.Success -> {
                    val itemList = result.data.map { it.toDomain() }
                    _items.value = itemList
                    // Pre-populate assignments from existing data
                    _assignments.value = itemList
                        .filter { it.assignedTo.isNotEmpty() }
                        .associate { item ->
                            item.id to item.assignedTo.map { it.userId }.toSet()
                        }
                }
                else -> _error.value = "Failed to load expense items"
            }
            _isLoading.value = false
        }
    }

    // ── Save assignments (edit flow only) ────────────────────────────────────

    fun saveAssignments(expenseId: String) {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            val result = safeApiCall {
                expenseApiService.assignItems(
                    expenseId = expenseId,
                    request   = ItemAssignmentRequest(buildAssignmentsMap()),
                )
            }
            _saveState.value = when (result) {
                is ApiResult.Success -> SaveState.Success
                else                 -> SaveState.Error("Failed to save assignments")
            }
        }
    }

    fun resetSaveState() { _saveState.value = SaveState.Idle }

    // ── Assignment manipulation ───────────────────────────────────────────────

    fun toggleAssignment(itemId: String, userId: String) {
        val current = _assignments.value.toMutableMap()
        val itemAssigned = current[itemId]?.toMutableSet() ?: mutableSetOf()
        if (itemAssigned.contains(userId)) itemAssigned.remove(userId) else itemAssigned.add(userId)
        current[itemId] = itemAssigned
        _assignments.value = current
    }

    fun assignAll(itemId: String, memberIds: List<String>) {
        val current = _assignments.value.toMutableMap()
        current[itemId] = memberIds.toSet()
        _assignments.value = current
    }

    fun clearAssignment(itemId: String) {
        val current = _assignments.value.toMutableMap()
        current.remove(itemId)
        _assignments.value = current
    }

    fun setAssignmentsFromMap(map: Map<String, List<String>>) {
        _assignments.value = map.mapValues { it.value.toSet() }
    }

    fun buildAssignmentsMap(): Map<String, List<String>> =
        _assignments.value
            .filter { it.value.isNotEmpty() }
            .mapValues { it.value.toList() }
}

sealed class SaveState {
    object Idle    : SaveState()
    object Saving  : SaveState()
    object Success : SaveState()
    data class Error(val message: String) : SaveState()
}