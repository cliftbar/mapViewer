package site.cliftbar.mapviewer.config

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import net.mamoe.yamlkt.Yaml

actual val platformConfigPath: String? = NSBundle.mainBundle.pathForResource("config", "yaml")

@OptIn(ExperimentalForeignApi::class)
actual fun readFile(path: String): String? {
    return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
}

actual fun parseYamlConfig(content: String): Config? {
    return try {
        Yaml.decodeFromString(Config.serializer(), content)
    } catch (e: Exception) {
        null
    }
}
