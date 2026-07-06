package com.life.app.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

data class ModelDownloadState(
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val isReady: Boolean = false,
    val error: String? = null
)

class VoskModelManager(private val context: Context) {

    companion object {
        private const val TAG = "VoskModel"
        private const val MODEL_NAME = "vosk-model-small-cn-0.22"
        private const val MODEL_URL =
            "https://alphacephei.com/vosk/models/$MODEL_NAME.zip"
    }

    private val _downloadState = MutableStateFlow(ModelDownloadState())
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()
    private val downloadLock = Any()

    val modelPath: String
        get() {
            val dir = context.getExternalFilesDir("vosk_models")
                ?: context.filesDir.resolve("vosk_models")
            return File(dir, MODEL_NAME).absolutePath
        }

    fun isModelDownloaded(): Boolean {
        val modelDir = File(modelPath)
        return modelDir.exists() && modelDir.isDirectory && modelDir.listFiles()?.isNotEmpty() == true
    }

    suspend fun ensureModel(): Boolean {
        if (isModelDownloaded()) {
            _downloadState.value = ModelDownloadState(isReady = true)
            return true
        }
        synchronized(downloadLock) {
            if (_downloadState.value.isDownloading) return false
            _downloadState.value = ModelDownloadState(isDownloading = true, progress = 0f)
        }
        return downloadModel()
    }

    private suspend fun downloadModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            val externalDir = context.getExternalFilesDir("vosk_models")
            val modelsDir = externalDir ?: File(context.filesDir, "vosk_models")
            if (!modelsDir.exists()) modelsDir.mkdirs()

            val zipFile = File(modelsDir, "$MODEL_NAME.zip")
            val url = URL(MODEL_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 60000

            try {
                connection.connect()
                val contentLength = connection.contentLength

                connection.inputStream.use { input ->
                    FileOutputStream(zipFile).use { output ->
                        val buffer = ByteArray(8192)
                        var totalRead = 0L
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                _downloadState.value = _downloadState.value.copy(
                                    progress = totalRead.toFloat() / contentLength
                                )
                            }
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }

            extractZip(zipFile, modelsDir)
            zipFile.delete()

            if (isModelDownloaded()) {
                _downloadState.value = ModelDownloadState(isReady = true)
                true
            } else {
                _downloadState.value = ModelDownloadState(error = "模型解压失败")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "模型下载失败", e)
            _downloadState.value = ModelDownloadState(error = e.localizedMessage ?: "下载失败")
            false
        }
    }

    private fun extractZip(zipFile: File, destDir: File) {
        val canonicalDest = destDir.canonicalPath
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val entryFile = File(destDir, entry.name)
                if (!entryFile.canonicalPath.startsWith(canonicalDest + File.separator)
                    && entryFile.canonicalPath != canonicalDest) {
                    Log.w(TAG, "跳过非法路径: ${entry.name}")
                    zis.closeEntry()
                    entry = zis.nextEntry
                    continue
                }
                if (entry.isDirectory) {
                    entryFile.mkdirs()
                } else {
                    entryFile.parentFile?.mkdirs()
                    FileOutputStream(entryFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
