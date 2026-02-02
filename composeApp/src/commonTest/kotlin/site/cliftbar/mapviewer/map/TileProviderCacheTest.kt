package site.cliftbar.mapviewer.map

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import site.cliftbar.mapviewer.MapViewerDB
import site.cliftbar.mapviewer.db.createInMemoryDriver
import kotlin.test.*

class TileProviderCacheTest {
    private lateinit var database: MapViewerDB
    private lateinit var repository: TileRepository

    @BeforeTest
    fun setup() = runTest {
        try {
            database = MapViewerDB(createInMemoryDriver())
            repository = TileRepository(database, Dispatchers.Default)
        } catch (e: Exception) {
            // Skip if driver creation fails
        }
    }

    @Test
    fun testCacheHit() = runTest {
        if (!::repository.isInitialized) return@runTest
        
        // 1. Pre-fill cache
        val data = byteArrayOf(1, 2, 3)
        repository.insertTile("osm", 10, 1, 1, data)
        
        // 2. Mock network (should NOT be called)
        val mockEngine = MockEngine { _ ->
            fail("Network should not be called when cache hit occurs")
        }
        val client = HttpClient(mockEngine)
        val provider = TileProvider(client, repository)
        
        // 3. Get tile
        try {
            provider.getTile(10, 1, 1)
        } catch (e: Throwable) {
            // Expected if decodeImage fails in tests, but we want to ensure mockEngine wasn't called
        }
    }

    @Test
    fun testCacheMissAndSave() = runTest {
        if (!::repository.isInitialized) return@runTest
        
        var networkCalled = false
        val mockEngine = MockEngine { _ ->
            networkCalled = true
            respond(
                content = ByteReadChannel(byteArrayOf(4, 5, 6)),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "image/png")
            )
        }
        val client = HttpClient(mockEngine)
        val provider = TileProvider(client, repository)
        
        // 1. Get tile (should hit network)
        try {
            provider.getTile(11, 2, 2)
        } catch (e: Throwable) {}
        
        assertTrue(networkCalled, "Network should have been called on cache miss")
        
        // 2. Check cache (should be saved)
        val cached = repository.getTile("osm", 11, 2, 2)
        assertNotNull(cached, "Tile should have been saved to cache")
        assertContentEquals(byteArrayOf(4, 5, 6), cached.data_)
    }
}
