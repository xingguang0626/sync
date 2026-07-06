package com.life.app.data.remote

import com.life.app.data.remote.dto.ApiResponseDto
import com.life.app.data.remote.dto.CreateScheduleDto
import com.life.app.data.remote.dto.LifestyleAdviceDto
import com.life.app.data.remote.dto.NluParseResponseDto
import com.life.app.data.remote.dto.NluRequestDto
import com.life.app.data.remote.dto.ReminderListResponseDto
import com.life.app.data.remote.dto.SettingsDto
import com.life.app.data.remote.dto.TimelineResponseDto
import com.life.app.domain.model.Schedule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

class KtorHomeApi(
    private val client: HttpClient
) : HomeApi {

    companion object {
        private const val PATH_TIMELINE = "/api/timeline"
        private const val PATH_REMINDERS_CHECK = "/api/reminders/check"
        private const val PATH_SCHEDULES = "/api/schedules"
        private const val PATH_NLU_PARSE = "/api/nlu/parse"
        private const val PATH_SETTINGS = "/api/settings"
        private const val PATH_LIFESTYLE_ADVICE = "/api/ai/lifestyle-advice"
    }

    override suspend fun getTimeline(date: String): TimelineResponseDto =
        client.get(PATH_TIMELINE) {
            parameter("date", date)
        }.body()

    override suspend fun checkReminders(date: String): ReminderListResponseDto =
        client.get(PATH_REMINDERS_CHECK) {
            parameter("date", date)
        }.body()

    override suspend fun createSchedule(data: CreateScheduleDto): ApiResponseDto<Schedule> =
        client.post(PATH_SCHEDULES) {
            setBody(data)
        }.body()

    override suspend fun getSchedule(id: Long): ApiResponseDto<Schedule> =
        client.get("$PATH_SCHEDULES/$id").body()

    // 第 4 阶段：改用标准 RESTful PUT 方法
    override suspend fun updateSchedule(id: Long, data: CreateScheduleDto): ApiResponseDto<Schedule> =
        client.put("$PATH_SCHEDULES/$id") {
            setBody(data)
        }.body()

    // 第 4 阶段：改用标准 RESTful DELETE 方法（去掉旧的 /delete 后缀）
    override suspend fun deleteSchedule(id: Long): ApiResponseDto<Unit> =
        client.delete("$PATH_SCHEDULES/$id").body()

    override suspend fun markComplete(id: Long): ApiResponseDto<Schedule> =
        client.post("$PATH_SCHEDULES/$id/complete").body()

    // 第 4 阶段新增：顺延日程（AI 提醒采纳建议时调用）
    override suspend fun postponeSchedule(id: Long, newDate: String?): ApiResponseDto<Schedule> =
        client.post("$PATH_SCHEDULES/$id/postpone") {
            setBody(mapOf("new_date" to newDate))
        }.body()

    override suspend fun parseNlu(text: String, todayDate: String): NluParseResponseDto =
        client.post(PATH_NLU_PARSE) {
            setBody(NluRequestDto(text = text, todayDate = todayDate))
        }.body()

    override suspend fun clearAll(): ApiResponseDto<Unit> =
        client.delete("$PATH_SCHEDULES/clear").body()

    override suspend fun getSettings(): SettingsDto =
        client.get(PATH_SETTINGS).body()

    override suspend fun updateSettings(body: Map<String, String>): SettingsDto =
        client.put(PATH_SETTINGS) {
            setBody(body)
        }.body()

    override suspend fun getLifestyleAdvice(): LifestyleAdviceDto =
        client.post(PATH_LIFESTYLE_ADVICE).body()
}