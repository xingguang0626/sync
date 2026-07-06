package com.life.app.data.mock

import com.life.app.data.remote.HomeApi
import com.life.app.data.remote.dto.ApiResponseDto
import com.life.app.data.remote.dto.CreateScheduleDto
import com.life.app.data.remote.dto.LifestyleAdviceDataDto
import com.life.app.data.remote.dto.LifestyleAdviceDto
import com.life.app.data.remote.dto.NluParseDataDto
import com.life.app.data.remote.dto.NluParseResponseDto
import com.life.app.data.remote.dto.ParsedDetailsDto
import com.life.app.data.remote.dto.RawTimelineItemDto
import com.life.app.data.remote.dto.ReminderListResponseDto
import com.life.app.data.remote.dto.SettingsDto
import com.life.app.data.remote.dto.TimelineDataDto
import com.life.app.data.remote.dto.TimelineResponseDto
import com.life.app.domain.model.Priority
import com.life.app.domain.model.ReminderType
import com.life.app.domain.model.RepeatType
import com.life.app.domain.model.Schedule
import com.life.app.domain.model.ScheduleStatus
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * 第一阶段使用的假数据。包含 1 个 conflict_pair 演示冲突展示。
 * 真实联调时把 Hilt Module 里 provideHomeApi 换成 KtorHomeApi 即可，
 * 上层 Repository / ViewModel / UI 一行不改。
 */
class MockApi : HomeApi {

    companion object {
        const val MOCK_DATE = "2026-07-03"
        const val MOCK_WEEKDAY = "周五"
        const val MOCK_GREETING = "今天也要好好照顾自己呀 🌿"
    }

    private val mockSchedules: MutableList<Schedule> = mutableListOf(
        Schedule(
            id = 1, title = "早餐", date = MOCK_DATE,
            startTime = "07:00", endTime = "07:30", durationMinutes = 30,
            priority = Priority.P2, status = ScheduleStatus.COMPLETED,
            note = "粥 + 鸡蛋"
        ),
        Schedule(
            id = 2, title = "晨间阅读", date = MOCK_DATE,
            startTime = "08:00", endTime = "08:45", durationMinutes = 45,
            priority = Priority.P2, status = ScheduleStatus.COMPLETED
        ),
        Schedule(
            id = 3, title = "健身", date = MOCK_DATE,
            startTime = "09:00", endTime = "10:00", durationMinutes = 60,
            priority = Priority.P1, status = ScheduleStatus.COMPLETED
        ),
        Schedule(
            id = 4, title = "午餐", date = MOCK_DATE,
            startTime = "12:00", endTime = "12:45", durationMinutes = 45,
            priority = Priority.P2, status = ScheduleStatus.COMPLETED
        ),
        Schedule(
            id = 5, title = "午睡", date = MOCK_DATE,
            startTime = "13:30", endTime = "14:15", durationMinutes = 45,
            priority = Priority.P1, status = ScheduleStatus.COMPLETED
        ),
        Schedule(
            id = 6, title = "深度学习", date = MOCK_DATE,
            startTime = "15:00", endTime = "16:30", durationMinutes = 90,
            priority = Priority.P0, status = ScheduleStatus.IN_PROGRESS,
            note = "Kotlin + Compose 实战"
        ),
        Schedule(
            id = 7, title = "休息", date = MOCK_DATE,
            startTime = "16:30", endTime = "17:00", durationMinutes = 30,
            priority = Priority.P2, status = ScheduleStatus.PENDING
        ),
        Schedule(
            id = 8, title = "晚读", date = MOCK_DATE,
            startTime = "19:00", endTime = "20:00", durationMinutes = 60,
            priority = Priority.P1, status = ScheduleStatus.PENDING,
            repeat = RepeatType.DAILY
        ),
        Schedule(
            id = 9, title = "Deadline 项目交付", date = MOCK_DATE,
            startTime = "22:30", endTime = "23:00", durationMinutes = 30,
            priority = Priority.P0, status = ScheduleStatus.PENDING,
            note = "今晚必须提交"
        )
    )

    override suspend fun getTimeline(date: String): TimelineResponseDto {
        delay(150)
        val items = mutableListOf<RawTimelineItemDto>()

        // 1. 早餐
        items += RawTimelineItemDto(type = "single", schedule = mockSchedules[0])
        // 2. 晨间阅读
        items += RawTimelineItemDto(type = "single", schedule = mockSchedules[1])
        // 3. 健身
        items += RawTimelineItemDto(type = "single", schedule = mockSchedules[2])
        // 4. 午餐
        items += RawTimelineItemDto(type = "single", schedule = mockSchedules[3])
        // 5. 午睡
        items += RawTimelineItemDto(type = "single", schedule = mockSchedules[4])
        // 6. 深度学习 (P0 进行中)
        items += RawTimelineItemDto(type = "single", schedule = mockSchedules[5])
        // 7. 休息
        items += RawTimelineItemDto(type = "single", schedule = mockSchedules[6])
        // 8. 晚读
        items += RawTimelineItemDto(type = "single", schedule = mockSchedules[7])
        // 9. Deadline 项目交付 (P0)
        items += RawTimelineItemDto(type = "single", schedule = mockSchedules[8])

        return TimelineResponseDto(
            ok = true,
            data = TimelineDataDto(date = date, items = items)
        )
    }

    override suspend fun checkReminders(date: String): ReminderListResponseDto {
        delay(80)
        return ReminderListResponseDto(
            ok = true,
            data = listOf(
                com.life.app.domain.model.Reminder(
                    type = ReminderType.EVENING_P0,
                    priority = 2,
                    message = "识别到你今晚有 deadline，建议开启加班模式，并顺延睡眠计划。",
                    suggestion = "采纳建议",
                    relatedScheduleIds = listOf(9L, 8L)
                )
            )
        )
    }

    override suspend fun createSchedule(data: CreateScheduleDto): ApiResponseDto<Schedule> {
        delay(300)
        return ApiResponseDto(
            ok = true,
            data = Schedule(
                id = (mockSchedules.size + 1).toLong(),
                title = data.title,
                date = data.date,
                startTime = data.startTime,
                endTime = "00:00",
                durationMinutes = data.duration,
                priority = Priority.valueOf(data.priority),
                status = ScheduleStatus.PENDING,
                repeat = try { RepeatType.valueOf(data.repeat) } catch (_: Exception) { RepeatType.NONE },
                note = data.note
            )
        )
    }

    override suspend fun getSchedule(id: Long): ApiResponseDto<Schedule> {
        delay(100)
        val schedule = mockSchedules.find { it.id == id }
        return if (schedule != null) {
            ApiResponseDto(ok = true, data = schedule)
        } else {
            ApiResponseDto(ok = false, error = "日程不存在")
        }
    }

    override suspend fun updateSchedule(id: Long, data: CreateScheduleDto): ApiResponseDto<Schedule> {
        delay(300)
        val existing = mockSchedules.find { it.id == id }
        return if (existing != null) {
            val updated = existing.copy(
                title = data.title,
                date = data.date,
                startTime = data.startTime,
                durationMinutes = data.duration,
                priority = Priority.valueOf(data.priority),
                note = data.note
            )
            ApiResponseDto(ok = true, data = updated)
        } else {
            ApiResponseDto(ok = false, error = "日程不存在")
        }
    }

    override suspend fun deleteSchedule(id: Long): ApiResponseDto<Unit> {
        delay(200)
        val existing = mockSchedules.find { it.id == id }
        return if (existing != null) {
            mockSchedules.removeAll { it.id == id }
            ApiResponseDto(ok = true, data = Unit)
        } else {
            ApiResponseDto(ok = false, error = "日程不存在")
        }
    }

    override suspend fun markComplete(id: Long): ApiResponseDto<Schedule> {
        delay(200)
        val existing = mockSchedules.find { it.id == id }
        return if (existing != null) {
            val updated = existing.copy(status = ScheduleStatus.COMPLETED)
            mockSchedules[mockSchedules.indexOf(existing)] = updated
            ApiResponseDto(ok = true, data = updated)
        } else {
            ApiResponseDto(ok = false, error = "日程不存在")
        }
    }

    /** 第四阶段新增：顺延日程（newDate 为 null 则顺延到明天） */
    override suspend fun postponeSchedule(id: Long, newDate: String?): ApiResponseDto<Schedule> {
        delay(150)
        val existing = mockSchedules.find { it.id == id }
        if (existing == null) {
            return ApiResponseDto(ok = false, error = "日程不存在")
        }
        val targetDate = newDate ?: java.time.LocalDate.now()
            .plusDays(1).toString()
        val updated = existing.copy(date = targetDate)
        mockSchedules[mockSchedules.indexOf(existing)] = updated
        return ApiResponseDto(ok = true, data = updated)
    }

    override suspend fun parseNlu(text: String, todayDate: String): NluParseResponseDto {
        delay(120)
        val t = text.trim()
        val timePattern = Regex("""(\d{1,2})[:：点](\d{0,2})?""")
        val timeMatch = timePattern.find(t)
        val hour = timeMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 21
        val minute = timeMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
        val startTime = "%02d:%02d".format(hour, minute)
        val endTime = "%02d:%02d".format((hour + 1).coerceAtMost(23), minute)

        val title = when {
            t.contains("deadline", ignoreCase = true) || t.contains("截止") -> "Deadline"
            t.contains("学习") || t.contains("复习") -> "学习"
            t.contains("会议") || t.contains("开会") -> "会议"
            t.contains("跑步") || t.contains("健身") -> "跑步"
            t.length >= 2 -> t.take(20)
            else -> "新日程"
        }
        val priority = when {
            t.contains("P0") || t.contains("重要") || t.contains("deadline", ignoreCase = true) -> "P0"
            t.contains("P2") || t.contains("不急") -> "P2"
            else -> "P1"
        }
        val confidence = when {
            t.length >= 6 && timeMatch != null -> 0.88f
            t.length >= 4 -> 0.72f
            else -> 0.40f
        }

        val draft = buildJsonObject {
            put("title", JsonPrimitive(title))
            put("date", JsonPrimitive(todayDate))
            put("start_time", JsonPrimitive(startTime))
            put("end_time", JsonPrimitive(endTime))
            put("duration", JsonPrimitive(60))
            put("priority", JsonPrimitive(priority))
            put("repeat", JsonPrimitive("none"))
        }
        val details = ParsedDetailsDto(
            dateRaw = if (t.contains("明天")) "明天" else if (t.contains("今晚") || t.contains("晚上")) "今晚" else null,
            timeRaw = timeMatch?.value?.trim(),
            durationRaw = null,
            actionRaw = title,
            rawOriginal = text
        )
        return NluParseResponseDto(
            ok = true,
            data = NluParseDataDto(
                intent = "create_schedule",
                draft = draft,
                confidence = confidence,
                parsedDetails = details,
                needConfirmation = true
            )
        )
    }

    override suspend fun clearAll(): ApiResponseDto<Unit> {
        delay(100)
        mockSchedules.clear()
        return ApiResponseDto(ok = true, data = Unit)
    }

    override suspend fun getSettings(): SettingsDto {
        delay(50)
        return SettingsDto(ok = true, data = mapOf("default_priority" to "P1", "default_duration" to "60"))
    }

    override suspend fun updateSettings(body: Map<String, String>): SettingsDto {
        delay(50)
        return SettingsDto(ok = true, data = body)
    }

    override suspend fun getLifestyleAdvice(): LifestyleAdviceDto {
        delay(200)
        return LifestyleAdviceDto(
            ok = true,
            data = LifestyleAdviceDataDto(
                advice = "你今天完成了 4 项日程，专注时长 3 小时 45 分钟。上午深度学习时间安排得很好，建议继续保持。下午的会议在 14:00，记得提前 10 分钟准备。今天还没有安排运动时间，可以考虑晚餐后散步 30 分钟来放松身心。",
                model = "mock"
            )
        )
    }
}