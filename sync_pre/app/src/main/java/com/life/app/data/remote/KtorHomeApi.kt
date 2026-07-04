package com.life.app.data.remote

import com.life.app.data.remote.dto.ReminderListResponseDto
import com.life.app.data.remote.dto.TimelineResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * 第二阶段启用。前端默认按 RESTful /api/* 风格约定路径；
 * 如果后端用别的命名（如 /timeline），改下面两个 endpoint 常量即可。
 */
class KtorHomeApi(
    private val client: HttpClient
) : HomeApi {

    companion object {
        private const val PATH_TIMELINE = "/api/timeline"
        private const val PATH_REMINDERS_CHECK = "/api/reminders/check"
    }

    override suspend fun getTimeline(date: String): TimelineResponseDto =
        client.get(PATH_TIMELINE) {
            parameter("date", date)
        }.body()

    override suspend fun checkReminders(date: String): ReminderListResponseDto =
        client.get(PATH_REMINDERS_CHECK) {
            parameter("date", date)
        }.body()
}