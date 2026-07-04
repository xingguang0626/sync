package com.life.app.data.mock

import com.life.app.data.remote.HomeApi
import com.life.app.data.remote.dto.RawTimelineItemDto
import com.life.app.data.remote.dto.ReminderListResponseDto
import com.life.app.data.remote.dto.TimelineDataDto
import com.life.app.data.remote.dto.TimelineResponseDto
import com.life.app.domain.model.Priority
import com.life.app.domain.model.ReminderType
import com.life.app.domain.model.RepeatType
import com.life.app.domain.model.Schedule
import com.life.app.domain.model.ScheduleStatus
import kotlinx.coroutines.delay

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

    private val mockSchedules: List<Schedule> = listOf(
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
}