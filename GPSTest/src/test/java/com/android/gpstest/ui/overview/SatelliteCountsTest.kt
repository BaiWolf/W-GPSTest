package com.android.gpstest.ui.overview

import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.model.SatelliteStatus
import com.android.gpstest.library.model.SbasType
import org.junit.Assert.*
import org.junit.Test

class SatelliteCountsTest {
    private fun signal(id: Int, system: GnssType, cn0: Float, used: Boolean,
        augmentation: SbasType = SbasType.UNKNOWN) =
        SatelliteStatus(id, system, cn0, false, false, used, 0f, 0f).apply {
            sbasType = augmentation
        }

    @Test fun countsDualFrequencySatelliteOnceAndSeparatesSameIdsAcrossSystems() {
        val counts = summarizeSatellites(listOf(
            signal(12, GnssType.NAVSTAR, 30f, false),
            signal(12, GnssType.NAVSTAR, 25f, true),
            signal(13, GnssType.NAVSTAR, 0f, false),
            signal(12, GnssType.BEIDOU, 28f, true)))
        val gps = counts.single { it.system == GnssType.NAVSTAR }
        val beidou = counts.single { it.system == GnssType.BEIDOU }
        assertEquals(1, gps.used)
        assertEquals(1, gps.visible)
        assertEquals(1, beidou.used)
        assertEquals(1, beidou.visible)
        assertEquals(2, counts.sumOf { it.used })
        assertEquals(2, counts.sumOf { it.visible })
    }

    @Test fun keepsSbasOperatorsAndUnknownSystemsInReconciledTotals() {
        val counts = summarizeSatellites(listOf(
            signal(120, GnssType.SBAS, 24f, true, SbasType.WAAS),
            signal(120, GnssType.SBAS, 29f, false, SbasType.WAAS),
            signal(123, GnssType.SBAS, 26f, false, SbasType.EGNOS),
            signal(9, GnssType.UNKNOWN, 20f, false)))
        assertEquals(1, counts.single { it.augmentation == SbasType.WAAS }.used)
        assertEquals(1, counts.single { it.augmentation == SbasType.EGNOS }.visible)
        assertEquals(1, counts.single { it.system == GnssType.UNKNOWN }.visible)
        assertEquals(1, counts.sumOf { it.used })
        assertEquals(3, counts.sumOf { it.visible })
    }

    @Test fun emptyReceiverShowsSixKnownSystemsWithZeroCounts() {
        val counts = summarizeSatellites(emptyList())
        assertEquals(6, counts.size)
        assertTrue(counts.all { it.used == 0 && it.visible == 0 && it.representative == null })
        assertEquals(6, counts.map { it.system }.distinct().size)
    }
}
