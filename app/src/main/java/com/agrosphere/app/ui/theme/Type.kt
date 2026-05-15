package com.agrosphere.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Display = FontFamily.SansSerif

val AgroTypography = Typography(
    displayLarge = TextStyle(Display, FontWeight.Black, 40.sp, lineHeight = 44.sp, letterSpacing = (-1.2).sp),
    displayMedium = TextStyle(Display, FontWeight.ExtraBold, 32.sp, lineHeight = 36.sp, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(Display, FontWeight.Bold, 26.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(Display, FontWeight.Bold, 22.sp, lineHeight = 26.sp),
    headlineSmall = TextStyle(Display, FontWeight.SemiBold, 18.sp, lineHeight = 22.sp),
    titleLarge = TextStyle(Display, FontWeight.SemiBold, 18.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(Display, FontWeight.SemiBold, 15.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(Display, FontWeight.Medium, 13.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp),
    bodyLarge = TextStyle(Display, FontWeight.Normal, 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(Display, FontWeight.Normal, 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(Display, FontWeight.Normal, 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(Display, FontWeight.SemiBold, 14.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(Display, FontWeight.Medium, 12.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(Display, FontWeight.Medium, 11.sp, letterSpacing = 0.6.sp),
)
