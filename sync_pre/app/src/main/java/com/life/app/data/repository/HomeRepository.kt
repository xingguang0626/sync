package com.life.app.data.repository

import com.life.app.data.mock.MockApi
import com.life.app.data.remote.ApiResult
import com.life.app.data.remote.HomeApi
import com.life.app.data.remote.mapper.toDomain
import com.life.app.data.remote.toApiResult
import com.life.app.domain.model.HomePageData
import javax.inject.Inject
import javax.inject.Named

class HomeRepository @Inject constructor(
    @Named("homeApi") private val api: HomeApi
) {
    suspend fun fetchHomeData(date: String): ApiResult<HomePageData> {
        return runCatching {
            val timelineResp = api.getTimeline(date).toApiResult()
            val remindersResp = api.checkReminders(date).toApiResult()

            when {
                timelineResp is ApiResult.Failure -> timelineResp
                remindersResp is ApiResult.Failure -> remindersResp
                timelineResp is ApiResult.Success && remindersResp is ApiResult.Success -> {
                    val timelineData = timelineResp.data
                    val items = timelineData.items.map { it.toDomain() }
                    val topReminder = remindersResp.data.minByOrNull { it.priority }
                    ApiResult.Success(
                        HomePageData(
                            date = timelineData.date,
                            weekday = MockApi.MOCK_WEEKDAY,                  // 第一阶段硬编码
                            greeting = MockApi.MOCK_GREETING,
                            timelineItems = items,
                            topReminder = topReminder,
                            empty = items.isEmpty()
                        )
                    )
                }
                else -> ApiResult.Failure("未知状态")
            }
        }.getOrElse { e ->
            ApiResult.Failure(e.message ?: "未知异常")
        }
    }
}