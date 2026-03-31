package com.prathik.fairshare.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Balance
import com.prathik.fairshare.domain.model.User
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

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getMyProfileUseCase  : GetMyProfileUseCase,
    private val updateProfileUseCase : UpdateProfileUseCase,
    private val getAllBalancesUseCase : GetAllBalancesUseCase,
    private val logoutUseCase        : LogoutUseCase,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _profile = MutableStateFlow<User?>(null)
    val profile: StateFlow<User?> = _profile.asStateFlow()

    private val _balanceSummary = MutableStateFlow<BalanceSummary?>(null)
    val balanceSummary: StateFlow<BalanceSummary?> = _balanceSummary.asStateFlow()

    private val _actionState = MutableStateFlow<AccountActionState>(AccountActionState.Idle)
    val actionState: StateFlow<AccountActionState> = _actionState.asStateFlow()

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true

            val profileDeferred  = async { getMyProfileUseCase() }
            val balanceDeferred  = async { getAllBalancesUseCase() }

            when (val result = profileDeferred.await()) {
                is ApiResult.Success -> _profile.value = result.data
                else -> Unit
            }
            when (val result = balanceDeferred.await()) {
                is ApiResult.Success -> _balanceSummary.value = buildSummary(result.data)
                else -> Unit
            }

            _isLoading.value = false
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

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onDone()
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
        return BalanceSummary(
            owedToMe   = owedToMe,
            youOwe     = youOwe,
            netBalance = owedToMe - youOwe,
            currency   = balances.firstOrNull()?.currency ?: "USD",
        )
    }
}

sealed class AccountActionState {
    object Idle      : AccountActionState()
    object LoggedOut : AccountActionState()
    data class Success(val message: String) : AccountActionState()
    data class Error(val message: String)   : AccountActionState()
}