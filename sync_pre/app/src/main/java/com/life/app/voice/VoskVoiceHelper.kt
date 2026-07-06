package com.life.app.voice

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer

class VoskVoiceHelper {

    companion object {
        private const val TAG = "VoskVoice"
        private const val SAMPLE_RATE = 16000f
    }

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var recognitionJob: Job? = null
    @Volatile private var isRunning = false
    private var currentRecorder: AudioRecord? = null

    fun initialize(modelPath: String): Boolean {
        return try {
            release()
            model = Model(modelPath)
            recognizer = Recognizer(model!!, SAMPLE_RATE)
            Log.d(TAG, "Vosk 初始化成功: $modelPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Vosk 初始化失败", e)
            false
        }
    }

    fun startListening(
        scope: CoroutineScope,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isRunning || recognizer == null) return
        isRunning = true

        recognitionJob = scope.launch(Dispatchers.IO) {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE.toInt(),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val recorder = try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE.toInt(),
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize * 2
                )
            } catch (e: SecurityException) {
                withContext(Dispatchers.Main) { onError("麦克风权限未授予") }
                isRunning = false
                return@launch
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError("录音初始化失败: ${e.localizedMessage}") }
                isRunning = false
                return@launch
            }

            currentRecorder = recorder
            var finalDelivered = false

            try {
                recorder.startRecording()
                val buffer = ShortArray(bufferSize)

                while (isRunning && isActive) {
                    val bytesRead = recorder.read(buffer, 0, buffer.size)
                    if (bytesRead < 0) break
                    if (bytesRead > 0) {
                        val accepted = recognizer!!.acceptWaveForm(buffer, bytesRead)
                        if (accepted) {
                            val result = JSONObject(recognizer!!.result)
                            val text = result.optString("text", "")
                            if (text.isNotBlank()) {
                                finalDelivered = true
                                withContext(Dispatchers.Main) { onFinalResult(text) }
                            }
                        } else {
                            val partial = JSONObject(recognizer!!.partialResult)
                            val text = partial.optString("partial", "")
                            if (text.isNotBlank()) {
                                withContext(Dispatchers.Main) { onPartialResult(text) }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "识别过程出错", e)
                withContext(Dispatchers.Main) { onError("识别出错: ${e.localizedMessage}") }
            } finally {
                try { recorder.stop() } catch (_: Exception) {}
                recorder.release()
                currentRecorder = null

                if (!finalDelivered) {
                    try {
                        val finalResult = JSONObject(recognizer!!.finalResult)
                        val text = finalResult.optString("text", "")
                        if (text.isNotBlank()) {
                            withContext(Dispatchers.Main) { onFinalResult(text) }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun stopListening() {
        isRunning = false
        currentRecorder?.let {
            try { it.stop() } catch (_: Exception) {}
        }
        recognitionJob?.cancel()
        recognitionJob = null
    }

    fun isActive(): Boolean = isRunning && recognizer != null

    fun release() {
        stopListening()
        try { recognizer?.close() } catch (_: Exception) {}
        try { model?.close() } catch (_: Exception) {}
        recognizer = null
        model = null
    }
}
