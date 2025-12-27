package site.cliftbar.mapviewer.tracks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
        println(gpx)
        // Basic check for contents
        assertEquals(true, gpx.contains("<name>Test Track</name>"))
        assertEquals(true, gpx.contains("lat=\"10.0\""))
        assertEquals(true, gpx.contains("lon=\"20.0\""))
        assertEquals(true, gpx.contains("<ele>100.0</ele>"))
        assertEquals(true, gpx.contains("<time>1600000000000</time>"))
    }
}
