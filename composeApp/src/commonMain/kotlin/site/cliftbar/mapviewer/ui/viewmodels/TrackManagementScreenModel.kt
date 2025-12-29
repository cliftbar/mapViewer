package site.cliftbar.mapviewer.ui.viewmodels

import androidx.compose.runtime.mutableStateListOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import site.cliftbar.mapviewer.tracks.LineStyle
import site.cliftbar.mapviewer.tracks.Track
import site.cliftbar.mapviewer.tracks.TrackRepository

class TrackManagementScreenModel(
    private val trackRepository: TrackRepository
) : ScreenModel {
    val tracks = mutableStateListOf<Track>()

    init {
        refreshTracks()
    }

    fun refreshTracks() {
        screenModelScope.launch(Dispatchers.Main) {
            tracks.clear()
            tracks.addAll(trackRepository.getAllTracks())
        }
    }

    suspend fun importTrack(content: String, format: String): Track? {
        val track = trackRepository.importTrack(content, format)
        if (track != null) {
            tracks.add(track)
        }
        return track
    }

    fun updateTrackVisibility(id: String, visible: Boolean) {
        screenModelScope.launch {
            trackRepository.updateTrackVisibility(id, visible)
            val index = tracks.indexOfFirst { it.id == id }
            if (index != -1) {
                tracks[index] = tracks[index].copy(visible = visible)
            }
        }
    }

    fun updateTrackStyle(id: String, color: String, style: LineStyle) {
        screenModelScope.launch {
            trackRepository.updateTrackStyle(id, color, style)
            val index = tracks.indexOfFirst { it.id == id }
            if (index != -1) {
                tracks[index] = tracks[index].copy(color = color, lineStyle = style)
            }
        }
    }

    fun deleteTrack(id: String) {
        screenModelScope.launch {
            trackRepository.deleteTrack(id)
            tracks.removeAll { it.id == id }
        }
    }

    fun exportTrack(track: Track, format: String): String? {
        return trackRepository.exportTrack(track, format)
    }
}
