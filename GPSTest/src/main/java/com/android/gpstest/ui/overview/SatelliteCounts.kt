package com.android.gpstest.ui.overview

import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.model.SatelliteStatus
import com.android.gpstest.library.model.SbasType

data class SatelliteCounts(
    val system: GnssType,
    val augmentation: SbasType,
    val used: Int,
    val visible: Int,
    val representative: SatelliteStatus?
)

/** Count satellites, not frequency observations; keep augmentation operators separate. */
fun summarizeSatellites(signals: List<SatelliteStatus>): List<SatelliteCounts> {
    val satellites = signals.groupBy { it.gnssType to it.svid }
    val systems = satellites.values.groupBy { satellite ->
        val representative = satellite.firstOrNull {
            it.gnssType == GnssType.SBAS && it.sbasType != SbasType.UNKNOWN
        } ?: satellite.first()
        representative.gnssType to if (representative.gnssType == GnssType.SBAS)
            representative.sbasType else SbasType.UNKNOWN
    }
    val majorSystems = listOf(GnssType.NAVSTAR, GnssType.GLONASS, GnssType.GALILEO,
        GnssType.BEIDOU, GnssType.QZSS, GnssType.IRNSS).map { it to SbasType.UNKNOWN }
    val otherSystems = systems.keys.filter { it !in majorSystems }
        .sortedWith(compareBy({ it.first.ordinal }, { it.second.ordinal }))
    return (majorSystems + otherSystems).map { (system, augmentation) ->
        val groups = systems[system to augmentation].orEmpty()
        val representative = groups.flatten().firstOrNull {
            system != GnssType.SBAS || it.sbasType == augmentation
        }
        SatelliteCounts(system, augmentation,
            groups.count { satellite -> satellite.any { it.usedInFix } },
            groups.count { satellite -> satellite.any { it.cn0DbHz > 0f } },
            representative)
    }
}
