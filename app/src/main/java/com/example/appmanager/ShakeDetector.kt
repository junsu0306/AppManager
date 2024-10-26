package com.example.appmanager

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    private val shakeThreshold = 12f  // 흔들기 감지 민감도
    private val minTimeBetweenShakes = 1000  // 두 번의 흔들기 사이 최소 시간 (밀리초)
    private var lastShakeTime: Long = 0

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val accelerationMagnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            val currentTime = System.currentTimeMillis()
            if (accelerationMagnitude > shakeThreshold && (currentTime - lastShakeTime) > minTimeBetweenShakes) {
                lastShakeTime = currentTime
                onShake()  // 흔들기 감지 시 콜백 호출
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
