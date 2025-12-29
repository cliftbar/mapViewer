package site.cliftbar.mapviewer.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import platform.darwin.NSObject

class IosFilePicker : FilePicker {
    @OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
    override suspend fun pickFile(extensions: List<String>): String? {
        val deferred = CompletableDeferred<String?>()
        
        val types = extensions.mapNotNull { ext ->
            when (ext.lowercase()) {
                "gpx" -> UTType.typeWithFilenameExtension("gpx") ?: UTTypeContent
                "json" -> UTTypeJSON
                "geojson" -> UTType.typeWithFilenameExtension("geojson") ?: UTTypeContent
                else -> UTType.typeWithFilenameExtension(ext) ?: UTTypeContent
            }
        }

        val picker = UIDocumentPickerViewController(forOpeningContentTypes = types)
        picker.delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                if (url != null) {
                    val isAccessed = url.startAccessingSecurityScopedResource()
                    try {
                        val content = NSString.stringWithContentsOfURL(url, NSUTF8StringEncoding, null)
                        if (content == null) {
                            NSLog("[DEBUG_LOG] NSString.stringWithContentsOfURL returned null for URL: %@", url.absoluteString)
                            // Try reading as NSData as a fallback
                            val data = NSData.dataWithContentsOfURL(url)
                            if (data != null) {
                                val stringFromData = NSString.create(data = data, encoding = NSUTF8StringEncoding)
                                deferred.complete(stringFromData?.toString())
                            } else {
                                deferred.complete(null)
                            }
                        } else {
                            deferred.complete(content)
                        }
                    } catch (e: Throwable) {
                        NSLog("[DEBUG_LOG] Error reading file: %@", e.message)
                        deferred.complete(null)
                    } finally {
                        if (isAccessed) {
                            url.stopAccessingSecurityScopedResource()
                        }
                    }
                } else {
                    deferred.complete(null)
                }
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                deferred.complete(null)
            }
        }

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(picker, animated = true, completion = null)

        return deferred.await()
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun saveFile(filename: String, content: String) {
        val deferred = CompletableDeferred<Unit>()
        
        val tempDir = NSFileManager.defaultManager.temporaryDirectory
        val fileURL = tempDir.URLByAppendingPathComponent(filename)!!
        
        (content as NSString).writeToURL(fileURL, true, NSUTF8StringEncoding, null)

        val picker = UIDocumentPickerViewController(forExportingURLs = listOf(fileURL))
        picker.delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                deferred.complete(Unit)
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                deferred.complete(Unit)
            }
        }

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(picker, animated = true, completion = null)

        deferred.await()
    }
}

@Composable
actual fun rememberFilePicker(): FilePicker = remember { IosFilePicker() }
