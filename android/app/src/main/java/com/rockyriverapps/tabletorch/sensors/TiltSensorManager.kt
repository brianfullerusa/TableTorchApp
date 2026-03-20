package com.rockyriverapps.tabletorch.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

/**
 * Manages device tilt detection using the accelerometer.
 * Calculates the tilt angle for brightness adjustment.
 *
 * Lifecycle: Call [startListening] when the activity resumes and [stopListening] when it pauses.
 * This class does not hold a strong reference to the context after initialization.
 */
class TiltSensorManager(context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "TiltSensorManager"

        /** Low-pass filter coefficient for smoothing sensor values (0.0-1.0, higher = more smoothing) */
        private const val LOW_PASS_FILTER_ALPHA = 0.8f

        /** Minimum magnitude to consider sensor data valid */
        private const val MIN_MAGNITUDE_THRESHOLD = 0.1f

        /** Minimum tilt change threshold to emit updates (~0.5 degrees) - reduces StateFlow emissions */
        private const val TILT_CHANGE_THRESHOLD = 0.01
    }

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val accelerometer: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val gravitySensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)

    private val _tiltAngle = MutableStateFlow(0.0)
    val tiltAngle: StateFlow<Double> = _tiltAngle.asStateFlow()

    @Volatile
    private var isListening = false

    // Gravity values for calculating orientation — guarded by gravityLock
    private val gravity = FloatArray(3)
    private val gravityLock = Any()

    // Track last emitted tilt to reduce StateFlow emissions
    private var lastEmittedTilt = 0.0

    /**
     * Check if a suitable sensor is available on this device.
     * Prefers gravity sensor over accelerometer for stability.
     *
     * @return true if sensor manager exists and either gravity sensor or accelerometer is available
     */
    fun isAvailable(): Boolean = sensorManager != null && (gravitySensor != null || accelerometer != null)

    /**
     * Start listening for sensor updates.
     * Uses SENSOR_DELAY_UI (~15Hz) for reasonable battery efficiency.
     *
     * @return true if sensor registration succeeded, false if no sensor available or already listening
     */
    fun startListening(): Boolean {
        synchronized(this) {
            if (isListening) {
                Log.d(TAG, "Already listening to sensor")
                return true
            }

            val manager = sensorManager ?: run {
                Log.w(TAG, "SensorManager not available")
                return false
            }

            // Prefer gravity sensor if available (more stable, already filtered)
            val sensor = gravitySensor ?: accelerometer
            return if (sensor != null) {
                val registered = manager.registerListener(
                    this,
                    sensor,
                    SensorManager.SENSOR_DELAY_UI // ~15Hz, better battery than SENSOR_DELAY_GAME
                )
                if (registered) {
                    isListening = true
                    Log.d(TAG, "Started listening to ${sensor.name}")
                    true
                } else {
                    Log.w(TAG, "Failed to register sensor listener for ${sensor.name}")
                    false
                }
            } else {
                Log.w(TAG, "No suitable sensor available for tilt detection")
                false
            }
        }
    }

    /**
     * Stop listening for sensor updates.
     * Safe to call even if not currently listening.
     */
    fun stopListening() {
        synchronized(this) {
            if (isListening) {
                sensorManager?.unregisterListener(this)
                isListening = false
                Log.d(TAG, "Stopped listening to sensor")
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                synchronized(gravityLock) {
                    // Low-pass filter for smoother values
                    gravity[0] = LOW_PASS_FILTER_ALPHA * gravity[0] + (1 - LOW_PASS_FILTER_ALPHA) * event.values[0]
                    gravity[1] = LOW_PASS_FILTER_ALPHA * gravity[1] + (1 - LOW_PASS_FILTER_ALPHA) * event.values[1]
                    gravity[2] = LOW_PASS_FILTER_ALPHA * gravity[2] + (1 - LOW_PASS_FILTER_ALPHA) * event.values[2]
                }

                calculateTiltAngle()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Log accuracy changes for debugging
        Log.d(TAG, "Sensor accuracy changed: ${sensor?.name} -> $accuracy")
    }

    /**
     * Calculate tilt angle from gravity vector.
     * 0 = device flat (screen up OR down), PI/2 = device vertical
     *
     * Uses abs(normalizedZ) so both face-up and face-down orientations
     * result in 0 degrees tilt (100% brightness).
     *
     * Only emits updates when the change exceeds TILT_CHANGE_THRESHOLD
     * to reduce StateFlow emissions and prevent unnecessary recompositions.
     */
    private fun calculateTiltAngle() {
        val gx: Float
        val gy: Float
        val gz: Float
        synchronized(gravityLock) {
            gx = gravity[0]
            gy = gravity[1]
            gz = gravity[2]
        }

        // Calculate total magnitude
        val magnitude = sqrt(gx * gx + gy * gy + gz * gz)

        if (magnitude > MIN_MAGNITUDE_THRESHOLD) {
            // Normalize gravity vector
            val normalizedZ = gz / magnitude

            // Calculate tilt angle from horizontal plane
            // Use abs() so both face-up (gz positive) and face-down (gz negative)
            // result in 0 degrees tilt when the device is flat.
            // When flat: |normalizedZ| ≈ 1.0, so acos(1.0) ≈ 0
            // When vertical: |normalizedZ| ≈ 0, so acos(0) ≈ PI/2
            val tilt = acos(abs(normalizedZ).coerceIn(0f, 1f).toDouble())

            // Only emit if change is significant - reduces StateFlow emissions
            // Uses direct value assignment instead of update{} lambda to avoid allocations
            if (abs(tilt - lastEmittedTilt) > TILT_CHANGE_THRESHOLD) {
                _tiltAngle.value = tilt
                lastEmittedTilt = tilt
            }
        }
    }
}
