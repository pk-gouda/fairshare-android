package com.prathik.fairshare.ui.groups

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.prathik.fairshare.ui.components.FsErrorScreen
import com.prathik.fairshare.ui.components.FsSkeletonBlock
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import kotlinx.coroutines.launch

private const val BASE_URL = "https://fairshareapp.app/join/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInviteScreen(
    onBack   : () -> Unit,
    viewModel: GroupInviteViewModel = hiltViewModel(),
) {
    val inviteCode  by viewModel.inviteCode.collectAsState()
    val groupName   by viewModel.groupName.collectAsState()
    val groupImage  by viewModel.groupImage.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    var showConfirmRegen by remember { mutableStateOf(false) }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is GroupInviteActionState.Success -> {
                snackbarHost.showSnackbar(s.message)
                viewModel.resetActionState()
            }
            is GroupInviteActionState.Error -> {
                snackbarHost.showSnackbar(s.message)
                viewModel.resetActionState()
            }
            else -> Unit
        }
    }

    // Confirm regenerate dialog
    if (showConfirmRegen) {
        AlertDialog(
            onDismissRequest  = { showConfirmRegen = false },
            containerColor    = Surface2,
            title = { Text("Change invite link?", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
            text  = { Text("The old link will stop working immediately. Anyone with it won't be able to join.", color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { showConfirmRegen = false; viewModel.regenerateCode() }) {
                    Text("Change", color = Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRegen = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar         = { FsTopBar(title = "Invite via link", onBack = onBack) },
    ) { innerPadding ->

        when {
            isLoading       -> GroupInviteSkeleton(Modifier.padding(innerPadding))
            inviteCode == null -> FsErrorScreen(
                message = "Could not load invite link",
                onRetry = { viewModel.retryLoad() },
            )
            else -> {
                val code = inviteCode!!
                val inviteLink = "$BASE_URL$code"

                Column(
                    modifier            = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(Spacing.xl))

                    // QR code
                    val qrBitmap = remember(code) { generateGroupQrBitmap(inviteLink, 512) }
                    qrBitmap?.let { bmp ->
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(Radius.xl))
                                .background(androidx.compose.ui.graphics.Color.White)
                                .padding(16.dp),
                        ) {
                            Image(
                                bitmap             = bmp.asImageBitmap(),
                                contentDescription = "Group invite QR",
                                modifier           = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Spacer(Modifier.height(Spacing.lg))

                    // Group image — show photo if available, fallback to emoji on error
                    if (!groupImage.isNullOrBlank()) {
                        var imageLoadFailed by remember(groupImage) { mutableStateOf(false) }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                .background(Surface2),
                        ) {
                            if (!imageLoadFailed) {
                                coil.compose.AsyncImage(
                                    model              = groupImage,
                                    contentDescription = groupName,
                                    contentScale       = androidx.compose.ui.layout.ContentScale.Crop,
                                    onError            = { imageLoadFailed = true },
                                    modifier           = Modifier.fillMaxSize(),
                                )
                            } else {
                                Text("👥", fontSize = 28.sp)
                            }
                        }
                        Spacer(Modifier.height(Spacing.md))
                    }

                    // Group name
                    groupName?.let { name ->
                        Text(
                            text       = name,
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    // Invite link preview
                    Text(
                        text      = inviteLink,
                        fontSize  = 12.sp,
                        color     = Green400,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(Spacing.xl))

                    // Actions card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.xl))
                            .background(Surface2)
                            .padding(horizontal = Spacing.md),
                    ) {
                        // Share link
                        InviteActionRow(
                            icon     = Icons.Outlined.Share,
                            iconTint = Green400,
                            label    = "Share invite link",
                            onClick  = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Join my group on FairShare!\n$inviteLink")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share via"))
                            },
                        )
                        HorizontalDivider(color = Surface3, thickness = 0.5.dp, modifier = Modifier.padding(start = 44.dp))

                        // Copy link
                        InviteActionRow(
                            icon     = Icons.Outlined.Link,
                            iconTint = TextSecondary,
                            label    = "Copy link",
                            onClick  = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Invite link", inviteLink))
                                scope.launch { snackbarHost.showSnackbar("Link copied to clipboard") }
                            },
                        )
                        HorizontalDivider(color = Surface3, thickness = 0.5.dp, modifier = Modifier.padding(start = 44.dp))

                        // Copy code only
                        InviteActionRow(
                            icon     = Icons.Outlined.ContentCopy,
                            iconTint = TextSecondary,
                            label    = "Copy code only",
                            onClick  = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Invite code", code))
                                scope.launch { snackbarHost.showSnackbar("Code copied to clipboard") }
                            },
                        )
                        HorizontalDivider(color = Surface3, thickness = 0.5.dp, modifier = Modifier.padding(start = 44.dp))

                        // Change code
                        InviteActionRow(
                            icon      = Icons.Outlined.Refresh,
                            iconTint  = Negative,
                            label     = "Change invite link",
                            labelColor = Negative,
                            onClick   = { showConfirmRegen = true },
                        )
                    }

                    Spacer(Modifier.height(Spacing.xl))

                    Text(
                        text      = "Anyone with this link can join the group.\nOnly share it with people you trust.",
                        fontSize  = 13.sp,
                        color     = TextTertiary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                    )

                    Spacer(Modifier.height(Spacing.xxxl))
                }
            }
        }
    }
}

@Composable
private fun InviteActionRow(
    icon       : androidx.compose.ui.graphics.vector.ImageVector,
    iconTint   : androidx.compose.ui.graphics.Color,
    label      : String,
    labelColor : androidx.compose.ui.graphics.Color = TextPrimary,
    onClick    : () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.md))
        Text(text = label, fontSize = 14.sp, color = labelColor, modifier = Modifier.weight(1f))
        Text(text = "›", fontSize = 18.sp, color = TextTertiary)
    }
}

private fun generateGroupQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bits  = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp   = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bits[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bmp
    } catch (e: Exception) { null }
}
// ── GroupInvite skeleton placeholder ─────────────────────────────────────────

@Composable
private fun GroupInviteSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Group name placeholder
        FsSkeletonBlock(height = 18.dp, widthFraction = 0.5f, cornerRadius = 6.dp)
        // QR code square placeholder
        FsSkeletonBlock(height = 220.dp, widthFraction = 0.7f, cornerRadius = 12.dp)
        // Invite link placeholder
        FsSkeletonBlock(height = 14.dp, widthFraction = 0.8f, cornerRadius = 4.dp)
        // Action button placeholders
        FsSkeletonBlock(height = 44.dp, widthFraction = 1f, cornerRadius = 8.dp)
        FsSkeletonBlock(height = 44.dp, widthFraction = 1f, cornerRadius = 8.dp)
    }
}