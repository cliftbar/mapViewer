package site.cliftbar.mapviewer.config

import java.io.File
import net.mamoe.yamlkt.Yaml

actual val platformConfigPath: String? = "config.yaml"

actual fun readFile(path: String): String? {
    val file = File(path)
    return if (file.exists()) file.readText() else null
}

actual fun parseYamlConfig(content: String): Config? {
    return try {
        Yaml.decodeFromString(Config.serializer(), content)
    } catch (e: Exception) {
        null
    }
}
