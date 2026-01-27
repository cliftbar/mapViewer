package site.cliftbar.mapviewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import site.cliftbar.mapviewer.tracks.Track
import site.cliftbar.mapviewer.tracks.stats.AvgSpeedBasis
import site.cliftbar.mapviewer.tracks.stats.DistanceUnit
import site.cliftbar.mapviewer.tracks.stats.SpeedUnit
import site.cliftbar.mapviewer.tracks.stats.StoppedTimeAlgorithmId
import site.cliftbar.mapviewer.tracks.stats.TrackStatsCalculator
import site.cliftbar.mapviewer.tracks.stats.TrackStatsContext
import site.cliftbar.mapviewer.tracks.stats.TrackStatsPrefs

@Composable
fun TrackStatsPanel(
    track: Track,
    context: TrackStatsContext,
    prefs: TrackStatsPrefs?,
    onUpdatePrefs: (TrackStatsPrefs) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    allowOverrides: Boolean = false
) {
    val calculator = remember { TrackStatsCalculator() }
    val stats = remember(track, context) { calculator.computeStats(track, context) }
    val currentPrefs = prefs ?: TrackStatsPrefs(trackId = track.id)
    var showOverrides by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth().padding(12.dp)) {
        if (showHeader) {
            Text(track.name, style = MaterialTheme.typography.titleMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Avg speed", style = MaterialTheme.typography.labelLarge)
            AvgSpeedBasis.values().forEach { basis ->
                FilterChip(
                    selected = context.avgSpeedBasis == basis,
                    onClick = { onUpdatePrefs(currentPrefs.copy(avgSpeedBasis = basis)) },
                    label = { Text(basis.name.lowercase().replace('_', ' ')) }
                )
            }
        }

        stats.forEach { stat ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stat.definition.label, style = MaterialTheme.typography.bodySmall)
                Text(stat.formattedValue, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (allowOverrides) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showOverrides = !showOverrides }) {
                    Text(if (showOverrides) "Hide overrides" else "Show overrides")
                }
                OutlinedButton(onClick = { onUpdatePrefs(TrackStatsPrefs(trackId = track.id)) }) {
                    Text("Reset overrides")
                }
            }

            if (showOverrides) {
                OverrideDropdown(
                    label = "Distance units",
                    options = DistanceUnit.values().toList(),
                    selected = currentPrefs.distanceUnit,
                    defaultLabel = context.distanceUnit.label,
                    optionLabel = { it.label }
                ) { unit ->
                    onUpdatePrefs(currentPrefs.copy(distanceUnit = unit))
                }

                OverrideDropdown(
                    label = "Speed units",
                    options = SpeedUnit.values().toList(),
                    selected = currentPrefs.speedUnit,
                    defaultLabel = context.speedUnit.label,
                    optionLabel = { it.label }
                ) { unit ->
                    onUpdatePrefs(currentPrefs.copy(speedUnit = unit))
                }

                OverrideDropdown(
                    label = "Stopped time algorithm",
                    options = StoppedTimeAlgorithmId.values().toList(),
                    selected = currentPrefs.stoppedAlgorithmId,
                    defaultLabel = context.stoppedAlgorithmId.name.lowercase().replace('_', ' '),
                    optionLabel = { it.name.lowercase().replace('_', ' ') }
                ) { algorithm ->
                    onUpdatePrefs(currentPrefs.copy(stoppedAlgorithmId = algorithm))
                }

                var thresholdText by remember(currentPrefs.stoppedSpeedThresholdMps) {
                    mutableStateOf((currentPrefs.stoppedSpeedThresholdMps ?: context.stoppedSpeedThresholdMps).toString())
                }
                var minStopText by remember(currentPrefs.stoppedMinStopSeconds) {
                    mutableStateOf((currentPrefs.stoppedMinStopSeconds ?: context.stoppedMinStopSeconds).toString())
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Speed threshold (m/s)", modifier = Modifier.weight(1f))
                    TextField(
                        value = thresholdText,
                        onValueChange = { text ->
                            thresholdText = text
                            text.toDoubleOrNull()?.let { value ->
                                onUpdatePrefs(currentPrefs.copy(stoppedSpeedThresholdMps = value))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Min stop seconds", modifier = Modifier.weight(1f))
                    TextField(
                        value = minStopText,
                        onValueChange = { text ->
                            minStopText = text
                            text.toIntOrNull()?.let { value ->
                                onUpdatePrefs(currentPrefs.copy(stoppedMinStopSeconds = value))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> OverrideDropdown(
    label: String,
    options: List<T>,
    selected: T?,
    defaultLabel: String,
    optionLabel: (T) -> String,
    onSelect: (T?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(selected?.let(optionLabel) ?: "Default ($defaultLabel)")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Default ($defaultLabel)") },
                    onClick = {
                        onSelect(null)
                        expanded = false
                    }
                )
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
