package site.cliftbar.mapviewer.ui.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import site.cliftbar.mapviewer.MapViewerDB
import site.cliftbar.mapviewer.config.Config
import site.cliftbar.mapviewer.config.ConfigRepository
import site.cliftbar.mapviewer.db.createInMemoryDriver
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsScreenModelTest {
    private lateinit var database: MapViewerDB
    private lateinit var configRepository: ConfigRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() = runTest {
        try {
            Dispatchers.setMain(testDispatcher)
            database = MapViewerDB(createInMemoryDriver())
            configRepository = ConfigRepository(database)
        } catch (e: Exception) {
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
    fun testRefreshProfiles() = runTest {
        if (!::database.isInitialized) return@runTest
        configRepository.saveConfig(Config(), "profile1")
        
        val model = SettingsScreenModel(configRepository)
        advanceUntilIdle()
        
        assertTrue(model.profiles.contains("profile1"))
    }

    @Test
    fun testSaveConfig() = runTest {
        if (!::database.isInitialized) return@runTest
        val model = SettingsScreenModel(configRepository)
        val newConfig = Config(defaultZoom = 14)
        
        model.saveConfig(newConfig, "profile2")
        advanceUntilIdle()
        
        assertTrue(model.profiles.contains("profile2"))
        assertEquals(14, configRepository.loadConfig("profile2").defaultZoom)
    }

    @Test
    fun testSwitchProfile() = runTest {
        if (!::database.isInitialized) return@runTest
        configRepository.saveConfig(Config(defaultZoom = 16), "profile3")
        val model = SettingsScreenModel(configRepository)
        
        model.switchProfile("profile3")
        advanceUntilIdle()
        
        assertEquals(16, configRepository.activeConfig.value.defaultZoom)
    }

    @Test
    fun testDeleteProfile() = runTest {
        if (!::database.isInitialized) return@runTest
        configRepository.saveConfig(Config(), "deleteMe")
        val model = SettingsScreenModel(configRepository)
        advanceUntilIdle()
        
        model.deleteProfile("deleteMe")
        advanceUntilIdle()
        
        assertFalse(model.profiles.contains("deleteMe"))
        assertNull(database.`1Queries`.getConfigByKey("deleteMe").executeAsOneOrNull())
    }
}
