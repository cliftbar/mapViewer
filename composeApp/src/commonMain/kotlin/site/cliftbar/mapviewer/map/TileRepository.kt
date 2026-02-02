@file:OptIn(kotlin.time.ExperimentalTime::class)
package site.cliftbar.mapviewer.map

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.withContext
import site.cliftbar.mapviewer.MapViewerDB
import site.cliftbar.mapviewer.Tile_cache
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.Instant as KInstant

class TileRepository(
    private val db: MapViewerDB,
    private val ioContext: CoroutineContext,
    private val maxCacheSize: Int = 5000 // Number of tiles, for example
) {
    private val queries = db.tile_cacheQueries

    /**
     * Retrieves a tile from the cache and updates its last accessed timestamp.
     */
    suspend fun getTile(layerId: String, zoom: Int, x: Int, y: Int): Tile_cache? = withContext(ioContext) {
        val tile = queries.getTile(layerId, zoom.toLong(), x.toLong(), y.toLong()).awaitAsOneOrNull()
        if (tile != null) {
            db.transaction {
                queries.updateLastAccessed(getNowMillis(), layerId, zoom.toLong(), x.toLong(), y.toLong())
            }
        }
        tile
    }

    private var mockMillis: Long? = null

    /**
     * For testing purposes, allows mocking the current time.
     */
    fun setMockMillis(millis: Long) { mockMillis = millis }

    private fun getNowMillis(): Long = mockMillis ?: KInstant.parse("2024-01-01T00:00:00Z").toEpochMilliseconds()

    /**
     * Inserts a tile into the cache and performs maintenance (expiry and LRU eviction).
     */
    suspend fun insertTile(
        layerId: String,
        zoom: Int,
        x: Int,
        y: Int,
        data: ByteArray,
        expiryDays: Int = 30
    ) = withContext(ioContext) {
        val now = getNowMillis()
        val expiry = now + expiryDays.days.inWholeMilliseconds
        
        db.transaction {
            queries.insertTile(zoom.toLong(), x.toLong(), y.toLong(), layerId, data, expiry, now)
            
            // 1. Delete expired tiles
            queries.deleteExpiredTiles(now)
        }

        // 2. LRU eviction if over capacity (separate check/transaction to keep it manageable)
        val count = queries.getCacheSize().awaitAsOne()
        if (count > maxCacheSize.toLong()) {
            db.transaction {
                val currentCount = queries.getCacheSize().awaitAsOne()
                if (currentCount > maxCacheSize.toLong()) {
                    val toDelete = (currentCount - maxCacheSize.toLong())
                    val oldest = queries.getOldestTiles(toDelete).awaitAsList()
                    oldest.forEach { tile ->
                        queries.deleteTiles(tile.layer_id, tile.zoom, tile.x, tile.y)
                    }
                }
            }
        }
    }

    /**
     * Clears all tiles from the cache.
     */
    suspend fun clearCache() = withContext(ioContext) {
        queries.clearCache()
    }
}
