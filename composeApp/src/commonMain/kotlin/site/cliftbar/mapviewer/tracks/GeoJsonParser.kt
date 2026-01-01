package site.cliftbar.mapviewer.tracks

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class GeoJson(
    val type: String,
    val features: List<GeoJsonFeature> = emptyList()
)

@Serializable
data class GeoJsonFeature(
    val type: String,
    val geometry: GeoJsonGeometry,
    val properties: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class GeoJsonGeometry(
    val type: String,
    val coordinates: JsonArray
)

object GeoJsonParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(content: String): List<Track> {
        return try {
            val geoJson = json.decodeFromString<GeoJson>(content)
            val trackFeatures = geoJson.features.filter { it.geometry.type == "LineString" || it.geometry.type == "MultiLineString" }
            
            if (trackFeatures.isEmpty()) return emptyList()
            
            trackFeatures.map { feature ->
                val name = feature.properties["name"]?.jsonPrimitive?.content ?: "Imported GeoJSON"
                val segments = mutableListOf<TrackSegment>()
                
                if (feature.geometry.type == "LineString") {
                    segments.add(parseLineString(feature.geometry.coordinates))
                } else if (feature.geometry.type == "MultiLineString") {
                    feature.geometry.coordinates.forEach { 
                        segments.add(parseLineString(it.jsonArray))
                    }
                }
                
                Track(
                    id = "",
                    name = name,
                    segments = segments
                )
            }
        } catch (e: Throwable) {
            println("[DEBUG_LOG] GeoJSON Parse Error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseLineString(coordinates: JsonArray): TrackSegment {
        val points = coordinates.map { coord ->
            val arr = coord.jsonArray
            TrackPoint(
                longitude = arr[0].jsonPrimitive.double,
                latitude = arr[1].jsonPrimitive.double,
                elevation = if (arr.size > 2) arr[2].jsonPrimitive.double else null
            )
        }
        return TrackSegment(points)
    }

    fun serialize(track: Track): String {
        val coordinates = if (track.segments.size == 1) {
            JsonArray(track.segments[0].points.map { point ->
                buildJsonArray {
                    add(point.longitude)
                    add(point.latitude)
                    point.elevation?.let { add(it) }
                }
            })
        } else {
            JsonArray(track.segments.map { segment ->
                JsonArray(segment.points.map { point ->
                    buildJsonArray {
                        add(point.longitude)
                        add(point.latitude)
                        point.elevation?.let { add(it) }
                    }
                })
            })
        }

        val type = if (track.segments.size == 1) "LineString" else "MultiLineString"

        val geoJson = GeoJson(
            type = "FeatureCollection",
            features = listOf(
                GeoJsonFeature(
                    type = "Feature",
                    geometry = GeoJsonGeometry(
                        type = type,
                        coordinates = coordinates
                    ),
                    properties = buildJsonObject {
                        put("name", track.name)
                    }
                )
            )
        )
        return json.encodeToString(geoJson)
    }
}
