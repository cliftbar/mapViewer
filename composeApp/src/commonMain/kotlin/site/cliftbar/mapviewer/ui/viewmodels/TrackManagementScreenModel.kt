package site.cliftbar.mapviewer.ui.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import site.cliftbar.mapviewer.tracks.Folder
import site.cliftbar.mapviewer.tracks.LineStyle
import site.cliftbar.mapviewer.tracks.Track
import site.cliftbar.mapviewer.tracks.TrackRepository
import site.cliftbar.mapviewer.tracks.stats.TrackStatsPrefs

class TrackManagementScreenModel(
    private val trackRepository: TrackRepository
) : BaseScreenModel() {
    /**
     * The list of all tracks.
     */
    val tracks = mutableStateListOf<Track>()

    /**
     * The list of all folders in a hierarchy.
     */
    val folders = mutableStateListOf<Folder>()

    /**
     * A map of selected track IDs.
     */
    val selectedTrackIds = mutableStateMapOf<String, Boolean>()

    /**
     * A map of track IDs to their stats preferences.
     */
    val trackStatsPrefs = mutableStateMapOf<String, TrackStatsPrefs>()

    init {
        // We will call refreshTracks() explicitly when needed, 
        // but for now, we want to ensure it's not called twice or concurrently in a way that causes issues.
    }

    /**
     * Refreshes the list of tracks, folders, and stats preferences from the repository.
     */
    fun refreshTracks() = screenModelScope.launch(exceptionHandler) {
        val allTracks = trackRepository.getAllTracks()
        val folderHierarchy = trackRepository.getFolderHierarchy()
        val statsPrefs = trackRepository.getTrackStatsPrefsMap()
        withContext(Dispatchers.Main) {
            tracks.clear()
            tracks.addAll(allTracks)
            folders.clear()
            folders.addAll(folderHierarchy)
            selectedTrackIds.clear()
            trackStatsPrefs.clear()
            trackStatsPrefs.putAll(statsPrefs)
        }
    }

    /**
     * Creates a new folder.
     * 
     * @param name The name of the folder.
     * @param parentId The ID of the parent folder, if any.
     */
    fun createFolder(name: String, parentId: String?) = screenModelScope.launch(exceptionHandler) {
        trackRepository.createFolder(name, parentId)
        refreshTracks()
    }

    /**
     * Deletes a folder.
     * 
     * @param id The ID of the folder to delete.
     */
    fun deleteFolder(id: String) = screenModelScope.launch(exceptionHandler) {
        trackRepository.deleteFolder(id)
        refreshTracks()
    }

    /**
     * Adds all selected tracks to a folder.
     * 
     * @param folderId The ID of the destination folder.
     */
    fun addSelectedTracksToFolder(folderId: String) = screenModelScope.launch(exceptionHandler) {
        val idsToAdd = selectedTrackIds.keys.toList()
        trackRepository.addTracksToFolder(idsToAdd, folderId)
        refreshTracks()
    }

    /**
     * Removes all selected tracks from a folder.
     * 
     * @param folderId The ID of the folder to remove tracks from.
     */
    fun removeSelectedTracksFromFolder(folderId: String) = screenModelScope.launch(exceptionHandler) {
        val idsToRemove = selectedTrackIds.keys.toList()
        trackRepository.removeTracksFromFolder(idsToRemove, folderId)
        refreshTracks()
    }

    /**
     * Imports a track from the given content and format.
     * 
     * @param content The track data content.
     * @param format The format of the content (e.g., "gpx", "geojson").
     * @return The list of imported tracks.
     */
    suspend fun importTrack(content: String, format: String): List<Track> {
        val importedTracks = try {
            trackRepository.importTrack(content, format)
        } catch (e: Exception) {
            handleError(e)
            emptyList()
        }

        if (importedTracks.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                tracks.addAll(importedTracks)
            }
        }
        return importedTracks
    }

    /**
     * Updates the visibility of a track.
     * 
     * @param id The ID of the track.
     * @param visible Whether the track should be visible.
     */
    fun updateTrackVisibility(id: String, visible: Boolean) = screenModelScope.launch(exceptionHandler) {
        trackRepository.updateTrackVisibility(id, visible)
        withContext(Dispatchers.Main) {
            val index = tracks.indexOfFirst { it.id == id }
            if (index != -1) {
                tracks[index] = tracks[index].copy(visible = visible)
            }
        }
    }

    /**
     * Updates the visual style of a track.
     * 
     * @param id The ID of the track.
     * @param color The new color of the track.
     * @param style The new line style of the track.
     */
    fun updateTrackStyle(id: String, color: String, style: LineStyle) = screenModelScope.launch(exceptionHandler) {
        trackRepository.updateTrackStyle(id, color, style)
        withContext(Dispatchers.Main) {
            val index = tracks.indexOfFirst { it.id == id }
            if (index != -1) {
                tracks[index] = tracks[index].copy(color = color, lineStyle = style)
            }
        }
    }

    /**
     * Deletes a track.
     * 
     * @param id The ID of the track to delete.
     */
    fun deleteTrack(id: String) = screenModelScope.launch(exceptionHandler) {
        trackRepository.deleteTrack(id)
        withContext(Dispatchers.Main) {
            tracks.removeAll { it.id == id }
            selectedTrackIds.remove(id)
        }
    }

    /**
     * Exports a track to the given format.
     * 
     * @param track The track to export.
     * @param format The destination format.
     * @param onResult Callback with the exported content, or null if export failed.
     */
    fun exportTrack(track: Track, format: String, onResult: (String?) -> Unit) {
        screenModelScope.launch(exceptionHandler) {
            val result = trackRepository.exportTrack(track, format)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    /**
     * Toggles the selection state of a track.
     * 
     * @param id The ID of the track.
     */
    fun toggleSelection(id: String) {
        val current = selectedTrackIds[id] ?: false
        if (!current) {
            selectedTrackIds[id] = true
        } else {
            selectedTrackIds.remove(id)
        }
    }

    /**
     * Clears all track selections.
     */
    fun clearSelection() {
        selectedTrackIds.clear()
    }

    /**
     * Selects all visible tracks.
     */
    fun selectAll() {
        tracks.forEach { track ->
            selectedTrackIds[track.id] = true
        }
    }

    /**
     * Deletes all currently selected tracks.
     */
    fun deleteSelectedTracks() = screenModelScope.launch(exceptionHandler) {
        val idsToDelete = selectedTrackIds.keys.toList()
        idsToDelete.forEach { id ->
            trackRepository.deleteTrack(id)
        }
        withContext(Dispatchers.Main) {
            tracks.removeAll { it.id in idsToDelete }
            selectedTrackIds.clear()
        }
    }

    /**
     * Updates the visibility of all selected tracks.
     * 
     * @param visible Whether the tracks should be visible.
     */
    fun updateSelectedTracksVisibility(visible: Boolean) = screenModelScope.launch(exceptionHandler) {
        val idsToUpdate = selectedTrackIds.keys.toList()
        idsToUpdate.forEach { id ->
            trackRepository.updateTrackVisibility(id, visible)
        }
        withContext(Dispatchers.Main) {
            tracks.forEachIndexed { index, track ->
                if (track.id in idsToUpdate) {
                    tracks[index] = track.copy(visible = visible)
                }
            }
        }
    }

    /**
     * Updates the style of all selected tracks.
     * 
     * @param color The new color.
     * @param style The new line style.
     */
    fun updateSelectedTracksStyle(color: String, style: LineStyle) = screenModelScope.launch(exceptionHandler) {
        val idsToUpdate = selectedTrackIds.keys.toList()
        idsToUpdate.forEach { id ->
            trackRepository.updateTrackStyle(id, color, style)
        }
        withContext(Dispatchers.Main) {
            tracks.forEachIndexed { index, track ->
                if (track.id in idsToUpdate) {
                    tracks[index] = track.copy(color = color, lineStyle = style)
                }
            }
        }
    }

    /**
     * Updates the track stats preferences for a specific track.
     * 
     * @param prefs The new preferences.
     */
    fun updateTrackStatsPrefs(prefs: TrackStatsPrefs) = screenModelScope.launch(exceptionHandler) {
        trackRepository.saveTrackStatsPrefs(prefs)
        withContext(Dispatchers.Main) {
            trackStatsPrefs[prefs.trackId] = prefs
        }
    }
}
