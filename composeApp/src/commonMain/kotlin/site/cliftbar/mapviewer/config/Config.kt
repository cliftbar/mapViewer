package site.cliftbar.mapviewer.config

import kotlinx.serialization.Serializable
import site.cliftbar.mapviewer.tracks.stats.AvgSpeedBasis
import site.cliftbar.mapviewer.tracks.stats.DistanceUnit
import site.cliftbar.mapviewer.tracks.stats.SpeedUnit
import site.cliftbar.mapviewer.tracks.stats.StoppedTimeAlgorithmId

@Serializable
enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

@Serializable
data class Config(
    val defaultZoom: Int = 12,
    val initialLat: Double = 45.5152,
    val initialLon: Double = -122.6784,
    val activeBaseMapId: String = "osm",
    val activeOverlayIds: List<String> = emptyList(),
    val offlineMode: Boolean = false,
    val theme: AppTheme = AppTheme.SYSTEM,
    val defaultAvgSpeedBasis: AvgSpeedBasis = AvgSpeedBasis.MOVING_TIME,
    val stoppedTimeAlgorithmId: StoppedTimeAlgorithmId = StoppedTimeAlgorithmId.SPEED_THRESHOLD,
    val stoppedTimeSpeedThresholdMps: Double = 0.1,
    val stoppedTimeMinStopSeconds: Int = 120,
    val defaultDistanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
    val defaultSpeedUnit: SpeedUnit = SpeedUnit.KMH
)
