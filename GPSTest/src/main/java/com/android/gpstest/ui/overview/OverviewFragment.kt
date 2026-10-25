package com.android.gpstest.ui.overview

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.Application.Companion.prefs
import com.android.gpstest.R
import com.android.gpstest.library.data.FixState
import com.android.gpstest.library.data.LocationRepository
import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.model.SatelliteStatus
import com.android.gpstest.library.model.SbasType
import com.android.gpstest.library.ui.SignalInfoViewModel
import com.android.gpstest.library.util.CarrierFreqUtils
import com.android.gpstest.library.util.PreferenceUtil
import com.android.gpstest.ui.status.Flag
import com.android.gpstest.ui.status.FlagImage
import com.android.gpstest.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@AndroidEntryPoint
class OverviewFragment : Fragment() {
    private val signals: SignalInfoViewModel by activityViewModels()
    private val motion: MotionViewModel by activityViewModels()
    @Inject lateinit var repository: LocationRepository

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val direction = if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) ==
                    View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    AppTheme(darkTheme = PreferenceUtil.darkTheme(app, prefs)) {
                        val running by repository.receivingLocationUpdates.collectAsState()
                        val fix by signals.fixState.observeAsState(FixState.NotAcquired)
                        val location by signals.location.observeAsState()
                        val allSignals by signals.allStatuses.observeAsState(emptyList())
                        val displayedSignals by signals.filteredStatuses.observeAsState(emptyList())
                        // Read the timer on both pages so status also expires without new callbacks.
                        val now = produceState(System.currentTimeMillis()) {
                            while (true) {
                                value = System.currentTimeMillis()
                                delay(1000)
                            }
                        }.value
                        val age = location?.let {
                            (SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos) / 1e9
                        } ?: Double.POSITIVE_INFINITY
                        val fresh = running && fix is FixState.Acquired &&
                            location?.provider != "reset" && age in 0.0..15.0
                        val status = receiverStatus(running, fresh, allSignals)
                        Surface(color = if (MaterialTheme.colors.isLight) Color(0xFFF4F5F9)
                            else MaterialTheme.colors.background) {
                            if (arguments?.getBoolean(DASHBOARD) == true) {
                                val msl by signals.altitudeMsl.observeAsState(Double.NaN)
                                Dashboard(status, fresh, location, msl, now, motion)
                            } else {
                                SatelliteOverview(status, fresh,
                                    if (running) displayedSignals else emptyList(),
                                    if (running) allSignals else emptyList(), location)
                            }
                        }
                    }
                }
            }
        }

    companion object {
        const val SATELLITES_TAG = "w_satellites"
        const val DASHBOARD_TAG = "w_dashboard"
        private const val DASHBOARD = "dashboard"
        fun create(dashboard: Boolean) = OverviewFragment().apply {
            arguments = Bundle().apply { putBoolean(DASHBOARD, dashboard) }
        }
    }
}

private val GoodSignal = Color(0xFF2E9D66)
private fun signalColor(cn0: Float) = when {
    cn0 <= 0 -> Color(0xFF9AA1AD)
    cn0 < 20 -> Color(0xFFD85A55)
    cn0 < 30 -> Color(0xFFE2A12C)
    else -> GoodSignal
}
private fun number(value: Double?, digits: Int = 1): String =
    if (value != null && value.isFinite()) String.format(Locale.getDefault(), "%." + digits + "f", value) else "—"

@Composable
private fun Panel(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = 1.dp) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
private fun ReceiverState(status: ReceiverStatus) {
    val label = stringResource(when (status) {
        ReceiverStatus.Off -> R.string.w_receiver_off
        ReceiverStatus.SatelliteFix -> R.string.w_fix_acquired
        ReceiverStatus.PositionOnly -> R.string.w_position_only
        ReceiverStatus.Waiting -> R.string.w_waiting
    })
    val color = when (status) {
        ReceiverStatus.SatelliteFix -> GoodSignal
        ReceiverStatus.PositionOnly -> Color(0xFFE2A12C)
        else -> MaterialTheme.colors.onSurface.copy(alpha = .35f)
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.body2, modifier = Modifier.weight(1f))
        Text("GNSS", style = MaterialTheme.typography.caption, color = MaterialTheme.colors.primary)
    }
}

@Composable
private fun SatelliteOverview(status: ReceiverStatus, fresh: Boolean, signals: List<SatelliteStatus>,
    allSignals: List<SatelliteStatus>, location: Location?) {
    // A dual-frequency satellite contributes one satellite, with the strongest signal shown.
    val groups = signals.groupBy { it.gnssType to it.svid }
    var constellation by remember { mutableStateOf<GnssType?>(null) }
    val rows = groups.values.filter { constellation == null || it.first().gnssType == constellation }
        .sortedWith(compareBy({ it.first().gnssType.ordinal }, { it.first().svid }))
    // Totals describe the receiver; constellation filters only affect the rows below.
    val systemCounts = summarizeSatellites(allSignals)
    val used = systemCounts.sumOf { it.used }
    val visible = systemCounts.sumOf { it.visible }
    val strongest = allSignals.maxOfOrNull { it.cn0DbHz }?.toDouble()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ReceiverState(status) }
        item {
            Panel {
                Row(Modifier.fillMaxWidth()) {
                    Statistic(used.toString(), stringResource(R.string.w_used), Modifier.weight(1f))
                    Statistic(visible.toString(), stringResource(R.string.w_visible), Modifier.weight(1f))
                    Statistic(number(strongest, 0), stringResource(R.string.w_best_cn0), Modifier.weight(1f))
                }
                ConstellationCounts(systemCounts)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                TextButton(onClick = { constellation = null }) {
                    Text(stringResource(R.string.w_all), fontWeight = if (constellation == null) FontWeight.Bold else FontWeight.Normal)
                }
                GnssType.values().filter { type -> groups.keys.any { it.first == type } }.forEach { type ->
                    TextButton(onClick = { constellation = if (constellation == type) null else type }) {
                        Text(constellationName(type), fontWeight = if (constellation == type) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.w_satellite), Modifier.width(104.dp), style = MaterialTheme.typography.caption)
                Text("C/N₀ · dB-Hz", Modifier.weight(1f), style = MaterialTheme.typography.caption)
                Text(stringResource(R.string.w_used_short), Modifier.width(28.dp), style = MaterialTheme.typography.caption, textAlign = TextAlign.Center)
                Text(stringResource(R.string.w_azimuth), Modifier.width(40.dp), style = MaterialTheme.typography.caption, textAlign = TextAlign.End)
                Text(stringResource(R.string.w_elevation), Modifier.width(40.dp), style = MaterialTheme.typography.caption, textAlign = TextAlign.End)
            }
        }
        if (rows.isEmpty()) item {
            Panel {
                Text(stringResource(if (signals.isEmpty()) R.string.w_no_satellites else R.string.w_no_filtered_satellites),
                    style = MaterialTheme.typography.body1)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.w_satellite_hint), style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = .65f))
            }
        }
        items(rows, key = { it.first().gnssType.name + it.first().svid }) { group ->
            SatelliteRow(group)
        }
        item {
            Panel {
                Text("WGS84", color = MaterialTheme.colors.primary, style = MaterialTheme.typography.caption)
                Spacer(Modifier.height(6.dp))
                Text(if (fresh && location != null) coordinates(location) else "—",
                    style = MaterialTheme.typography.body1)
                Text(stringResource(R.string.w_signal_note), style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun ConstellationCounts(counts: List<SatelliteCounts>) {
    val usedLabel = stringResource(R.string.w_used)
    val visibleLabel = stringResource(R.string.w_visible)
    Column {
        Divider(Modifier.padding(vertical = 12.dp))
        Text(usedLabel + " / " + visibleLabel, style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = .65f))
        Spacer(Modifier.height(8.dp))
        counts.chunked(3).forEachIndexed { index, row ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { count ->
                    val name = if (count.system == GnssType.SBAS &&
                        count.augmentation != SbasType.UNKNOWN) count.augmentation.name
                        else constellationName(count.system)
                    val description = name + ", " + usedLabel + ": " + count.used +
                        ", " + visibleLabel + ": " + count.visible
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) {
                        contentDescription = description
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SummaryFlag(count)
                            Spacer(Modifier.width(3.dp))
                            Text(name, style = MaterialTheme.typography.caption)
                        }
                        Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(count.used.toString(), color = GoodSignal,
                                fontSize = 20.sp, fontWeight = FontWeight.Medium)
                            Text(" / ", color = MaterialTheme.colors.onSurface.copy(alpha = .45f),
                                fontSize = 15.sp)
                            Text(count.visible.toString(), color = MaterialTheme.colors.primary,
                                fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SummaryFlag(count: SatelliteCounts) {
    val modifier = Modifier.width(28.dp)
    val status = count.representative
    if (status != null) {
        Flag(status, modifier)
    } else {
        // No sample is needed to show the existing icon for an unavailable constellation.
        val flag = when (count.system) {
            GnssType.NAVSTAR -> R.drawable.ic_flag_usa to R.string.gps_content_description
            GnssType.GLONASS -> R.drawable.ic_flag_russia to R.string.glonass_content_description
            GnssType.GALILEO -> R.drawable.ic_flag_european_union to R.string.galileo_content_description
            GnssType.BEIDOU -> R.drawable.ic_flag_china to R.string.beidou_content_description
            GnssType.QZSS -> R.drawable.ic_flag_japan to R.string.qzss_content_description
            GnssType.IRNSS -> R.drawable.ic_flag_india to R.string.irnss_content_description
            else -> null
        }
        if (flag != null) FlagImage(flag.first, flag.second, modifier) else Spacer(modifier)
    }
}

@Composable
private fun Statistic(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colors.primary)
        Text(label, style = MaterialTheme.typography.caption, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SatelliteRow(group: List<SatelliteStatus>) {
    val best = group.maxByOrNull { it.cn0DbHz }!!
    val used = group.any { it.usedInFix }
    val bands = group.filter { it.hasCarrierFrequency }.map { CarrierFreqUtils.getCarrierFrequencyLabel(it) }
        .distinct().joinToString("/\u200B")
    val usedLabel = stringResource(if (used) R.string.w_used_in_fix else R.string.w_not_used)
    Card(shape = RoundedCornerShape(8.dp), elevation = 0.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(104.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Flag(best, Modifier.width(30.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(best.svid.toString(), fontWeight = FontWeight.Medium,
                        fontSize = 15.sp, maxLines = 1)
                }
                Text(if (bands.isBlank()) constellationName(best.gnssType) else bands,
                    style = MaterialTheme.typography.caption)
            }
            Column(Modifier.weight(1f).padding(end = 4.dp)) {
                Text(number(best.cn0DbHz.takeIf { it > 0 }?.toDouble(), 0), fontSize = 14.sp,
                    color = signalColor(best.cn0DbHz), fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = (best.cn0DbHz / 60f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = signalColor(best.cn0DbHz),
                    backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = .08f))
            }
            Text(if (used) "✓" else "—", Modifier.width(28.dp).semantics { contentDescription = usedLabel },
                textAlign = TextAlign.Center, color = if (used) GoodSignal else MaterialTheme.colors.onSurface.copy(alpha = .4f))
            Text(number(best.azimuthDegrees.toDouble(), 0) + "°", Modifier.width(40.dp), fontSize = 13.sp, textAlign = TextAlign.End)
            Text(number(best.elevationDegrees.toDouble(), 0) + "°", Modifier.width(40.dp), fontSize = 13.sp, textAlign = TextAlign.End)
        }
    }
}

private fun constellationName(type: GnssType) = when (type) {
    GnssType.NAVSTAR -> "GPS"
    GnssType.GLONASS -> "GLONASS"
    GnssType.GALILEO -> "Galileo"
    GnssType.BEIDOU -> "BeiDou"
    GnssType.QZSS -> "QZSS"
    GnssType.IRNSS -> "NavIC"
    GnssType.SBAS -> "SBAS"
    else -> "GNSS"
}
private fun coordinates(location: Location) = String.format(Locale.getDefault(), "%.6f°, %.6f°",
    location.latitude, location.longitude)

@Composable
private fun Dashboard(status: ReceiverStatus, fresh: Boolean, location: Location?, altitudeMsl: Double,
    now: Long, motion: MotionViewModel) {
    val context = LocalContext.current
    val speedUnit = PreferenceUtil.speedUnits(context, prefs)
    val speedFactor = when (speedUnit) {
        PreferenceUtil.KILOMETERS_PER_HOUR -> 3.6
        PreferenceUtil.METERS_PER_SECOND -> 1.0
        else -> 2.2369362921
    }
    val unitLabel = when (speedUnit) {
        PreferenceUtil.KILOMETERS_PER_HOUR -> "km/h"
        PreferenceUtil.METERS_PER_SECOND -> "m/s"
        else -> "mph"
    }
    val speed = if (fresh && location?.hasSpeed() == true) location.speed.toDouble() * speedFactor else null
    val bearing = if (fresh && location?.hasBearing() == true) location.bearing.toDouble() else null
    val msl = if (altitudeMsl.isFinite()) altitudeMsl else if (Build.VERSION.SDK_INT >= 34 &&
        location?.hasMslAltitude() == true) location.mslAltitudeMeters else Double.NaN
    val altitude = if (!fresh) null else if (msl.isFinite()) msl
        else if (location?.hasAltitude() == true) location.altitude else null
    val metric = PreferenceUtil.distanceUnits(context, prefs).equals(PreferenceUtil.METERS, true)
    val distanceFactor = if (metric) 1.0 else 3.280839895
    val distanceUnit = if (metric) "m" else "ft"

    // Use spare height for larger instruments while keeping a compact fallback on short screens.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val roomy = maxHeight >= 700.dp && maxWidth >= 340.dp
        val panelPadding = if (roomy) 16.dp else 12.dp
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(if (roomy) 12.dp else 8.dp)) {
            item { ReceiverState(status) }
            item { SpeedAndCourse(speed, speedFactor, unitLabel, bearing, roomy) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric(stringResource(R.string.w_average), number(motion.average?.times(speedFactor)) + " " + unitLabel, Modifier.weight(1f), roomy)
                    Metric(stringResource(R.string.w_maximum), number(motion.maximum?.times(speedFactor)) + " " + unitLabel, Modifier.weight(1f), roomy)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Metric(stringResource(if (msl.isFinite()) R.string.w_altitude_msl else R.string.w_altitude_wgs84),
                        number(altitude?.times(distanceFactor)) + " " + distanceUnit, Modifier.weight(1f), roomy)
                    Metric(stringResource(R.string.w_accuracy),
                        number(if (fresh && location?.hasAccuracy() == true) location.accuracy * distanceFactor else null) + " " + distanceUnit,
                        Modifier.weight(1f), roomy)
                }
            }
            item {
                Panel(padding = panelPadding) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ClockColumn(stringResource(R.string.w_local_clock), now, null, Modifier.weight(1f), roomy)
                        ClockColumn(stringResource(R.string.w_fix_utc),
                            if (fresh) location?.time else null, TimeZone.getTimeZone("UTC"), Modifier.weight(1f), roomy)
                    }
                }
            }
            item {
                Panel(padding = panelPadding) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("WGS84", color = MaterialTheme.colors.primary, style = MaterialTheme.typography.caption)
                        Spacer(Modifier.width(8.dp))
                        Text(if (fresh && location != null) coordinates(location) else "—",
                            modifier = Modifier.weight(1f), textAlign = TextAlign.End,
                            style = MaterialTheme.typography.body2.copy(textDirection = TextDirection.Ltr))
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.w_session_note), Modifier.weight(1f),
                        fontSize = if (roomy) 12.sp else 11.sp, lineHeight = if (roomy) 17.sp else 15.sp)
                    TextButton(onClick = motion::reset) { Text(stringResource(R.string.w_reset_stats)) }
                }
            }
        }
    }
}

@Composable
private fun SpeedAndCourse(speed: Double?, factor: Double, unit: String, bearing: Double?, roomy: Boolean) {
    Panel(padding = if (roomy) 16.dp else 12.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(if (roomy) 16.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.w_speed), style = MaterialTheme.typography.subtitle1)
                Box(Modifier.fillMaxWidth().height(if (roomy) 176.dp else 128.dp), contentAlignment = Alignment.Center) {
                    val primary = MaterialTheme.colors.primary
                    val track = MaterialTheme.colors.onSurface.copy(alpha = .08f)
                    Canvas(Modifier.size(if (roomy) 188.dp else 160.dp, if (roomy) 176.dp else 128.dp)) {
                        val diameter = minOf(size.width, size.height) - 14.dp.toPx()
                        val origin = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f + 8.dp.toPx())
                        drawArc(track, 150f, 240f, false, origin, Size(diameter, diameter), style = Stroke(6.dp.toPx()))
                        val progress = ((speed ?: 0.0) / (160.0 / 3.6 * factor)).coerceIn(0.0, 1.0)
                        drawArc(primary, 150f, (progress * 240).toFloat(), false, origin, Size(diameter, diameter), style = Stroke(6.dp.toPx()))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(number(speed), fontSize = if (roomy) 52.sp else 42.sp, fontWeight = FontWeight.Light, color = primary)
                        Text(unit, style = MaterialTheme.typography.caption)
                    }
                }
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.w_course), style = MaterialTheme.typography.caption, textAlign = TextAlign.Center)
                Compass(bearing, Modifier.width(if (roomy) 76.dp else 60.dp))
                Text(if (bearing == null) "—" else number(bearing, 0) + "°",
                    fontSize = if (roomy) 30.sp else 25.sp, color = MaterialTheme.colors.primary)
                Text(stringResource(R.string.w_course_hint), fontSize = if (roomy) 11.sp else 10.sp, lineHeight = if (roomy) 15.sp else 13.sp,
                    textAlign = TextAlign.Center)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.w_gnss_speed), Modifier.weight(1f),
                fontSize = if (roomy) 11.sp else 10.sp, lineHeight = if (roomy) 15.sp else 13.sp)
            Text(stringResource(R.string.w_units_settings), Modifier.weight(1f),
                fontSize = if (roomy) 11.sp else 10.sp, lineHeight = if (roomy) 15.sp else 13.sp, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun ClockColumn(label: String, timestamp: Long?, zone: TimeZone?, modifier: Modifier, roomy: Boolean) {
    fun format(pattern: String): String = if (timestamp == null) "—" else
        SimpleDateFormat(pattern, Locale.getDefault()).apply {
            if (zone != null) timeZone = zone
        }.format(Date(timestamp))
    Column(modifier) {
        Text(label, Modifier.heightIn(min = 28.dp), style = MaterialTheme.typography.caption)
        Text(format("HH:mm:ss"), fontSize = if (roomy) 28.sp else 24.sp,
            style = MaterialTheme.typography.body1.copy(textDirection = TextDirection.Ltr))
        Text(format("yyyy-MM-dd"), style = MaterialTheme.typography.caption.copy(textDirection = TextDirection.Ltr))
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier, roomy: Boolean) {
    Panel(modifier, padding = if (roomy) 16.dp else 12.dp) {
        Text(label, style = MaterialTheme.typography.caption)
        Text(value, fontSize = if (roomy) 26.sp else 23.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colors.primary)
    }
}

@Composable
private fun Compass(bearing: Double?, modifier: Modifier) {
    val primary = MaterialTheme.colors.primary
    val track = MaterialTheme.colors.onSurface.copy(alpha = .15f)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("N", style = MaterialTheme.typography.caption)
        Spacer(Modifier.height(8.dp))
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f).padding(2.dp)) {
            val radius = size.minDimension / 2f
            drawCircle(track, style = Stroke(1.dp.toPx()))
            if (bearing != null) rotate(bearing.toFloat()) {
                val arrow = Path().apply {
                    moveTo(center.x, center.y - radius * .8f)
                    lineTo(center.x + radius * .25f, center.y + radius * .6f)
                    lineTo(center.x, center.y + radius * .35f)
                    lineTo(center.x - radius * .25f, center.y + radius * .6f)
                    close()
                }
                drawPath(arrow, primary)
            }
        }
    }
}
