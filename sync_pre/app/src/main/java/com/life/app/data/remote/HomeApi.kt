package com.life.app.data.remote

import com.life.app.data.remote.dto.ApiResponseDto
import com.life.app.data.remote.dto.CreateScheduleDto
import com.life.app.data.remote.dto.LifestyleAdviceDto
import com.life.app.data.remote.dto.NluParseResponseDto
import com.life.app.data.remote.dto.ReminderListResponseDto
import com.life.app.data.remote.dto.SettingsDto
import com.life.app.data.remote.dto.TimelineResponseDto
import com.life.app.domain.model.Schedule

/**
 * 第一阶段：MockApi 实现
 * 第二阶段：KtorHomeApi 实现（实际调 Python 后端 8800 端口）
 */
interface HomeApi {
    suspend fun getTimeline(date: String): TimelineResponseDto
    suspend fun checkReminders(date: String): ReminderListResponseDto
    suspend fun createSchedule(data: CreateScheduleDto): ApiResponseDto<Schedule>
    suspend fun getSchedule(id: Long): ApiResponseDto<Schedule>
    suspend fun updateSchedule(id: Long, data: CreateScheduleDto): ApiResponseDto<Schedule>
    suspend fun deleteSchedule(id: Long): ApiResponseDto<Unit>
    suspend fun markComplete(id: Long): ApiResponseDto<Schedule>
    /** 第四阶段新增：顺延日程（默认明天，可指定 newDate） */
    suspend fun postponeSchedule(id: Long, newDate: String? = null): ApiResponseDto<Schedule>
    /** 第三阶段新增：自然语言解析 */
    suspend fun parseNlu(text: String, todayDate: String): NluParseResponseDto
    /** P2 清空所有日程（设置保留） */
    suspend fun clearAll(): ApiResponseDto<Unit>
    /** P2 获取设置 */
    suspend fun getSettings(): SettingsDto
    /** P2 更新设置 */
    suspend fun updateSettings(body: Map<String, String>): SettingsDto
    /** AI 智能生活建议 */
    suspend fun getLifestyleAdvice(): LifestyleAdviceDto
}