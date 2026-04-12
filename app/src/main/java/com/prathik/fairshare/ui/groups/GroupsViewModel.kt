package com.prathik.fairshare.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.usecase.balance.GetAllBalancesUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupBalancesUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupsUseCase
import kotlinx.coroutines.awaitAll
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for GroupsHomeScreen.
 *
 * Loads groups and balances in parallel using async/await.
 * Exposes separate state flows so the UI can show partial data
 * while the rest is loading.
 */
@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val getGroupsUseCase: GetGroupsUseCase,
    private val getAllBalancesUseCase: GetAllBalancesUseCase,
    private val getGroupBalancesUseCase: GetGroupBalancesUseCase,
) : ViewModel() {

    // ── Groups state ──────────────────────────────────────────────────────────
    private val _groupsState = MutableStateFlow<GroupsUiState>(GroupsUiState.Loading)
    val groupsState: StateFlow<GroupsUiState> = _groupsState.asStateFlow()

    // ── Search query ──────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchChanged(query: String) {
        _searchQuery.value = query
    }

    // ── Balance summary state ─────────────────────────────────────────────────
    private val _balanceSummary = MutableStateFlow<BalanceSummary?>(null)
    val balanceSummary: StateFlow<BalanceSummary?> = _balanceSummary.asStateFlow()

    // ── Per-group balance: groupId → net amount for current user ──────────────
    // Positive = others owe you in this group, Negative = you owe in this group
    // null key not present = no expenses yet
    private val _groupBalanceMap = MutableStateFlow<Map<String, Double>>(emptyMap())
    val groupBalanceMap: StateFlow<Map<String, Double>> = _groupBalanceMap.asStateFlow()

    init {
        loadData()
    }

    /**
     * Loads groups and balances in parallel.
     * Both calls start simultaneously — UI gets data as soon as each completes.
     */
    fun loadData() {
        viewModelScope.launch {
            // Only show loading spinner if we have no data yet
            if (_groupsState.value !is GroupsUiState.Success) {
                _groupsState.value = GroupsUiState.Loading
            }

            // Load groups — instant from Room cache after first launch
            when (val result = getGroupsUseCase()) {
                is ApiResult.Success -> {
                    _groupsState.value = GroupsUiState.Success(result.data)

                    // Load per-group balances and overall summary in background
                    // Groups show immediately — balance cards fill in when ready
                    launch {
                        val groupBalanceResults = result.data.map { group ->
                            async {
                                group.id to when (val r = getGroupBalancesUseCase(group.id)) {
                                    is ApiResult.Success -> r.data.sumOf { it.amount }
                                    else -> null
                                }
                            }
                        }.awaitAll()
                        _groupBalanceMap.value = groupBalanceResults
                            .mapNotNull { (id, bal) -> bal?.let { id to it } }
                            .toMap()
                    }

                    launch {
                        when (val result = getAllBalancesUseCase()) {
                            is ApiResult.Success -> _balanceSummary.value = calculateSummary(result.data)
                            else -> Unit
                        }
                    }
                }
                is ApiResult.NetworkError -> {
                    if (_groupsState.value !is GroupsUiState.Success) {
                        _groupsState.value = GroupsUiState.Error(
                            message = result.message,
                            isNetwork = true,
                        )
                    }
                }
                else -> {
                    if (_groupsState.value !is GroupsUiState.Success) {
                        _groupsState.value = GroupsUiState.Error(
                            message = "Failed to load groups. Please try again.",
                            isNetwork = false,
                        )
                    }
                }
            }
        }
    }

    /**
     * Calculates net balance summary from the list of all balances.
     *
     * owedToMe  — sum of positive balances (others owe you)
     * youOwe    — sum of negative balances (you owe others), shown as positive
     * netBalance — owedToMe - youOwe
     */
    private fun calculateSummary(balances: List<Balance>): BalanceSummary {
        var owedToMe = 0.0
        var youOwe = 0.0

        balances.forEach { balance ->
            if (balance.amount > 0) owedToMe += balance.amount
            else youOwe += Math.abs(balance.amount)
        }

        return BalanceSummary(
            owedToMe = owedToMe,
            youOwe = youOwe,
            netBalance = owedToMe - youOwe,
            currency = balances.firstOrNull()?.currency ?: "USD",
        )
    }
}

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class GroupsUiState {
    object Loading : GroupsUiState()
    data class Success(val groups: List<Group>) : GroupsUiState()
    data class Error(val message: String, val isNetwork: Boolean) : GroupsUiState()
}

data class BalanceSummary(
    val owedToMe: Double,
    val youOwe: Double,
    val netBalance: Double,
    val currency: String,
)