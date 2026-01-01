package site.cliftbar.mapviewer.tracks

import kotlinx.serialization.*
import nl.adaptivity.xmlutil.serialization.*
import nl.adaptivity.xmlutil.XMLConstants
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
        repairNamespaces = true
        autoPolymorphic = true
    }

    fun parse(content: String): List<Track> {
        val sanitizedContent = if (!content.contains("xmlns=\"http://www.topografix.com/GPX/1/1\"") && !content.contains("xmlns:p=\"http://www.topografix.com/GPX/1/1\"")) {
            content.replaceFirst("<gpx", "<gpx xmlns=\"http://www.topografix.com/GPX/1/1\"")
        } else {
            content
        }
        return try {
            val gpxData = xml.decodeFromString<GpxData>(sanitizedContent)
            if (gpxData.tracks.isEmpty()) {
                println("[DEBUG_LOG] GPX Parse Error: No tracks found in GPX data")
                return emptyList()
            }
            
            gpxData.tracks.map { gpxTrack ->
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
            }
        } catch (e: Throwable) {
            println("[DEBUG_LOG] GPX Parse Error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    fun serialize(track: Track): String {
        val gpxData = GpxData(
            tracks = listOf(
                GpxTrack(
                    name = track.name,
                    segments = track.segments.map { segment ->
                        GpxSegment(
                            points = segment.points.map { point ->
                                GpxPoint(
                                    lat = point.latitude,
                                    lon = point.longitude,
                                    ele = point.elevation,
                                    time = point.time?.let { Instant.fromEpochMilliseconds(it).toString() }
                                )
                            }
                        )
                    }
                )
            )
        )
        return xml.encodeToString(gpxData)
    }
}
