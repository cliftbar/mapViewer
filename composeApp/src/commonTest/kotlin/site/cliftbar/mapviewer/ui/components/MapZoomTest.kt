package site.cliftbar.mapviewer.ui.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class MapZoomTest {
    @Test
    fun preservesFocusLatLonAcrossZoom() {
        val currentZoom = 5
        val newZoom = 7
        val focus = Offset(400f, 300f)
        val centerOffset = Offset(-128f, 64f)
        val tileSize = 256

        val focusTileX = (focus.x - centerOffset.x) / tileSize
        val focusTileY = (focus.y - centerOffset.y) / tileSize
        val lat = tileYToLat(focusTileY.toDouble(), currentZoom)
        val lon = tileXToLon(focusTileX.toDouble(), currentZoom)

        val newCenterOffset = calculateZoomedCenterOffset(
            currentZoom = currentZoom,
            newZoom = newZoom,
            focus = focus,
            centerOffset = centerOffset,
            tileSize = tileSize
        )

        val newFocusTileX = (focus.x - newCenterOffset.x) / tileSize
        val newFocusTileY = (focus.y - newCenterOffset.y) / tileSize
        val newLat = tileYToLat(newFocusTileY.toDouble(), newZoom)
        val newLon = tileXToLon(newFocusTileX.toDouble(), newZoom)

        assertTrue(abs(lat - newLat) < 1e-6)
        assertTrue(abs(lon - newLon) < 1e-6)
    }
}
