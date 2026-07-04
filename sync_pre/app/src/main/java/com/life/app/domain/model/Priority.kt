package com.life.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Priority {
    @SerialName("P0") P0,
    @SerialName("P1") P1,
    @SerialName("P2") P2;

    val displayName: String get() = when (this) {
        P0 -> "P0 最高优先级"
        P1 -> "P1 中等优先级"
        P2 -> "P2 一般优先级"
    }
}