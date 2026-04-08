package com.prathik.fairshare.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.repository.FriendRepository
import com.prathik.fairshare.domain.repository.ImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportSplitwiseViewModel @Inject constructor(
    private val importRepository: ImportRepository,
    private val friendRepository: FriendRepository,
    private val tokenStore      : EncryptedTokenStore,
) : ViewModel() {

    val currentUserFullName: String get() = tokenStore.getFullName()

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    // CSV member names parsed from the header — used for "Which one is you?" dialog
    private val _csvMemberNames = MutableStateFlow<List<String>>(emptyList())
    val csvMemberNames: StateFlow<List<String>> = _csvMemberNames.asStateFlow()

    // After group import — unclaimed members
    private val _unclaimedMembers = MutableStateFlow<List<GroupMember>>(emptyList())
    val unclaimedMembers: StateFlow<List<GroupMember>> = _unclaimedMembers.asStateFlow()

    private val _claimState = MutableStateFlow<ClaimState>(ClaimState.Idle)
    val claimState: StateFlow<ClaimState> = _claimState.asStateFlow()

    init { loadFriends() }

    /** Parse active member names from a Splitwise CSV header for "Which one is you?" */
    fun parseCsvNames(csvContent: String) {
        val header = csvContent.lines().firstOrNull {
            it.contains(",") && !it.startsWith("Note") && !it.isBlank()
                    && it.contains("Date", ignoreCase = true)
        } ?: return
        val cols = header.split(",").map { it.trim().removeSurrounding("\"") }
        // Splitwise: Date, Description, Category, Cost, Currency, Name1, Name2, ...
        if (cols.size > 5) {
            _csvMemberNames.value = cols.drop(5)
                .filter { it.isNotBlank() && !it.contains("(removed)", ignoreCase = true) }
        }
    }

    fun clearCsvNames() { _csvMemberNames.value = emptyList() }

    private fun loadFriends() {
        viewModelScope.launch {
            when (val result = friendRepository.getFriends()) {
                is ApiResult.Success -> _friends.value = result.data
                else -> Unit
            }
        }
    }

    fun importGroup(csvContent: String, groupName: String, importerCsvName: String?) {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Loading("Importing group…")
            when (val result = importRepository.importGroup(csvContent, groupName, importerCsvName)) {
                is ApiResult.Success -> {
                    val data = result.data
                    val groupId = data["groupId"] as? String
                    val groupNameResult = data["groupName"] as? String ?: groupName
                    val expensesCreated = (data["expensesCreated"] as? Int) ?: 0
                    val settlementsCreated = (data["settlementsCreated"] as? Int) ?: 0
                    val warnings = (data["warnings"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                    // Load unclaimed members if we got a groupId
                    val unclaimed = if (groupId != null) {
                        when (val u = importRepository.getUnclaimedMembers(groupId)) {
                            is ApiResult.Success -> u.data
                            else -> emptyList()
                        }
                    } else emptyList()

                    _unclaimedMembers.value = unclaimed
                    _uiState.value = ImportUiState.GroupSuccess(
                        groupId          = groupId,
                        groupName        = groupNameResult,
                        expensesCreated  = expensesCreated,
                        settlementsCreated = settlementsCreated,
                        warnings         = warnings,
                        unclaimedMembers = unclaimed,
                    )
                }
                is ApiResult.ValidationError -> _uiState.value =
                    ImportUiState.Error("Invalid CSV format. Please export directly from Splitwise.")
                is ApiResult.NetworkError -> _uiState.value =
                    ImportUiState.Error("No internet connection. Please try again.")
                else -> _uiState.value =
                    ImportUiState.Error("Import failed. Please check the CSV and try again.")
            }
        }
    }

    fun importFriend(csvContent: String) {
        viewModelScope.launch {
            _uiState.value = ImportUiState.Loading("Importing expenses…")
            when (val result = importRepository.importFriend(csvContent)) {
                is ApiResult.Success -> {
                    val data = result.data
                    val expensesCreated = (data["expensesCreated"] as? Int) ?: 0
                    val settlementsCreated = (data["settlementsCreated"] as? Int) ?: 0
                    _uiState.value = ImportUiState.FriendSuccess(
                        expensesCreated    = expensesCreated,
                        settlementsCreated = settlementsCreated,
                    )
                }
                is ApiResult.ValidationError -> _uiState.value =
                    ImportUiState.Error("Invalid CSV format. Please export directly from Splitwise.")
                is ApiResult.NetworkError -> _uiState.value =
                    ImportUiState.Error("No internet connection. Please try again.")
                else -> _uiState.value =
                    ImportUiState.Error("Import failed. Please check the CSV and try again.")
            }
        }
    }

    fun assignPlaceholder(groupId: String, placeholderUserId: String, friendUserId: String) {
        viewModelScope.launch {
            _claimState.value = ClaimState.Loading
            when (importRepository.assignPlaceholder(groupId, placeholderUserId, friendUserId)) {
                is ApiResult.Success -> {
                    _unclaimedMembers.value = _unclaimedMembers.value
                        .filter { it.userId != placeholderUserId }
                    _claimState.value = ClaimState.Done
                }
                else -> _claimState.value = ClaimState.Error("Failed to assign member.")
            }
        }
    }

    fun claimIdentity(groupId: String, placeholderUserId: String) {
        viewModelScope.launch {
            _claimState.value = ClaimState.Loading
            when (importRepository.claimIdentity(groupId, placeholderUserId)) {
                is ApiResult.Success -> {
                    _unclaimedMembers.value = _unclaimedMembers.value
                        .filter { it.userId != placeholderUserId }
                    _claimState.value = ClaimState.Done
                }
                else -> _claimState.value = ClaimState.Error("Failed to claim identity.")
            }
        }
    }

    fun resetClaimState() { _claimState.value = ClaimState.Idle }
    fun reset() { _uiState.value = ImportUiState.Idle }
    fun setError(message: String) { _uiState.value = ImportUiState.Error(message) }

}

// ── UI States ─────────────────────────────────────────────────────────────────

sealed class ImportUiState {
    object Idle : ImportUiState()
    data class Loading(val message: String) : ImportUiState()
    data class GroupSuccess(
        val groupId           : String?,
        val groupName         : String,
        val expensesCreated   : Int,
        val settlementsCreated: Int,
        val warnings          : List<String>,
        val unclaimedMembers  : List<GroupMember>,
    ) : ImportUiState()
    data class FriendSuccess(
        val expensesCreated   : Int,
        val settlementsCreated: Int,
    ) : ImportUiState()
    data class Error(val message: String) : ImportUiState()
}

sealed class ClaimState {
    object Idle    : ClaimState()
    object Loading : ClaimState()
    object Done    : ClaimState()
    data class Error(val message: String) : ClaimState()
}