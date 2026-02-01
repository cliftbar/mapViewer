package site.cliftbar.mapviewer.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import site.cliftbar.mapviewer.config.Config
import site.cliftbar.mapviewer.config.ConfigRepository
import site.cliftbar.mapviewer.map.MapLayer
import site.cliftbar.mapviewer.tracks.Track
import site.cliftbar.mapviewer.tracks.TrackRepository
import site.cliftbar.mapviewer.tracks.stats.TrackStatsPrefs

class MapScreenModel(
    initialConfig: Config,
    private val configRepository: ConfigRepository,
    private val trackRepository: TrackRepository
) : BaseScreenModel() {
    private var _config = initialConfig

    /**
     * The list of currently visible tracks on the map.
     */
    val activeTracks = mutableStateListOf<Track>()

    /**
     * A map of track IDs to their stats preferences.
     */
    val trackStatsPrefs = mutableStateMapOf<String, TrackStatsPrefs>()

    private var _selectedTrackId by mutableStateOf<String?>(null)

    /**
     * The ID of the currently selected track, if any.
     */
    val selectedTrackId: String?
        get() = _selectedTrackId

    private var _zoom by mutableStateOf(initialConfig.defaultZoom)

    /**
     * The current zoom level of the map.
     */
    var zoom: Int
        get() = _zoom
        set(value) {
            _zoom = value
            saveState()
        }

    private var _centerOffset by mutableStateOf(Offset.Zero)

    /**
     * The current center offset of the map view.
     */
    var centerOffset: Offset
        get() = _centerOffset
        set(value) {
            _centerOffset = value
            saveState()
        }

    /**
     * Whether the map view has been initialized with the correct size and config.
     */
    var initialized by mutableStateOf(false)

    private var _viewSize by mutableStateOf(IntSize.Zero)

    /**
     * The size of the map view in pixels.
     */
    var viewSize: IntSize
        get() = _viewSize
        set(value) {
            _viewSize = value
            if (value.width > 0 && value.height > 0) {
                saveState()
            }
        }

    /**
     * The list of active map layers (base maps and overlays).
     */
    val activeLayers = mutableStateListOf<MapLayer>().apply {
        // Find base map
        val baseMap = MapLayer.allLayers.find { it.id == initialConfig.activeBaseMapId } ?: MapLayer.OpenStreetMap
        add(baseMap)
        // Add overlays
        initialConfig.activeOverlayIds.forEach { id ->
            MapLayer.allLayers.find { it.id == id }?.let { add(it) }
        }
    }

    init {
        // We will call refreshTracks() explicitly when needed to avoid double refresh or init issues
        screenModelScope.launch(exceptionHandler) {
            // Wait for non-default config if possible, or just update when it arrives
            configRepository.activeConfig.collect { config ->
                _config = config
                if (!initialized) {
                    _zoom = config.defaultZoom
                    // Re-initialize activeLayers from new config if not yet modified by user
                    activeLayers.clear()
                    val baseMap = MapLayer.allLayers.find { it.id == config.activeBaseMapId } ?: MapLayer.OpenStreetMap
                    activeLayers.add(baseMap)
                    config.activeOverlayIds.forEach { id ->
                        MapLayer.allLayers.find { it.id == id }?.let { activeLayers.add(it) }
                    }
                }
            }
        }
    }

    /**
     * Refreshes the active tracks and stats preferences from the repository.
     */
    fun refreshTracks() {
        screenModelScope.launch(exceptionHandler) {
            activeTracks.clear()
            activeTracks.addAll(trackRepository.getVisibleTracks())
            trackStatsPrefs.clear()
            trackStatsPrefs.putAll(trackRepository.getTrackStatsPrefsMap())
            if (_selectedTrackId == null || activeTracks.none { it.id == _selectedTrackId }) {
                _selectedTrackId = activeTracks.firstOrNull()?.id
            }
        }
    }

    /**
     * Updates the currently selected track.
     * 
     * @param trackId The ID of the track to select, or null to deselect.
     */
    fun updateSelectedTrack(trackId: String?) {
        _selectedTrackId = trackId
    }

    /**
     * Updates the track stats preferences for a specific track.
     * 
     * @param prefs The new preferences.
     */
    fun updateTrackStatsPrefs(prefs: TrackStatsPrefs) {
        screenModelScope.launch(exceptionHandler) {
            trackRepository.saveTrackStatsPrefs(prefs)
            trackStatsPrefs[prefs.trackId] = prefs
        }
    }

    /**
     * Triggers a state save after updating active layers.
     */
    fun updateActiveLayers() {
        saveState()
    }

    private var saveJob: Job? = null

    /**
     * Persists the current map state (zoom, center, layers) to the configuration.
     */
    private fun saveState() {
        if (viewSize.width <= 0 || viewSize.height <= 0) return

        saveJob?.cancel()
        saveJob = screenModelScope.launch(exceptionHandler) {
            delay(500) // Debounce 500ms
            val baseMapId = activeLayers.find { !it.isOverlay }?.id ?: "osm"
            val overlayIds = activeLayers.filter { it.isOverlay }.map { it.id }

            // Convert current offset to lat/lon for persistence
            val centerX = (viewSize.width / 2f - centerOffset.x) / 256.0
            val centerY = (viewSize.height / 2f - centerOffset.y) / 256.0
            val lat = site.cliftbar.mapviewer.ui.components.tileYToLat(centerY, zoom)
            val lon = site.cliftbar.mapviewer.ui.components.tileXToLon(centerX, zoom)
            
            _config = _config.copy(
                defaultZoom = zoom,
                initialLat = lat,
                initialLon = lon,
                activeBaseMapId = baseMapId,
                activeOverlayIds = overlayIds
            )
            configRepository.saveConfig(_config)
        }
    }
}
