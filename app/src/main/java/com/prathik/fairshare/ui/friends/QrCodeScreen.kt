package com.prathik.fairshare.ui.friends

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.prathik.fairshare.ui.components.FsLoadingScreen
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScreen(
    onBack   : () -> Unit,
    viewModel: QrCodeViewModel = hiltViewModel(),
) {
    val friendCode by viewModel.friendCode.collectAsState()
    val isLoading  by viewModel.isLoading.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is QrCodeActionState.Success -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            is QrCodeActionState.Error   -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar         = { FsTopBar(title = "My QR Code", onBack = onBack) },
    ) { innerPadding ->

        if (isLoading) { FsLoadingScreen(); return@Scaffold }

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(Spacing.xl))

            // QR code image
            friendCode?.let { code ->
                val qrBitmap = remember(code) { generateQrBitmap(code, 512) }
                qrBitmap?.let {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(Radius.xl))
                            .background(androidx.compose.ui.graphics.Color.White)
                            .padding(16.dp),
                    ) {
                        Image(
                            bitmap             = it.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier           = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Friend code text
            Text(
                text       = friendCode ?: "",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                letterSpacing = 3.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text      = "Share this code or link to add friends",
                fontSize  = 13.sp,
                color     = TextTertiary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Actions card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.xl))
                    .background(Surface2)
                    .padding(horizontal = Spacing.md),
            ) {
                // Share
                QrActionRow(
                    icon    = Icons.Outlined.Share,
                    iconTint = Green400,
                    label   = "Share code",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Add me on FairShare! My code: ${friendCode}\nhttps://fairshare.app/join/${friendCode}")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share via"))
                    },
                )
                HorizontalDivider(color = Surface3, thickness = 0.5.dp, modifier = Modifier.padding(start = 44.dp))

                // Copy
                QrActionRow(
                    icon     = Icons.Outlined.ContentCopy,
                    iconTint = TextSecondary,
                    label    = "Copy code",
                    onClick  = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("FairShare Code", friendCode))
                        scope.launch { snackbarHost.showSnackbar("Code copied to clipboard") }
                    },
                )
                HorizontalDivider(color = Surface3, thickness = 0.5.dp, modifier = Modifier.padding(start = 44.dp))

                // Change code
                QrActionRow(
                    icon     = Icons.Outlined.Refresh,
                    iconTint = Negative,
                    label    = "Change code",
                    onClick  = { viewModel.regenerateCode() },
                )
            }
        }
    }
}

@Composable
private fun QrActionRow(
    icon    : androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    label   : String,
    onClick : () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(text = label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(text = "›", fontSize = 18.sp, color = TextTertiary)
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bits  = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp   = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        bmp
    } catch (e: Exception) { null }
}