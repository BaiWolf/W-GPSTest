package com.android.gpstest.ui.overview

import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.model.SatelliteStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiverStatusTest {
    private fun signal(used: Boolean) =
        SatelliteStatus(8, GnssType.NAVSTAR, 35f, false, false, used, 20f, 45f)

    @Test fun positionArrivingBeforeSatelliteConfirmationIsNotShownAsSatelliteFix() {
        assertEquals(ReceiverStatus.Waiting, receiverStatus(true, false, emptyList()))
        assertEquals(ReceiverStatus.PositionOnly, receiverStatus(true, true, emptyList()))
        val visible = listOf(signal(false))
        assertEquals(1, summarizeSatellites(visible).sumOf { it.visible })
        assertEquals(0, summarizeSatellites(visible).sumOf { it.used })
        assertEquals(ReceiverStatus.PositionOnly, receiverStatus(true, true, visible))
        val confirmed = listOf(signal(true))
        assertEquals(ReceiverStatus.SatelliteFix, receiverStatus(true, true, confirmed))
        assertEquals(1, summarizeSatellites(confirmed).sumOf { it.used })
    }

    @Test fun freshCoordinatesDoNotKeepGreenStatusAfterSatelliteUseStops() {
        assertEquals(ReceiverStatus.SatelliteFix, receiverStatus(true, true, listOf(signal(true))))
        assertEquals(ReceiverStatus.PositionOnly, receiverStatus(true, true, listOf(signal(false))))
    }

    @Test fun stoppedOrStalePositionCannotKeepSatelliteFix() {
        val previous = listOf(signal(true))
        assertEquals(ReceiverStatus.Off, receiverStatus(false, true, previous))
        assertEquals(ReceiverStatus.Waiting, receiverStatus(true, false, previous))
        assertEquals(ReceiverStatus.Waiting, receiverStatus(true, false, emptyList()))
    }
}
