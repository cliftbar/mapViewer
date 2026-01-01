package site.cliftbar.mapviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import kotlinx.coroutines.CompletableDeferred
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.FileReader
import org.w3c.files.get

import kotlin.js.ExperimentalWasmJsInterop

@Composable
actual fun rememberFilePicker(): FilePicker = remember { WebFilePicker() }

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("function(blob) { return URL.createObjectURL(blob); }")
external fun createObjectURL(blob: Blob): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("function(url) { URL.revokeObjectURL(url); }")
external fun revokeObjectURL(url: String): Unit

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("function(content) { return new Blob([content]); }")
external fun createBlob(content: String): Blob

@OptIn(ExperimentalWasmJsInterop::class)
class WebFilePicker : FilePicker {
    override suspend fun pickFile(extensions: List<String>): String? {
        val deferred = CompletableDeferred<String?>()
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = extensions.joinToString(",") { ".$it" }

        input.onchange = {
            val file = input.files?.get(0)
            if (file != null) {
                val reader = FileReader()
                reader.onload = {
                    deferred.complete(reader.result.toString())
                }
                reader.onerror = {
                    deferred.complete(null)
                }
                reader.readAsText(file)
            } else {
                deferred.complete(null)
            }
        }

        input.click()
        return deferred.await()
    }

    override suspend fun saveFile(filename: String, content: String) {
        val blob = createBlob(content)
        val url = createObjectURL(blob)
        val link = document.createElement("a") as HTMLAnchorElement
        link.href = url
        link.download = filename
        link.click()
        revokeObjectURL(url)
    }
}
