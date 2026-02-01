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

        val result = GeoJsonParser.parse(json)
        assertTrue(result is ParserResult.Success)
        val tracks = result.tracks
        assertEquals(1, tracks.size)
        val track = tracks[0]
        assertEquals("Test Track", track.name)
        assertEquals(1, track.segments.size)
        assertEquals(4, track.segments[0].points.size)
        assertEquals(102.0, track.segments[0].points[0].longitude)
        assertEquals(0.0, track.segments[0].points[0].latitude)
    }

    @Test
    fun testGpxMultipleTracksParsing() {
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx xmlns="http://www.topografix.com/GPX/1/1" version="1.1" creator="Test">
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

        val result = GpxParser.parse(gpx)
        assertTrue(result is ParserResult.Success)
        val tracks = result.tracks
        assertEquals(2, tracks.size)
        assertEquals("Track 1", tracks[0].name)
        assertEquals("Track 2", tracks[1].name)
    }

    @Test
    fun testGpxNamespaceParsing() {
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx xmlns="http://www.topografix.com/GPX/1/1" version="1.1" creator="Test">
              <trk>
                <name>Track with Namespace</name>
                <trkseg><trkpt lat="45.0" lon="-122.0"/></trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val result = GpxParser.parse(gpx)
        assertTrue(result is ParserResult.Success)
        val tracks = result.tracks
        assertEquals(1, tracks.size)
        assertEquals("Track with Namespace", tracks[0].name)
    }

    @Test
    fun testGpxPrefixedNamespaceParsing() {
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <p:gpx xmlns:p="http://www.topografix.com/GPX/1/1" version="1.1" creator="Test">
              <p:trk>
                <p:name>Track with Prefix</p:name>
                <p:trkseg><p:trkpt lat="45.0" lon="-122.0"/></p:trkseg>
              </p:trk>
            </p:gpx>
        """.trimIndent()

        val result = GpxParser.parse(gpx)
        assertTrue(result is ParserResult.Success)
        val tracks = result.tracks
        assertEquals(1, tracks.size)
        assertEquals("Track with Prefix", tracks[0].name)
    }

    @Test
    fun testGeoJsonMultipleFeaturesParsing() {
        val json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": { "type": "LineString", "coordinates": [[102.0, 0.0], [103.0, 1.0]] },
                  "properties": { "name": "Track 1" }
                },
                {
                  "type": "Feature",
                  "geometry": { "type": "LineString", "coordinates": [[104.0, 2.0], [105.0, 3.0]] },
                  "properties": { "name": "Track 2" }
                }
              ]
            }
        """.trimIndent()

        val result = GeoJsonParser.parse(json)
        assertTrue(result is ParserResult.Success)
        val tracks = result.tracks
        assertEquals(2, tracks.size)
        assertEquals("Track 1", tracks[0].name)
        assertEquals("Track 2", tracks[1].name)
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

    @Test
    fun testGpxLargeFileParsing() {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?><gpx xmlns="http://www.topografix.com/GPX/1/1" version="1.1" creator="Test"><trk><name>Large Track</name><trkseg>""")
        // Generate ~10,000 points to create a reasonably large file without timing out on JS
        for (i in 0 until 10000) {
            sb.append("""<trkpt lat="${45.0 + i * 0.0001}" lon="${-122.0 + i * 0.0001}"><ele>${100.0 + i}</ele><time>2024-01-01T00:00:00Z</time></trkpt>""")
        }
        sb.append("""</trkseg></trk></gpx>""")
        val largeGpx = sb.toString()

        val result = GpxParser.parse(largeGpx)
        assertTrue(result is ParserResult.Success)
        assertEquals(1, result.tracks.size)
        assertEquals(10000, result.tracks[0].segments[0].points.size)
    }

    @Test
    fun testGpxMalformedXml() {
        val malformedGpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx xmlns="http://www.topografix.com/GPX/1/1" version="1.1">
              <trk>
                <name>Malformed
                <trkseg>
                  <trkpt lat="45.0" lon="-122.0"/>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val result = GpxParser.parse(malformedGpx)
        assertTrue(result is ParserResult.Error)
    }

    @Test
    fun testGeoJsonMalformedJson() {
        val malformedJson = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "LineString",
                    "coordinates": [[102.0, 0.0], [103.0, 1.0]
                  }
                }
              ]
            }
        """.trimIndent()

        val result = GeoJsonParser.parse(malformedJson)
        assertTrue(result is ParserResult.Error)
    }

    @Test
    fun testGpxNoTracks() {
        val gpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx xmlns="http://www.topografix.com/GPX/1/1" version="1.1" creator="Test">
            </gpx>
        """.trimIndent()

        val result = GpxParser.parse(gpx)
        assertTrue(result is ParserResult.Error)
        assertEquals("No tracks found in GPX data", result.message)
    }
}
