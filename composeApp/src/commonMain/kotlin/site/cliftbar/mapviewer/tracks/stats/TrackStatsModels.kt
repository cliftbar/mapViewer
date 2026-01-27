package site.cliftbar.mapviewer.tracks.stats

import kotlinx.serialization.Serializable
import site.cliftbar.mapviewer.config.Config

@Serializable
enum class AvgSpeedBasis {
    TOTAL_TIME,
    MOVING_TIME
}

@Serializable
enum class StoppedTimeAlgorithmId {
    SPEED_THRESHOLD
}

@Serializable
enum class DistanceUnit(val label: String) {
    METERS("m"),
    KILOMETERS("km"),
    FEET("ft"),
    MILES("mi")
}

@Serializable
enum class SpeedUnit(val label: String) {
    KMH("km/h"),
    MPS("m/s"),
    MPH("mph"),
    KNOTS("kn")
}

data class TrackStatsPrefs(
    val trackId: String,
    val avgSpeedBasis: AvgSpeedBasis? = null,
    val stoppedAlgorithmId: StoppedTimeAlgorithmId? = null,
    val stoppedSpeedThresholdMps: Double? = null,
    val stoppedMinStopSeconds: Int? = null,
    val distanceUnit: DistanceUnit? = null,
    val speedUnit: SpeedUnit? = null
)

data class TrackStatsContext(
    val avgSpeedBasis: AvgSpeedBasis,
    val stoppedAlgorithmId: StoppedTimeAlgorithmId,
    val stoppedSpeedThresholdMps: Double,
    val stoppedMinStopSeconds: Int,
    val distanceUnit: DistanceUnit,
    val speedUnit: SpeedUnit
) {
    companion object {
        fun fromConfig(config: Config, prefs: TrackStatsPrefs?): TrackStatsContext {
            return TrackStatsContext(
                avgSpeedBasis = prefs?.avgSpeedBasis ?: config.defaultAvgSpeedBasis,
                stoppedAlgorithmId = prefs?.stoppedAlgorithmId ?: config.stoppedTimeAlgorithmId,
                stoppedSpeedThresholdMps = prefs?.stoppedSpeedThresholdMps ?: config.stoppedTimeSpeedThresholdMps,
                stoppedMinStopSeconds = prefs?.stoppedMinStopSeconds ?: config.stoppedTimeMinStopSeconds,
                distanceUnit = prefs?.distanceUnit ?: config.defaultDistanceUnit,
                speedUnit = prefs?.speedUnit ?: config.defaultSpeedUnit
            )
        }
    }
}

enum class TrackStatId {
    SPEED_MIN,
    SPEED_MAX,
    SPEED_AVG,
    ELEVATION_MIN,
    ELEVATION_MAX,
    ELEVATION_GAIN,
    ELEVATION_LOSS,
    ELEVATION_NET,
    STOPPED_TIME
}

data class TrackStatDefinition(
    val id: TrackStatId,
    val label: String
)

data class TrackStatResult(
    val definition: TrackStatDefinition,
    val value: Double?,
    val formattedValue: String
)

data class SpeedSample(
    val distanceMeters: Double,
    val durationSeconds: Double,
    val speedMps: Double
)
