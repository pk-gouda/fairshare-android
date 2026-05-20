package com.prathik.fairshare.ui.friends

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsPrimaryButton
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
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeScreen(
    onBack          : () -> Unit,
    onCodeScanned   : (String) -> Unit,
    onScanConsumed  : (() -> Unit) -> Unit = {},
    viewModel       : QrCodeViewModel = hiltViewModel(),
) {
    val friendCode  by viewModel.friendCode.collectAsState()
    val isLoading   by viewModel.isLoading.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val scanned = remember { mutableStateOf(false) }

    // Reset when switching back to Scan tab
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) scanned.value = false
    }
    // Register reset callback so MainShell can reset after dialog confirm/cancel
    LaunchedEffect(Unit) {
        onScanConsumed { scanned.value = false }
    }
    val tabs = listOf("Scan", "My Code")

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
        topBar = {
            Column {
                FsTopBar(title = "", onBack = onBack)
                TabRow(
                    selectedTabIndex  = selectedTab,
                    containerColor    = Surface0,
                    contentColor      = TextPrimary,
                    indicator         = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier  = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color     = Green400,
                        )
                    },
                    divider = {},
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick  = { selectedTab = index },
                            text = {
                                Text(
                                    text       = title,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                    color      = if (selectedTab == index) TextPrimary else TextSecondary,
                                    fontSize   = 15.sp,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            0 -> ScanTab(
                modifier      = Modifier.padding(innerPadding),
                scanned       = scanned.value,
                onScanned     = { scanned.value = true },
                onCodeScanned = onCodeScanned,
            )
            1 -> MyCodeTab(
                modifier     = Modifier.padding(innerPadding),
                friendCode   = friendCode,
                isLoading    = isLoading,
                context      = context,
                scope        = scope,
                snackbarHost = snackbarHost,
                onRetry      = { viewModel.retryLoad() },
                onRegenerate = { viewModel.regenerateCode() },
            )
        }
    }
}

// ── Scan Tab ─────────────────────────────────────────────────────────────────

@Composable
private fun ScanTab(
    modifier      : Modifier,
    scanned       : Boolean,
    onScanned     : () -> Unit,
    onCodeScanned : (String) -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier         = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (hasPermission) {
            CameraPreview(
                onCodeScanned = { code ->
                    if (!scanned) {
                        onScanned()
                        onCodeScanned(code)
                    }
                }
            )
            // Viewfinder overlay
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .border(2.dp, Green400, RoundedCornerShape(16.dp))
            )
            // Hint label
            Box(
                modifier         = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.xxxl)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xl)
                    .clip(RoundedCornerShape(12.dp))
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text      = "Align the QR code inside the box",
                    color     = androidx.compose.ui.graphics.Color.White,
                    fontSize  = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.padding(Spacing.xl),
            ) {
                Text("📷", fontSize = 48.sp)
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    "Camera access needed",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Allow camera access to scan QR codes and add friends or join groups instantly.",
                    fontSize  = 14.sp,
                    color     = TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.xl))
                FsPrimaryButton(
                    text    = "Allow camera access",
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                )
            }
        }
    }
}

// ── My Code Tab ───────────────────────────────────────────────────────────────

@Composable
private fun MyCodeTab(
    modifier     : Modifier,
    friendCode   : String?,
    isLoading    : Boolean,
    context      : Context,
    scope        : kotlinx.coroutines.CoroutineScope,
    snackbarHost : SnackbarHostState,
    onRetry      : () -> Unit,
    onRegenerate : () -> Unit,
) {
    if (isLoading) { FsLoadingScreen(); return }

    if (friendCode.isNullOrBlank()) {
        com.prathik.fairshare.ui.components.FsErrorScreen(
            message = "Could not load your QR code",
            onRetry = onRetry,
        )
        return
    }

    Column(
        modifier            = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(Spacing.xl))

        val qrBitmap = remember(friendCode) { generateQrBitmap(friendCode, 512) }
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

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text          = friendCode,
            fontSize      = 22.sp,
            fontWeight    = FontWeight.Bold,
            color         = TextPrimary,
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.xl))
                .background(Surface2)
                .padding(horizontal = Spacing.md),
        ) {
            QrActionRow(
                icon     = Icons.Outlined.Share,
                iconTint = Green400,
                label    = "Share code",
                onClick  = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Add me on FairShare! My code: $friendCode\nhttps://fairshareapp.app/friend/$friendCode")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share via"))
                },
            )
            HorizontalDivider(color = Surface3, thickness = 0.5.dp, modifier = Modifier.padding(start = 44.dp))
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
            QrActionRow(
                icon     = Icons.Outlined.Refresh,
                iconTint = Negative,
                label    = "Change code",
                onClick  = onRegenerate,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xl))
        Text(
            text      = "Anyone can use your code to add you on FairShare.\nOnly share it with people you trust.",
            fontSize  = 13.sp,
            color     = TextTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

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

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onCodeScanned: (String) -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor       = remember { Executors.newSingleThreadExecutor() }
    val scanner        = remember { BarcodeScanning.getClient() }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            scanner.close()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory  = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                                    ?.rawValue
                                    ?.let { onCodeScanned(it) }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                } catch (e: Exception) { e.printStackTrace() }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
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