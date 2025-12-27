package site.cliftbar.mapviewer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import site.cliftbar.mapviewer.config.Config
import site.cliftbar.mapviewer.config.ConfigRepository

import androidx.compose.foundation.clickable
import site.cliftbar.mapviewer.config.AppTheme

class SettingsScreen(
    private val configRepository: ConfigRepository
) : Tab {
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
        val config by configRepository.activeConfig.collectAsState()
        var currentConfig by remember(config) { mutableStateOf(config) }

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

            Spacer(modifier = Modifier.weight(1f))

            Button(onClick = { configRepository.saveConfig(currentConfig) }) {
                Text("Save Configuration")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Management
            Text("Profiles", style = MaterialTheme.typography.titleMedium)
            
            val profiles = remember { mutableStateListOf<String>() }
            LaunchedEffect(Unit) {
                profiles.addAll(configRepository.getAllProfiles())
            }

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
                            configRepository.saveConfig(currentConfig, newProfileName)
                            if (!profiles.contains(newProfileName)) {
                                profiles.add(newProfileName)
                            }
                            newProfileName = ""
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Add")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            profiles.forEach { profile ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(profile, modifier = Modifier.weight(1f))
                    if (profile != "config") {
                        TextButton(onClick = {
                            configRepository.switchProfile(profile)
                        }) {
                            Text("Load")
                        }
                        IconButton(onClick = {
                            configRepository.deleteProfile(profile)
                            profiles.remove(profile)
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
}
