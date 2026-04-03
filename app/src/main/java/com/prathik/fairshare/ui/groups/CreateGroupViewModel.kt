package com.prathik.fairshare.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.GroupType
import com.prathik.fairshare.domain.usecase.group.CreateGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val createGroupUseCase: CreateGroupUseCase,
) : ViewModel() {

    private val _name        = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _selectedType = MutableStateFlow(GroupType.FRIENDS)
    val selectedType: StateFlow<GroupType> = _selectedType.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState = MutableStateFlow<CreateGroupActionState>(CreateGroupActionState.Idle)
    val actionState: StateFlow<CreateGroupActionState> = _actionState.asStateFlow()

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    fun onNameChanged(value: String) {
        _name.value = value
        _nameError.value = null
    }

    fun onDescriptionChanged(value: String) { _description.value = value }
    fun onTypeSelected(type: GroupType)      { _selectedType.value = type }

    fun createGroup() {
        val name = _name.value.trim()
        if (name.isBlank()) { _nameError.value = "Group name is required"; return }
        if (name.length < 2) { _nameError.value = "Name must be at least 2 characters"; return }

        viewModelScope.launch {
            _isLoading.value = true
            when (val result = createGroupUseCase(
                name        = name,
                type        = _selectedType.value.name,
                description = _description.value.trim().ifBlank { null },
            )) {
                is ApiResult.Success -> _actionState.value = CreateGroupActionState.Success(result.data.id)
                is ApiResult.ValidationError -> _nameError.value = result.message
                else -> _actionState.value = CreateGroupActionState.Error("Failed to create group")
            }
            _isLoading.value = false
        }
    }

    fun resetActionState() { _actionState.value = CreateGroupActionState.Idle }
}

sealed class CreateGroupActionState {
    object Idle : CreateGroupActionState()
    data class Success(val groupId: String) : CreateGroupActionState()
    data class Error(val message: String)   : CreateGroupActionState()
}