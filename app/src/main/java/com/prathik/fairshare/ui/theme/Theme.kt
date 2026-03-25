package com.prathik.fairshare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FairShareColorScheme = darkColorScheme(
    primary              = Green400,
    onPrimary            = Black,
    primaryContainer     = Green900,
    onPrimaryContainer   = Green200,

    secondary            = Orange400,
    onSecondary          = Black,
    secondaryContainer   = Orange900,
    onSecondaryContainer = Color(0xFFFFB89A),

    background           = Surface0,
    onBackground         = TextPrimary,

    surface              = Surface1,
    onSurface            = TextPrimary,
    surfaceVariant       = Surface2,
    onSurfaceVariant     = TextSecondary,

    outline              = Surface4,
    outlineVariant       = Surface3,

    error                = Orange400,
    onError              = Black,
    errorContainer       = Orange900,
    onErrorContainer     = Color(0xFFFFB89A),

    scrim                = Color(0xCC000000),
    inverseSurface       = Color(0xFFFFFFFF),
    inverseOnSurface     = Surface0,
    inversePrimary       = Green600,
)

@Composable
fun FairShareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FairShareColorScheme,
        typography  = FairShareTypography,
        content     = content,
    )
}