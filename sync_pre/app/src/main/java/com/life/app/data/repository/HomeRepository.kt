package com.life.app.data.repository

import android.util.Log
import com.life.app.data.remote.ApiResult
import com.life.app.data.remote.HomeApi
import com.life.app.data.remote.dto.ApiResponseDto
import com.life.app.data.remote.dto.CreateScheduleDto
import com.life.app.data.remote.dto.NluParseDataDto
import com.life.app.data.remote.mapper.toApiResult
import com.life.app.data.remote.mapper.toDomain
import com.life.app.domain.model.HomePageData
import com.life.app.domain.model.Schedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named

class HomeRepository @Inject constructor(
    @Named("homeApi") private val api: HomeApi
) {
    /** 获取首页数据：时间轴 + 提醒 */
    suspend fun fetchHomeData(date: String): ApiResult<HomePageData> {
        return runCatching {
            val timelineResp = api.getTimeline(date)
            val remindersResp = api.checkReminders(date)

            if (!timelineResp.ok || timelineResp.data == null) {
                return@runCatching ApiResult.Failure(timelineResp.error ?: "时间轴加载失败")
            }
            if (!remindersResp.ok || remindersResp.data == null) {
                return@runCatching ApiResult.Failure(remindersResp.error ?: "提醒加载失败")
            }

            val timelineData = timelineResp.data
            val items = timelineData.items.map { it.toDomain() }
            val topReminder = remindersResp.data.minByOrNull { it.priority }
            ApiResult.Success(
                HomePageData(
                    date = timelineData.date,
                    weekday = weekdayOf(timelineData.date),
                    greeting = greetingFor(timelineData.date),
                    timelineItems = items,
                    topReminder = topReminder,
                    empty = items.isEmpty()
                )
            )
        }.getOrElse { e ->
            Log.e("Sync", "[HomeRepo] fetchHomeData 异常: ${e.message}", e)
            ApiResult.Failure(e.message ?: "未知异常")
        }
    }

    /** 创建日程并返回服务端生成的日程对象 */
    suspend fun createSchedule(data: CreateScheduleDto): ApiResult<Schedule> {
        return runCatching {
            val resp: ApiResponseDto<Schedule> = api.createSchedule(data)
            if (resp.ok && resp.data != null) {
                ApiResult.Success(resp.data)
            } else {
                ApiResult.Failure(resp.error ?: "创建失败")
            }
        }.getOrElse { e ->
            Log.e("Sync", "[HomeRepo] createSchedule 异常: ${e.message}", e)
            ApiResult.Failure(e.message ?: "未知异常")
        }
    }

    /** 第三阶段新增：自然语言解析 */
    suspend fun parseNlu(text: String): ApiResult<NluParseDataDto> {
        return runCatching {
            api.parseNlu(text, today()).toApiResult()
        }.getOrElse { e ->
            ApiResult.Failure(e.message ?: "NLU 解析失败")
        }
    }

    /** 按 ID 查询单条日程 */
    suspend fun getSchedule(id: Long): ApiResult<Schedule> {
        return runCatching {
            val resp = api.getSchedule(id)
            if (resp.ok && resp.data != null) ApiResult.Success(resp.data)
            else ApiResult.Failure(resp.error ?: "日程不存在")
        }.getOrElse { e -> ApiResult.Failure(e.message ?: "未知异常") }
    }

    /** 更新日程 */
    suspend fun updateSchedule(id: Long, data: CreateScheduleDto): ApiResult<Schedule> {
        return runCatching {
            val resp = api.updateSchedule(id, data)
            if (resp.ok && resp.data != null) ApiResult.Success(resp.data)
            else ApiResult.Failure(resp.error ?: "更新失败")
        }.getOrElse { e -> ApiResult.Failure(e.message ?: "未知异常") }
    }

    /** 删除日程 */
    suspend fun deleteSchedule(id: Long): ApiResult<Unit> {
        return runCatching {
            val resp = api.deleteSchedule(id)
            if (resp.ok) ApiResult.Success(resp.data ?: Unit)
            else ApiResult.Failure(resp.error ?: "删除失败")
        }.getOrElse { e -> ApiResult.Failure(e.message ?: "未知异常") }
    }

    /** 标记完成 */
    suspend fun markComplete(id: Long): ApiResult<Schedule> {
        return runCatching {
            val resp = api.markComplete(id)
            if (resp.ok && resp.data != null) ApiResult.Success(resp.data)
            else ApiResult.Failure(resp.error ?: "操作失败")
        }.getOrElse { e -> ApiResult.Failure(e.message ?: "未知异常") }
    }

    /** 第四阶段新增：顺延日程（newDate 为 null 则顺延到明天） */
    suspend fun postponeSchedule(id: Long, newDate: String? = null): ApiResult<Schedule> {
        return runCatching {
            val resp = api.postponeSchedule(id, newDate)
            if (resp.ok && resp.data != null) ApiResult.Success(resp.data)
            else ApiResult.Failure(resp.error ?: "顺延失败")
        }.getOrElse { e -> ApiResult.Failure(e.message ?: "未知异常") }
    }

    /** P2 清空所有日程 */
    suspend fun clearAll(): ApiResult<Unit> {
        return runCatching {
            val resp = api.clearAll()
            if (resp.ok) ApiResult.Success(Unit)
            else ApiResult.Failure(resp.error ?: "清空失败")
        }.getOrElse { e -> ApiResult.Failure(e.message ?: "未知异常") }
    }

    /** P2 获取设置 */
    suspend fun getSettings(): ApiResult<Map<String, String>> {
        return runCatching {
            val resp = api.getSettings()
            if (resp.ok && resp.data != null) ApiResult.Success(resp.data)
            else ApiResult.Failure(resp.error ?: "获取设置失败")
        }.getOrElse { e -> ApiResult.Failure(e.message ?: "未知异常") }
    }

    /** P2 更新设置 */
    suspend fun updateSettings(body: Map<String, String>): ApiResult<Map<String, String>> {
        return runCatching {
            val resp = api.updateSettings(body)
            if (resp.ok && resp.data != null) ApiResult.Success(resp.data)
            else ApiResult.Failure(resp.error ?: "更新设置失败")
        }.getOrElse { e -> ApiResult.Failure(e.message ?: "未知异常") }
    }

    /** AI 智能生活建议 */
    suspend fun getLifestyleAdvice(): ApiResult<String> {
        return runCatching {
            val resp = api.getLifestyleAdvice()
            if (resp.ok && resp.data != null) ApiResult.Success(resp.data.advice)
            else ApiResult.Failure(resp.error ?: "AI 建议获取失败")
        }.getOrElse { e -> ApiResult.Failure(e.message ?: "未知异常") }
    }

    companion object {
        private val WEEKDAY_NAMES = mapOf(
            DayOfWeek.MONDAY to "周一", DayOfWeek.TUESDAY to "周二",
            DayOfWeek.WEDNESDAY to "周三", DayOfWeek.THURSDAY to "周四",
            DayOfWeek.FRIDAY to "周五", DayOfWeek.SATURDAY to "周六",
            DayOfWeek.SUNDAY to "周日"
        )

        fun today(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        fun weekdayOf(dateStr: String): String {
            return try {
                val d = LocalDate.parse(dateStr)
                WEEKDAY_NAMES[d.dayOfWeek] ?: ""
            } catch (_: Exception) { "" }
        }

        fun greetingFor(dateStr: String): String {
            return try {
                val d = LocalDate.parse(dateStr)
                val today = LocalDate.now()
                when {
                    d == today        -> "今天也要好好照顾自己呀"
                    d == today.plusDays(1) -> "明天也要元气满满"
                    else             -> "来规划你的一天吧"
                }
            } catch (_: Exception) { "来规划你的一天吧" }
        }
    }
}