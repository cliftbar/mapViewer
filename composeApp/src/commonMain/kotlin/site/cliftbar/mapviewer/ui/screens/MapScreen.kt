package site.cliftbar.mapviewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
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

        Box(modifier = Modifier.fillMaxSize()) {
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

            val selectedTrack = screenModel.selectedTrackId?.let { trackId ->
                screenModel.activeTracks.firstOrNull { it.id == trackId }
            }
            Box(modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp)) {
                Column(
                    modifier = Modifier.widthIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .align(Alignment.End),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 2.dp,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box {
                                FilledTonalButton(
                                    onClick = { showTrackMenu = true },
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
                                    onDismissRequest = { showTrackMenu = false }
                                ) {
                                    screenModel.activeTracks.forEach { track ->
                                        DropdownMenuItem(
                                            text = { Text(track.name) },
                                            onClick = {
                                                screenModel.updateSelectedTrack(track.id)
                                                showTrackMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            selectedTrack?.let { track ->
                                val prefs = screenModel.trackStatsPrefs[track.id]
                                val context = TrackStatsContext.fromConfig(config, prefs)
                                val canJumpToTrack = track.segments.any { it.points.isNotEmpty() }
                                FilledTonalButton(
                                    onClick = {
                                        val viewSize = screenModel.viewSize
                                        if (viewSize.width <= 0 || viewSize.height <= 0) return@FilledTonalButton
                                        val center = trackCenterLatLon(track) ?: return@FilledTonalButton
                                        val tileSize = 256
                                        val tileX = latLonToTileX(center.second, screenModel.zoom)
                                        val tileY = latLonToTileY(center.first, screenModel.zoom)
                                        screenModel.centerOffset = Offset(
                                            (viewSize.width / 2f) - (tileX * tileSize).toFloat(),
                                            (viewSize.height / 2f) - (tileY * tileSize).toFloat()
                                        )
                                    },
                                    enabled = canJumpToTrack,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Jump to track")
                                }
                                TrackStatsPanel(
                                    track = track,
                                    context = context,
                                    prefs = prefs,
                                    onUpdatePrefs = { updated -> screenModel.updateTrackStatsPrefs(updated) },
                                    showHeader = false,
                                    allowOverrides = false,
                                    alignEnd = false,
                                    compact = false,
                                    fillWidth = true,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .widthIn(min = 44.dp, max = 64.dp)
                            .align(Alignment.End),
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
