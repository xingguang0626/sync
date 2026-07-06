package com.life.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 创建日程的请求体。
 * 字段名使用 snake_case，与 Python 后端 POST /api/schedules 对齐。
 */
@Serializable
data class CreateScheduleDto(
    @SerialName("title")      val title: String,
    @SerialName("date")       val date: String,
    @SerialName("start_time") val startTime: String,
    @SerialName("duration")   val duration: Int,
    @SerialName("priority")   val priority: String,
    @SerialName("repeat")     val repeat: String = "none",
    @SerialName("note")       val note: String = ""
)
