package site.cliftbar.mapviewer.tracks

import kotlin.test.*

class TrackParserTest {

    @Test
    fun testGeoJsonParsing() {
        val json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "LineString",
                    "coordinates": [
                      [102.0, 0.0],
                      [103.0, 1.0],
                      [104.0, 0.0],
                      [105.0, 1.0]
                    ]
                  },
                  "properties": {
                    "name": "Test Track"
                  }
                }
              ]
            }
        """.trimIndent()

        val track = GeoJsonParser.parse(json)
        assertNotNull(track)
        assertEquals("Test Track", track.name)
        assertEquals(1, track.segments.size)
        assertEquals(4, track.segments[0].points.size)
        assertEquals(102.0, track.segments[0].points[0].longitude)
        assertEquals(0.0, track.segments[0].points[0].latitude)
    }

    @Test
    fun testGpxSerialization() {
        val track = Track(
            id = "1",
            name = "Test Track",
            segments = listOf(
                TrackSegment(
                    points = listOf(
                        TrackPoint(10.0, 20.0, 100.0, 1600000000000L),
                        TrackPoint(11.0, 21.0, 110.0, 1600000060000L)
                    )
                )
            )
        )

        val gpx = GpxParser.serialize(track)
        assertNotNull(gpx)
        // Basic check for contents
        assertTrue(gpx.contains("<name>Test Track</name>"), "Should contain name")
        assertTrue(gpx.contains("lat=\"10"), "Should contain lat 10")
        assertTrue(gpx.contains("lon=\"20"), "Should contain lon 20")
        assertTrue(gpx.contains("<ele>100"), "Should contain elevation")
        assertTrue(gpx.contains("<time>2020-09-13T12:26:40Z</time>"), "Should contain time in ISO-8601 format")
    }
}
