package com.prathik.fairshare.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsPrimaryButton
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.SyneFontFamily
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Handles the fairshare://confirm-email-change?token=xxx deep link.
 *
 * When the user taps "Confirm New Email" in their email, the deep link opens
 * the app here. This screen immediately fires the verify API call and shows
 * success or error feedback — no manual input required.
 */
@Composable
fun ConfirmEmailChangeScreen(
    token   : String?,
    onDone  : () -> Unit,
    viewModel: ConfirmEmailChangeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Fire verification as soon as the token is available
    LaunchedEffect(token) {
        if (!token.isNullOrBlank()) {
            viewModel.confirm(token)
        } else {
            viewModel.setError("Invalid or missing confirmation link.")
        }
    }

    // Auto-navigate to home 2 seconds after success
    LaunchedEffect(uiState) {
        if (uiState is ConfirmEmailChangeUiState.Success) {
            delay(2000L)
            onDone()
        }
    }

    Box(
        modifier         = Modifier.fillMaxSize().background(Surface0),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xxl),
        ) {
            when (val state = uiState) {

                is ConfirmEmailChangeUiState.Loading -> {
                    CircularProgressIndicator(
                        color    = Green400,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(Spacing.xxl))
                    Text(
                        text       = "Confirming your new email…",
                        fontFamily = SyneFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp,
                        color      = TextPrimary,
                        textAlign  = TextAlign.Center,
                    )
                }

                is ConfirmEmailChangeUiState.Success -> {
                    Icon(
                        imageVector        = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint               = Green400,
                        modifier           = Modifier.size(72.dp),
                    )
                    Spacer(Modifier.height(Spacing.xxl))
                    Text(
                        text       = "Email updated!",
                        fontFamily = SyneFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 36.sp,
                        color      = TextPrimary,
                        textAlign  = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text      = "Your email address has been changed successfully.",
                        fontSize  = 15.sp,
                        color     = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }

                is ConfirmEmailChangeUiState.Error -> {
                    Icon(
                        imageVector        = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint               = Negative,
                        modifier           = Modifier.size(72.dp),
                    )
                    Spacer(Modifier.height(Spacing.xxl))
                    Text(
                        text       = "Confirmation failed",
                        fontFamily = SyneFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 32.sp,
                        color      = TextPrimary,
                        textAlign  = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text      = state.message,
                        fontSize  = 15.sp,
                        color     = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                    )
                    Spacer(Modifier.height(Spacing.xxl))
                    FsPrimaryButton(
                        text     = "Go to app",
                        onClick  = onDone,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}