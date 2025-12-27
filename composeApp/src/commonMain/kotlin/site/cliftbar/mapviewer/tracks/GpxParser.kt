package site.cliftbar.mapviewer.tracks

import kotlinx.serialization.*

@Serializable
data class GpxData(
    @SerialName("version")
    val version: String = "1.1",
    @SerialName("creator")
    val creator: String = "MapViewer",
    @SerialName("trk")
    val tracks: List<GpxTrack> = emptyList()
)

@Serializable
data class GpxTrack(
    @SerialName("name")
    val name: String? = null,
    @SerialName("trkseg")
    val segments: List<GpxSegment> = emptyList()
)

@Serializable
data class GpxSegment(
    @SerialName("trkpt")
    val points: List<GpxPoint> = emptyList()
)

@Serializable
data class GpxPoint(
    @SerialName("lat")
    val lat: Double,
    @SerialName("lon")
    val lon: Double,
    @SerialName("ele")
    val ele: Double? = null,
    @SerialName("time")
    val time: String? = null
)

@OptIn(kotlin.ExperimentalStdlibApi::class, kotlin.time.ExperimentalTime::class)
object GpxParser {
    fun parse(content: String): Track? {
        // Simple manual parsing since XML library is having issues
        // This is a placeholder for a more robust manual parser or another library
        return null 
    }

    @OptIn(kotlin.ExperimentalStdlibApi::class, kotlin.time.ExperimentalTime::class)
    fun serialize(track: Track): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"MapViewer\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        sb.append("  <trk>\n")
        sb.append("    <name>${track.name}</name>\n")
        track.segments.forEach { segment ->
            sb.append("    <trkseg>\n")
            segment.points.forEach { point ->
                sb.append("      <trkpt lat=\"${point.latitude}\" lon=\"${point.longitude}\">\n")
                point.elevation?.let { sb.append("        <ele>$it</ele>\n") }
                point.time?.let { 
                    // Manual format if Instant is missing at runtime
                    sb.append("        <time>$it</time>\n")
                }
                sb.append("      </trkpt>\n")
            }
            sb.append("    </trkseg>\n")
        }
        sb.append("  </trk>\n")
        sb.append("</gpx>")
        return sb.toString()
    }
}
