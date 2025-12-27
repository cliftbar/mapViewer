package site.cliftbar.mapviewer.config

import kotlinx.serialization.Serializable

@Serializable
enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

@Serializable
data class Config(
    val defaultZoom: Int = 12,
    val initialLat: Double = 45.5152,
    val initialLon: Double = -122.6784,
    val activeBaseMapId: String = "osm",
    val activeOverlayIds: List<String> = emptyList(),
    val offlineMode: Boolean = false,
    val theme: AppTheme = AppTheme.SYSTEM
)
