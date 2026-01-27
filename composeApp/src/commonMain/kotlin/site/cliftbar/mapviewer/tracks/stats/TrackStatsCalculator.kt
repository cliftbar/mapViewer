package site.cliftbar.mapviewer.tracks.stats

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.PI
import site.cliftbar.mapviewer.tracks.Track

class TrackStatsCalculator {
    private val statDefinitions = listOf(
        TrackStatDefinition(TrackStatId.SPEED_MIN, "Min speed"),
        TrackStatDefinition(TrackStatId.SPEED_MAX, "Max speed"),
        TrackStatDefinition(TrackStatId.SPEED_AVG, "Avg speed"),
        TrackStatDefinition(TrackStatId.ELEVATION_MIN, "Min elevation"),
        TrackStatDefinition(TrackStatId.ELEVATION_MAX, "Max elevation"),
        TrackStatDefinition(TrackStatId.ELEVATION_GAIN, "Elevation gain"),
        TrackStatDefinition(TrackStatId.ELEVATION_LOSS, "Elevation loss"),
        TrackStatDefinition(TrackStatId.ELEVATION_NET, "Net elevation"),
        TrackStatDefinition(TrackStatId.STOPPED_TIME, "Stopped time")
    )

    fun computeStats(track: Track, context: TrackStatsContext): List<TrackStatResult> {
        val speedSamples = buildSpeedSamples(track)
        val totalDistanceMeters = speedSamples.sumOf { it.distanceMeters }
        val totalTimeSeconds = speedSamples.sumOf { it.durationSeconds }

        val stoppedTimeSeconds = StoppedTimeAlgorithmRegistry
            .getAlgorithm(context.stoppedAlgorithmId)
            .computeStoppedTimeSeconds(
                samples = speedSamples,
                speedThresholdMps = context.stoppedSpeedThresholdMps,
                minStopSeconds = context.stoppedMinStopSeconds
            )

        val movingTimeSeconds = (totalTimeSeconds - stoppedTimeSeconds).coerceAtLeast(0.0)

        val speedMin = speedSamples.minOfOrNull { it.speedMps }
        val speedMax = speedSamples.maxOfOrNull { it.speedMps }
        val avgSpeed = when (context.avgSpeedBasis) {
            AvgSpeedBasis.TOTAL_TIME -> if (totalTimeSeconds > 0.0) totalDistanceMeters / totalTimeSeconds else null
            AvgSpeedBasis.MOVING_TIME -> if (movingTimeSeconds > 0.0) totalDistanceMeters / movingTimeSeconds else null
        }

        val elevationStats = computeElevationStats(track)

        val statValues = mapOf(
            TrackStatId.SPEED_MIN to speedMin,
            TrackStatId.SPEED_MAX to speedMax,
            TrackStatId.SPEED_AVG to avgSpeed,
            TrackStatId.ELEVATION_MIN to elevationStats.min,
            TrackStatId.ELEVATION_MAX to elevationStats.max,
            TrackStatId.ELEVATION_GAIN to elevationStats.gain,
            TrackStatId.ELEVATION_LOSS to elevationStats.loss,
            TrackStatId.ELEVATION_NET to elevationStats.net,
            TrackStatId.STOPPED_TIME to if (totalTimeSeconds > 0.0) stoppedTimeSeconds else null
        )

        return statDefinitions.map { definition ->
            val value = statValues[definition.id]
            TrackStatResult(
                definition = definition,
                value = value,
                formattedValue = formatStat(definition.id, value, context)
            )
        }
    }

    private fun buildSpeedSamples(track: Track): List<SpeedSample> {
        return track.segments.flatMap { segment ->
            val points = segment.points
            if (points.size < 2) return@flatMap emptyList()

            val samples = mutableListOf<SpeedSample>()
            for (index in 0 until points.size - 1) {
                val start = points[index]
                val end = points[index + 1]
                val startTime = start.time
                val endTime = end.time
                if (startTime == null || endTime == null) continue
                val durationSeconds = (endTime - startTime) / 1000.0
                if (durationSeconds <= 0.0) continue
                val distanceMeters = haversineMeters(
                    start.latitude,
                    start.longitude,
                    end.latitude,
                    end.longitude
                )
                val speedMps = distanceMeters / durationSeconds
                samples.add(
                    SpeedSample(
                        distanceMeters = distanceMeters,
                        durationSeconds = durationSeconds,
                        speedMps = speedMps
                    )
                )
            }
            samples
        }
    }

    private data class ElevationStats(
        val min: Double?,
        val max: Double?,
        val gain: Double?,
        val loss: Double?,
        val net: Double?
    )

    private fun computeElevationStats(track: Track): ElevationStats {
        val pointsWithElevation = track.segments.flatMap { it.points }.filter { it.elevation != null }
        val minElevation = pointsWithElevation.minOfOrNull { it.elevation ?: 0.0 }
        val maxElevation = pointsWithElevation.maxOfOrNull { it.elevation ?: 0.0 }

        var gain = 0.0
        var loss = 0.0
        var hasPairs = false

        track.segments.forEach { segment ->
            val points = segment.points
            for (index in 0 until points.size - 1) {
                val start = points[index].elevation
                val end = points[index + 1].elevation
                if (start == null || end == null) continue
                val delta = end - start
                if (delta > 0) gain += delta else loss += abs(delta)
                hasPairs = true
            }
        }

        val elevationOrdered = pointsWithElevation
        val netElevation = if (elevationOrdered.size >= 2) {
            val first = elevationOrdered.first().elevation ?: 0.0
            val last = elevationOrdered.last().elevation ?: 0.0
            last - first
        } else {
            null
        }

        return ElevationStats(
            min = minElevation,
            max = maxElevation,
            gain = if (hasPairs) gain else null,
            loss = if (hasPairs) loss else null,
            net = netElevation
        )
    }

    private fun formatStat(id: TrackStatId, value: Double?, context: TrackStatsContext): String {
        if (value == null || value.isNaN() || value.isInfinite()) return "-"
        return when (id) {
            TrackStatId.SPEED_MIN,
            TrackStatId.SPEED_MAX,
            TrackStatId.SPEED_AVG -> formatSpeed(value, context.speedUnit)
            TrackStatId.ELEVATION_MIN,
            TrackStatId.ELEVATION_MAX,
            TrackStatId.ELEVATION_GAIN,
            TrackStatId.ELEVATION_LOSS,
            TrackStatId.ELEVATION_NET -> formatDistance(value, context.distanceUnit)
            TrackStatId.STOPPED_TIME -> formatDuration(value)
        }
    }

    private fun formatDuration(seconds: Double): String {
        val totalSeconds = seconds.roundToInt().coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val remainingSeconds = totalSeconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m ${remainingSeconds}s"
            minutes > 0 -> "${minutes}m ${remainingSeconds}s"
            else -> "${remainingSeconds}s"
        }
    }

    private fun formatDistance(meters: Double, unit: DistanceUnit): String {
        val (value, suffix, decimals) = when (unit) {
            DistanceUnit.METERS -> Triple(meters, "m", 0)
            DistanceUnit.KILOMETERS -> Triple(meters / 1000.0, "km", 2)
            DistanceUnit.FEET -> Triple(meters * 3.28084, "ft", 0)
            DistanceUnit.MILES -> Triple(meters / 1609.344, "mi", 2)
        }
        val formatted = formatDecimal(value, decimals)
        return "$formatted $suffix"
    }

    private fun formatSpeed(metersPerSecond: Double, unit: SpeedUnit): String {
        val (value, suffix) = when (unit) {
            SpeedUnit.KMH -> Pair(metersPerSecond * 3.6, "km/h")
            SpeedUnit.MPS -> Pair(metersPerSecond, "m/s")
            SpeedUnit.MPH -> Pair(metersPerSecond * 2.236936, "mph")
            SpeedUnit.KNOTS -> Pair(metersPerSecond * 1.943844, "kn")
        }
        val formatted = formatDecimal(value, 1)
        return "$formatted $suffix"
    }

    private fun formatDecimal(value: Double, decimals: Int): String {
        if (decimals <= 0) return value.roundToInt().toString()
        val factor = 10.0.pow(decimals)
        val rounded = round(value * factor) / factor
        val text = rounded.toString()
        val parts = text.split(".")
        if (parts.size == 1) {
            return text + "." + "0".repeat(decimals)
        }
        val fraction = parts[1].padEnd(decimals, '0').take(decimals)
        return parts[0] + "." + fraction
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)
        val startLat = toRadians(lat1)
        val endLat = toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
            sin(dLon / 2) * sin(dLon / 2) * cos(startLat) * cos(endLat)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun toRadians(degrees: Double): Double {
        return degrees * (PI / 180.0)
    }
}
