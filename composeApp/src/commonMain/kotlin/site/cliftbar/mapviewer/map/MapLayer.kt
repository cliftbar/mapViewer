package site.cliftbar.mapviewer.map

sealed class MapLayer(
    val id: String,
    val name: String,
    val urlTemplate: String,
    val isOverlay: Boolean = false,
    val attribution: String
) {
    object OpenStreetMap : MapLayer(
        id = "osm",
        name = "OpenStreetMap",
        urlTemplate = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        attribution = "© OpenStreetMap contributors"
    )

    object OpenCycleMap : MapLayer(
        id = "opencyclemap",
        name = "OpenCycleMap",
        urlTemplate = "https://tile.thunderforest.com/cycle/{z}/{x}/{y}.png", // Requires API key usually, but let's list it
        attribution = "© Thunderforest, © OpenStreetMap contributors"
    )

    object OpenSnowMap : MapLayer(
        id = "opensnowmap",
        name = "OpenSnowMap",
        urlTemplate = "https://tiles.opensnowmap.org/pistes/{z}/{x}/{y}.png",
        isOverlay = true,
        attribution = "© OpenSnowMap.org, © OpenStreetMap contributors"
    )

    object WaymarkedTrailsSki : MapLayer(
        id = "waymarked-ski",
        name = "Waymarked Trails (Ski)",
        urlTemplate = "https://tile.waymarkedtrails.org/slopes/{z}/{x}/{y}.png",
        isOverlay = true,
        attribution = "© Waymarked Trails, © OpenStreetMap contributors"
    )

    companion object {
        val allLayers: List<MapLayer> by lazy {
            listOf(
                OpenStreetMap,
                OpenCycleMap,
                OpenSnowMap,
                WaymarkedTrailsSki
            )
        }
    }
}
