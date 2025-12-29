package site.cliftbar.mapviewer.platform

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidFilePicker(
    private val context: android.content.Context,
    private val pickLauncher: (Array<String>) -> Unit,
    private val saveLauncher: (String) -> Unit
) : FilePicker {
    private var pickDeferred: CompletableDeferred<String?>? = null
    private var saveDeferred: CompletableDeferred<Unit>? = null
    private var contentToSave: String? = null

    override suspend fun pickFile(extensions: List<String>): String? {
        pickDeferred = CompletableDeferred()
        
        val mimeTypes = extensions.map { ext ->
            when (ext.lowercase()) {
                "gpx" -> "application/gpx+xml"
                "json" -> "application/json"
                "geojson" -> "application/geo+json"
                else -> "*/*"
            }
        }.toTypedArray()
        
        val finalMimeTypes = if (mimeTypes.isEmpty()) arrayOf("*/*") else mimeTypes

        try {
            pickLauncher(finalMimeTypes)
        } catch (e: Exception) {
            e.printStackTrace()
            pickDeferred?.complete(null)
            pickDeferred = null
            return null
        }
        return pickDeferred?.await()
    }

    override suspend fun saveFile(filename: String, content: String) {
        saveDeferred = CompletableDeferred()
        contentToSave = content
        try {
            saveLauncher(filename)
        } catch (e: Exception) {
            e.printStackTrace()
            saveDeferred?.complete(Unit)
            saveDeferred = null
            contentToSave = null
            return
        }
        saveDeferred?.await()
    }

    fun onFilePicked(uri: Uri?) {
        if (uri == null) {
            pickDeferred?.complete(null)
            pickDeferred = null
            return
        }

        // Offload reading to IO thread to avoid ANR/Crash on main thread
        val deferred = pickDeferred
        pickDeferred = null
        
        // We can't easily launch a coroutine from here without a scope, 
        // but pickFile is already suspended and waiting.
        // Wait, pickFile is waiting on pickDeferred.await().
        // So we should do the reading and THEN complete it.
        
        // However, this callback is on the main thread.
        // We should probably pass the URI back and let pickFile handle the reading?
        // Yes, that's better.
        deferred?.complete(uri.toString())
    }

    fun onFileSelectedForSave(uri: Uri?) {
        val deferred = saveDeferred
        saveDeferred = null
        
        if (uri == null) {
            deferred?.complete(Unit)
            return
        }

        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(contentToSave?.toByteArray() ?: byteArrayOf())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        contentToSave = null
        deferred?.complete(Unit)
    }
}

@Composable
actual fun rememberFilePicker(): FilePicker {
    val context = LocalContext.current
    val pickerState = remember { mutableStateOf<AndroidFilePicker?>(null) }
    
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pickerState.value?.onFilePicked(uri)
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        pickerState.value?.onFileSelectedForSave(uri)
    }

    val filePicker = remember {
        AndroidFilePicker(
            context,
            pickLauncher = { mimeTypes -> pickLauncher.launch(mimeTypes) },
            saveLauncher = { filename -> saveLauncher.launch(filename) }
        )
    }
    
    // Use an effect to keep the pickerState in sync
    SideEffect {
        pickerState.value = filePicker
    }

    // Wrap the picker to handle reading on Dispatchers.IO
    return remember(filePicker) {
        object : FilePicker {
            override suspend fun pickFile(extensions: List<String>): String? {
                val uriString = filePicker.pickFile(extensions) ?: return null
                return withContext(Dispatchers.IO) {
                    try {
                        val uri = Uri.parse(uriString)
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }

            override suspend fun saveFile(filename: String, content: String) {
                filePicker.saveFile(filename, content)
            }
        }
    }
}
