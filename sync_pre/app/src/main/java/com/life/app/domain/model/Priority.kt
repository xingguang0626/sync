package com.life.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Priority(val label: String, val displayName: String) {
    @SerialName("P0") P0("P0", "高优先"),
    @SerialName("P1") P1("P1", "中优先"),
    @SerialName("P2") P2("P2", "低优先")
}