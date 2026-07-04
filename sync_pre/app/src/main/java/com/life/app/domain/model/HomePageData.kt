package com.life.app.domain.model

data class HomePageData(
    val date: String,
    val weekday: String,
    val greeting: String,
    val timelineItems: List<TimelineItem>,
    val topReminder: Reminder?,
    val empty: Boolean
)