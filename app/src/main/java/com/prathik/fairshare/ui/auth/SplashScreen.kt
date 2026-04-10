package com.prathik.fairshare.ui.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.theme.Black
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.SyneFontFamily
import kotlinx.coroutines.delay

/**
 * Splash screen — shown on app launch.
 *
 * Displays animated concentric rings + FairShare wordmark.
 * Checks IsLoggedInUseCase after a minimum 1.5s display:
 * - logged in  → navigate to Groups (clearing back stack)
 * - logged out → navigate to Login (clearing back stack)
 */
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToGroups: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rings")

    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring1",
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring2",
    )
    val ring3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ring3",
    )

    LaunchedEffect(Unit) {
        delay(1200L)
        if (viewModel.isLoggedIn()) {
            onNavigateToGroups()
        } else {
            onNavigateToLogin()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier
            .fillMaxSize()
            .background(Black),
    ) {
        // Animated rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx     = size.width / 2f
            val cy     = size.height / 2f
            val color  = Green400

            listOf(ring1, ring2, ring3).forEachIndexed { index, progress ->
                val maxRadius = 280f + index * 60f
                val radius    = progress * maxRadius
                val alpha     = (1f - progress).coerceIn(0f, 1f)
                drawCircle(
                    color  = color.copy(alpha = alpha * 0.4f),
                    radius = radius,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style  = Stroke(width = 2f),
                )
            }
        }

        // Wordmark
        Text(
            text       = "FairShare",
            fontFamily = SyneFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize   = 36.sp,
            color      = Green400,
            modifier   = Modifier.offset(y = (-16).dp),
        )
    }
}