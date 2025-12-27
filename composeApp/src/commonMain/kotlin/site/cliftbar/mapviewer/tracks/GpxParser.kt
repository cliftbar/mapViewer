package site.cliftbar.mapviewer.tracks

import kotlinx.serialization.*
import nl.adaptivity.xmlutil.serialization.*
import kotlinx.datetime.Instant

@Serializable
@XmlSerialName("gpx", "http://www.topografix.com/GPX/1/1", "")
data class GpxData(
    @SerialName("version")
    val version: String = "1.1",
    @SerialName("creator")
    val creator: String = "MapViewer",
    @SerialName("trk")
    val tracks: List<GpxTrack> = emptyList()
)

@Serializable
@XmlSerialName("trk", "http://www.topografix.com/GPX/1/1", "")
data class GpxTrack(
    @XmlElement(true)
    @SerialName("name")
    val name: String? = null,
    @SerialName("trkseg")
    val segments: List<GpxSegment> = emptyList()
)

@Serializable
@XmlSerialName("trkseg", "http://www.topografix.com/GPX/1/1", "")
data class GpxSegment(
    @SerialName("trkpt")
    val points: List<GpxPoint> = emptyList()
)

@Serializable
@XmlSerialName("trkpt", "http://www.topografix.com/GPX/1/1", "")
data class GpxPoint(
    @SerialName("lat")
    val lat: Double,
    @SerialName("lon")
    val lon: Double,
    @XmlElement(true)
    @SerialName("ele")
    val ele: Double? = null,
    @XmlElement(true)
    @SerialName("time")
    val time: String? = null
)

@OptIn(kotlin.ExperimentalStdlibApi::class, kotlin.time.ExperimentalTime::class)
object GpxParser {
    private val xml = XML {
        defaultPolicy {
            ignoreUnknownChildren()
        }
    }

    fun parse(content: String): Track? {
        return try {
            val gpxData = xml.decodeFromString<GpxData>(content)
            val gpxTrack = gpxData.tracks.firstOrNull() ?: return null
            
            val segments = gpxTrack.segments.map { segment ->
                TrackSegment(
                    points = segment.points.map { point ->
                        TrackPoint(
                            latitude = point.lat,
                            longitude = point.lon,
                            elevation = point.ele,
                            time = point.time?.let { 
                                // Basic fallback parsing if Instant fails at runtime
                                try {
                                    Instant.parse(it).toEpochMilliseconds()
                                } catch (e: Throwable) {
                                    null
                                }
                            }
                        )
                    }
                )
            }
            
            Track(
                id = "",
                name = gpxTrack.name ?: "Imported GPX",
                segments = segments
            )
        } catch (e: Exception) {
            println("[DEBUG_LOG] GPX Parse Error: ${e.message}")
            e.printStackTrace()
            null
        }
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
