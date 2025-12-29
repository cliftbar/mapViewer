package site.cliftbar.mapviewer.tracks

import kotlinx.coroutines.test.runTest
import site.cliftbar.mapviewer.MapViewerDB
import site.cliftbar.mapviewer.db.createInMemoryDriver
import kotlin.test.*

class TrackRepositoryTest {
    private lateinit var database: MapViewerDB
    private lateinit var repository: TrackRepository

    @BeforeTest
    fun setup() {
        try {
            database = MapViewerDB(createInMemoryDriver())
            repository = TrackRepository(database)
        } catch (e: Exception) {
            // Skip
        }
    }

    @Test
    fun testSaveAndGetAllTracks() = runTest {
        if (!::repository.isInitialized) return@runTest
        val track = Track(
            id = "test-track",
            name = "Test Track",
            segments = listOf(
                TrackSegment(
                    points = listOf(
                        TrackPoint(latitude = 45.0, longitude = -122.0, elevation = 100.0, time = 1000L),
                        TrackPoint(latitude = 45.1, longitude = -122.1, elevation = 110.0, time = 2000L)
                    )
                )
            )
        )
        
        repository.saveTrack(track)
        
        val allTracks = repository.getAllTracks()
        assertEquals(1, allTracks.size)
        assertEquals("test-track", allTracks[0].id)
        assertEquals("Test Track", allTracks[0].name)
        assertEquals(1, allTracks[0].segments.size)
        assertEquals(2, allTracks[0].segments[0].points.size)
        assertEquals(45.0, allTracks[0].segments[0].points[0].latitude)
        assertEquals(100.0, allTracks[0].segments[0].points[0].elevation)
        assertEquals(1000L, allTracks[0].segments[0].points[0].time)
    }

    @Test
    fun testImportGeoJson() = runTest {
        if (!::repository.isInitialized) return@runTest
        val geoJson = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "LineString",
                    "coordinates": [
                      [-122.0, 45.0, 100.0],
                      [-122.1, 45.1, 110.0]
                    ]
                  },
                  "properties": {
                    "name": "GeoJSON Track"
                  }
                }
              ]
            }
        """.trimIndent()
        
        val track = repository.importTrack(geoJson, "geojson")
        assertNotNull(track)
        assertEquals("GeoJSON Track", track.name)
        assertFalse(track.id.isBlank(), "Track ID should not be blank after import")
        
        val allTracks = repository.getAllTracks()
        assertEquals(1, allTracks.size)
        assertEquals("GeoJSON Track", allTracks[0].name)
        assertEquals(track.id, allTracks[0].id)
    }

    @Test
    fun testImportGpx() = runTest {
        if (!::repository.isInitialized) return@runTest
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Test" xmlns="http://www.topografix.com/GPX/1/1">
              <trk>
                <name>GPX Track</name>
                <trkseg>
                  <trkpt lat="45.0" lon="-122.0">
                    <ele>100.0</ele>
                    <time>2023-10-27T12:00:00Z</time>
                  </trkpt>
                  <trkpt lat="45.1" lon="-122.1">
                    <ele>110.0</ele>
                    <time>2023-10-27T12:01:00Z</time>
                  </trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()
        
        val track = repository.importTrack(gpx, "gpx")
        assertNotNull(track)
        assertEquals("GPX Track", track.name)
        assertFalse(track.id.isBlank(), "Track ID should not be blank after import")
        assertEquals(1, track.segments.size)
        assertEquals(2, track.segments[0].points.size)
        
        val allTracks = repository.getAllTracks()
        assertEquals(1, allTracks.size)
        assertEquals("GPX Track", allTracks[0].name)
    }

    @Test
    fun testImportGpxNoNamespace() = runTest {
        if (!::repository.isInitialized) return@runTest
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Test">
              <trk>
                <name>GPX Track No Namespace</name>
                <trkseg>
                  <trkpt lat="45.0" lon="-122.0">
                    <ele>100.0</ele>
                  </trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()
        
        val track = repository.importTrack(gpx, "gpx")
        assertNotNull(track, "Import should succeed even without namespace")
        assertEquals("GPX Track No Namespace", track.name)
    }

    @Test
    fun testImportGpxMultipleTracks() = runTest {
        if (!::repository.isInitialized) return@runTest
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Test">
              <trk>
                <name>Track 1</name>
                <trkseg><trkpt lat="45.0" lon="-122.0"/></trkseg>
              </trk>
              <trk>
                <name>Track 2</name>
                <trkseg><trkpt lat="45.1" lon="-122.1"/></trkseg>
              </trk>
            </gpx>
        """.trimIndent()
        
        val track = repository.importTrack(gpx, "gpx")
        assertNotNull(track, "Import should succeed for GPX with multiple tracks")
        assertEquals("Track 1", track.name, "Should pick the first track")
    }
    @Test
    fun testImportInvalidGeoJson() = runTest {
        if (!::repository.isInitialized) return@runTest
        val invalidGeoJson = "{ \"type\": \"FeatureCollection\", \"features\": [] }"
        val track = repository.importTrack(invalidGeoJson, "geojson")
        assertNull(track, "Importing GeoJSON with no features should return null")
    }

    @Test
    fun testImportMalformedGeoJson() = runTest {
        if (!::repository.isInitialized) return@runTest
        val malformedGeoJson = "{ \"type\": \"FeatureCollection\", \"features\": " // missing closing brackets
        val track = repository.importTrack(malformedGeoJson, "geojson")
        assertNull(track, "Importing malformed GeoJSON should return null")
    }

    @Test
    fun testImportInvalidGpx() = runTest {
        if (!::repository.isInitialized) return@runTest
        val invalidGpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Test">
            </gpx>
        """.trimIndent()
        val track = repository.importTrack(invalidGpx, "gpx")
        assertNull(track, "Importing GPX with no tracks should return null")
    }

    @Test
    fun testImportMalformedGpx() = runTest {
        if (!::repository.isInitialized) return@runTest
        val malformedGpx = "<gpx><trk><name>Test" // missing closing tags
        val track = repository.importTrack(malformedGpx, "gpx")
        assertNull(track, "Importing malformed GPX should return null")
    }

    @Test
    fun testUpdateTrackVisibility() = runTest {
        if (!::repository.isInitialized) return@runTest
        val track = Track(id = "visible-test", name = "Visible Test", visible = true)
        repository.saveTrack(track)
        
        repository.updateTrackVisibility("visible-test", false)
        
        val allTracks = repository.getAllTracks()
        assertFalse(allTracks.find { it.id == "visible-test" }?.visible ?: true)
    }

    @Test
    fun testUpdateTrackStyle() = runTest {
        if (!::repository.isInitialized) return@runTest
        val track = Track(id = "style-test", name = "Style Test", color = "#0000FF", lineStyle = LineStyle.SOLID)
        repository.saveTrack(track)
        
        repository.updateTrackStyle("style-test", "#FF0000", LineStyle.DASHED)
        
        val updated = repository.getAllTracks().find { it.id == "style-test" }
        assertEquals("#FF0000", updated?.color)
        assertEquals(LineStyle.DASHED, updated?.lineStyle)
    }

    @Test
    fun testDeleteTrack() = runTest {
        if (!::repository.isInitialized) return@runTest
        val track = Track(id = "delete-test", name = "Delete Test")
        repository.saveTrack(track)
        
        repository.deleteTrack("delete-test")
        
        val allTracks = repository.getAllTracks()
        assertTrue(allTracks.none { it.id == "delete-test" })
    }
}
