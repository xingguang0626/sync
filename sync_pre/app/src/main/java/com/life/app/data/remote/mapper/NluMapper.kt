package com.life.app.data.remote.mapper

import com.life.app.data.remote.dto.NluParseDataDto
import com.life.app.data.remote.dto.NluParseResponseDto
import com.life.app.data.remote.ApiResult
import com.life.app.ui.home.components.NluDraftPreview
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * NLU 解析响应 → UI 层 NluDraftPreview 的转换。
 * 只处理 create_schedule 场景；其它意图返回带空字段的预览 + unknown 标记。
 */
private val json = Json { ignoreUnknownKeys = true; isLenient = true }

fun NluParseResponseDto.toApiResult(): ApiResult<NluParseDataDto> = when {
    ok && data != null -> ApiResult.Success(data)
    else -> ApiResult.Failure(error ?: "解析失败")
}

/**
 * 把后端 draft 转成 UI 用的 NluDraftPreview。
 * 若意图不是 create_schedule，返回"低置信度 + 引导手动填写"的占位预览。
 */
fun NluParseDataDto.toDraftPreview(): NluDraftPreview {
    if (intent != "create_schedule") {
        return NluDraftPreview(
            title = "",
            date = "",
            startTime = "",
            endTime = "",
            priority = "",
            repeat = "无",
            confidence = confidence,
            uncertainFields = setOf("title", "time", "priority")
        )
    }

    val obj: JsonObject = runCatching { draft?.jsonObject ?: return lowConfidence() }.getOrElse { return lowConfidence() }
    val title       = obj["title"].stringOr("")
    val date        = obj["date"].stringOr("")
    val startTime   = obj["start_time"].stringOr("")
    val endTime     = obj["end_time"].stringOr("")
    val priority    = obj["priority"].stringOr("P1")
    val repeat      = obj["repeat"].stringOr("none")
    val durationRaw = obj["duration"].stringOr("").ifBlank { null }

    // uncertainFields：parsed_details 缺失的字段视为不确定
    val uncertain = mutableSetOf<String>()
    if (parsedDetails.dateRaw.isNullOrBlank()) uncertain += "time"
    if (parsedDetails.timeRaw.isNullOrBlank()) uncertain += "time"
    if (parsedDetails.durationRaw.isNullOrBlank() && durationRaw.isNullOrBlank()) uncertain += "time"
    if (priority == "P1" && parsedDetails.actionRaw.isNullOrBlank()) uncertain += "priority"

    val repeatDisplay = when (repeat) {
        "daily"    -> "每天"
        "weekly"   -> "每周"
        "weekdays" -> "工作日"
        "weekends" -> "周末"
        "monthly"  -> "每月"
        else       -> "无"
    }

    return NluDraftPreview(
        title = title.ifBlank { "新日程" },
        date = date,
        startTime = startTime,
        endTime = endTime.ifBlank { startTime },
        priority = priority,
        repeat = repeatDisplay,
        confidence = confidence,
        uncertainFields = uncertain
    )
}

private fun NluParseDataDto.lowConfidence(): NluDraftPreview =
    NluDraftPreview(
        title = "",
        date = "",
        startTime = "",
        endTime = "",
        priority = "P1",
        repeat = "无",
        confidence = confidence,
        uncertainFields = setOf("title", "time", "priority")
    )

private fun JsonElement?.stringOr(default: String): String =
    this?.jsonPrimitive?.contentOrNull ?: default