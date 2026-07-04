package com.life.app.data.remote

import com.life.app.data.remote.dto.ReminderListResponseDto
import com.life.app.data.remote.dto.TimelineResponseDto

/**
 * 第一阶段：MockApi 实现
 * 第二阶段：KtorHomeApi 实现（实际调 Python 后端 8800 端口）
 */
interface HomeApi {
    suspend fun getTimeline(date: String): TimelineResponseDto
    suspend fun checkReminders(date: String): ReminderListResponseDto
}