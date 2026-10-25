package com.android.gpstest.ui.overview

import android.location.Location
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class MotionViewModel : ViewModel() {
    private val statistics = MotionStatistics()
    var average by mutableStateOf<Double?>(null)
        private set
    var maximum by mutableStateOf<Double?>(null)
        private set

    fun record(location: Location) {
        val age = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1e9
        if (location.provider == "reset" || age !in 0.0..15.0) {
            statistics.pause()
            return
        }
        statistics.record(location.elapsedRealtimeNanos / 1e9,
            if (location.hasSpeed()) location.speed.toDouble() else null)
        average = statistics.averageMetersPerSecond
        maximum = statistics.maximumMetersPerSecond
    }

    fun pause() = statistics.pause()

    fun reset() {
        statistics.reset()
        average = null
        maximum = null
    }
}
