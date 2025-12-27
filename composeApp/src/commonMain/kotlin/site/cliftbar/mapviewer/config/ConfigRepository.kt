package site.cliftbar.mapviewer.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import site.cliftbar.mapviewer.MapViewerDB

class ConfigRepository(private val database: MapViewerDB) {
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _activeConfig = MutableStateFlow(loadConfig())
    val activeConfig: StateFlow<Config> = _activeConfig.asStateFlow()

    fun loadConfig(name: String = "config"): Config {
        var config = Config()

        // 1. Load from SQLite
        val sqliteValue = database.`1Queries`.getConfigByKey(name).executeAsOneOrNull()
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

    fun saveConfig(config: Config, name: String = "config") {
        val stringValue = json.encodeToString(config)
        database.`1Queries`.upsertConfig(name, stringValue)
        if (name == "config") {
            _activeConfig.value = config
        }
    }

    fun switchProfile(name: String) {
        val config = loadConfig(name)
        // If we switch to a different profile, we also update "config" (the active one) 
        // OR we just update the activeConfig flow if we want "config" to always be the active one.
        // The user said "make the config key 'config'", so let's keep the active one there.
        saveConfig(config, "config")
    }

    fun getAllProfiles(): List<String> {
        return database.`1Queries`.getAllConfigKeys().executeAsList()
    }

    fun deleteProfile(name: String) {
        if (name != "config") { // Don't allow deleting the default config
            database.`1Queries`.deleteConfigByKey(name)
        }
    }

    private fun mergeConfigs(base: Config, override: Config): Config {
        return override
    }
}
