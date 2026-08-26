package com.listen.expensetracker.data.model

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppDimensTest {
    @Test
    fun testCornerRadii() {
        assertEquals(10.dp, AppDimens.CornerCard)
        assertEquals(20.dp, AppDimens.CornerPill)
        assertEquals(8.dp, AppDimens.CornerButton)
        assertEquals(50.dp, AppDimens.CornerCircle)
    }

    @Test
    fun testSpacingTokens() {
        assertEquals(2.dp, AppDimens.SpaceExtraSmall)
        assertEquals(4.dp, AppDimens.SpaceSmall)
        assertEquals(6.dp, AppDimens.SpaceMedium)
        assertEquals(8.dp, AppDimens.SpaceStandard)
        assertEquals(12.dp, AppDimens.SpaceLarge)
        assertEquals(16.dp, AppDimens.SpaceSection)
        assertEquals(72.dp, AppDimens.SpaceBottomFab)
    }

    @Test
    fun testIconAndComponentSizes() {
        assertEquals(12.dp, AppDimens.IconSizeSmall)
        assertEquals(16.dp, AppDimens.IconSizeMedium)
        assertEquals(24.dp, AppDimens.IconSizeLarge)
        assertEquals(36.dp, AppDimens.ButtonHeightCompact)
        assertEquals(136.dp, AppDimens.ChartHeightStandard)
        assertEquals(18.dp, AppDimens.ChartBarWidth)
    }

    @Test
    fun testTextSizeTokens() {
        assertEquals(9.sp, AppDimens.TextMicro)
        assertEquals(10.sp, AppDimens.TextCaption)
        assertEquals(11.sp, AppDimens.TextSmall)
        assertEquals(12.sp, AppDimens.TextBody)
        assertEquals(13.sp, AppDimens.TextSubtitle)
        assertEquals(14.sp, AppDimens.TextTitle)
        assertEquals(16.sp, AppDimens.TextHeader)
        assertEquals(18.sp, AppDimens.TextDisplay)
    }
}
