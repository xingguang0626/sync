package com.life.app.ui.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.life.app.domain.model.Schedule

/**
 * 根据日程标题推断对应图标。无匹配时返回默认日历 icon。
 */
object ScheduleIcons {
    fun forSchedule(schedule: Schedule): ImageVector = forTitle(schedule.title)

    fun forTitle(title: String): ImageVector {
        val t = title.lowercase()
        return when {
            "早餐" in t || "早" in t && "饭" in t -> Icons.Outlined.Restaurant
            "午餐" in t || "午饭" in t -> Icons.Outlined.Restaurant
            "晚餐" in t || "晚饭" in t || "吃饭" in t -> Icons.Outlined.Restaurant

            "晨间阅读" in t || "晚读" in t || "阅读" in t || "读书" in t -> Icons.AutoMirrored.Outlined.MenuBook

            "健身" in t || "运动" in t || "跑步" in t -> Icons.Outlined.FitnessCenter
            "哑铃" in t -> Icons.Outlined.FitnessCenter

            "午睡" in t || "小憩" in t || "睡觉" in t -> Icons.Outlined.Hotel

            "深度学习" in t || "学习" in t || "复习" in t -> Icons.Outlined.LaptopChromebook

            "休息" in t || "放松" in t -> Icons.Outlined.Coffee

            "会议" in t || "小组" in t -> Icons.Outlined.Groups

            "deadline" in t || "截止" in t || "提交" in t || "交付" in t || "汇报" in t
                || "考试" in t || "面试" in t -> Icons.Outlined.Flag

            "作业" in t || "写" in t -> Icons.Outlined.Edit

            "通勤" in t || "路上" in t -> Icons.Outlined.DirectionsCar

            "背单词" in t -> Icons.Outlined.Translate

            else -> Icons.AutoMirrored.Outlined.EventNote
        }
    }
}