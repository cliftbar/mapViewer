package site.cliftbar.mapviewer.tracks.stats

interface StoppedTimeAlgorithm {
    val id: StoppedTimeAlgorithmId
    fun computeStoppedTimeSeconds(
        samples: List<SpeedSample>,
        speedThresholdMps: Double,
        minStopSeconds: Int
    ): Double
}

class SpeedThresholdStoppedTimeAlgorithm : StoppedTimeAlgorithm {
    override val id: StoppedTimeAlgorithmId = StoppedTimeAlgorithmId.SPEED_THRESHOLD

    override fun computeStoppedTimeSeconds(
        samples: List<SpeedSample>,
        speedThresholdMps: Double,
        minStopSeconds: Int
    ): Double {
        if (samples.isEmpty()) return 0.0
        var stoppedSeconds = 0.0
        var currentBlockSeconds = 0.0

        samples.forEach { sample ->
            if (sample.speedMps < speedThresholdMps) {
                currentBlockSeconds += sample.durationSeconds
            } else {
                if (currentBlockSeconds >= minStopSeconds) {
                    stoppedSeconds += currentBlockSeconds
                }
                currentBlockSeconds = 0.0
            }
        }

        if (currentBlockSeconds >= minStopSeconds) {
            stoppedSeconds += currentBlockSeconds
        }

        return stoppedSeconds
    }
}

object StoppedTimeAlgorithmRegistry {
    private val algorithms = listOf(
        SpeedThresholdStoppedTimeAlgorithm()
    )

    fun getAlgorithm(id: StoppedTimeAlgorithmId): StoppedTimeAlgorithm {
        return algorithms.firstOrNull { it.id == id } ?: algorithms.first()
    }

    fun listAlgorithms(): List<StoppedTimeAlgorithm> = algorithms
}
