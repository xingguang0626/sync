package com.life.app.data.remote

import com.life.app.data.remote.dto.ApiResponseDto

sealed interface ApiResult<out T> {
    data object Loading : ApiResult<Nothing>
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: String) : ApiResult<Nothing>
}

fun <T> ApiResponseDto<T>.toApiResult(): ApiResult<T> = when {
    ok && data != null -> ApiResult.Success(data)
    else -> ApiResult.Failure(error ?: "未知错误")
}