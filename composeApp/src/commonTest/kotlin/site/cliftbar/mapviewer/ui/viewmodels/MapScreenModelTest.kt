package site.cliftbar.mapviewer.ui.viewmodels

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import site.cliftbar.mapviewer.MapViewerDB
import site.cliftbar.mapviewer.config.Config
import site.cliftbar.mapviewer.config.ConfigRepository
import site.cliftbar.mapviewer.db.createInMemoryDriver
import site.cliftbar.mapviewer.tracks.TrackRepository
import site.cliftbar.mapviewer.ui.components.latLonToTileX
import site.cliftbar.mapviewer.ui.components.latLonToTileY
import site.cliftbar.mapviewer.ui.components.tileXToLon
import site.cliftbar.mapviewer.ui.components.tileYToLat
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class MapScreenModelTest {
    private lateinit var database: MapViewerDB
    private lateinit var configRepository: ConfigRepository
    private lateinit var trackRepository: TrackRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        try {
            Dispatchers.setMain(testDispatcher)
            database = MapViewerDB(createInMemoryDriver())
            configRepository = ConfigRepository(database)
            trackRepository = TrackRepository(database)
        } catch (e: Exception) {
            // Skip tests if driver creation fails (e.g. on Android/Web target in commonTest)
        }
    }

    @AfterTest
    fun tearDown() {
        try {
            Dispatchers.resetMain()
        } catch (e: Exception) {
        }
    }

    @Test
    fun testInitialState() {
        if (!::database.isInitialized) return
        val initialConfig = Config(defaultZoom = 10, initialLat = 45.0, initialLon = -122.0)
        val model = MapScreenModel(initialConfig, configRepository, trackRepository)
        
        assertEquals(10, model.zoom)
        assertEquals(Offset.Zero, model.centerOffset)
    }

    @Test
    fun testZoomPersistence() {
        if (!::database.isInitialized) return
        val initialConfig = Config(defaultZoom = 10)
        val model = MapScreenModel(initialConfig, configRepository, trackRepository)
        model.viewSize = IntSize(1000, 1000)
        
        model.zoom = 12
        
        val savedConfig = configRepository.activeConfig.value
        assertEquals(12, savedConfig.defaultZoom)
    }

    @Test
    fun testCenterOffsetPersistence() {
        if (!::database.isInitialized) return
        val initialConfig = Config(defaultZoom = 10, initialLat = 45.0, initialLon = -122.0)
        val model = MapScreenModel(initialConfig, configRepository, trackRepository)
        model.viewSize = IntSize(1000, 1000)
        
        // Move by 256 pixels (1 tile)
        model.centerOffset = Offset(256f, 256f)
        
        val savedConfig = configRepository.activeConfig.value
        assertNotEquals(45.0, savedConfig.initialLat)
        assertNotEquals(-122.0, savedConfig.initialLon)
    }

    @Test
    fun testCoordinateMath() {
        val lat = 45.523062
        val lon = -122.676482
        val zoom = 12
        
        val x = latLonToTileX(lon, zoom)
        val y = latLonToTileY(lat, zoom)
        
        val backLon = tileXToLon(x, zoom)
        val backLat = tileYToLat(y, zoom)
        
        assertEquals(lon, backLon, 0.000001)
        assertEquals(lat, backLat, 0.000001)
    }
}
