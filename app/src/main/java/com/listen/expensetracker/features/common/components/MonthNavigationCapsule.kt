package com.listen.expensetracker.features.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.listen.uicomponent.components.CommonNavigationCapsule
import com.listen.uicomponent.theme.ListenTheme

const val PAGER_BASE_INDEX = 600
const val PAGER_PAGE_COUNT = 1200

/**
 * Month Navigation Capsule component for ListenExpenseTracker.
 * Displays previous/next buttons and a clickable center month title in a pill container.
 * Delegates to the generalized CommonNavigationCapsule component in ListenUiComponent (Rule 25).
 *
 * @param monthTitle Display text for the active month (e.g., "本月 (2026年08月)")
 * @param onPreviousMonth Callback triggered when tapping previous button or swiping right
 * @param onNextMonth Callback triggered when tapping next button or swiping left
 * @param onTitleClick Callback triggered when tapping the center title (e.g., open MonthPickerDialog)
 * @param modifier Composable modifier
 */
@Composable
fun MonthNavigationCapsule(
    monthTitle: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // [ListenUiComponent] 委托给下沉至组件库的通用胶囊导航步进器 CommonNavigationCapsule (Rule 25)
    CommonNavigationCapsule(
        title = monthTitle,
        onPrevious = onPreviousMonth,
        onNext = onNextMonth,
        onTitleClick = onTitleClick,
        enableSwipe = true,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun MonthNavigationCapsulePreview() {
    ListenTheme {
        MonthNavigationCapsule(
            monthTitle = "本月 (2026年08月)",
            onPreviousMonth = {},
            onNextMonth = {},
            onTitleClick = {}
        )
    }
}
