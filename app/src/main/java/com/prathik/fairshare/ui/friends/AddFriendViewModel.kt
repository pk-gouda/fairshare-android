package com.prathik.fairshare.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.User
import com.prathik.fairshare.domain.repository.FriendRepository
import com.prathik.fairshare.domain.usecase.friend.SendFriendRequestUseCase
import com.prathik.fairshare.domain.usecase.user.GetMyProfileUseCase
import com.prathik.fairshare.domain.usecase.user.SearchUserByEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SelectedPerson(
    val id          : String,
    val displayName : String,
    val emailOrPhone: String,
    val isExistingUser: Boolean,
    val isPlaceholder : Boolean = false,
    val userId        : String? = null,
)

@HiltViewModel
class AddFriendViewModel @Inject constructor(
    private val searchUserByEmailUseCase: SearchUserByEmailUseCase,
    private val sendFriendRequestUseCase: SendFriendRequestUseCase,
    private val friendRepository        : FriendRepository,
    private val getMyProfileUseCase     : GetMyProfileUseCase,
) : ViewModel() {

    private var myEmail: String = ""

    init {
        viewModelScope.launch {
            when (val result = getMyProfileUseCase()) {
                is ApiResult.Success -> myEmail = result.data.email
                else -> Unit
            }
        }
    }

    private val _searchQuery   = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResult  = MutableStateFlow<User?>(null)
    val searchResult: StateFlow<User?> = _searchResult.asStateFlow()

    private val _isSearching   = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _noResultFound = MutableStateFlow(false)
    val noResultFound: StateFlow<Boolean> = _noResultFound.asStateFlow()

    private val _selectedPeople = MutableStateFlow<List<SelectedPerson>>(emptyList())
    val selectedPeople: StateFlow<List<SelectedPerson>> = _selectedPeople.asStateFlow()

    private val _screen = MutableStateFlow<AddFriendScreen>(AddFriendScreen.Search)
    val screen: StateFlow<AddFriendScreen> = _screen.asStateFlow()

    private val _inviteName         = MutableStateFlow("")
    val inviteName: StateFlow<String> = _inviteName.asStateFlow()

    private val _inviteEmailOrPhone = MutableStateFlow("")
    val inviteEmailOrPhone: StateFlow<String> = _inviteEmailOrPhone.asStateFlow()

    private val _editingPerson = MutableStateFlow<SelectedPerson?>(null)

    private val _inviteMode = MutableStateFlow(InviteMode.Invite)
    val inviteMode: StateFlow<InviteMode> = _inviteMode.asStateFlow()

    private val _isLoading    = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionState  = MutableStateFlow<AddFriendActionState>(AddFriendActionState.Idle)
    val actionState: StateFlow<AddFriendActionState> = _actionState.asStateFlow()

    private var searchJob: Job? = null

    fun onSearchChanged(query: String) {
        _searchQuery.value   = query
        _searchResult.value  = null
        _noResultFound.value = false

        searchJob?.cancel()
        if (query.trim().length < 3) return

        if (query.trim().equals(myEmail, ignoreCase = true)) {
            _actionState.value = AddFriendActionState.Error("You can't add yourself")
            return
        }

        searchJob = viewModelScope.launch {
            delay(400)
            _isSearching.value = true
            when (val result = searchUserByEmailUseCase(query.trim())) {
                is ApiResult.Success  -> { _searchResult.value = result.data; _noResultFound.value = false }
                is ApiResult.NotFound -> { _searchResult.value = null; _noResultFound.value = true }
                else                  -> { _searchResult.value = null; _noResultFound.value = false }
            }
            _isSearching.value = false
        }
    }

    fun selectExistingUser(user: User) {
        if (_selectedPeople.value.any { it.id == user.id }) return
        _selectedPeople.value += SelectedPerson(
            id            = user.id,
            displayName   = user.fullName,
            emailOrPhone  = user.email,
            isExistingUser = true,
            userId        = user.id,
        )
        _searchQuery.value  = ""
        _searchResult.value = null
        _noResultFound.value = false
    }

    fun removePerson(personId: String) {
        _selectedPeople.value = _selectedPeople.value.filter { it.id != personId }
    }

    fun onInviteNameChanged(v: String)         { _inviteName.value = v }
    fun onInviteEmailOrPhoneChanged(v: String) { _inviteEmailOrPhone.value = v }

    fun showInviteForm(person: SelectedPerson? = null, mode: InviteMode = InviteMode.Invite) {
        _editingPerson.value      = person
        _inviteMode.value         = mode
        _inviteName.value         = person?.displayName ?: ""
        _inviteEmailOrPhone.value = person?.emailOrPhone ?: ""
        _screen.value = AddFriendScreen.InviteForm
    }

    fun switchToInviteMode() {
        _inviteMode.value         = InviteMode.Invite
        _inviteEmailOrPhone.value = ""
    }

    fun confirmInvitePerson() {
        val name          = _inviteName.value.trim()
        val contact       = _inviteEmailOrPhone.value.trim()
        val isPlaceholder = _inviteMode.value == InviteMode.Placeholder

        if (name.isBlank()) {
            _actionState.value = AddFriendActionState.Error("Please enter a name"); return
        }
        if (!isPlaceholder && contact.isBlank()) {
            _actionState.value = AddFriendActionState.Error("Please enter an email or phone number"); return
        }
        val editing = _editingPerson.value
        val person = SelectedPerson(
            id            = editing?.id ?: java.util.UUID.randomUUID().toString().replace("-", ""),
            displayName   = name,
            emailOrPhone  = if (isPlaceholder) "" else contact,
            isExistingUser = false,
            isPlaceholder = isPlaceholder,
        )
        _selectedPeople.value = if (editing != null)
            _selectedPeople.value.map { if (it.id == editing.id) person else it }
        else
            _selectedPeople.value + person

        _inviteName.value         = ""
        _inviteEmailOrPhone.value = ""
        _editingPerson.value      = null
        _screen.value = AddFriendScreen.Search
    }

    fun cancelInviteForm() {
        _inviteName.value         = ""
        _inviteEmailOrPhone.value = ""
        _editingPerson.value      = null
        _screen.value = AddFriendScreen.Search
    }

    fun goToReview()  { _screen.value = AddFriendScreen.Review }
    fun backToSearch() { _screen.value = AddFriendScreen.Search }

    fun submitAll(onDone: () -> Unit) {
        val people = _selectedPeople.value
        if (people.isEmpty()) return
        viewModelScope.launch {
            _isLoading.value = true
            var successCount = 0
            val errorMessages = mutableListOf<String>()

            people.forEach { person ->
                when {
                    // Placeholder — create on backend (persists across reinstalls)
                    person.isPlaceholder -> {
                        when (friendRepository.createPlaceholder(person.displayName)) {
                            is ApiResult.Success  -> successCount++
                            is ApiResult.Conflict -> errorMessages.add("${person.displayName} is already in your friends list")
                            else -> errorMessages.add("Failed to add ${person.displayName}")
                        }
                    }

                    // Existing user with account
                    person.isExistingUser && person.userId != null -> {
                        when (sendFriendRequestUseCase(person.userId)) {
                            is ApiResult.Success  -> successCount++
                            is ApiResult.Conflict -> errorMessages.add("${person.displayName} is already your friend")
                            else -> errorMessages.add("Failed to add ${person.displayName}")
                        }
                    }

                    // Invited by email — create on backend
                    else -> {
                        if (person.emailOrPhone.equals(myEmail, ignoreCase = true)) {
                            errorMessages.add("You can't invite yourself")
                        } else {
                            when (friendRepository.inviteFriend(
                                email = person.emailOrPhone,
                                name  = person.displayName,
                            )) {
                                is ApiResult.Success  -> successCount++
                                is ApiResult.Conflict -> errorMessages.add("${person.emailOrPhone} has already been invited")
                                else -> errorMessages.add("Failed to invite ${person.displayName}")
                            }
                        }
                    }
                }
            }

            _isLoading.value = false

            if (errorMessages.isNotEmpty()) {
                _actionState.value = AddFriendActionState.Error(errorMessages.joinToString("\n"))
            }

            if (successCount > 0) {
                if (errorMessages.isEmpty()) {
                    _actionState.value = AddFriendActionState.Success(
                        if (successCount == 1) "Done!" else "$successCount friends added!"
                    )
                }
                onDone()
            }
        }
    }

    fun resetActionState() { _actionState.value = AddFriendActionState.Idle }
}

enum class AddFriendScreen { Search, InviteForm, Review }
enum class InviteMode { Invite, Placeholder }

sealed class AddFriendActionState {
    object Idle : AddFriendActionState()
    data class Success(val message: String) : AddFriendActionState()
    data class Error(val message: String)   : AddFriendActionState()
}