package com.prathik.fairshare.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.User
import com.prathik.fairshare.domain.repository.UserRepository
import com.prathik.fairshare.domain.usecase.auth.LogoutUseCase
import com.prathik.fairshare.domain.usecase.balance.GetAllBalancesUseCase
import com.prathik.fairshare.domain.usecase.user.GetMyProfileUseCase
import com.prathik.fairshare.domain.usecase.user.UpdateProfileUseCase
import com.prathik.fairshare.ui.groups.BalanceSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.prathik.fairshare.domain.model.BalanceCurrencyEntry

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getMyProfileUseCase  : GetMyProfileUseCase,
    private val updateProfileUseCase : UpdateProfileUseCase,
    private val getAllBalancesUseCase : GetAllBalancesUseCase,
    private val logoutUseCase        : LogoutUseCase,
    private val userRepository       : UserRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _manualRefreshing = MutableStateFlow(false)
    val manualRefreshing: StateFlow<Boolean> = _manualRefreshing.asStateFlow()

    private val _profileLoadFailed = MutableStateFlow(false)
    val profileLoadFailed: StateFlow<Boolean> = _profileLoadFailed.asStateFlow()

    private var initialLoadDone = false

    private val _profile = MutableStateFlow<User?>(null)
    val profile: StateFlow<User?> = _profile.asStateFlow()

    private val _balanceSummary = MutableStateFlow<BalanceSummary?>(null)
    val balanceSummary: StateFlow<BalanceSummary?> = _balanceSummary.asStateFlow()

    private val _actionState = MutableStateFlow<AccountActionState>(AccountActionState.Idle)
    val actionState: StateFlow<AccountActionState> = _actionState.asStateFlow()

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            initialLoadDone = false

            // Show skeleton until a real User profile object exists.
            // EncryptedTokenStore has userId/fullName but not email/notifications/authProvider —
            // not enough to safely render AccountScreen without blank/fake fields.
            if (_profile.value == null) {
                _isLoading.value = true
            }

            // Step 2: Network fetch.
            val profileDeferred = async { getMyProfileUseCase() }
            val balanceDeferred = async { getAllBalancesUseCase() }

            when (val result = profileDeferred.await()) {
                is ApiResult.Success -> {
                    _profile.value = result.data
                    _profileLoadFailed.value = false
                }
                is ApiResult.NetworkError -> {
                    if (_profile.value == null) _profileLoadFailed.value = true
                }
                else -> {
                    if (_profile.value == null) _profileLoadFailed.value = true
                }
            }
            when (val result = balanceDeferred.await()) {
                is ApiResult.Success -> _balanceSummary.value = buildSummary(result.data)
                else -> Unit
            }

            _isLoading.value = false
            initialLoadDone = true
        }
    }

    fun refresh(manual: Boolean = false) {
        if (!initialLoadDone) return
        viewModelScope.launch {
            if (manual) _manualRefreshing.value = true
            try {
                val profileDeferred = async { getMyProfileUseCase() }
                val balanceDeferred = async { getAllBalancesUseCase() }
                when (val result = profileDeferred.await()) {
                    is ApiResult.Success -> {
                        _profile.value = result.data
                        _profileLoadFailed.value = false
                    }
                    else -> Unit  // keep existing profile visible
                }
                when (val result = balanceDeferred.await()) {
                    is ApiResult.Success -> _balanceSummary.value = buildSummary(result.data)
                    else -> Unit
                }
            } finally {
                if (manual) _manualRefreshing.value = false
            }
        }
    }

    fun toggleNotifications() {
        val current = _profile.value?.notificationEnabled ?: return
        viewModelScope.launch {
            when (val result = updateProfileUseCase(notificationEnabled = !current)) {
                is ApiResult.Success -> _profile.value = result.data
                else -> _actionState.value = AccountActionState.Error("Failed to update notifications")
            }
        }
    }

    fun updateCurrency(currency: String) {
        viewModelScope.launch {
            when (val result = updateProfileUseCase(preferredCurrency = currency)) {
                is ApiResult.Success -> _profile.value = result.data
                else -> _actionState.value = AccountActionState.Error("Failed to update currency")
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onDone()
        }
    }

    /**
     * Deactivate the account with optional password verification.
     *
     * LOCAL accounts: [password] must be provided and will be verified server-side
     * against the BCrypt hash before the action proceeds.
     *
     * GOOGLE/APPLE accounts: [password] is null — the valid JWT is sufficient.
     * The server skips the password check for non-LOCAL providers.
     *
     * On success: emits [AccountActionState.Deactivated]. The caller (AccountScreen)
     * is responsible for triggering logout + routing to Login.
     */
    fun deactivateAccount(password: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            _actionState.value = AccountActionState.Loading
            when (val result = userRepository.deactivateAccount(password)) {
                is ApiResult.Success -> {
                    // Clear local session — server invalidates tokens on next request.
                    logoutUseCase()
                    _actionState.value = AccountActionState.Deactivated
                    onDone()
                }
                is ApiResult.HttpError -> _actionState.value = AccountActionState.Error(
                    result.message ?: "Failed to deactivate account"
                )
                // BusinessLogicException (e.g. unsettled balances) returns 409 Conflict
                is ApiResult.Conflict -> _actionState.value = AccountActionState.Error(
                    result.message ?: "Cannot deactivate account"
                )
                is ApiResult.Unauthorized -> _actionState.value = AccountActionState.Error(
                    result.message ?: "Incorrect password"
                )
                else -> _actionState.value = AccountActionState.Error(
                    "Something went wrong. Please try again."
                )
            }
        }
    }

    /**
     * Permanently delete the account with optional password verification.
     *
     * Same password policy as [deactivateAccount]. This action is irreversible —
     * personal data is anonymised on the server and all local caches are cleared.
     *
     * On success: emits [AccountActionState.Deleted]. The caller routes to Login.
     */
    fun deleteAccount(password: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            _actionState.value = AccountActionState.Loading
            when (val result = userRepository.deleteAccount(password)) {
                is ApiResult.Success -> {
                    // logoutUseCase clears tokens; UserRepositoryImpl.deleteAccount()
                    // already cleared all local DAOs on success.
                    logoutUseCase()
                    _actionState.value = AccountActionState.Deleted
                    onDone()
                }
                is ApiResult.HttpError -> _actionState.value = AccountActionState.Error(
                    result.message ?: "Failed to delete account"
                )
                // BusinessLogicException (e.g. unsettled balances) returns 409 Conflict
                is ApiResult.Conflict -> _actionState.value = AccountActionState.Error(
                    result.message ?: "Cannot delete account"
                )
                is ApiResult.Unauthorized -> _actionState.value = AccountActionState.Error(
                    result.message ?: "Incorrect password"
                )
                else -> _actionState.value = AccountActionState.Error(
                    "Something went wrong. Please try again."
                )
            }
        }
    }

    fun resetActionState() { _actionState.value = AccountActionState.Idle }

    private fun buildSummary(balances: List<Balance>): BalanceSummary {
        var owedToMe = 0.0
        var youOwe   = 0.0
        for (b in balances) {
            if (b.amount > 0) owedToMe += b.amount
            else youOwe += Math.abs(b.amount)
        }
        // Build per-currency entries for the multi-currency summary
        val entries = balances.groupBy { it.currency }.map { (currency, list) ->
            BalanceCurrencyEntry(
                currency = currency,
                owedToMe = list.filter { it.amount > 0 }.sumOf { it.amount },
                youOwe   = list.filter { it.amount < 0 }.sumOf { -it.amount },
                net      = list.sumOf { it.amount },
            )
        }.filter { it.owedToMe > 0.0 || it.youOwe > 0.0 }
        return BalanceSummary(
            owedToMe = owedToMe,
            youOwe   = youOwe,
            entries  = entries,
        )
    }
}

sealed class AccountActionState {
    object Idle        : AccountActionState()
    object Loading     : AccountActionState()
    object LoggedOut   : AccountActionState()
    object Deactivated : AccountActionState()
    object Deleted     : AccountActionState()
    data class Success(val message: String) : AccountActionState()
    data class Error(val message: String)   : AccountActionState()
}