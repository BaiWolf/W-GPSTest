package com.android.gpstest.ui.overview

/** Time-weighted GNSS speeds. Missing samples and pauses never count as zero speed. */
class MotionStatistics {
    var maximumMetersPerSecond: Double? = null
        private set
    var durationSeconds = 0.0
        private set
    private var integratedSpeed = 0.0
    private var previousTime: Double? = null
    private var previousSpeed: Double? = null
    val averageMetersPerSecond: Double?
        get() = if (durationSeconds > 0) integratedSpeed / durationSeconds else null

    fun record(seconds: Double, metersPerSecond: Double?) {
        if (!seconds.isFinite() || seconds <= 0) return
        val lastTime = previousTime
        if (lastTime != null && seconds <= lastTime) return
        if (metersPerSecond == null || !metersPerSecond.isFinite() || metersPerSecond < 0) {
            pause()
            return
        }
        maximumMetersPerSecond = maxOf(maximumMetersPerSecond ?: 0.0, metersPerSecond)
        if (lastTime != null && previousSpeed != null) {
            val delta = seconds - lastTime
            // Do not integrate across a stopped receiver, stale fix, or long sampling gap.
            if (delta <= 15.0) {
                integratedSpeed += (previousSpeed!! + metersPerSecond) * 0.5 * delta
                durationSeconds += delta
            }
        }
        previousTime = seconds
        previousSpeed = metersPerSecond
    }

    fun pause() {
        previousTime = null
        previousSpeed = null
    }

    fun reset() {
        pause()
        maximumMetersPerSecond = null
        durationSeconds = 0.0
        integratedSpeed = 0.0
    }
}
