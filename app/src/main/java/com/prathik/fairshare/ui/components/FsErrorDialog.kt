package com.prathik.fairshare.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary

/**
 * Standard error dialog for unexpected failures, blocked actions, and server errors.
 *
 * Use this instead of a snackbar whenever:
 *  - a backend/network request fails
 *  - a business rule blocks an action
 *  - the user must read the error before continuing
 *
 * Do NOT use for:
 *  - inline field validation (use field error text instead)
 *  - success confirmations (use snackbar instead)
 *  - offline pending operations (show subtle inline state instead)
 */
@Composable
fun FsErrorDialog(
    title          : String,
    message        : String,
    confirmText    : String = "OK",
    onDismiss      : () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Surface2,
        title = {
            Text(
                text       = title,
                color      = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 16.sp,
            )
        },
        text = {
            Text(
                text     = message,
                color    = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(confirmText, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

/**
 * Map an ApiResult error type to a user-readable [FsErrorDialogState].
 * Import this at the ViewModel layer or call at UI layer from LaunchedEffect.
 */
data class FsErrorDialogState(
    val title  : String,
    val message: String,
)

fun apiErrorDialogState(apiMessage: String?): FsErrorDialogState {
    val msg = apiMessage?.trim().orEmpty()
    return when {
        msg.contains("network", ignoreCase = true) ||
                msg.contains("connect", ignoreCase = true) ||
                msg.contains("timeout", ignoreCase = true) ||
                msg.contains("offline", ignoreCase = true) ||
                msg.contains("No internet", ignoreCase = true) ->
            FsErrorDialogState(
                title   = "You're offline",
                message = "We couldn't complete this action because your device appears to be offline. Please check your connection and try again.",
            )
        msg.contains("Session expired", ignoreCase = true) ||
                msg.contains("Unauthorized", ignoreCase = true) ->
            FsErrorDialogState(
                title   = "Session expired",
                message = "Please sign in again to continue.",
            )
        msg.contains("permission", ignoreCase = true) ||
                msg.contains("not allowed", ignoreCase = true) ||
                msg.contains("forbidden", ignoreCase = true) ->
            FsErrorDialogState(
                title   = "Action not allowed",
                message = msg.ifBlank { "You don't have permission to do this." },
            )
        msg.contains("not found", ignoreCase = true) ||
                msg.contains("no longer exist", ignoreCase = true) ->
            FsErrorDialogState(
                title   = "Item unavailable",
                message = msg.ifBlank { "This item may have been deleted or changed on another device." },
            )
        msg.isNotBlank() ->
            FsErrorDialogState(
                title   = "Something went wrong",
                message = msg,
            )
        else ->
            FsErrorDialogState(
                title   = "Something went wrong",
                message = "We couldn't complete this action right now. Please try again in a moment.",
            )
    }
}