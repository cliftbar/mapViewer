package site.cliftbar.mapviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JvmFilePicker : FilePicker {
    override suspend fun pickFile(extensions: List<String>): String? = withContext(Dispatchers.IO) {
        val chooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Tracks", *extensions.toTypedArray())
        }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.readText()
        } else {
            null
        }
    }

    override suspend fun saveFile(filename: String, content: String) = withContext(Dispatchers.IO) {
        val chooser = JFileChooser().apply {
            selectedFile = File(filename)
        }
        val result = chooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.writeText(content)
        }
    }
}

@Composable
actual fun rememberFilePicker(): FilePicker = remember { JvmFilePicker() }
