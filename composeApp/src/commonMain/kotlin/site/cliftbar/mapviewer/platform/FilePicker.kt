package site.cliftbar.mapviewer.platform

import androidx.compose.runtime.Composable

interface FilePicker {
    suspend fun pickFile(extensions: List<String>): String? // Returns content or path? Let's say content for now.
    suspend fun saveFile(filename: String, content: String)
}

@Composable
expect fun rememberFilePicker(): FilePicker
