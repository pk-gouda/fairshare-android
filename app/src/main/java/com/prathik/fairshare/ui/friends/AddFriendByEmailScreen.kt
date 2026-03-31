package com.prathik.fairshare.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.ComponentSize
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendByEmailScreen(
    onBack   : () -> Unit,
    viewModel: AddFriendViewModel = hiltViewModel(),
) {
    val email        by viewModel.email.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val isLoading    by viewModel.isLoading.collectAsState()
    val actionState  by viewModel.actionState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is AddFriendActionState.Success -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            is AddFriendActionState.Error   -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar         = { FsTopBar(title = "Add a friend", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg),
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))

            // Search bar
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.lg))
                    .background(Surface2)
                    .padding(horizontal = Spacing.md, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Search,
                    contentDescription = null,
                    tint               = Green400,
                    modifier           = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                BasicTextField(
                    value         = email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    singleLine    = true,
                    textStyle     = TextStyle(fontSize = 15.sp, color = TextPrimary),
                    cursorBrush   = SolidColor(Green400),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction    = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { viewModel.searchUser() },
                    ),
                    decorationBox = { inner ->
                        if (email.isEmpty()) {
                            Text("Enter email address...", fontSize = 15.sp, color = TextTertiary)
                        }
                        inner()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                )
                // Clear button
                if (email.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(TextTertiary.copy(alpha = 0.3f))
                            .then(Modifier.clip(CircleShape)),
                    ) {
                        Text("×", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text     = "Search by email to find friends already on FairShare",
                fontSize = 12.sp,
                color    = TextTertiary,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            when {
                isLoading -> FsLoadingScreen()

                searchResult != null -> {
                    // Found user
                    val user = searchResult!!
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.xl))
                            .background(Surface2)
                            .padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(
                            name   = user.fullName,
                            userId = user.id,
                            size   = ComponentSize.avatarLg,
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = user.fullName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(text = user.email,    fontSize = 12.sp, color = TextTertiary)
                        }
                        // Send request button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .clip(RoundedCornerShape(Radius.lg))
                                .background(Green400)
                                .then(Modifier.padding(horizontal = Spacing.md, vertical = 8.dp)),
                        ) {
                            Text(
                                text       = "Add",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Surface0,
                                modifier   = Modifier
                                    .clip(RoundedCornerShape(Radius.lg))
                                    .then(
                                        Modifier.padding(0.dp)
                                    ),
                            )
                        }
                    }
                }

                email.isNotEmpty() && searchResult == null && !isLoading -> {
                    // No result
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(text = "👤", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Text(text = "No user found", fontSize = 15.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text      = "Want to invite them to FairShare?",
                            fontSize  = 13.sp,
                            color     = TextTertiary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .clip(RoundedCornerShape(Radius.xl))
                                .background(Surface2)
                                .then(Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.md)),
                        ) {
                            Text(
                                text       = "Send invite",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color      = Green400,
                            )
                        }
                    }
                }
            }
        }
    }
}