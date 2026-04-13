package com.prathik.fairshare.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.network.api.ReceiptApiService
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.ExpenseItem
import com.prathik.fairshare.domain.model.GroupMember
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemAssignmentViewModel @Inject constructor(
    private val receiptApiService: ReceiptApiService,
) : ViewModel() {

    private val _items       = MutableStateFlow<List<ExpenseItem>>(emptyList())
    val items: StateFlow<List<ExpenseItem>> = _items.asStateFlow()

    private val _isLoading   = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error       = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // itemId → set of userIds assigned
    private val _assignments = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val assignments: StateFlow<Map<String, Set<String>>> = _assignments.asStateFlow()

    fun loadItems(receiptId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = safeApiCall { receiptApiService.getReceiptItems(receiptId) }
            when (result) {
                is ApiResult.Success -> {
                    _items.value = result.data.map { it.toDomain() }
                    // Default: all members assigned to every item (equals split)
                    _assignments.value = emptyMap()
                }
                else -> _error.value = "Failed to load receipt items"
            }
            _isLoading.value = false
        }
    }

    fun toggleAssignment(itemId: String, userId: String) {
        val current = _assignments.value.toMutableMap()
        val itemAssigned = current[itemId]?.toMutableSet() ?: mutableSetOf()
        if (itemAssigned.contains(userId)) {
            itemAssigned.remove(userId)
        } else {
            itemAssigned.add(userId)
        }
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

    /**
     * Returns the final assignments map ready for CreateExpenseRequest.
     * Items with empty/null assignments default to all members (backend handles this).
     */
    fun buildAssignmentsMap(): Map<String, List<String>> =
        _assignments.value
            .filter { it.value.isNotEmpty() }
            .mapValues { it.value.toList() }
}