package com.prathik.fairshare.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.prathik.fairshare.R

val SyneFontFamily = FontFamily(
    Font(R.font.syne_regular,   FontWeight.Normal),
    Font(R.font.syne_medium,    FontWeight.Medium),
    Font(R.font.syne_semibold,  FontWeight.SemiBold),
    Font(R.font.syne_bold,      FontWeight.Bold),
    Font(R.font.syne_extrabold, FontWeight.ExtraBold),
)

val InterFontFamily = FontFamily(
    Font(R.font.inter_light,    FontWeight.Light),
    Font(R.font.inter_regular,  FontWeight.Normal),
    Font(R.font.inter_medium,   FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

val FairShareTypography = Typography(

    // Large hero numbers — net balance, expense amount
    displayLarge = TextStyle(
        fontFamily    = SyneFontFamily,
        fontWeight    = FontWeight.ExtraBold,
        fontSize      = 40.sp,
        lineHeight    = 44.sp,
        letterSpacing = (-1.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily    = SyneFontFamily,
        fontWeight    = FontWeight.ExtraBold,
        fontSize      = 32.sp,
        lineHeight    = 36.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily    = SyneFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 24.sp,
        lineHeight    = 28.sp,
        letterSpacing = (-0.5).sp,
    ),

    // Screen titles, section headers
    headlineLarge = TextStyle(
        fontFamily    = SyneFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 22.sp,
        lineHeight    = 26.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily    = SyneFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 18.sp,
        lineHeight    = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily    = SyneFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 15.sp,
        lineHeight    = 19.sp,
        letterSpacing = (-0.1).sp,
    ),

    // Top bar, card titles
    titleLarge = TextStyle(
        fontFamily    = SyneFontFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 17.sp,
        lineHeight    = 21.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 15.sp,
        lineHeight    = 19.sp,
        letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 13.sp,
        lineHeight    = 17.sp,
        letterSpacing = 0.sp,
    ),

    // Expense descriptions, names
    bodyLarge = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 15.sp,
        lineHeight    = 22.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 13.sp,
        lineHeight    = 19.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 12.sp,
        lineHeight    = 17.sp,
        letterSpacing = 0.sp,
    ),

    // Chips, badges, bottom nav, section labels
    labelLarge = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 13.sp,
        lineHeight    = 17.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        lineHeight    = 15.sp,
        letterSpacing = 0.05.sp,
    ),
    labelSmall = TextStyle(
        fontFamily    = InterFontFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 10.sp,
        lineHeight    = 14.sp,
        letterSpacing = 0.08.sp,
    ),
)