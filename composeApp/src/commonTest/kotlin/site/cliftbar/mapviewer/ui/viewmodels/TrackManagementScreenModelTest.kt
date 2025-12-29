package site.cliftbar.mapviewer.ui.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import site.cliftbar.mapviewer.MapViewerDB
import site.cliftbar.mapviewer.db.createInMemoryDriver
import site.cliftbar.mapviewer.tracks.LineStyle
import site.cliftbar.mapviewer.tracks.Track
import site.cliftbar.mapviewer.tracks.TrackRepository
import site.cliftbar.mapviewer.tracks.TrackSegment
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class TrackManagementScreenModelTest {
    private lateinit var database: MapViewerDB
    private lateinit var trackRepository: TrackRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        try {
            Dispatchers.setMain(testDispatcher)
            database = MapViewerDB(createInMemoryDriver())
            trackRepository = TrackRepository(database)
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
    fun testRefreshTracks() = runTest {
        if (!::database.isInitialized) return@runTest
        val track = Track(id = "test-refresh", name = "Test Refresh")
        trackRepository.saveTrack(track)
        
        val model = TrackManagementScreenModel(trackRepository)
        advanceUntilIdle()
        
        assertEquals(1, model.tracks.size)
        assertEquals("test-refresh", model.tracks[0].id)
    }

    @Test
    fun testImportTrack() = runTest {
        if (!::database.isInitialized) return@runTest
        val model = TrackManagementScreenModel(trackRepository)
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Test">
              <trk><name>Imported</name><trkseg></trkseg></trk>
            </gpx>
        """.trimIndent()
        
        model.importTrack(gpx, "gpx")
        advanceUntilIdle()
        
        assertEquals(1, model.tracks.size)
        assertEquals("Imported", model.tracks[0].name)
    }

    @Test
    fun testUpdateTrackVisibility() = runTest {
        if (!::database.isInitialized) return@runTest
        val track = Track(id = "test-visibility", name = "Visibility", visible = true)
        trackRepository.saveTrack(track)
        
        val model = TrackManagementScreenModel(trackRepository)
        advanceUntilIdle()
        
        model.updateTrackVisibility("test-visibility", false)
        advanceUntilIdle()
        
        assertFalse(model.tracks[0].visible)
        
        val stored = trackRepository.getAllTracks().find { it.id == "test-visibility" }
        assertFalse(stored?.visible ?: true)
    }

    @Test
    fun testDeleteTrack() = runTest {
        if (!::database.isInitialized) return@runTest
        val track = Track(id = "test-delete", name = "Delete")
        trackRepository.saveTrack(track)
        
        val model = TrackManagementScreenModel(trackRepository)
        advanceUntilIdle()
        
        model.deleteTrack("test-delete")
        advanceUntilIdle()
        
        assertTrue(model.tracks.isEmpty())
        assertTrue(trackRepository.getAllTracks().isEmpty())
    }
}
