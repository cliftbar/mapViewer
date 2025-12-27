package site.cliftbar.mapviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class WebFilePicker : FilePicker {
    override suspend fun pickFile(extensions: List<String>): String? = null
    override suspend fun saveFile(filename: String, content: String) {}
}

@Composable
actual fun rememberFilePicker(): FilePicker = remember { WebFilePicker() }
