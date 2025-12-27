package site.cliftbar.mapviewer.tracks

import site.cliftbar.mapviewer.MapViewerDB
import site.cliftbar.mapviewer.db.createInMemoryDriver
import kotlin.test.*

class TrackRepositoryTest {
    private lateinit var database: MapViewerDB
    private lateinit var repository: TrackRepository

    @BeforeTest
    fun setup() {
        database = MapViewerDB(createInMemoryDriver())
        repository = TrackRepository(database)
    }

    @Test
    fun testSaveAndGetAllTracks() {
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
    fun testImportGeoJson() {
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
    fun testImportGpx() {
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
}
