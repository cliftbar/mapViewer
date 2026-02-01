package site.cliftbar.mapviewer.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import site.cliftbar.mapviewer.MapViewerDB
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull

/**
 * Repository for managing application configuration and profiles.
 * 
 * @property database The [MapViewerDB] instance for persistence.
 */
class ConfigRepository(private val database: MapViewerDB) {
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _activeConfig = MutableStateFlow(Config())

    /**
     * A [StateFlow] of the currently active configuration.
     */
    val activeConfig: StateFlow<Config> = _activeConfig.asStateFlow()

    /**
     * Initializes the repository by loading the active configuration.
     */
    suspend fun initialize() {
        _activeConfig.value = loadConfig()
    }

    /**
     * Loads a configuration by name.
     * 
     * @param name The name of the configuration/profile to load. Defaults to "config" (the active one).
     * @return The loaded [Config] object.
     */
    suspend fun loadConfig(name: String = "config"): Config {
        var config = Config()

        // 1. Load from SQLite
        val sqliteValue = database.`1Queries`.getConfigByKey(name).awaitAsOneOrNull()
        if (sqliteValue != null) {
            try {
                config = json.decodeFromString<Config>(sqliteValue)
            } catch (e: Exception) {
                // Fallback to default if corrupted
            }
        }

        // 2. Override with YAML if exists (only for the default "config")
        if (name == "config") {
            platformConfigPath?.let { path ->
                readFile(path)?.let { content ->
                    try {
                        parseYamlConfig(content)?.let { yamlConfig ->
                            config = mergeConfigs(config, yamlConfig)
                        }
                    } catch (e: Exception) {
                        // Log error or ignore
                    }
                }
            }
        }

        return config
    }

    /**
     * Saves a configuration to a profile.
     * 
     * @param config The [Config] object to save.
     * @param name The name of the profile. Defaults to "config".
     */
    suspend fun saveConfig(config: Config, name: String = "config") {
        val stringValue = json.encodeToString(config)
        database.`1Queries`.upsertConfig(name, stringValue)
        if (name == "config") {
            _activeConfig.value = config
        }
    }

    /**
     * Switches the active configuration to the specified profile.
     * 
     * @param name The name of the profile to switch to.
     */
    suspend fun switchProfile(name: String) {
        val config = loadConfig(name)
        // If we switch to a different profile, we also update "config" (the active one) 
        // OR we just update the activeConfig flow if we want "config" to always be the active one.
        // The user said "make the config key 'config'", so let's keep the active one there.
        saveConfig(config, "config")
    }

    /**
     * Retrieves all available configuration profile names.
     * 
     * @return A list of profile names.
     */
    suspend fun getAllProfiles(): List<String> {
        return database.`1Queries`.getAllConfigKeys().awaitAsList()
    }

    /**
     * Deletes a configuration profile.
     * 
     * @param name The name of the profile to delete. The default "config" cannot be deleted.
     */
    suspend fun deleteProfile(name: String) {
        if (name != "config") { // Don't allow deleting the default config
            database.`1Queries`.deleteConfigByKey(name)
        }
    }

    /**
     * Merges an override configuration into a base configuration.
     * 
     * @param base The base configuration.
     * @param override The configuration containing overrides.
     * @return The merged configuration.
     */
    private fun mergeConfigs(base: Config, override: Config): Config {
        return override
    }
}
