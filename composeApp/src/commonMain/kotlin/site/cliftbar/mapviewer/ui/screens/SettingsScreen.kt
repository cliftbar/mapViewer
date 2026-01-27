package site.cliftbar.mapviewer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import androidx.compose.foundation.clickable
import site.cliftbar.mapviewer.config.AppTheme

import cafe.adriel.voyager.core.model.rememberScreenModel
import site.cliftbar.mapviewer.ui.viewmodels.SettingsScreenModel
import site.cliftbar.mapviewer.tracks.stats.AvgSpeedBasis
import site.cliftbar.mapviewer.tracks.stats.DistanceUnit
import site.cliftbar.mapviewer.tracks.stats.SpeedUnit
import site.cliftbar.mapviewer.tracks.stats.StoppedTimeAlgorithmId

class SettingsScreen : Tab {
    override val options: TabOptions
        @Composable
        get() = remember {
            TabOptions(
                index = 2u,
                title = "Settings"
            )
        }

    @Composable
    override fun Content() {
        val configRepository = site.cliftbar.mapviewer.LocalConfigRepository.current
        val screenModel = rememberScreenModel { SettingsScreenModel(configRepository) }
        val config by screenModel.activeConfig.collectAsState()
        var currentConfig by remember(config) { mutableStateOf(config) }
        var speedThresholdText by remember(currentConfig.stoppedTimeSpeedThresholdMps) {
            mutableStateOf(currentConfig.stoppedTimeSpeedThresholdMps.toString())
        }
        var minStopSecondsText by remember(currentConfig.stoppedTimeMinStopSeconds) {
            mutableStateOf(currentConfig.stoppedTimeMinStopSeconds.toString())
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            
            Spacer(modifier = Modifier.height(16.dp))

            // Zoom level setting
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Default Zoom: ${currentConfig.defaultZoom}")
                Slider(
                    value = currentConfig.defaultZoom.toFloat(),
                    onValueChange = { currentConfig = currentConfig.copy(defaultZoom = it.toInt()) },
                    valueRange = 0f..19f,
                    steps = 18
                )
            }

            // Offline mode toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Offline Mode")
                Switch(
                    checked = currentConfig.offlineMode,
                    onCheckedChange = { currentConfig = currentConfig.copy(offlineMode = it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Selection
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppTheme.values().forEach { theme ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { currentConfig = currentConfig.copy(theme = theme) }
                            .padding(horizontal = 8.dp)
                    ) {
                        RadioButton(
                            selected = currentConfig.theme == theme,
                            onClick = { currentConfig = currentConfig.copy(theme = theme) }
                        )
                        Text(theme.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("General", style = MaterialTheme.typography.titleMedium)
            SettingDropdown(
                label = "Distance units",
                options = DistanceUnit.values().toList(),
                selected = currentConfig.defaultDistanceUnit,
                optionLabel = { it.label }
            ) { unit ->
                currentConfig = currentConfig.copy(defaultDistanceUnit = unit)
            }
            SettingDropdown(
                label = "Speed units",
                options = SpeedUnit.values().toList(),
                selected = currentConfig.defaultSpeedUnit,
                optionLabel = { it.label }
            ) { unit ->
                currentConfig = currentConfig.copy(defaultSpeedUnit = unit)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Stats", style = MaterialTheme.typography.titleMedium)
            Text("Avg speed basis", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvgSpeedBasis.values().forEach { basis ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { currentConfig = currentConfig.copy(defaultAvgSpeedBasis = basis) }
                            .padding(horizontal = 8.dp)
                    ) {
                        RadioButton(
                            selected = currentConfig.defaultAvgSpeedBasis == basis,
                            onClick = { currentConfig = currentConfig.copy(defaultAvgSpeedBasis = basis) }
                        )
                        Text(basis.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() })
                    }
                }
            }

            SettingDropdown(
                label = "Stopped time algorithm",
                options = StoppedTimeAlgorithmId.values().toList(),
                selected = currentConfig.stoppedTimeAlgorithmId,
                optionLabel = { it.name.lowercase().replace('_', ' ') }
            ) { algo ->
                currentConfig = currentConfig.copy(stoppedTimeAlgorithmId = algo)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Speed threshold (m/s)", modifier = Modifier.width(180.dp))
                TextField(
                    value = speedThresholdText,
                    onValueChange = { text ->
                        speedThresholdText = text
                        text.toDoubleOrNull()?.let { value ->
                            currentConfig = currentConfig.copy(stoppedTimeSpeedThresholdMps = value)
                        }
                    },
                    modifier = Modifier.width(140.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Min stop seconds", modifier = Modifier.width(180.dp))
                TextField(
                    value = minStopSecondsText,
                    onValueChange = { text ->
                        minStopSecondsText = text
                        text.toIntOrNull()?.let { value ->
                            currentConfig = currentConfig.copy(stoppedTimeMinStopSeconds = value)
                        }
                    },
                    modifier = Modifier.width(140.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = { screenModel.saveConfig(currentConfig) }) {
                Text("Save Configuration")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Management
            Text("Profiles", style = MaterialTheme.typography.titleMedium)
            
            var newProfileName by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    label = { Text("New Profile Name") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            screenModel.saveConfig(currentConfig, newProfileName)
                            newProfileName = ""
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            screenModel.profiles.forEach { profile ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(profile, modifier = Modifier.weight(1f))
                    if (profile != "config") {
                        TextButton(onClick = {
                            screenModel.switchProfile(profile)
                        }) {
                            Text("Load")
                        }
                        IconButton(onClick = {
                            screenModel.deleteProfile(profile)
                        }) {
                            Text("X") // Use Icon later
                        }
                    } else {
                        Text("(Active)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    @Composable
    private fun <T> SettingDropdown(
        label: String,
        options: List<T>,
        selected: T,
        optionLabel: (T) -> String,
        onSelect: (T) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f))
            Box {
                TextButton(onClick = { expanded = true }) {
                    Text(optionLabel(selected))
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
}
