package site.cliftbar.mapviewer.config

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val defaultZoom: Int = 12,
    val initialLat: Double = 45.5152,
    val initialLon: Double = -122.6784,
    val activeBaseMapId: String = "osm",
    val activeOverlayIds: List<String> = emptyList(),
    val offlineMode: Boolean = false
)
