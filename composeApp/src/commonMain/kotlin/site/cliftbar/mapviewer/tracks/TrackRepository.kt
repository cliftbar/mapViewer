package site.cliftbar.mapviewer.tracks

import site.cliftbar.mapviewer.MapViewerDB
import site.cliftbar.mapviewer.Track_points
import site.cliftbar.mapviewer.Tracks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class TrackRepository(private val database: MapViewerDB) {
    private val queries = database.`1Queries`

    suspend fun getAllTracks(): List<Track> = withContext(Dispatchers.Default) {
        val trackEntities = queries.getAllTracks().executeAsList()
        trackEntities.map { entity ->
            val points = queries.getTrackPoints(entity.id).executeAsList()
            val segments = points.groupBy { it.segment_index }
                .map { (_, segmentPoints) ->
                    TrackSegment(
                        points = segmentPoints.map { p ->
                            TrackPoint(
                                latitude = p.latitude,
                                longitude = p.longitude,
                                elevation = p.elevation,
                                time = p.time
                            )
                        }
                    )
                }
            Track(
                id = entity.id,
                name = entity.name,
                segments = segments,
                color = entity.color,
                lineStyle = LineStyle.valueOf(entity.line_style),
                visible = entity.visible != 0L
            )
        }
    }

    suspend fun getVisibleTracks(): List<Track> = withContext(Dispatchers.Default) {
        val trackEntities = queries.getVisibleTracks().executeAsList()
        trackEntities.map { entity ->
            val points = queries.getTrackPoints(entity.id).executeAsList()
            val segments = points.groupBy { it.segment_index }
                .map { (_, segmentPoints) ->
                    TrackSegment(
                        points = segmentPoints.map { p ->
                            TrackPoint(
                                latitude = p.latitude,
                                longitude = p.longitude,
                                elevation = p.elevation,
                                time = p.time
                            )
                        }
                    )
                }
            Track(
                id = entity.id,
                name = entity.name,
                segments = segments,
                color = entity.color,
                lineStyle = LineStyle.valueOf(entity.line_style),
                visible = entity.visible != 0L
            )
        }
    }

    suspend fun saveTrack(track: Track): String = withContext(Dispatchers.Default) {
        val id = if (track.id.isBlank()) Random.nextLong().toString() else track.id
        queries.transaction {
            queries.insertTrack(
                id = id,
                name = track.name,
                color = track.color,
                line_style = track.lineStyle.name,
                visible = if (track.visible) 1L else 0L
            )
            queries.deleteAllPoints(id)
            track.segments.forEachIndexed { segmentIndex, segment ->
                segment.points.forEach { point ->
                    try {
                        queries.insertPoint(
                            track_id = id,
                            segment_index = segmentIndex.toLong(),
                            latitude = point.latitude,
                            longitude = point.longitude,
                            elevation = point.elevation,
                            time = point.time
                        )
                    } catch (e: Exception) {
                        println("[DEBUG_LOG] Error inserting point for track $id: ${e.message}")
                    }
                }
            }
        }
        id
    }

    suspend fun updateTrackVisibility(id: String, visible: Boolean) = withContext(Dispatchers.Default) {
        queries.updateTrackVisibility(if (visible) 1L else 0L, id)
    }

    suspend fun updateTrackStyle(id: String, color: String, lineStyle: LineStyle) = withContext(Dispatchers.Default) {
        queries.updateTrackStyle(color, lineStyle.name, id)
    }

    suspend fun deleteTrack(id: String) = withContext(Dispatchers.Default) {
        queries.deleteTrack(id)
    }

    suspend fun importTrack(content: String, format: String): Track? {
        try {
            val track = withContext(Dispatchers.Default) {
                when (format.lowercase()) {
                    "gpx" -> GpxParser.parse(content)
                    "geojson" -> GeoJsonParser.parse(content)
                    else -> null
                }
            } ?: return null
            
            val id = saveTrack(track)
            return track.copy(id = id)
        } catch (e: Exception) {
            println("[DEBUG_LOG] importTrack failed: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    fun exportTrack(track: Track, format: String): String? {
        return when (format.lowercase()) {
            "gpx" -> GpxParser.serialize(track)
            "geojson" -> GeoJsonParser.serialize(track)
            else -> null
        }
    }
}
