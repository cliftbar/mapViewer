package site.cliftbar.mapviewer.tracks.stats

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import site.cliftbar.mapviewer.tracks.Track
import site.cliftbar.mapviewer.tracks.TrackPoint
import site.cliftbar.mapviewer.tracks.TrackSegment

class TrackStatsCalculatorTest {
    private val calculator = TrackStatsCalculator()

    @Test
    fun singlePointTrackHasNoSpeedStats() {
        val track = Track(
            id = "t1",
            name = "Single",
            segments = listOf(
                TrackSegment(points = listOf(TrackPoint(45.0, -122.0, elevation = 10.0, time = 0L)))
            )
        )
        val stats = calculator.computeStats(track, defaultContext())
        val results = stats.associateBy { it.definition.id }

        assertNull(results[TrackStatId.SPEED_MIN]?.value)
        assertNull(results[TrackStatId.SPEED_MAX]?.value)
        assertNull(results[TrackStatId.SPEED_AVG]?.value)
        assertEquals(10.0, results[TrackStatId.ELEVATION_MIN]?.value)
        assertEquals(10.0, results[TrackStatId.ELEVATION_MAX]?.value)
        assertNull(results[TrackStatId.ELEVATION_NET]?.value)
    }

    @Test
    fun missingTimestampsSkipsSpeedSamples() {
        val track = Track(
            id = "t2",
            name = "Missing Times",
            segments = listOf(
                TrackSegment(
                    points = listOf(
                        TrackPoint(45.0, -122.0, time = null),
                        TrackPoint(45.0001, -122.0001, time = 1000L)
                    )
                )
            )
        )
        val stats = calculator.computeStats(track, defaultContext())
        val avgSpeed = stats.first { it.definition.id == TrackStatId.SPEED_AVG }
        assertNull(avgSpeed.value)
    }

    @Test
    fun missingElevationsReturnNullElevationStats() {
        val track = Track(
            id = "t2-elev",
            name = "No Elevations",
            segments = listOf(
                TrackSegment(
                    points = listOf(
                        TrackPoint(45.0, -122.0, elevation = null, time = 0L),
                        TrackPoint(45.0001, -122.0001, elevation = null, time = 1000L)
                    )
                )
            )
        )
        val stats = calculator.computeStats(track, defaultContext())
        val results = stats.associateBy { it.definition.id }
        assertNull(results[TrackStatId.ELEVATION_MIN]?.value)
        assertNull(results[TrackStatId.ELEVATION_MAX]?.value)
        assertNull(results[TrackStatId.ELEVATION_GAIN]?.value)
        assertNull(results[TrackStatId.ELEVATION_LOSS]?.value)
        assertNull(results[TrackStatId.ELEVATION_NET]?.value)
    }

    @Test
    fun stoppedTimeBlocksRespectMinimumDuration() {
        val track = Track(
            id = "t3",
            name = "Stops",
            segments = listOf(
                TrackSegment(
                    points = listOf(
                        TrackPoint(45.0, -122.0, time = 0L),
                        TrackPoint(45.0, -122.0, time = 60000L),
                        TrackPoint(45.0, -122.0, time = 120000L)
                    )
                )
            )
        )
        val stats = calculator.computeStats(track, defaultContext())
        val stopped = stats.first { it.definition.id == TrackStatId.STOPPED_TIME }
        assertNotNull(stopped.value)
        assertEquals(120.0, stopped.value, 0.5)
    }

    @Test
    fun avgSpeedMovingTimeIsHigherThanTotalTimeWhenStopped() {
        val track = Track(
            id = "t4",
            name = "Moving vs Total",
            segments = listOf(
                TrackSegment(
                    points = listOf(
                        TrackPoint(45.0, -122.0, time = 0L),
                        TrackPoint(45.0, -121.999, time = 1000L),
                        TrackPoint(45.0, -121.999, time = 121000L)
                    )
                )
            )
        )
        val totalContext = defaultContext().copy(avgSpeedBasis = AvgSpeedBasis.TOTAL_TIME)
        val movingContext = defaultContext().copy(avgSpeedBasis = AvgSpeedBasis.MOVING_TIME)

        val totalAvg = calculator.computeStats(track, totalContext)
            .first { it.definition.id == TrackStatId.SPEED_AVG }.value
        val movingAvg = calculator.computeStats(track, movingContext)
            .first { it.definition.id == TrackStatId.SPEED_AVG }.value

        assertNotNull(totalAvg)
        assertNotNull(movingAvg)
        assertTrue(movingAvg > totalAvg)
    }

    @Test
    fun testInternationalDateLineCrossing() {
        val track = Track(
            id = "idl",
            name = "IDL Crossing",
            segments = listOf(
                TrackSegment(
                    points = listOf(
                        TrackPoint(0.0, 179.99, time = 0L),
                        TrackPoint(0.0, -179.99, time = 1000L)
                    )
                )
            )
        )
        // Distance should be small, not halfway around the world
        val stats = calculator.computeStats(track, defaultContext())
        val avgSpeed = stats.first { it.definition.id == TrackStatId.SPEED_AVG }.value
        assertNotNull(avgSpeed)
        // Distance is ~2224 meters. Time is 1s. Speed ~2224 m/s.
        // If it failed and went the long way, distance would be ~40,000 km. Speed ~40,000,000 m/s.
        assertTrue(avgSpeed < 5000.0, "Speed $avgSpeed is too high, likely went the long way around IDL")
    }

    @Test
    fun testHighFrequencyData() {
        val track = Track(
            id = "hf",
            name = "10Hz Data",
            segments = listOf(
                TrackSegment(
                    points = (0 until 100).map { i ->
                        TrackPoint(45.0, -122.0 + i * 0.000001, time = i * 100L) // 100ms intervals
                    }
                )
            )
        )
        val stats = calculator.computeStats(track, defaultContext())
        val avgSpeed = stats.first { it.definition.id == TrackStatId.SPEED_AVG }.value
        assertNotNull(avgSpeed)
        assertTrue(avgSpeed > 0.0)
    }

    private fun defaultContext(): TrackStatsContext {
        return TrackStatsContext(
            avgSpeedBasis = AvgSpeedBasis.MOVING_TIME,
            stoppedAlgorithmId = StoppedTimeAlgorithmId.SPEED_THRESHOLD,
            stoppedSpeedThresholdMps = 0.1,
            stoppedMinStopSeconds = 120,
            distanceUnit = DistanceUnit.METERS,
            speedUnit = SpeedUnit.MPS
        )
    }
}
