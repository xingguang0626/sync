package com.life.app.ui.home

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 基于 vivo ASR WebSocket 接口的语音识别封装（OkHttp 实现）。
 * 使用 AudioRecord 采集 PCM 16kHz 16bit 单声道，通过 OkHttp WebSocket 发送。
 */
class VoiceAsrHelper {

    companion object {
        private const val TAG = "VoiceAsr"
        private const val SAMPLE_RATE = 16000
        private const val APP_KEY = "sk-xuanji-2026241446-amRxSXBYT3BWUHFVR1lGdA=="
        private const val ASR_URL = "wss://api-ai.vivo.com.cn/asr/v2"
        private const val FRAME_SIZE = SAMPLE_RATE * 2 * 40 / 1000 // = 1280 字节
        private const val MAX_DURATION_SECONDS = 55
    }

    private val json = Json { ignoreUnknownKeys = true }

    sealed interface AsrState {
        data object Idle : AsrState
        data object Connecting : AsrState
        data object Listening : AsrState
        data class PartialResult(val text: String) : AsrState
        data class FinalResult(val text: String) : AsrState
        data class Error(val message: String) : AsrState
    }

    private val _state = MutableStateFlow<AsrState>(AsrState.Idle)
    val state: StateFlow<AsrState> = _state.asStateFlow()

    private var mainJob: Job? = null
    @Volatile private var ws: WebSocket? = null
    private var recorder: AudioRecord? = null

    fun start() {
        if (_state.value is AsrState.Connecting || _state.value is AsrState.Listening) return
        mainJob = kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            runAsr()
            _state.value = AsrState.Idle
        }
    }

    fun stop() {
        mainJob?.cancel()
        mainJob = null
        try { ws?.close(1000, "user cancel") } catch (_: Exception) {}
        ws = null
    }

    private suspend fun runAsr() = coroutineScope {
        _state.value = AsrState.Connecting

        // 1. AudioRecord
        val minBufSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBufSize, FRAME_SIZE * 2)
            )
        } catch (e: Exception) {
            _state.value = AsrState.Error("麦克风不可用：${e.message}")
            return@coroutineScope
        }
        this@VoiceAsrHelper.recorder = recorder
        recorder.startRecording()

        try {
            // 2. OkHttp WebSocket
            val userId = "u${UUID.randomUUID().toString().replace("-", "").take(32)}"
            val requestId = UUID.randomUUID().toString().replace("-", "").take(32)
            val systemTime = System.currentTimeMillis().toString()

            val wsUrl = buildString {
                append(ASR_URL)
                append("?model=unknown&system_version=unknown&client_version=unknown")
                append("&package=unknown&sdk_version=unknown&user_id=$userId")
                append("&android_version=unknown&system_time=$systemTime")
                append("&net_type=1&engineid=shortasrinput&requestId=$requestId")
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("Authorization", "Bearer $APP_KEY")
                .build()

            var resultText = ""
            var errorMsg: String? = null
            var connected = false
            var finished = false

            val listener = object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket 已连接")
                    connected = true
                    _state.value = AsrState.Listening

                    // 发送 started
                    val startJson = buildString {
                        append("{\"type\":\"started\",")
                        append("\"request_id\":\"$requestId\",")
                        append("\"asr_info\":{")
                        append("\"end_vad_time\":800,")
                        append("\"audio_type\":\"pcm\",")
                        append("\"chinese2digital\":1,")
                        append("\"punctuation\":1")
                        append("}}")
                    }
                    ws.send(startJson)
                    Log.d(TAG, "发送 started")
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    try {
                        val obj = json.parseToJsonElement(text) as? JsonObject ?: return
                        val action = obj["action"]?.jsonPrimitive?.content ?: ""
                        when (action) {
                            "result" -> {
                                val data = obj["data"] as? JsonObject
                                val t = data?.get("text")?.jsonPrimitive?.content ?: ""
                                val isLast = data?.get("is_last")?.jsonPrimitive?.boolean ?: false
                                val reformation = data?.get("reformation")?.jsonPrimitive?.int ?: 0
                                if (t.isNotBlank()) {
                                    resultText = if (reformation == 1) t else resultText + t
                                    _state.value = AsrState.PartialResult(resultText)
                                }
                                if (isLast) {
                                    _state.value = AsrState.FinalResult(resultText)
                                    finished = true
                                    ws.close(1000, "done")
                                }
                            }
                            "error" -> {
                                val desc = obj["desc"]?.jsonPrimitive?.content ?: "未知错误"
                                errorMsg = desc
                                _state.value = AsrState.Error(desc)
                                ws.close(1000, "error")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "解析消息失败: $text", e)
                    }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket 失败", t)
                    errorMsg = t.localizedMessage ?: "WebSocket 连接失败"
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket 关闭: $code $reason")
                }
            }

            ws = client.newWebSocket(request, listener)
            this@VoiceAsrHelper.ws = ws

            // 等待连接
            val startTime = System.currentTimeMillis()
            while (isActive && !connected && errorMsg == null) {
                if (System.currentTimeMillis() - startTime > 10_000) {
                    errorMsg = "连接超时"
                    break
                }
                delay(50)
            }

            if (errorMsg != null) {
                _state.value = AsrState.Error(errorMsg!!)
                return@coroutineScope
            }

            // 3. 发送音频帧
            val buffer = ByteArray(FRAME_SIZE)
            val recordStart = System.currentTimeMillis()
            while (isActive && ws != null && !finished && errorMsg == null) {
                if ((System.currentTimeMillis() - recordStart) / 1000 > MAX_DURATION_SECONDS) break

                val bytesRead = recorder.read(buffer, 0, FRAME_SIZE)
                if (bytesRead > 0) {
                    val frame = if (bytesRead == FRAME_SIZE) buffer else buffer.copyOf(bytesRead)
                    ws?.send(frame.toByteString())
                }
            }

            // 4. 发送结束
            if (!finished && errorMsg == null) {
                ws?.send("--end--".toByteArray().toByteString())
                // 等一会儿接收最终结果
                delay(1500)
            }

        } catch (e: kotlinx.coroutines.CancellationException) {
            // 用户主动取消 → 正常退出，不报错
        } catch (e: Exception) {
            Log.e(TAG, "ASR 异常", e)
            _state.value = AsrState.Error("语音识别失败：${e.localizedMessage}")
        } finally {
            try { recorder.stop() } catch (_: Exception) {}
            try { recorder.release() } catch (_: Exception) {}
            this@VoiceAsrHelper.recorder = null
            try { ws?.close(1000, "done") } catch (_: Exception) {}
            ws = null
        }
    }
}
