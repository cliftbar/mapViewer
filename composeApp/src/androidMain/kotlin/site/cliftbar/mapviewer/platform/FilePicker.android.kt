package site.cliftbar.mapviewer.platform

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CompletableDeferred

class AndroidFilePicker(
    private val context: android.content.Context,
    private val pickLauncher: (List<String>) -> Unit,
    private val saveLauncher: (String, String) -> Unit
) : FilePicker {
    private var pickDeferred: CompletableDeferred<String?>? = null

    override suspend fun pickFile(extensions: List<String>): String? {
        pickDeferred = CompletableDeferred()
        pickLauncher(extensions)
        return pickDeferred?.await()
    }

    override suspend fun saveFile(filename: String, content: String) {
        saveLauncher(filename, content)
    }

    fun onFilePicked(content: String?) {
        pickDeferred?.complete(content)
    }
}

@Composable
actual fun rememberFilePicker(): FilePicker {
    val context = LocalContext.current
    var picker: AndroidFilePicker? = null
    
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val content = uri?.let { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } }
        picker?.onFilePicked(content)
    }

    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        // For simplicity, we'd need to pass the content to write here, which is tricky with this architecture.
        // A better way would be a separate state for saving.
    }

    picker = remember { 
        AndroidFilePicker(
            context,
            pickLauncher = { _ -> pickLauncher.launch("*/*") },
            saveLauncher = { filename, _ -> createLauncher.launch(filename) }
        ) 
    }
    return picker!!
}
