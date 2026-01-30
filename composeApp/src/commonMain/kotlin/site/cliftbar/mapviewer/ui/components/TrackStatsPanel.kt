package site.cliftbar.mapviewer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
    allowOverrides: Boolean = false,
    alignEnd: Boolean = false,
    compact: Boolean = false,
    fillWidth: Boolean = true
) {
    val calculator = remember { TrackStatsCalculator() }
    val stats = calculator.computeStats(track, context)
    val currentPrefs = prefs ?: TrackStatsPrefs(trackId = track.id)
    var showOverrides by remember { mutableStateOf(false) }
    var showAvgSpeedMenu by remember { mutableStateOf(false) }
    val avgSpeedLabel = when (context.avgSpeedBasis) {
        AvgSpeedBasis.TOTAL_TIME -> "Total time"
        AvgSpeedBasis.MOVING_TIME -> "Moving time"
    }
    val rowArrangement = if (alignEnd) {
        Arrangement.spacedBy(12.dp, Alignment.End)
    } else {
        Arrangement.SpaceBetween
    }
    val textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    val labelStyle = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall
    val valueStyle = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
    val verticalPadding = if (compact) 2.dp else 6.dp
    val contentPadding = if (compact) 8.dp else 12.dp
    val containerModifier = if (fillWidth) {
        modifier.fillMaxWidth()
    } else {
        modifier.wrapContentWidth(if (alignEnd) Alignment.End else Alignment.Start)
    }

    Column(
        modifier = containerModifier.padding(contentPadding),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        if (showHeader) {
            Text(track.name, style = MaterialTheme.typography.titleMedium)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Avg speed", style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge)
            Box(modifier = Modifier.padding(start = 8.dp)) {
                TextButton(onClick = { showAvgSpeedMenu = true }) {
                    Text(avgSpeedLabel)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = showAvgSpeedMenu,
                    onDismissRequest = { showAvgSpeedMenu = false }
                ) {
                    AvgSpeedBasis.values().forEach { basis ->
                        DropdownMenuItem(
                            text = { Text(basis.name.lowercase().replace('_', ' ')) },
                            onClick = {
                                onUpdatePrefs(currentPrefs.copy(avgSpeedBasis = basis))
                                showAvgSpeedMenu = false
                            }
                        )
                    }
                }
            }
        }

        stats.forEach { stat ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = verticalPadding),
                horizontalArrangement = rowArrangement
            ) {
                Text(
                    stat.definition.label,
                    style = labelStyle,
                    textAlign = textAlign
                )
                Text(
                    stat.formattedValue,
                    style = valueStyle,
                    textAlign = textAlign
                )
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
