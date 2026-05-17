package com.agrosphere.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Display = FontFamily.SansSerif

val AgroTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Black,
        fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-1.2).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.8).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 26.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 22.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp, lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.3.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, letterSpacing = 0.3.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, letterSpacing = 0.6.sp,
    ),
)
