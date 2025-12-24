package site.cliftbar.mapviewer.map

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeImage(bytes: ByteArray): ImageBitmap

class TileProvider(private val client: HttpClient) {
    suspend fun getTile(zoom: Int, x: Int, y: Int, layer: MapLayer = MapLayer.OpenStreetMap): ImageBitmap? {
        val url = layer.urlTemplate
            .replace("{z}", zoom.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())
        return try {
            val response = client.get(url)
            val bytes = response.readBytes()
            decodeImage(bytes)
        } catch (e: Exception) {
            null
        }
    }
}
