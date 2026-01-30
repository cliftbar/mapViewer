package site.cliftbar.mapviewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clipToBounds
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import site.cliftbar.mapviewer.LocalBottomPanelContent
import site.cliftbar.mapviewer.map.MapLayer
import site.cliftbar.mapviewer.map.TileProvider
import site.cliftbar.mapviewer.network.httpClient
import site.cliftbar.mapviewer.ui.components.MapView
import site.cliftbar.mapviewer.ui.components.TrackStatsPanel
import site.cliftbar.mapviewer.ui.components.calculateZoomedCenterOffset
import site.cliftbar.mapviewer.ui.components.latLonToTileX
import site.cliftbar.mapviewer.ui.components.latLonToTileY
import site.cliftbar.mapviewer.ui.viewmodels.MapScreenModel
import site.cliftbar.mapviewer.tracks.stats.TrackStatsContext
import site.cliftbar.mapviewer.tracks.Track

class MapScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 0u,
                title = "Map"
            )
        }

    @Composable
    override fun Content() {
        val configRepository = site.cliftbar.mapviewer.LocalConfigRepository.current
        val trackRepository = site.cliftbar.mapviewer.LocalTrackRepository.current
        val bottomPanelContent = LocalBottomPanelContent.current
        val config by configRepository.activeConfig.collectAsState()
        val screenModel = rememberScreenModel { MapScreenModel(config, configRepository, trackRepository) }
        val tileProvider = remember { TileProvider(httpClient) }
        var showLayerMenu by remember { mutableStateOf(false) }
        var showTrackMenu by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            screenModel.refreshTracks()
        }

        val maxZoom = 19
        fun updateZoomAt(newZoom: Int, focus: Offset) {
            if (screenModel.viewSize.width <= 0 || screenModel.viewSize.height <= 0) return
            val clampedZoom = newZoom.coerceIn(0, maxZoom)
            if (clampedZoom == screenModel.zoom) return

            val previousZoom = screenModel.zoom
            val newCenterOffset = calculateZoomedCenterOffset(
                currentZoom = previousZoom,
                newZoom = clampedZoom,
                focus = focus,
                centerOffset = screenModel.centerOffset
            )
            screenModel.zoom = clampedZoom
            screenModel.centerOffset = newCenterOffset
        }
        fun trackCenterLatLon(track: Track): Pair<Double, Double>? {
            val points = track.segments.asSequence().flatMap { it.points.asSequence() }.toList()
            if (points.isEmpty()) return null
            val minLat = points.minOf { it.latitude }
            val maxLat = points.maxOf { it.latitude }
            val minLon = points.minOf { it.longitude }
            val maxLon = points.maxOf { it.longitude }
            return (minLat + maxLat) / 2.0 to (minLon + maxLon) / 2.0
        }

        DisposableEffect(Unit) {
            onDispose { bottomPanelContent.value = null }
        }

        val selectedTrack = screenModel.selectedTrackId?.let { trackId ->
            screenModel.activeTracks.firstOrNull { it.id == trackId }
        }

        SideEffect {
            bottomPanelContent.value = {
                MapBottomPanelContent(
                    selectedTrack = selectedTrack,
                    tracks = screenModel.activeTracks,
                    config = config,
                    showTrackMenu = showTrackMenu,
                    onShowTrackMenuChange = { showTrackMenu = it },
                    onSelectTrack = { trackId -> screenModel.updateSelectedTrack(trackId) },
                    onJumpToTrack = { track ->
                        val viewSize = screenModel.viewSize
                        if (viewSize.width <= 0 || viewSize.height <= 0) return@MapBottomPanelContent
                        val center = trackCenterLatLon(track) ?: return@MapBottomPanelContent
                        val tileSize = 256
                        val tileX = latLonToTileX(center.second, screenModel.zoom)
                        val tileY = latLonToTileY(center.first, screenModel.zoom)
                        screenModel.centerOffset = Offset(
                            (viewSize.width / 2f) - (tileX * tileSize).toFloat(),
                            (viewSize.height / 2f) - (tileY * tileSize).toFloat()
                        )
                    },
                    trackStatsPrefs = screenModel.trackStatsPrefs,
                    onUpdatePrefs = { updated -> screenModel.updateTrackStatsPrefs(updated) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
            MapView(
                tileProvider = tileProvider,
                zoom = screenModel.zoom,
                centerOffset = screenModel.centerOffset,
                onCenterOffsetChange = { screenModel.centerOffset = it },
                initialized = screenModel.initialized,
                onInitializedChange = { screenModel.initialized = it },
                viewSize = screenModel.viewSize,
                onViewSizeChange = { screenModel.viewSize = it },
                onZoomRequest = { delta, focus -> updateZoomAt(screenModel.zoom + delta, focus) },
                activeLayers = screenModel.activeLayers,
                activeTracks = screenModel.activeTracks,
                initialLat = config.initialLat,
                initialLon = config.initialLon
            )

            Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                Column(
                    modifier = Modifier.widthIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                            FilledTonalButton(
                                onClick = { showLayerMenu = true }
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = "Layers")
                                Text("Layers", modifier = Modifier.padding(start = 8.dp))
                            }

                            DropdownMenu(
                                expanded = showLayerMenu,
                                onDismissRequest = { showLayerMenu = false }
                            ) {
                                val layers = listOf(
                                    MapLayer.OpenStreetMap,
                                    MapLayer.OpenCycleMap,
                                    MapLayer.OpenSnowMap,
                                    MapLayer.WaymarkedTrailsSki
                                )
                                val baseLayers = layers.filter { !it.isOverlay }
                                val overlayLayers = layers.filter { it.isOverlay }

                                Text(
                                    "Base Maps",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                baseLayers.forEach { layer ->
                                    DropdownMenuItem(
                                        text = { Text(layer.name) },
                                        onClick = {
                                            if (screenModel.activeLayers.isNotEmpty() && !screenModel.activeLayers[0].isOverlay) {
                                                screenModel.activeLayers[0] = layer
                                            } else {
                                                screenModel.activeLayers.add(0, layer)
                                            }
                                            screenModel.updateActiveLayers()
                                            showLayerMenu = false
                                        },
                                        trailingIcon = {
                                            if (screenModel.activeLayers.contains(layer)) {
                                                Icon(Icons.Default.Layers, contentDescription = "Selected")
                                            }
                                        }
                                    )
                                }

                                HorizontalDivider()

                                Text(
                                    "Overlays",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                overlayLayers.forEach { layer ->
                                    DropdownMenuItem(
                                        text = { Text(layer.name) },
                                        onClick = {
                                            if (screenModel.activeLayers.contains(layer)) {
                                                screenModel.activeLayers.remove(layer)
                                            } else {
                                                screenModel.activeLayers.add(layer)
                                            }
                                            screenModel.updateActiveLayers()
                                            showLayerMenu = false
                                        },
                                        trailingIcon = {
                                            if (screenModel.activeLayers.contains(layer)) {
                                                Icon(Icons.Default.Layers, contentDescription = "Selected")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.widthIn(min = 44.dp, max = 64.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    val focus = Offset(
                                        screenModel.viewSize.width / 2f,
                                        screenModel.viewSize.height / 2f
                                    )
                                    updateZoomAt(screenModel.zoom + 1, focus)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("+", style = MaterialTheme.typography.titleLarge)
                            }
                            FilledTonalButton(
                                onClick = {
                                    val focus = Offset(
                                        screenModel.viewSize.width / 2f,
                                        screenModel.viewSize.height / 2f
                                    )
                                    updateZoomAt(screenModel.zoom - 1, focus)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            ) {
                                Text("-", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapBottomPanelContent(
    selectedTrack: Track?,
    tracks: List<Track>,
    config: site.cliftbar.mapviewer.config.Config,
    showTrackMenu: Boolean,
    onShowTrackMenuChange: (Boolean) -> Unit,
    onSelectTrack: (String?) -> Unit,
    onJumpToTrack: (Track) -> Unit,
    trackStatsPrefs: Map<String, site.cliftbar.mapviewer.tracks.stats.TrackStatsPrefs>,
    onUpdatePrefs: (site.cliftbar.mapviewer.tracks.stats.TrackStatsPrefs) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                FilledTonalButton(
                    onClick = { onShowTrackMenuChange(true) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        selectedTrack?.name ?: "Select track",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = showTrackMenu,
                    onDismissRequest = { onShowTrackMenuChange(false) }
                ) {
                    tracks.forEach { track ->
                        DropdownMenuItem(
                            text = { Text(track.name) },
                            onClick = {
                                onSelectTrack(track.id)
                                onShowTrackMenuChange(false)
                            }
                        )
                    }
                }
            }

            val canJumpToTrack = selectedTrack?.segments?.any { it.points.isNotEmpty() } == true
            FilledTonalButton(
                onClick = {
                    val track = selectedTrack ?: return@FilledTonalButton
                    onJumpToTrack(track)
                },
                enabled = canJumpToTrack
            ) {
                Text("Jump")
            }
        }

        selectedTrack?.let { track ->
            val prefs = trackStatsPrefs[track.id]
            val context = TrackStatsContext.fromConfig(config, prefs)
            TrackStatsPanel(
                track = track,
                context = context,
                prefs = prefs,
                onUpdatePrefs = onUpdatePrefs,
                showHeader = false,
                allowOverrides = false,
                alignEnd = false,
                compact = true,
                fillWidth = true,
                columns = 2,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }
    }
}
