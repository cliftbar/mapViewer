package site.cliftbar.mapviewer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import site.cliftbar.mapviewer.map.MapLayer
import site.cliftbar.mapviewer.map.TileProvider
import site.cliftbar.mapviewer.tracks.Track
import kotlin.math.*

fun latLonToTileX(lon: Double, zoom: Int): Double {
    return (lon + 180.0) / 360.0 * (1 shl zoom)
}

fun latLonToTileY(lat: Double, zoom: Int): Double {
    return (1.0 - ln(tan(lat * PI / 180.0) + 1.0 / cos(lat * PI / 180.0)) / PI) / 2.0 * (1 shl zoom)
}

fun tileXToLon(x: Double, zoom: Int): Double {
    return x / (1 shl zoom) * 360.0 - 180.0
}

fun tileYToLat(y: Double, zoom: Int): Double {
    val n = PI - 2.0 * PI * y / (1 shl zoom)
    return 180.0 / PI * atan(0.5 * (exp(n) - exp(-n)))
}

@Composable
fun MapView(
    tileProvider: TileProvider,
    zoom: Int,
    onZoomChange: (Int) -> Unit,
    centerOffset: Offset,
    onCenterOffsetChange: (Offset) -> Unit,
    initialized: Boolean,
    onInitializedChange: (Boolean) -> Unit,
    viewSize: IntSize,
    onViewSizeChange: (IntSize) -> Unit,
    activeLayers: List<MapLayer> = listOf(MapLayer.OpenStreetMap),
    activeTracks: List<Track> = emptyList(),
    initialLat: Double = 45.5152,
    initialLon: Double = -122.6784
) {
    val tileSize = 256

    val coroutineScope = rememberCoroutineScope()
    val tiles = remember { mutableStateMapOf<String, ImageBitmap>() }
    val loadingTiles = remember { mutableStateSetOf<String>() }

    val currentCenterOffset = rememberUpdatedState(centerOffset)
    val currentOnCenterOffsetChange = rememberUpdatedState(onCenterOffsetChange)

    fun updateZoom(newZoom: Int) {
        if (newZoom == zoom || viewSize.width <= 0 || viewSize.height <= 0) return

        // 1. Calculate current geographic center
        val centerX = (viewSize.width / 2f - currentCenterOffset.value.x) / tileSize
        val centerY = (viewSize.height / 2f - currentCenterOffset.value.y) / tileSize
        val lat = tileYToLat(centerY.toDouble(), zoom)
        val lon = tileXToLon(centerX.toDouble(), zoom)

        // 2. Update zoom
        onZoomChange(newZoom)

        // 3. Recalculate centerOffset for new zoom to keep same lat/lon at center
        val newTileX = latLonToTileX(lon, newZoom)
        val newTileY = latLonToTileY(lat, newZoom)
        currentOnCenterOffsetChange.value(Offset(
            (viewSize.width / 2f) - (newTileX * tileSize).toFloat(),
            (viewSize.height / 2f) - (newTileY * tileSize).toFloat()
        ))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        currentOnCenterOffsetChange.value(currentCenterOffset.value + dragAmount)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            
            // Only update viewSize if it actually changed to avoid infinite save loops
            val newSize = IntSize(width.toInt(), height.toInt())
            if (newSize != viewSize) {
                onViewSizeChange(newSize)
            }

            if (!initialized && width > 0 && height > 0) {
                val tileX = latLonToTileX(initialLon, zoom)
                val tileY = latLonToTileY(initialLat, zoom)
                val newOffset = Offset(
                    (width / 2f) - (tileX * tileSize).toFloat(),
                    (height / 2f) - (tileY * tileSize).toFloat()
                )
                onCenterOffsetChange(newOffset)
                onInitializedChange(true)
            }

            val numTiles = 2.0.pow(zoom).toInt()

            val startX = max(0, floor(-centerOffset.x / tileSize).toInt())
            val endX = min(numTiles - 1, floor((width - centerOffset.x) / tileSize).toInt())
            val startY = max(0, floor(-centerOffset.y / tileSize).toInt())
            val endY = min(numTiles - 1, floor((height - centerOffset.y) / tileSize).toInt())

            for (x in startX..endX) {
                for (y in startY..endY) {
                    activeLayers.forEach { layer ->
                        val key = "${layer.id}-$zoom-$x-$y"
                        val tile = tiles[key]
                        if (tile != null) {
                            drawImage(
                                image = tile,
                                dstOffset = IntOffset(
                                    (x * tileSize + centerOffset.x).toInt(),
                                    (y * tileSize + centerOffset.y).toInt()
                                )
                            )
                        } else if (!loadingTiles.contains(key)) {
                            loadingTiles.add(key)
                            coroutineScope.launch {
                                val newTile = tileProvider.getTile(zoom, x, y, layer)
                                if (newTile != null) {
                                    tiles[key] = newTile
                                }
                                loadingTiles.remove(key)
                            }
                        }
                    }
                }
            }

            // Draw tracks
            activeTracks.forEach { track ->
                track.segments.forEach { segment ->
                    if (segment.points.size > 1) {
                        for (i in 0 until segment.points.size - 1) {
                            val p1 = segment.points[i]
                            val p2 = segment.points[i + 1]

                            val x1 = latLonToTileX(p1.longitude, zoom) * tileSize + centerOffset.x
                            val y1 = latLonToTileY(p1.latitude, zoom) * tileSize + centerOffset.y
                            val x2 = latLonToTileX(p2.longitude, zoom) * tileSize + centerOffset.x
                            val y2 = latLonToTileY(p2.latitude, zoom) * tileSize + centerOffset.y

                            // Simple culling - check if at least one point is on screen
                            if ((x1 in 0f..width || x2 in 0f..width) && (y1 in 0f..height || y2 in 0f..height)) {
                                drawLine(
                                    color = androidx.compose.ui.graphics.Color.Blue,
                                    start = Offset(x1.toFloat(), y1.toFloat()),
                                    end = Offset(x2.toFloat(), y2.toFloat()),
                                    strokeWidth = 4f
                                )
                            }
                        }
                    }
                }
            }
        }

        // Zoom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { if (zoom < 19) updateZoom(zoom + 1) },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("+")
            }
            FloatingActionButton(
                onClick = { if (zoom > 0) updateZoom(zoom - 1) }
            ) {
                Text("-")
            }
        }
    }
}
