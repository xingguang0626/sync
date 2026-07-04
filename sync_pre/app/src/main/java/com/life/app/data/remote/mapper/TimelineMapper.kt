package com.life.app.data.remote.mapper

import com.life.app.data.remote.dto.RawTimelineItemDto
import com.life.app.domain.model.TimelineItem

fun RawTimelineItemDto.toDomain(): TimelineItem = when (type) {
    "single" -> TimelineItem.Single(
        requireNotNull(schedule) { "single 类型必须包含 schedule 字段" }
    )
    "conflict_pair" -> TimelineItem.ConflictPair(
        requireNotNull(schedules) { "conflict_pair 类型必须包含 schedules 字段" }
    )
    "conflict_group" -> TimelineItem.ConflictGroup(
        requireNotNull(schedules) { "conflict_group 类型必须包含 schedules 字段" }
    )
    else -> throw IllegalArgumentException("未知的 timeline item type: $type")
}