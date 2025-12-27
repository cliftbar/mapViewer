package site.cliftbar.mapviewer.tracks

import site.cliftbar.mapviewer.MapViewerDB
import site.cliftbar.mapviewer.Track_points
import site.cliftbar.mapviewer.Tracks
import kotlin.random.Random

class TrackRepository(private val database: MapViewerDB) {
    private val queries = database.`1Queries`

    fun getAllTracks(): List<Track> {
        val trackEntities = queries.getAllTracks().executeAsList()
        return trackEntities.map { entity ->
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
                segments = segments
            )
        }
    }

    fun saveTrack(track: Track): String {
        val id = if (track.id.isBlank()) Random.nextLong().toString() else track.id
        queries.transaction {
            queries.insertTrack(id, track.name)
            track.segments.forEachIndexed { segmentIndex, segment ->
                segment.points.forEach { point ->
                    queries.insertPoint(
                        track_id = id,
                        segment_index = segmentIndex.toLong(),
                        latitude = point.latitude,
                        longitude = point.longitude,
                        elevation = point.elevation,
                        time = point.time
                    )
                }
            }
        }
        return id
    }

    fun deleteTrack(id: String) {
        queries.deleteTrack(id)
    }

    fun importTrack(content: String, format: String): Track? {
        val track = when (format.lowercase()) {
            "gpx" -> GpxParser.parse(content)
            "geojson" -> GeoJsonParser.parse(content)
            else -> null
        }
        track?.let { saveTrack(it) }
        return track
    }

    fun exportTrack(track: Track, format: String): String? {
        return when (format.lowercase()) {
            "gpx" -> GpxParser.serialize(track)
            "geojson" -> GeoJsonParser.serialize(track)
            else -> null
        }
    }
}
