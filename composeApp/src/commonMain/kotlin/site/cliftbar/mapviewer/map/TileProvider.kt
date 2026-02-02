package site.cliftbar.mapviewer.map

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeImage(bytes: ByteArray): ImageBitmap

class TileProvider(
    private val client: HttpClient,
    private val repository: TileRepository? = null
) {
    suspend fun getTile(zoom: Int, x: Int, y: Int, layer: MapLayer = MapLayer.OpenStreetMap): ImageBitmap? {
        // 1. Try Cache
        if (repository != null) {
            try {
                val cachedTile = repository.getTile(layer.id, zoom, x, y)
                if (cachedTile != null) {
                    return decodeImage(cachedTile.data_)
                }
            } catch (e: Exception) {
                // Fallback to network on cache error
            }
        }

        // 2. Try Network
        val url = layer.urlTemplate
            .replace("{z}", zoom.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())
        
        return try {
            val response = client.get(url)
            val bytes = response.readRawBytes()
            
            // 3. Save to Cache (asynchronous/background-ish if possible, but here we'll just do it)
            if (repository != null) {
                try {
                    repository.insertTile(layer.id, zoom, x, y, bytes)
                } catch (e: Exception) {
                    // Ignore cache insert errors
                }
            }
            
            decodeImage(bytes)
        } catch (e: Exception) {
            null
        }
    }
}
