package site.cliftbar.mapviewer.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import site.cliftbar.mapviewer.config.Config
import site.cliftbar.mapviewer.config.ConfigRepository
import site.cliftbar.mapviewer.tracks.TrackRepository
import site.cliftbar.mapviewer.map.MapLayer
import site.cliftbar.mapviewer.map.TileProvider
import site.cliftbar.mapviewer.network.httpClient
import site.cliftbar.mapviewer.ui.components.MapView
import site.cliftbar.mapviewer.ui.viewmodels.MapScreenModel

class MapScreen(
    private val configRepository: ConfigRepository,
    private val trackRepository: TrackRepository
) : Tab {
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
        val config by configRepository.activeConfig.collectAsState()
        val screenModel = rememberScreenModel { MapScreenModel(config, configRepository, trackRepository) }
        val tileProvider = remember { TileProvider(httpClient) }
        var showLayerMenu by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            screenModel.refreshTracks()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            MapView(
                tileProvider = tileProvider,
                zoom = screenModel.zoom,
                onZoomChange = { screenModel.zoom = it },
                centerOffset = screenModel.centerOffset,
                onCenterOffsetChange = { screenModel.centerOffset = it },
                initialized = screenModel.initialized,
                onInitializedChange = { screenModel.initialized = it },
                viewSize = screenModel.viewSize,
                onViewSizeChange = { screenModel.viewSize = it },
                activeLayers = screenModel.activeLayers,
                activeTracks = screenModel.activeTracks,
                initialLat = config.initialLat,
                initialLon = config.initialLon
            )

            // Layer Selection Button
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                SmallFloatingActionButton(
                    onClick = { showLayerMenu = true }
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Layers")
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
                                // It's a base layer, replace current base layer
                                // For now, assume the first layer is the base layer
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
    }
}
