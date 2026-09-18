package com.listen.expensetracker.core.tile

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.listen.expensetracker.core.quickadd.QuickAddActivity
import com.listen.expensetracker.data.model.AppConstants

/**
 * 控制中心快捷磁贴服务 (ExpenseQuickTileService)。
 * 允许用户在 Android 下拉通知栏/控制中心一键点击启动快速记账弹窗。
 * 兼容性说明：
 * - 在 Android 14+ (API 34+) 适配废弃的 startActivityAndCollapse(Intent)，采用 PendingIntent；
 * - 针对设备锁定状态，调用 unlockAndRun 请求系统锁屏验证后再启动。
 */
class ExpenseQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) {
            unlockAndRun {
                launchQuickAdd()
            }
        } else {
            launchQuickAdd()
        }
    }

    private fun launchQuickAdd() {
        val intent = createQuickAddIntent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        const val QUICK_ADD_URI = "${AppConstants.DeepLink.SCHEME}://${AppConstants.DeepLink.HOST_QUICK_ADD}"
        const val TILE_ACTION = Intent.ACTION_VIEW
        const val TILE_FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

        /**
         * 构建快速记账深度链接启动意图（暴露为纯逻辑函数供单元测试）。
         */
        fun createQuickAddIntent(context: Context): Intent {
            return Intent(context, QuickAddActivity::class.java).apply {
                action = TILE_ACTION
                data = Uri.parse(QUICK_ADD_URI)
                flags = TILE_FLAGS
            }
        }
    }
}
