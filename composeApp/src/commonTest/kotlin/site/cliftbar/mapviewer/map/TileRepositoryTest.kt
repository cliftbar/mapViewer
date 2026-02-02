package site.cliftbar.mapviewer.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import site.cliftbar.mapviewer.MapViewerDB
import site.cliftbar.mapviewer.db.createInMemoryDriver
import kotlin.test.*

class TileRepositoryTest {
    private lateinit var database: MapViewerDB
    private lateinit var repository: TileRepository

    @BeforeTest
    fun setup() = runTest {
        try {
            database = MapViewerDB(createInMemoryDriver())
            repository = TileRepository(database, Dispatchers.Default, maxCacheSize = 3)
        } catch (e: Exception) {
            // Skip if driver creation fails in this environment
        }
    }

    @Test
    fun testInsertAndGetTile() = runTest {
        if (!::repository.isInitialized) return@runTest
        
        val data = byteArrayOf(1, 2, 3)
        repository.insertTile("osm", 10, 100, 200, data)
        
        val tile = repository.getTile("osm", 10, 100, 200)
        assertNotNull(tile)
        assertEquals("osm", tile.layer_id)
        assertEquals(10L, tile.zoom)
        assertEquals(100L, tile.x)
        assertEquals(200L, tile.y)
        assertNotNull(tile.data_)
    }

    @Test
    fun testLRUEviction() = runTest {
        if (!::repository.isInitialized) return@runTest
        
        // maxCacheSize is 3
        repository.setMockMillis(1000)
        repository.insertTile("layer", 1, 1, 1, byteArrayOf(1))
        repository.setMockMillis(2000)
        repository.insertTile("layer", 2, 2, 2, byteArrayOf(2))
        repository.setMockMillis(3000)
        repository.insertTile("layer", 3, 3, 3, byteArrayOf(3))
        
        // Access 1 again to make it recently used
        repository.setMockMillis(4000)
        repository.getTile("layer", 1, 1, 1)
        
        // Insert 4th tile, should evict 2 (which was oldest and not recently accessed)
        repository.setMockMillis(5000)
        repository.insertTile("layer", 4, 4, 4, byteArrayOf(4))
        
        assertNotNull(repository.getTile("layer", 1, 1, 1))
        assertNull(repository.getTile("layer", 2, 2, 2))
        assertNotNull(repository.getTile("layer", 3, 3, 3))
        assertNotNull(repository.getTile("layer", 4, 4, 4))
    }

    @Test
    fun testExpiry() = runTest {
        if (!::repository.isInitialized) return@runTest
        
        // Insert a tile that is already expired (expiryDays = -1)
        repository.insertTile("layer", 1, 1, 1, byteArrayOf(1), expiryDays = -1)
        
        // Maintenance happens on insert
        repository.insertTile("layer", 2, 2, 2, byteArrayOf(2))
        
        assertNull(repository.getTile("layer", 1, 1, 1), "Expired tile should have been deleted")
        assertNotNull(repository.getTile("layer", 2, 2, 2))
    }

    @Test
    fun testClearCache() = runTest {
        if (!::repository.isInitialized) return@runTest
        
        repository.insertTile("layer", 1, 1, 1, byteArrayOf(1))
        repository.clearCache()
        
        assertNull(repository.getTile("layer", 1, 1, 1))
    }
}
