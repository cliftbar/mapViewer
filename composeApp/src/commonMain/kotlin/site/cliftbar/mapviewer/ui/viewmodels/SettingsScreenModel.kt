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
) : BaseScreenModel() {
    /**
     * A [StateFlow] of the active configuration.
     */
    val activeConfig = configRepository.activeConfig
    
    /**
     * The list of available configuration profiles.
     */
    val profiles = mutableStateListOf<String>()

    init {
        refreshProfiles()
    }

    /**
     * Refreshes the list of available profiles from the repository.
     */
    fun refreshProfiles() {
        screenModelScope.launch(exceptionHandler) {
            profiles.clear()
            profiles.addAll(configRepository.getAllProfiles())
        }
    }

    /**
     * Saves the given configuration to a profile.
     * 
     * @param config The configuration to save.
     * @param profileName The name of the profile. If null, saves to the default "config".
     */
    fun saveConfig(config: Config, profileName: String? = null) {
        screenModelScope.launch(exceptionHandler) {
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

    /**
     * Switches the active configuration to the specified profile.
     * 
     * @param profileName The name of the profile to switch to.
     */
    fun switchProfile(profileName: String) {
        screenModelScope.launch(exceptionHandler) {
            configRepository.switchProfile(profileName)
        }
    }

    /**
     * Deletes the specified profile.
     * 
     * @param profileName The name of the profile to delete.
     */
    fun deleteProfile(profileName: String) {
        screenModelScope.launch(exceptionHandler) {
            configRepository.deleteProfile(profileName)
            profiles.remove(profileName)
        }
    }
}
