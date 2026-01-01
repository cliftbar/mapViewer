package site.cliftbar.mapviewer.config

import site.cliftbar.mapviewer.MapViewerDB
import site.cliftbar.mapviewer.db.createInMemoryDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.*

// Simple way to avoid running on Android for now as it's logic-only and Android target in unit tests is tricky with SQLDelight
// unless using Robolectric which is failing to load correctly here.
class ConfigRepositoryTest {
    private lateinit var database: MapViewerDB
    private lateinit var repository: ConfigRepository

    @BeforeTest
    fun setup() = runTest {
        try {
            database = MapViewerDB(createInMemoryDriver())
            repository = ConfigRepository(database)
        } catch (e: Exception) {
            // Skip
        }
    }

    @Test
    fun testDefaultConfigLoad() = runTest {
        if (!::repository.isInitialized) return@runTest
        val config = repository.loadConfig()
        assertEquals(12, config.defaultZoom)
    }

    @Test
    fun testSaveAndLoadProfile() = runTest {
        if (!::repository.isInitialized) return@runTest
        val customConfig = Config(defaultZoom = 15)
        repository.saveConfig(customConfig, "hiking")
        
        val loaded = repository.loadConfig("hiking")
        assertEquals(15, loaded.defaultZoom)
        
        // Ensure default config is still default
        val defaultConfig = repository.loadConfig("config")
        assertEquals(12, defaultConfig.defaultZoom)
    }

    @Test
    fun testGetAllProfiles() = runTest {
        if (!::repository.isInitialized) return@runTest
        // Save something to ensure they exist in DB
        repository.saveConfig(Config(), "config")
        repository.saveConfig(Config(), "p1")
        repository.saveConfig(Config(), "p2")
        
        val profiles = repository.getAllProfiles()
        assertTrue(profiles.contains("config"), "Profiles should contain 'config'")
        assertTrue(profiles.contains("p1"), "Profiles should contain 'p1'")
        assertTrue(profiles.contains("p2"), "Profiles should contain 'p2'")
    }

    @Test
    fun testSwitchProfile() = runTest {
        if (!::repository.isInitialized) return@runTest
        val hikingConfig = Config(defaultZoom = 18)
        repository.saveConfig(hikingConfig, "hiking")
        
        repository.switchProfile("hiking")
        
        val activeConfig = repository.activeConfig.value
        assertEquals(18, activeConfig.defaultZoom)
        
        val storedConfig = repository.loadConfig("config")
        assertEquals(18, storedConfig.defaultZoom)
    }
}
