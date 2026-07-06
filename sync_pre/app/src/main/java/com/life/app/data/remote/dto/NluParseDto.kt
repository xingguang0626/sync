package com.life.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 自然语言解析响应。
 * 与 Python 后端 POST /api/nlu/parse 返回结构对齐。
 *
 * 返回结构：
 * {
 *   "ok": true,
 *   "data": {
 *     "intent": "create_schedule",
 *     "draft": { ... },
 *     "confidence": 0.95,
 *     "parsed_details": { ... },
 *     "need_confirmation": true
 *   }
 * }
 */
@Serializable
data class NluParseResponseDto(
    @SerialName("ok") val ok: Boolean = false,
    @SerialName("data") val data: NluParseDataDto? = null,
    @SerialName("error") val error: String? = null
)

@Serializable
data class NluParseDataDto(
    @SerialName("intent") val intent: String,                           // create_schedule / delete_schedule / ...
    @SerialName("draft") val draft: kotlinx.serialization.json.JsonElement? = null, // 结构随 intent 变化
    @SerialName("confidence") val confidence: Float = 0f,
    @SerialName("parsed_details") val parsedDetails: ParsedDetailsDto = ParsedDetailsDto(),
    @SerialName("need_confirmation") val needConfirmation: Boolean = true
)

@Serializable
data class ParsedDetailsDto(
    @SerialName("date_raw") val dateRaw: String? = null,
    @SerialName("time_raw") val timeRaw: String? = null,
    @SerialName("duration_raw") val durationRaw: String? = null,
    @SerialName("action_raw") val actionRaw: String? = null,
    @SerialName("raw_original") val rawOriginal: String? = null,
    @SerialName("raw") val raw: String? = null
)