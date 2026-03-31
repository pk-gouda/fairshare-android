package com.prathik.fairshare.ui.friends

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary

/**
 * Scan QR Code Screen
 *
 * TODO: Full camera implementation requires adding to build.gradle.kts:
 *
 * // CameraX
 * implementation("androidx.camera:camera-core:1.3.1")
 * implementation("androidx.camera:camera-camera2:1.3.1")
 * implementation("androidx.camera:camera-lifecycle:1.3.1")
 * implementation("androidx.camera:camera-view:1.3.1")
 *
 * // ML Kit Barcode Scanning
 * implementation("com.google.mlkit:barcode-scanning:17.2.0")
 *
 * Also add CAMERA permission to AndroidManifest.xml:
 * <uses-permission android:name="android.permission.CAMERA" />
 *
 * Once deps are added, replace this screen with the full camera implementation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrCodeScreen(
    onBack          : () -> Unit,
    onCodeScanned   : (String) -> Unit,
) {
    Scaffold(
        containerColor = Surface0,
        topBar         = { FsTopBar(title = "Scan QR Code", onBack = onBack) },
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📷", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(Spacing.lg))
                Text(
                    text       = "Camera scanner coming soon",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color      = TextSecondary,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text      = "Requires CameraX + ML Kit dependencies.\nSee ScanQrCodeScreen.kt for instructions.",
                    fontSize  = 13.sp,
                    color     = TextTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}