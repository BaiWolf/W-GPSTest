package com.android.gpstest.ui.overview

import com.android.gpstest.library.model.SatelliteStatus

internal enum class ReceiverStatus { Off, Waiting, PositionOnly, SatelliteFix }

/** A recent coordinate alone does not confirm use of fresh satellite measurements. */
internal fun receiverStatus(
    running: Boolean,
    hasFreshPosition: Boolean,
    satellites: List<SatelliteStatus>
): ReceiverStatus = when {
    !running -> ReceiverStatus.Off
    !hasFreshPosition -> ReceiverStatus.Waiting
    satellites.any { it.usedInFix } -> ReceiverStatus.SatelliteFix
    else -> ReceiverStatus.PositionOnly
}
