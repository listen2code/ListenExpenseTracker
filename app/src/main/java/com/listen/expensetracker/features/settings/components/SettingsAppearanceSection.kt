package com.listen.expensetracker.features.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.listen.expensetracker.core.i18n.AppLanguage
import com.listen.expensetracker.core.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonColorPicker
import com.listen.uicomponent.components.CommonSegmentedControl
import com.listen.uicomponent.components.CommonSwitchRow
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ListenTheme
import com.listen.uicomponent.theme.ThemeMode

/**
 * Settings Card for Theme Mode, Accent Color, and Language customization.
 *
 * @param themeMode 当前主题模式 (Light/Dark/System)
 * @param accentColor 当前主题强调色
 * @param isPureBlackDark 是否开启 AMOLED 纯黑模式
 */
@Composable
fun SettingsAppearanceSection(
    themeMode: ThemeMode,
    accentColor: AccentColor,
    onChangeThemeMode: (ThemeMode) -> Unit,
    onChangeAccentColor: (AccentColor) -> Unit,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isPureBlackDark: Boolean = false,
    onTogglePureBlackDark: (Boolean) -> Unit = {}
) {

    SurfaceCard(
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceStandard,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Theme",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.IconSizeLarge)
                )
                Text(
                    text = AppStrings.SETTINGS_APPEARANCE.tr(),
                    fontWeight = FontWeight.Bold,
                    fontSize = AppDimens.TextTitle
                )
            }

            // Theme Mode Segmented Switch
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
                Text(
                    text = AppStrings.SETTINGS_THEME_MODE.tr(),
                    fontSize = AppDimens.TextSubtitle,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val modes = listOf(
                    ThemeMode.LIGHT to AppStrings.THEME_LIGHT.tr(),
                    ThemeMode.DARK to AppStrings.THEME_DARK.tr(),
                    ThemeMode.SYSTEM to AppStrings.THEME_SYSTEM.tr()
                )

                CommonSegmentedControl(
                    items = modes.map { it.second },
                    selectedIndex = modes.indexOfFirst { it.first == themeMode }.coerceAtLeast(0),
                    onIndexChange = { index -> onChangeThemeMode(modes[index].first) }
                )

                // AMOLED 纯黑夜间节能模式开关：仅在深色和跟随系统选项时才显示
                AnimatedVisibility(
                    visible = themeMode == ThemeMode.DARK || themeMode == ThemeMode.SYSTEM,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    CommonSwitchRow(
                        title = AppStrings.AMOLED_PURE_BLACK_TITLE.tr(),
                        subtitle = AppStrings.AMOLED_PURE_BLACK_DESC.tr(),
                        checked = isPureBlackDark,
                        onCheckedChange = onTogglePureBlackDark,
                        contentPadding = 0.dp
                    )
                }
            }

            // Accent Color Selection Row
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
                Text(
                    text = AppStrings.SETTINGS_ACCENT_COLOR.tr(),
                    fontSize = AppDimens.TextSubtitle,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // [ListenUiComponent] 使用共通 CommonColorPicker 统一色环调色板组件 (Rule 25)
                CommonColorPicker(
                    colors = AccentColor.entries.map { it.colorHex },
                    selectedColor = accentColor.colorHex,
                    onColorSelected = { hex ->
                        AccentColor.entries.find { it.colorHex.equals(hex, ignoreCase = true) }?.let {
                            onChangeAccentColor(it)
                        }
                    },
                    circleSize = 36.dp,
                    checkIconSize = AppDimens.IconSizeMedium
                )
            }

            // Language Selector
            val currentLang = AppLanguage.current
            val currentLangLabel = when (currentLang) {
                "en" -> "English"
                "ja" -> "日本語"
                else -> "简体中文"
            }

            CommonButton(
                text = "${AppStrings.SETTINGS_LANGUAGE.tr()}: $currentLangLabel",
                onClick = {
                    val next = when (currentLang) {
                        "zh" -> "en"
                        "en" -> "ja"
                        else -> "zh"
                    }
                    onLanguageChange(next)
                },
                style = CommonButtonStyle.Outlined,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
@Preview(showBackground = true)
@Composable
fun SettingsAppearanceSectionPreview() {
    ExpenseStrings.init()
    ListenTheme {
        SettingsAppearanceSection(
            themeMode = ThemeMode.SYSTEM,
            accentColor = AccentColor.EMERALD,
            onChangeThemeMode = {},
            onChangeAccentColor = {},
            onLanguageChange = {}
        )
    }
}
