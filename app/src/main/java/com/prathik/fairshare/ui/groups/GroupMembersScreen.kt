package com.prathik.fairshare.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.usecase.group.GetGroupMembersUseCase
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.ComponentSize
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class GroupMembersViewModel @Inject constructor(
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _state = MutableStateFlow<GroupMembersUiState>(GroupMembersUiState.Loading)
    val state: StateFlow<GroupMembersUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = GroupMembersUiState.Loading
            when (val result = getGroupMembersUseCase(groupId)) {
                is ApiResult.Success     -> _state.value = GroupMembersUiState.Success(result.data)
                is ApiResult.NetworkError -> _state.value = GroupMembersUiState.Error("No internet connection.")
                else                     -> _state.value = GroupMembersUiState.Error("Failed to load members.")
            }
        }
    }
}

sealed class GroupMembersUiState {
    object Loading : GroupMembersUiState()
    data class Success(val members: List<GroupMember>) : GroupMembersUiState()
    data class Error(val message: String) : GroupMembersUiState()
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMembersScreen(
    onBack   : () -> Unit,
    viewModel: GroupMembersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Surface0,
        topBar         = { FsTopBar(title = "Members", onBack = onBack) },
    ) { innerPadding ->
        when (val s = state) {
            is GroupMembersUiState.Loading -> FsLoadingScreen()
            is GroupMembersUiState.Error   -> FsErrorScreen(
                message = s.message,
                onRetry = { viewModel.load() }
            )
            is GroupMembersUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = Spacing.lg)
                        .padding(top = Spacing.md),
                ) {
                    item {
                        Text(
                            text     = "${s.members.size} member${if (s.members.size != 1) "s" else ""}",
                            fontSize = 13.sp,
                            color    = TextSecondary,
                            modifier = Modifier.padding(bottom = Spacing.md),
                        )
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radius.xl))
                                .background(Surface2),
                        ) {
                            s.members.forEachIndexed { index, member ->
                                MemberRow(member)
                                if (index < s.members.lastIndex) {
                                    HorizontalDivider(
                                        color     = Surface3,
                                        thickness = 0.5.dp,
                                        modifier  = Modifier.padding(start = Spacing.lg + ComponentSize.avatarMd + Spacing.md),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(member: GroupMember) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FsAvatar(name = member.fullName, userId = member.userId, size = ComponentSize.avatarMd)
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = member.fullName,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
            )
            val isPlaceholder = member.email.startsWith("placeholder+")
            Text(
                text     = if (isPlaceholder) "Placeholder" else member.email,
                fontSize = 12.sp,
                color    = TextTertiary,
            )
        }
        // Joined date
        val joined = runCatching {
            LocalDateTime.parse(member.joinedAt)
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }.getOrNull()
        if (joined != null) {
            Text(text = joined, fontSize = 11.sp, color = TextTertiary)
        }
    }
}