package com.prathik.fairshare.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.usecase.expense.GetGroupExpensesUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val getGroupExpensesUseCase: GetGroupExpensesUseCase,
    private val getGroupsUseCase       : GetGroupsUseCase,
    savedStateHandle                   : SavedStateHandle,
) : ViewModel() {

    // Pre-filled group from GroupDetailScreen
    private val prefilledGroupId: String? = savedStateHandle["groupId"]

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _allExpenses = MutableStateFlow<List<Expense>>(emptyList())

    private val _results = MutableStateFlow<List<Expense>>(emptyList())
    val results: StateFlow<List<Expense>> = _results.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<String?>(prefilledGroupId)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAllData()
        // Debounce search to avoid firing on every keystroke
        _query
            .debounce(200)
            .distinctUntilChanged()
            .onEach { applyFilters() }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(q: String) { _query.value = q }

    fun onGroupFilterSelected(groupId: String?) {
        _selectedGroupId.value = groupId
        applyFilters()
    }

    private fun loadAllData() {
        viewModelScope.launch {
            _isLoading.value = true

            // Load groups for filter chips
            val groupsResult = getGroupsUseCase()
            if (groupsResult is ApiResult.Success) {
                _groups.value = groupsResult.data

                // Load expenses from all groups
                val allExpenses = mutableListOf<Expense>()
                val targetGroups = if (prefilledGroupId != null)
                    groupsResult.data.filter { it.id == prefilledGroupId }
                else groupsResult.data

                targetGroups.forEach { group ->
                    val expResult = getGroupExpensesUseCase(group.id)
                    if (expResult is ApiResult.Success) {
                        allExpenses.addAll(expResult.data)
                    }
                }
                _allExpenses.value = allExpenses.sortedByDescending { it.expenseDate }
                applyFilters()
            }
            _isLoading.value = false
        }
    }

    private fun applyFilters() {
        val q         = _query.value.trim().lowercase()
        val groupId   = _selectedGroupId.value

        _results.value = _allExpenses.value.filter { expense ->
            val matchesQuery = q.isBlank() || expense.description.lowercase().contains(q)
            val matchesGroup = groupId == null || expense.groupId == groupId
            matchesQuery && matchesGroup
        }
    }
}