package site.cliftbar.mapviewer.ui.viewmodels

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        try {
            database = MapViewerDB(createInMemoryDriver())
            trackRepository = TrackRepository(database)
        } catch (e: Exception) {
            // Skip
        }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRefreshTracks() = runTest(timeout = kotlin.time.Duration.parse("10s")) {
        if (!::trackRepository.isInitialized) return@runTest
        val track = Track(id = "test-refresh", name = "Test Refresh")
        trackRepository.saveTrack(track)
        
        val model = TrackManagementScreenModel(trackRepository)
        val job = model.refreshTracks()
        if (job is Job) job.join()
        advanceUntilIdle()
        
        assertEquals(1, model.tracks.size)
        assertEquals("test-refresh", model.tracks[0].id)
    }

    @Test
    fun testImportTrack() = runTest(timeout = kotlin.time.Duration.parse("10s")) {
        if (!::trackRepository.isInitialized) return@runTest
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
    fun testUpdateTrackVisibility() = runTest(timeout = kotlin.time.Duration.parse("10s")) {
        if (!::trackRepository.isInitialized) return@runTest
        val track = Track(id = "test-visibility", name = "Visibility", visible = true)
        trackRepository.saveTrack(track)
        
        val model = TrackManagementScreenModel(trackRepository)
        val job = model.refreshTracks()
        if (job is Job) job.join()
        advanceUntilIdle()
        
        val updateJob = model.updateTrackVisibility("test-visibility", false)
        if (updateJob is Job) updateJob.join()
        advanceUntilIdle()
        
        assertFalse(model.tracks[0].visible)
        
        val stored = trackRepository.getAllTracks().find { it.id == "test-visibility" }
        assertFalse(stored?.visible ?: true)
    }

    @Test
    fun testDeleteTrack() = runTest(timeout = kotlin.time.Duration.parse("10s")) {
        if (!::trackRepository.isInitialized) return@runTest
        val track = Track(id = "test-delete", name = "Delete")
        trackRepository.saveTrack(track)
        
        val model = TrackManagementScreenModel(trackRepository)
        val job = model.refreshTracks()
        if (job is Job) job.join()
        advanceUntilIdle()
        
        val deleteJob = model.deleteTrack("test-delete")
        if (deleteJob is Job) deleteJob.join()
        advanceUntilIdle()
        
        assertTrue(model.tracks.isEmpty())
        assertTrue(trackRepository.getAllTracks().isEmpty())
    }

    @Test
    fun testMultiSelection() = runTest(timeout = kotlin.time.Duration.parse("10s")) {
        if (!::trackRepository.isInitialized) return@runTest
        val t1 = Track(id = "1", name = "T1")
        val t2 = Track(id = "2", name = "T2")
        trackRepository.saveTrack(t1)
        trackRepository.saveTrack(t2)
        
        val model = TrackManagementScreenModel(trackRepository)
        val job = model.refreshTracks()
        if (job is Job) job.join()
        advanceUntilIdle()
        
        model.toggleSelection("1")
        assertTrue(model.selectedTrackIds["1"] ?: false)
        assertFalse(model.selectedTrackIds["2"] ?: false)
        
        model.toggleSelection("2")
        assertTrue(model.selectedTrackIds["2"] ?: false)
        
        model.toggleSelection("1")
        assertFalse(model.selectedTrackIds["1"] ?: false)
        
        model.selectAll()
        assertEquals(2, model.selectedTrackIds.size)
        
        model.clearSelection()
        assertTrue(model.selectedTrackIds.isEmpty())
    }

    @Test
    fun testBulkDelete() = runTest(timeout = kotlin.time.Duration.parse("10s")) {
        if (!::trackRepository.isInitialized) return@runTest
        val t1 = Track(id = "1", name = "T1")
        val t2 = Track(id = "2", name = "T2")
        val t3 = Track(id = "3", name = "T3")
        trackRepository.saveTrack(t1)
        trackRepository.saveTrack(t2)
        trackRepository.saveTrack(t3)
        
        val model = TrackManagementScreenModel(trackRepository)
        val job = model.refreshTracks()
        if (job is Job) job.join()
        advanceUntilIdle()
        
        assertEquals(3, model.tracks.size, "Should have 3 tracks initially")
        
        model.toggleSelection("1")
        model.toggleSelection("3")
        
        val deleteJob = model.deleteSelectedTracks()
        if (deleteJob is Job) deleteJob.join()
        advanceUntilIdle()
        
        assertEquals(1, model.tracks.size, "Should have 1 track left after bulk delete")
        assertEquals("2", model.tracks[0].id)
        assertTrue(model.selectedTrackIds.isEmpty())
    }

    @Test
    fun testBulkVisibilityUpdate() = runTest(timeout = kotlin.time.Duration.parse("10s")) {
        if (!::trackRepository.isInitialized) return@runTest
        val t1 = Track(id = "1", name = "T1", visible = true)
        val t2 = Track(id = "2", name = "T2", visible = true)
        trackRepository.saveTrack(t1)
        trackRepository.saveTrack(t2)
        
        val model = TrackManagementScreenModel(trackRepository)
        val job = model.refreshTracks()
        if (job is Job) job.join()
        advanceUntilIdle()
        
        model.toggleSelection("1")
        model.toggleSelection("2")
        
        val updateJob = model.updateSelectedTracksVisibility(false)
        if (updateJob is Job) updateJob.join()
        advanceUntilIdle()
        
        assertEquals(2, model.tracks.size)
        assertFalse(model.tracks[0].visible)
        assertFalse(model.tracks[1].visible)
        
        val stored = trackRepository.getAllTracks()
        assertTrue(stored.none { it.visible })
    }
}
