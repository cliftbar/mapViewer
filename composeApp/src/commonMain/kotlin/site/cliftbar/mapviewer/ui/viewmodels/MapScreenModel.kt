package site.cliftbar.mapviewer.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import cafe.adriel.voyager.core.model.ScreenModel
import site.cliftbar.mapviewer.map.MapLayer

class MapScreenModel : ScreenModel {
    var zoom by mutableStateOf(12)
    var centerOffset by mutableStateOf(Offset.Zero)
    var initialized by mutableStateOf(false)
    var viewSize by mutableStateOf(IntSize.Zero)
    val activeLayers = mutableStateListOf<MapLayer>(MapLayer.OpenStreetMap)
}
