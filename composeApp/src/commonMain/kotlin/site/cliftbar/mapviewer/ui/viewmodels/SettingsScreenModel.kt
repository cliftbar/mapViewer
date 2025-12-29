package site.cliftbar.mapviewer.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import site.cliftbar.mapviewer.config.Config
import site.cliftbar.mapviewer.config.ConfigRepository

class SettingsScreenModel(
    private val configRepository: ConfigRepository
) : ScreenModel {
    val activeConfig = configRepository.activeConfig
    
    val profiles = mutableStateListOf<String>()

    init {
        refreshProfiles()
    }

    fun refreshProfiles() {
        screenModelScope.launch {
            profiles.clear()
            profiles.addAll(configRepository.getAllProfiles())
        }
    }

    fun saveConfig(config: Config, profileName: String? = null) {
        screenModelScope.launch {
            if (profileName != null) {
                configRepository.saveConfig(config, profileName)
                if (!profiles.contains(profileName)) {
                    profiles.add(profileName)
                }
            } else {
                configRepository.saveConfig(config)
            }
        }
    }

    fun switchProfile(profileName: String) {
        screenModelScope.launch {
            configRepository.switchProfile(profileName)
        }
    }

    fun deleteProfile(profileName: String) {
        screenModelScope.launch {
            configRepository.deleteProfile(profileName)
            profiles.remove(profileName)
        }
    }
}
