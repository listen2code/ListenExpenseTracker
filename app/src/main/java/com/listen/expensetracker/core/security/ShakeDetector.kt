package com.listen.expensetracker.core.security

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import com.listen.arch.apm.ApmLogger
import kotlin.math.sqrt

/**
 * 轻量级加速度传感器手势监听器 (ShakeDetector)。
 * 用于监听用户“摇一摇手机”物理手势，具备节流防抖与传感器生命周期安全绑定机制。
 */
class ShakeDetector(
    private val onShake: () -> Unit,
    private val thresholdAcceleration: Float = 13.0f,
    private val throttleIntervalMs: Long = 1000L
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTimestamp = 0L

    /**
     * 注册传感器监听。
     */
    fun start(context: Context) {
        if (sensorManager != null) return
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelerometer?.let { sensor ->
                sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
                ApmLogger.i("ShakeDetector", "ShakeDetector registered successfully")
            }
        } catch (e: Exception) {
            ApmLogger.w("ShakeDetector", "Failed to register accelerometer: ${e.message}")
        }
    }

    /**
     * 注销传感器监听，避免后台电量消耗。
     */
    fun stop() {
        try {
            sensorManager?.unregisterListener(this)
            sensorManager = null
            accelerometer = null
            ApmLogger.i("ShakeDetector", "ShakeDetector unregistered")
        } catch (e: Exception) {
            ApmLogger.w("ShakeDetector", "Failed to unregister accelerometer: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // 计算总重力加速度矢量差
        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()
        val accelerationDelta = (gForce - 1.0f) * SensorManager.GRAVITY_EARTH

        if (accelerationDelta > thresholdAcceleration) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastShakeTimestamp >= throttleIntervalMs) {
                lastShakeTimestamp = now
                ApmLogger.i("ShakeDetector", "Shake gesture detected (delta: $accelerationDelta)")
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
