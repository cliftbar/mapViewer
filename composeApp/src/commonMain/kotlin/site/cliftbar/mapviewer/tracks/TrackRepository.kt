package site.cliftbar.mapviewer.tracks

import site.cliftbar.mapviewer.MapViewerDB
import site.cliftbar.mapviewer.Track_points
import site.cliftbar.mapviewer.Tracks
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class TrackRepository(private val database: MapViewerDB) {
    private val queries = database.`1Queries`

    suspend fun getAllTracks(): List<Track> = withContext(Dispatchers.Default) {
        val trackEntities = queries.getAllTracks().awaitAsList()
        trackEntities.map { entity ->
            val points = queries.getTrackPoints(entity.id).awaitAsList()
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
        val trackEntities = queries.getVisibleTracks().awaitAsList()
        trackEntities.map { entity ->
            val points = queries.getTrackPoints(entity.id).awaitAsList()
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
        database.transactionWithResult {
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
            id
        }
    }

    suspend fun updateTrackVisibility(id: String, visible: Boolean) = withContext(Dispatchers.Default) {
        queries.updateTrackVisibility(if (visible) 1L else 0L, id)
    }

    suspend fun updateTrackStyle(id: String, color: String, lineStyle: LineStyle) = withContext(Dispatchers.Default) {
        queries.updateTrackStyle(color, lineStyle.name, id)
    }

    suspend fun deleteTrack(id: String) = withContext(Dispatchers.Default) {
        database.transaction {
            queries.deleteAllPoints(id)
            queries.deleteTrack(id)
        }
    }

    suspend fun importTrack(content: String, format: String): List<Track> {
        try {
            val tracks = withContext(Dispatchers.Default) {
                when (format.lowercase()) {
                    "gpx" -> GpxParser.parse(content)
                    "geojson" -> GeoJsonParser.parse(content)
                    else -> emptyList()
                }
            }
            
            return tracks.map { track ->
                val id = saveTrack(track)
                track.copy(id = id)
            }
        } catch (e: Exception) {
            println("[DEBUG_LOG] importTrack failed: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }

    suspend fun exportTrack(track: Track, format: String): String? {
        return when (format.lowercase()) {
            "gpx" -> GpxParser.serialize(track)
            "geojson" -> GeoJsonParser.serialize(track)
            else -> null
        }
    }

    // --- Folder Operations ---

    suspend fun createFolder(name: String, parentId: String?): String = withContext(Dispatchers.Default) {
        val id = Random.nextLong().toString()
        queries.insertFolder(id, name, parentId)
        id
    }

    suspend fun deleteFolder(id: String) = withContext(Dispatchers.Default) {
        queries.deleteFolder(id)
    }

    suspend fun updateFolderName(id: String, name: String) = withContext(Dispatchers.Default) {
        queries.updateFolderName(name, id)
    }

    suspend fun updateFolderParent(id: String, parentId: String?) = withContext(Dispatchers.Default) {
        queries.updateFolderParent(parentId, id)
    }

    suspend fun addTracksToFolder(trackIds: List<String>, folderId: String) = withContext(Dispatchers.Default) {
        database.transaction {
            trackIds.forEach { trackId ->
                queries.addTrackToFolder(trackId, folderId)
            }
        }
    }

    suspend fun removeTracksFromFolder(trackIds: List<String>, folderId: String) = withContext(Dispatchers.Default) {
        database.transaction {
            trackIds.forEach { trackId ->
                queries.removeTrackFromFolder(trackId, folderId)
            }
        }
    }

    suspend fun getFolderHierarchy(): List<Folder> = withContext(Dispatchers.Default) {
        val allFolders = queries.getAllFolders().awaitAsList()
        val allTracksInFolders = allFolders.associate { folder ->
            folder.id to queries.getTracksInFolder(folder.id).awaitAsList().map { it.id }
        }

        fun buildTree(parentId: String?): List<Folder> {
            return allFolders.filter { it.parent_id == parentId }.map { entity ->
                Folder(
                    id = entity.id,
                    name = entity.name,
                    parentId = entity.parent_id,
                    subFolders = buildTree(entity.id),
                    trackIds = allTracksInFolders[entity.id] ?: emptyList()
                )
            }
        }

        buildTree(null)
    }

    suspend fun getFoldersForTrack(trackId: String): List<Folder> = withContext(Dispatchers.Default) {
        queries.getFoldersForTrack(trackId).awaitAsList().map { entity ->
            Folder(
                id = entity.id,
                name = entity.name,
                parentId = entity.parent_id
            )
        }
    }
}
