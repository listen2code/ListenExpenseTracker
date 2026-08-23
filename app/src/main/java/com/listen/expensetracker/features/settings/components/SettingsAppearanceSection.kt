package com.listen.expensetracker.features.settings.components

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import com.listen.uicomponent.theme.parseHexColor

/**
 * Settings Card for Theme Mode, Accent Color, Currency, and Language customization.
 */
@Composable
fun SettingsAppearanceSection(
    themeMode: ThemeMode,
    accentColor: AccentColor,
    currencySymbol: String,
    language: String,
    onChangeThemeMode: (ThemeMode) -> Unit,
    onChangeAccentColor: (AccentColor) -> Unit,
    onOpenCurrencyDialog: () -> Unit,
    onLanguageChange: (String) -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    SurfaceCard(
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceLarge,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceLarge)) {
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
                    text = StringsRes.get("settings_appearance", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = AppDimens.TextTitle
                )
            }

            // Theme Mode Segmented Switch
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
                Text(
                    text = StringsRes.get("settings_theme_mode", lang),
                    fontSize = AppDimens.TextSubtitle,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val modes = listOf(
                        ThemeMode.LIGHT to StringsRes.get("theme_light", lang),
                        ThemeMode.DARK to StringsRes.get("theme_dark", lang),
                        ThemeMode.SYSTEM to StringsRes.get("theme_system", lang)
                    )

                    modes.forEachIndexed { index, (mode, label) ->
                        SegmentedButton(
                            selected = themeMode == mode,
                            onClick = { onChangeThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                        ) {
                            Text(text = label, fontSize = AppDimens.TextSmall)
                        }
                    }
                }
            }

            // Accent Color Selection Row
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
                Text(
                    text = StringsRes.get("settings_accent_color", lang),
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

            // Currency & Language Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                OutlinedButton(
                    onClick = onOpenCurrencyDialog,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("${StringsRes.get("settings_currency", lang)} ($currencySymbol)", fontSize = AppDimens.TextSmall)
                }

                val currentLangLabel = when (language) {
                    "en" -> "English"
                    "ja" -> "日本語"
                    else -> "简体中文"
                }

                OutlinedButton(
                    onClick = {
                        val next = when (language) {
                            "zh" -> "en"
                            "en" -> "ja"
                            else -> "zh"
                        }
                        onLanguageChange(next)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("${StringsRes.get("settings_language", lang)}: $currentLangLabel", fontSize = AppDimens.TextSmall)
                }
            }
        }
    }
}
