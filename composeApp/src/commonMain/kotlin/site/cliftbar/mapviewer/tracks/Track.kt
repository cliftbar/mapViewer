package site.cliftbar.mapviewer.tracks

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
enum class LineStyle {
    SOLID, DASHED, DOTTED
}

@Serializable
data class Track(
    val id: String,
    val name: String,
    val segments: List<TrackSegment> = emptyList(),
    val color: String = "#0000FF",
    val lineStyle: LineStyle = LineStyle.SOLID,
    val visible: Boolean = true
)

@Serializable
data class TrackSegment(
    val points: List<TrackPoint> = emptyList()
)

@Serializable
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double? = null,
    val time: Long? = null // Timestamp in milliseconds
)
