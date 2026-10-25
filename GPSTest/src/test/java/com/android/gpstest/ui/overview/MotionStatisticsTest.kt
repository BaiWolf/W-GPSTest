package com.android.gpstest.ui.overview

import org.junit.Assert.*
import org.junit.Test

class MotionStatisticsTest {
    @Test fun averageUsesTimeAndIncludesStationarySamples() {
        val stats = MotionStatistics()
        stats.record(1.0, 10.0)
        stats.record(3.0, 20.0)
        stats.record(7.0, 0.0)
        assertEquals(70.0 / 6, stats.averageMetersPerSecond!!, 0.0001)
        assertEquals(20.0, stats.maximumMetersPerSecond!!, 0.0)
    }

    @Test fun duplicatesAndOldSamplesDoNotDistortResults() {
        val stats = MotionStatistics()
        stats.record(2.0, 5.0)
        stats.record(2.0, 100.0)
        stats.record(1.0, 100.0)
        stats.record(3.0, 5.0)
        assertEquals(5.0, stats.averageMetersPerSecond!!, 0.0)
        assertEquals(5.0, stats.maximumMetersPerSecond!!, 0.0)
    }

    @Test fun missingSpeedAndPausesDoNotCreateFalseZeroReadings() {
        val stats = MotionStatistics()
        stats.record(1.0, 10.0)
        stats.record(2.0, null)
        stats.record(3.0, 20.0)
        assertNull(stats.averageMetersPerSecond)
        stats.pause()
        stats.record(100.0, 30.0)
        assertNull(stats.averageMetersPerSecond)
        stats.record(101.0, 30.0)
        stats.record(200.0, 10.0)
        assertEquals(30.0, stats.averageMetersPerSecond!!, 0.0)
        stats.reset()
        assertNull(stats.maximumMetersPerSecond)
        assertNull(stats.averageMetersPerSecond)
    }
}
