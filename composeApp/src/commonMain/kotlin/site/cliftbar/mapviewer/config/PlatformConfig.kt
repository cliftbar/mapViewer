package site.cliftbar.mapviewer.config

expect val platformConfigPath: String?
expect fun readFile(path: String): String?
expect fun parseYamlConfig(content: String): Config?
