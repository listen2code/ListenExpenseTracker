package com.listen.expensetracker.features.settings.components

import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonSegmentedControl
import com.listen.uicomponent.components.CommonSwitchRow
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import com.listen.uicomponent.theme.parseHexColor

/**
 * Settings Card for Theme Mode, Accent Color, and Language customization.
 */
@Composable
fun SettingsAppearanceSection(
    themeMode: ThemeMode,
    accentColor: AccentColor,
    language: String,
    onChangeThemeMode: (ThemeMode) -> Unit,
    onChangeAccentColor: (AccentColor) -> Unit,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    lang: String = language,
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
                    text = AppStrings.SETTINGS_APPEARANCE.tr(lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = AppDimens.TextTitle
                )
            }

            // Theme Mode Segmented Switch
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
                Text(
                    text = AppStrings.SETTINGS_THEME_MODE.tr(lang),
                    fontSize = AppDimens.TextSubtitle,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val modes = listOf(
                    ThemeMode.LIGHT to AppStrings.THEME_LIGHT.tr(lang),
                    ThemeMode.DARK to AppStrings.THEME_DARK.tr(lang),
                    ThemeMode.SYSTEM to AppStrings.THEME_SYSTEM.tr(lang)
                )

                CommonSegmentedControl(
                    items = modes.map { it.second },
                    selectedIndex = modes.indexOfFirst { it.first == themeMode }.coerceAtLeast(0),
                    onIndexChange = { index -> onChangeThemeMode(modes[index].first) }
                )

                // AMOLED 纯黑夜间节能模式开关
                CommonSwitchRow(
                    title = AppStrings.AMOLED_PURE_BLACK_TITLE.tr(lang),
                    subtitle = AppStrings.AMOLED_PURE_BLACK_DESC.tr(lang),
                    checked = isPureBlackDark,
                    onCheckedChange = onTogglePureBlackDark,
                    contentPadding = 0.dp
                )
            }

            // Accent Color Selection Row
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
                Text(
                    text = AppStrings.SETTINGS_ACCENT_COLOR.tr(lang),
                    fontSize = AppDimens.TextSubtitle,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppDimens.SpaceExtraSmall),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AccentColor.entries.forEach { accent ->
                        val isSelected = accentColor == accent
                        val color = parseHexColor(accent.colorHex)

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { onChangeAccentColor(accent) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(AppDimens.IconSizeMedium)
                                )
                            }
                        }
                    }
                }
            }

            // Language Selector
            val currentLangLabel = when (language) {
                "en" -> "English"
                "ja" -> "日本語"
                else -> "简体中文"
            }

            CommonButton(
                text = "${AppStrings.SETTINGS_LANGUAGE.tr(lang)}: $currentLangLabel",
                onClick = {
                    val next = when (language) {
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
            language = "zh",
            onChangeThemeMode = {},
            onChangeAccentColor = {},
            onLanguageChange = {},
            lang = "zh"
        )
    }
}
