package com.listen.expensetracker.core.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.theme.ListenTheme

/**
 * 全屏生物识别锁定遮罩层 (BiometricLockOverlay)。
 * 当应用处于锁定状态时位于最高层级，完全遮蔽底层敏感财务数据并提供安全解锁入口。
 *
 * @param onUnlockRequest 点击解锁回调，拉起生物识别或锁屏密码鉴权
 * @param modifier Composable 修饰符（首个可选参数）
 * @param lang 国际化语言代码
 */
@Composable
fun BiometricLockOverlay(
    onUnlockRequest: () -> Unit,
    modifier: Modifier = Modifier,
    lang: String = "zh"
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 安全盾牌与指纹徽章
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 标题
            CommonText(
                text = AppStrings.SECURITY_LOCKED_TITLE.tr(lang),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 说明文案
            CommonText(
                text = AppStrings.SECURITY_LOCKED_DESC.tr(lang),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 解锁操作按钮
            CommonButton(
                text = AppStrings.SECURITY_UNLOCK_BUTTON.tr(lang),
                onClick = onUnlockRequest,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                cornerRadius = AppDimens.CornerButton,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BiometricLockOverlayPreview() {
    ExpenseStrings.init()
    ListenTheme {
        BiometricLockOverlay(
            onUnlockRequest = {},
            lang = "zh"
        )
    }
}
