package com.prathik.fairshare.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.repository.BalanceRepository
import com.prathik.fairshare.domain.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupBalancesViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val balanceRepository: BalanceRepository,
    private val tokenStore: EncryptedTokenStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])
    val currentUserId: String? = tokenStore.getUserId()

    private val _state = MutableStateFlow<GroupBalancesUiState>(GroupBalancesUiState.Loading)
    val state: StateFlow<GroupBalancesUiState> = _state.asStateFlow()

    init { load() }

    private var balancesLoaded = false

    fun load(silent: Boolean = false) {
        viewModelScope.launch {
            // Step 1: Render cached balances immediately — no network wait.
            val cached = balanceRepository.getCachedGroupBalances(groupId)
            if (cached.isNotEmpty()) {
                _state.value = GroupBalancesUiState.Success(
                    allBalances = cached,
                    memberNets  = computeMemberNets(cached),
                )
                balancesLoaded = true
            } else if (!silent && !balancesLoaded) {
                _state.value = GroupBalancesUiState.Loading
            }

            // Step 2: Network fetch.
            when (val result = groupRepository.getAllGroupBalances(groupId)) {
                is ApiResult.Success -> {
                    val balances = result.data
                    // Compute net per-member: sum of all their balances from their perspective
                    // Balance(userId=A, otherUserId=B, amount=+10) means B owes A $10
                    // We need each member's net: what they get back or owe across all others
                    val memberNets = computeMemberNets(balances)
                    _state.value = GroupBalancesUiState.Success(
                        allBalances = balances,
                        memberNets  = memberNets,
                    )
                    balancesLoaded = true
                }
                is ApiResult.NetworkError -> {
                    if (!balancesLoaded) _state.value = GroupBalancesUiState.Error("No internet connection.")
                }
                else -> {
                    if (!balancesLoaded) _state.value = GroupBalancesUiState.Error("Failed to load balances.")
                }
            }
        }
    }

    /**
     * Computes net balance per member by summing all their balance records.
     * Returns a map of userId → net amount (positive = gets back, negative = owes).
     * Also collects their display name.
     */
    private fun computeMemberNets(balances: List<Balance>): List<MemberNet> {
        // Collect all userIds and names from both sides of each balance record
        val nameMap = mutableMapOf<String, String>()
        val netMap  = mutableMapOf<String, Double>()

        balances.forEach { b ->
            nameMap[b.userId]      = nameMap.getOrDefault(b.userId, "")      // will fill below
            nameMap[b.otherUserId] = b.otherUserName

            // From b.userId's perspective: positive = otherUser owes them
            netMap[b.userId] = (netMap[b.userId] ?: 0.0) + b.amount
        }

        // Fill userId's own name from the otherUserName of the inverse record if not found
        // As a fallback just use what we have
        return netMap.keys.map { userId ->
            val userBalances = balances.filter { it.userId == userId && it.amount != 0.0 }
            // Per-currency nets — never sum across currencies
            val netByCurrency = userBalances.groupBy { it.currency }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .filter { it.value != 0.0 }
            val dominantNet = netByCurrency.entries.maxByOrNull { Math.abs(it.value) }
            MemberNet(
                userId        = userId,
                name          = nameMap[userId] ?: userId,
                net           = dominantNet?.value ?: 0.0,
                currency      = dominantNet?.key ?: "USD",
                netByCurrency = netByCurrency,
                details       = userBalances,
            )
        }.sortedByDescending { Math.abs(it.net) }
    }
}

data class MemberNet(
    val userId        : String,
    val name          : String,
    val net           : Double,           // dominant currency net (for sorting/ring)
    val currency      : String,           // dominant currency code
    val netByCurrency : Map<String, Double> = emptyMap(),  // full per-currency breakdown
    val details       : List<Balance>,    // per-pair breakdown
)

sealed class GroupBalancesUiState {
    object Loading : GroupBalancesUiState()
    data class Success(
        val allBalances: List<Balance>,
        val memberNets : List<MemberNet>,
    ) : GroupBalancesUiState()
    data class Error(val message: String) : GroupBalancesUiState()
}