package com.listen.expensetracker.data.model

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized Design System Dimensions and UI Constants.
 * Prevents magic number hardcoding across Composable components.
 */
object AppDimens {
    // Corner Radii
    val CornerCard = 10.dp
    val CornerPill = 20.dp
    val CornerButton = 8.dp
    val CornerCircle = 50.dp

    // Spacing Tokens
    val SpaceExtraSmall = 2.dp
    val SpaceSmall = 4.dp
    val SpaceMedium = 6.dp
    val SpaceStandard = 8.dp
    val SpaceLarge = 12.dp
    val SpaceSection = 16.dp
    val SpaceBottomFab = 72.dp

    // Icon & Component Sizes
    val IconSizeSmall = 12.dp
    val IconSizeMedium = 16.dp
    val IconSizeLarge = 24.dp
    val ButtonHeightCompact = 36.dp
    val ChartHeightStandard = 136.dp
    val ChartBarWidth = 18.dp

    // Text Size Tokens
    val TextMicro = 9.sp
    val TextCaption = 10.sp
    val TextSmall = 11.sp
    val TextBody = 12.sp
    val TextSubtitle = 13.sp
    val TextTitle = 14.sp
    val TextHeader = 16.sp
    val TextDisplay = 18.sp
}
