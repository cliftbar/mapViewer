package site.cliftbar.mapviewer.ui.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import site.cliftbar.mapviewer.tracks.LineStyle
import site.cliftbar.mapviewer.tracks.Track
import site.cliftbar.mapviewer.tracks.TrackRepository

class TrackManagementScreenModel(
    private val trackRepository: TrackRepository
) : ScreenModel {
    val tracks = mutableStateListOf<Track>()
    val selectedTrackIds = mutableStateMapOf<String, Boolean>()

    init {
        // We will call refreshTracks() explicitly when needed, 
        // but for now, we want to ensure it's not called twice or concurrently in a way that causes issues.
    }

    fun refreshTracks() = screenModelScope.launch {
        val allTracks = trackRepository.getAllTracks()
        withContext(Dispatchers.Main) {
            tracks.clear()
            tracks.addAll(allTracks)
            selectedTrackIds.clear()
        }
    }

    suspend fun importTrack(content: String, format: String): Track? {
        val track = trackRepository.importTrack(content, format)
        if (track != null) {
            withContext(Dispatchers.Main) {
                tracks.add(track)
            }
        }
        return track
    }

    fun updateTrackVisibility(id: String, visible: Boolean) = screenModelScope.launch {
        trackRepository.updateTrackVisibility(id, visible)
        withContext(Dispatchers.Main) {
            val index = tracks.indexOfFirst { it.id == id }
            if (index != -1) {
                tracks[index] = tracks[index].copy(visible = visible)
            }
        }
    }

    fun updateTrackStyle(id: String, color: String, style: LineStyle) = screenModelScope.launch {
        trackRepository.updateTrackStyle(id, color, style)
        withContext(Dispatchers.Main) {
            val index = tracks.indexOfFirst { it.id == id }
            if (index != -1) {
                tracks[index] = tracks[index].copy(color = color, lineStyle = style)
            }
        }
    }

    fun deleteTrack(id: String) = screenModelScope.launch {
        trackRepository.deleteTrack(id)
        withContext(Dispatchers.Main) {
            tracks.removeAll { it.id == id }
            selectedTrackIds.remove(id)
        }
    }

    fun exportTrack(track: Track, format: String): String? {
        return trackRepository.exportTrack(track, format)
    }

    fun toggleSelection(id: String) {
        val current = selectedTrackIds[id] ?: false
        if (!current) {
            selectedTrackIds[id] = true
        } else {
            selectedTrackIds.remove(id)
        }
    }

    fun clearSelection() {
        selectedTrackIds.clear()
    }

    fun selectAll() {
        tracks.forEach { track ->
            selectedTrackIds[track.id] = true
        }
    }

    fun deleteSelectedTracks() = screenModelScope.launch {
        val idsToDelete = selectedTrackIds.keys.toList()
        idsToDelete.forEach { id ->
            trackRepository.deleteTrack(id)
        }
        withContext(Dispatchers.Main) {
            tracks.removeAll { it.id in idsToDelete }
            selectedTrackIds.clear()
        }
    }

    fun updateSelectedTracksVisibility(visible: Boolean) = screenModelScope.launch {
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
}
