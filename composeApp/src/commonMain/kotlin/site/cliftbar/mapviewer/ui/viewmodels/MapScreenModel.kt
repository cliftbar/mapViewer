package site.cliftbar.mapviewer.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import site.cliftbar.mapviewer.config.Config
import site.cliftbar.mapviewer.config.ConfigRepository
import site.cliftbar.mapviewer.map.MapLayer
import site.cliftbar.mapviewer.tracks.Track
import site.cliftbar.mapviewer.tracks.TrackRepository

class MapScreenModel(
    initialConfig: Config,
    private val configRepository: ConfigRepository,
    private val trackRepository: TrackRepository
) : ScreenModel {
    private var _config = initialConfig

    val activeTracks = mutableStateListOf<Track>()

    init {
        // We will call refreshTracks() explicitly when needed to avoid double refresh or init issues
    }

    fun refreshTracks() {
        screenModelScope.launch {
            activeTracks.clear()
            activeTracks.addAll(trackRepository.getVisibleTracks())
        }
    }

    private var _zoom by mutableStateOf(initialConfig.defaultZoom)
    var zoom: Int
        get() = _zoom
        set(value) {
            _zoom = value
            saveState()
        }

    private var _centerOffset by mutableStateOf(Offset.Zero)
    var centerOffset: Offset
        get() = _centerOffset
        set(value) {
            _centerOffset = value
            saveState()
        }
    var initialized by mutableStateOf(false)
    private var _viewSize by mutableStateOf(IntSize.Zero)
    var viewSize: IntSize
        get() = _viewSize
        set(value) {
            _viewSize = value
            if (value.width > 0 && value.height > 0) {
                saveState()
            }
        }
    val activeLayers = mutableStateListOf<MapLayer>().apply {
        // Find base map
        val baseMap = MapLayer.allLayers.find { it.id == initialConfig.activeBaseMapId } ?: MapLayer.OpenStreetMap
        add(baseMap)
        // Add overlays
        initialConfig.activeOverlayIds.forEach { id ->
            MapLayer.allLayers.find { it.id == id }?.let { add(it) }
        }
    }

    fun updateActiveLayers() {
        saveState()
    }

    private fun saveState() {
        if (viewSize.width <= 0 || viewSize.height <= 0) return

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
