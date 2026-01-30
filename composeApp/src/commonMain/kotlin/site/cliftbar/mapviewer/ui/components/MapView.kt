package site.cliftbar.mapviewer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import site.cliftbar.mapviewer.map.MapLayer
import site.cliftbar.mapviewer.map.TileProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import site.cliftbar.mapviewer.tracks.LineStyle
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

fun calculateZoomedCenterOffset(
    currentZoom: Int,
    newZoom: Int,
    focus: Offset,
    centerOffset: Offset,
    tileSize: Int = 256
): Offset {
    val focusX = (focus.x - centerOffset.x) / tileSize
    val focusY = (focus.y - centerOffset.y) / tileSize
    val lat = tileYToLat(focusY.toDouble(), currentZoom)
    val lon = tileXToLon(focusX.toDouble(), currentZoom)

    val newTileX = latLonToTileX(lon, newZoom)
    val newTileY = latLonToTileY(lat, newZoom)
    return Offset(
        focus.x - (newTileX * tileSize).toFloat(),
        focus.y - (newTileY * tileSize).toFloat()
    )
}

@Composable
fun MapView(
    tileProvider: TileProvider,
    zoom: Int,
    centerOffset: Offset,
    onCenterOffsetChange: (Offset) -> Unit,
    initialized: Boolean,
    onInitializedChange: (Boolean) -> Unit,
    viewSize: IntSize,
    onViewSizeChange: (IntSize) -> Unit,
    onZoomRequest: (delta: Int, focus: Offset) -> Unit,
    activeLayers: List<MapLayer> = listOf(MapLayer.OpenStreetMap),
    activeTracks: List<Track> = emptyList(),
    initialLat: Double = 45.5152,
    initialLon: Double = -122.6784
) {
    val tileSize = 256

    val coroutineScope = rememberCoroutineScope()
    val tiles = remember { mutableStateMapOf<String, ImageBitmap>() }
    val loadingTiles = remember { mutableStateSetOf<String>() }
    var zoomAccumulator by remember { mutableStateOf(1f) }

    val currentCenterOffset = rememberUpdatedState(centerOffset)
    val currentOnCenterOffsetChange = rememberUpdatedState(onCenterOffsetChange)
    val currentOnZoomRequest = rememberUpdatedState(onZoomRequest)

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { newSize ->
                    if (newSize != viewSize && newSize.width > 0 && newSize.height > 0) {
                        onViewSizeChange(newSize)
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoomChange, _ ->
                        if (pan != Offset.Zero) {
                            currentOnCenterOffsetChange.value(currentCenterOffset.value + pan)
                        }
                        if (zoomChange != 1f) {
                            zoomAccumulator *= zoomChange
                            if (zoomAccumulator > 1.1f) {
                                currentOnZoomRequest.value(1, centroid)
                                zoomAccumulator = 1f
                            } else if (zoomAccumulator < 0.9f) {
                                currentOnZoomRequest.value(-1, centroid)
                                zoomAccumulator = 1f
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type != PointerEventType.Scroll) continue
                            val change = event.changes.firstOrNull() ?: continue
                            val delta = change.scrollDelta.y
                            if (delta == 0f) continue
                            currentOnZoomRequest.value(if (delta < 0f) 1 else -1, change.position)
                            change.consume()
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            
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
                val trackColor = try {
                    Color(track.color.substring(1).toLong(16) or 0xFF000000)
                } catch (e: Exception) {
                    Color.Blue
                }

                val pathEffect = when (track.lineStyle) {
                    LineStyle.SOLID -> null
                    LineStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    LineStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(2f, 10f), 0f)
                }

                track.segments.forEach { segment ->
                    if (segment.points.size > 1) {
                        val path = androidx.compose.ui.graphics.Path()
                        var first = true

                        for (p in segment.points) {
                            val x = latLonToTileX(p.longitude, zoom) * tileSize + centerOffset.x
                            val y = latLonToTileY(p.latitude, zoom) * tileSize + centerOffset.y

                            if (first) {
                                path.moveTo(x.toFloat(), y.toFloat())
                                first = false
                            } else {
                                path.lineTo(x.toFloat(), y.toFloat())
                            }
                        }

                        drawPath(
                            path = path,
                            color = trackColor,
                            style = Stroke(
                                width = 4f,
                                pathEffect = pathEffect
                            )
                        )
                    }
                }
            }
        }

    }
}
