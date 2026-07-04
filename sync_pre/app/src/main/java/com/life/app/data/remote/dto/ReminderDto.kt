package com.life.app.data.remote.dto

import com.life.app.domain.model.Reminder
import com.life.app.domain.model.ReminderType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReminderListResponseDto(
    @SerialName("ok") val ok: Boolean = false,
    @SerialName("data") val data: List<ReminderDto>? = null,
    @SerialName("error") val error: String? = null
)

// Reminder 在 domain/model/Reminder.kt 已经 @Serializable，
// 这里不需要单独的 ReminderDto，Ktor 直接反序列化到 domain.Reminder。
// 但如果想完全隔离 domain 和 DTO，可以在这里复制一份并加 mapper。
typealias ReminderDto = Reminder