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
import site.cliftbar.mapviewer.tracks.stats.AvgSpeedBasis
import site.cliftbar.mapviewer.tracks.stats.DistanceUnit
import site.cliftbar.mapviewer.tracks.stats.SpeedUnit
import site.cliftbar.mapviewer.tracks.stats.StoppedTimeAlgorithmId
import site.cliftbar.mapviewer.tracks.stats.TrackStatsPrefs

/**
 * Repository for managing tracks, folders, and track-specific statistics preferences.
 * 
 * @property database The [MapViewerDB] instance for persistence.
 */
class TrackRepository(private val database: MapViewerDB) {
    private val queries = database.`1Queries`
    private val statsQueries = database.track_stats_prefsQueries

    /**
     * Retrieves all tracks from the database, including their points and segments.
     * 
     * @return A list of [Track] objects.
     */
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

    /**
     * Retrieves only visible tracks from the database.
     * 
     * @return A list of visible [Track] objects.
     */
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

    /**
     * Saves a track and its points to the database.
     * 
     * @param track The [Track] object to save.
     * @return The ID of the saved track.
     */
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

    /**
     * Updates the visibility of a track.
     * 
     * @param id The ID of the track.
     * @param visible Whether the track should be visible.
     */
    suspend fun updateTrackVisibility(id: String, visible: Boolean) = withContext(Dispatchers.Default) {
        queries.updateTrackVisibility(if (visible) 1L else 0L, id)
    }

    /**
     * Updates the visual style of a track.
     * 
     * @param id The ID of the track.
     * @param color The new color hex string.
     * @param lineStyle The new [LineStyle].
     */
    suspend fun updateTrackStyle(id: String, color: String, lineStyle: LineStyle) = withContext(Dispatchers.Default) {
        queries.updateTrackStyle(color, lineStyle.name, id)
    }

    /**
     * Deletes a track and its associated points from the database.
     * 
     * @param id The ID of the track to delete.
     */
    suspend fun deleteTrack(id: String) = withContext(Dispatchers.Default) {
        database.transaction {
            queries.deleteAllPoints(id)
            queries.deleteTrack(id)
        }
    }

    /**
     * Imports a track from content string in the specified format.
     * 
     * @param content The track data content.
     * @param format The format of the content (e.g., "gpx", "geojson").
     * @return A list of imported [Track] objects.
     */
    suspend fun importTrack(content: String, format: String): List<Track> {
        try {
            val result = withContext(Dispatchers.Default) {
                when (format.lowercase()) {
                    "gpx" -> GpxParser.parse(content)
                    "geojson" -> GeoJsonParser.parse(content)
                    else -> ParserResult.Error("Unsupported format: $format")
                }
            }
            
            return when (result) {
                is ParserResult.Success -> {
                    result.tracks.map { track ->
                        val id = saveTrack(track)
                        track.copy(id = id)
                    }
                }
                is ParserResult.Error -> {
                    println("[DEBUG_LOG] importTrack failed: ${result.message}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            println("[DEBUG_LOG] importTrack unexpected error: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }

    /**
     * Exports a track to a string in the specified format.
     * 
     * @param track The [Track] object to export.
     * @param format The destination format (e.g., "gpx", "geojson").
     * @return The exported content string, or null if export failed.
     */
    suspend fun exportTrack(track: Track, format: String): String? {
        return when (format.lowercase()) {
            "gpx" -> GpxParser.serialize(track)
            "geojson" -> GeoJsonParser.serialize(track)
            else -> null
        }
    }

    // --- Folder Operations ---

    /**
     * Creates a new folder.
     * 
     * @param name The name of the folder.
     * @param parentId The ID of the parent folder, if any.
     * @return The ID of the newly created folder.
     */
    suspend fun createFolder(name: String, parentId: String?): String = withContext(Dispatchers.Default) {
        val id = Random.nextLong().toString()
        queries.insertFolder(id, name, parentId)
        id
    }

    /**
     * Deletes a folder.
     * 
     * @param id The ID of the folder to delete.
     */
    suspend fun deleteFolder(id: String) = withContext(Dispatchers.Default) {
        queries.deleteFolder(id)
    }

    /**
     * Updates the name of a folder.
     * 
     * @param id The ID of the folder.
     * @param name The new name.
     */
    suspend fun updateFolderName(id: String, name: String) = withContext(Dispatchers.Default) {
        queries.updateFolderName(name, id)
    }

    /**
     * Updates the parent folder of a folder.
     * 
     * @param id The ID of the folder to move.
     * @param parentId The ID of the new parent folder, or null.
     */
    suspend fun updateFolderParent(id: String, parentId: String?) = withContext(Dispatchers.Default) {
        queries.updateFolderParent(parentId, id)
    }

    /**
     * Adds tracks to a folder.
     * 
     * @param trackIds The list of track IDs to add.
     * @param folderId The ID of the destination folder.
     */
    suspend fun addTracksToFolder(trackIds: List<String>, folderId: String) = withContext(Dispatchers.Default) {
        database.transaction {
            trackIds.forEach { trackId ->
                queries.addTrackToFolder(trackId, folderId)
            }
        }
    }

    /**
     * Removes tracks from a folder.
     * 
     * @param trackIds The list of track IDs to remove.
     * @param folderId The ID of the folder.
     */
    suspend fun removeTracksFromFolder(trackIds: List<String>, folderId: String) = withContext(Dispatchers.Default) {
        database.transaction {
            trackIds.forEach { trackId ->
                queries.removeTrackFromFolder(trackId, folderId)
            }
        }
    }

    /**
     * Retrieves the complete folder hierarchy.
     * 
     * @return A list of root [Folder] objects, each containing its subfolders and tracks.
     */
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

    /**
     * Retrieves all folders that contain the specified track.
     * 
     * @param trackId The ID of the track.
     * @return A list of [Folder] objects.
     */
    suspend fun getFoldersForTrack(trackId: String): List<Folder> = withContext(Dispatchers.Default) {
        queries.getFoldersForTrack(trackId).awaitAsList().map { entity ->
            Folder(
                id = entity.id,
                name = entity.name,
                parentId = entity.parent_id
            )
        }
    }

    // --- Track Stats Preferences ---

    /**
     * Retrieves the statistics preferences for a specific track.
     * 
     * @param trackId The ID of the track.
     * @return The [TrackStatsPrefs] or null if not set.
     */
    suspend fun getTrackStatsPrefs(trackId: String): TrackStatsPrefs? = withContext(Dispatchers.Default) {
        statsQueries.getTrackStatsPrefs(trackId).awaitAsOneOrNull()?.toTrackStatsPrefs()
    }

    /**
     * Retrieves all track statistics preferences.
     * 
     * @return A map of track IDs to [TrackStatsPrefs].
     */
    suspend fun getTrackStatsPrefsMap(): Map<String, TrackStatsPrefs> = withContext(Dispatchers.Default) {
        statsQueries.getAllTrackStatsPrefs().awaitAsList()
            .mapNotNull { prefs -> prefs.toTrackStatsPrefs()?.let { prefs.track_id to it } }
            .toMap()
    }

    /**
     * Saves or updates track statistics preferences.
     * 
     * @param prefs The [TrackStatsPrefs] to save.
     */
    suspend fun saveTrackStatsPrefs(prefs: TrackStatsPrefs) = withContext(Dispatchers.Default) {
        statsQueries.upsertTrackStatsPrefs(
            track_id = prefs.trackId,
            avg_speed_basis = prefs.avgSpeedBasis?.name,
            stopped_algo_id = prefs.stoppedAlgorithmId?.name,
            stopped_speed_threshold_mps = prefs.stoppedSpeedThresholdMps,
            stopped_min_stop_seconds = prefs.stoppedMinStopSeconds?.toLong(),
            distance_unit = prefs.distanceUnit?.name,
            speed_unit = prefs.speedUnit?.name
        )
    }

    private fun site.cliftbar.mapviewer.Track_stats_prefs.toTrackStatsPrefs(): TrackStatsPrefs? {
        return TrackStatsPrefs(
            trackId = track_id,
            avgSpeedBasis = avg_speed_basis.toEnumOrNull { AvgSpeedBasis.valueOf(it) },
            stoppedAlgorithmId = stopped_algo_id.toEnumOrNull { StoppedTimeAlgorithmId.valueOf(it) },
            stoppedSpeedThresholdMps = stopped_speed_threshold_mps,
            stoppedMinStopSeconds = stopped_min_stop_seconds?.toInt(),
            distanceUnit = distance_unit.toEnumOrNull { DistanceUnit.valueOf(it) },
            speedUnit = speed_unit.toEnumOrNull { SpeedUnit.valueOf(it) }
        )
    }

    private inline fun <T> String?.toEnumOrNull(parser: (String) -> T): T? {
        return this?.let { value ->
            runCatching { parser(value) }.getOrNull()
        }
    }
}
