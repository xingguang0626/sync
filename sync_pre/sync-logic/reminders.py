"""
reminders.py — AI 提醒检测模块
===============================
每次日程变更后，前端调用此模块检测是否需要提醒用户。
按严重程度返回有序的提醒列表。

提醒类型（与前端 ReminderType 枚举对齐，snake_case）：
  time_conflict  — 时间冲突
  evening_p0     — 晚间 P0 任务
  overtime       — 任务超时
  late_end       — 结束时间过晚
  long_focus     — 连续学习过久
  move_pending   — 未完成任务顺延
"""

import db

# ---------------------------------------------------------------------------
# 配置常量
# ---------------------------------------------------------------------------

# 被视为"晚间"的起始时间（HH:MM 格式，包含此时间点）
EVENING_START = "20:00"

# 被视为"过晚"的结束时间阈值
LATE_END_THRESHOLD = "23:00"

# 单任务超时阈值（分钟）
OVERTIME_THRESHOLD = 120

# 学习类关键词（用于判断连续学习）
STUDY_KEYWORDS = ["学习", "复习", "作业", "刷题", "练习", "考试", "写", "背", "读"]

# 连续学习累计时长阈值（分钟）
LONG_FOCUS_THRESHOLD = 90


# ---------------------------------------------------------------------------
# 内部工具
# ---------------------------------------------------------------------------

def _time_to_minutes(t: str) -> int:
    """将 HH:MM 转换为分钟数（从 00:00 起算）"""
    h, m = map(int, t.split(":"))
    return h * 60 + m


def _is_overlap(a: dict, b: dict) -> bool:
    """判断两个日程时间段是否重叠"""
    return max(a["start_time"], b["start_time"]) < min(a["end_time"], b["end_time"])


def _is_study_task(title: str) -> bool:
    """判断日程标题是否属于学习类任务"""
    return any(kw in title for kw in STUDY_KEYWORDS)


# ---------------------------------------------------------------------------
# 检测规则（每条规则独立实现，返回单个提醒字典或 None）
# ---------------------------------------------------------------------------

def _check_time_conflict(schedules: list[dict]):
    """规则 1：时间冲突 — 两个日程时间段重叠"""
    conflicts = []
    for i in range(len(schedules)):
        for j in range(i + 1, len(schedules)):
            if _is_overlap(schedules[i], schedules[j]):
                a, b = schedules[i], schedules[j]
                overlap_start = max(a["start_time"], b["start_time"])
                overlap_end   = min(a["end_time"], b["end_time"])
                conflicts.append({
                    "type": "time_conflict",
                    "priority": 1,
                    "message": f"「{a['title']}」和「{b['title']}」时间冲突"
                               f"（{overlap_start}-{overlap_end}）",
                    "suggestion": f"建议将「{b['title'] if b['priority'] < a['priority'] else a['title']}」"
                                  f"顺延到 {a['end_time'] if b['priority'] < a['priority'] else b['end_time']}",
                    "related_schedule_ids": [a["id"], b["id"]]
                })
    return conflicts


def _check_evening_p0(schedules: list[dict]):
    """规则 2：晚间 P0 — 20:00 之后存在状态为 pending/in_progress 的 P0 任务"""
    threshold = _time_to_minutes(EVENING_START)
    reminders = []
    for s in schedules:
        if s["priority"] != "P0":
            continue
        if s["status"] not in ("pending", "in_progress"):
            continue
        if _time_to_minutes(s["start_time"]) >= threshold:
            reminders.append({
                "type": "evening_p0",
                "priority": 2,
                "message": f"今晚有高优先级任务「{s['title']}」",
                "suggestion": "需要的话可以开启专注模式",
                "related_schedule_ids": [s["id"]]
            })
    return reminders


def _check_overtime(schedules: list[dict]):
    """规则 3：任务超时 — 单日程持续超过 2 小时"""
    reminders = []
    for s in schedules:
        if s["status"] not in ("pending", "in_progress"):
            continue
        if s["duration"] > OVERTIME_THRESHOLD:
            reminders.append({
                "type": "overtime",
                "priority": 3,
                "message": f"「{s['title']}」已持续超过 {OVERTIME_THRESHOLD // 60} 小时",
                "suggestion": "请注意休息",
                "related_schedule_ids": [s["id"]]
            })
    return reminders


def _check_late_end(schedules: list[dict]):
    """规则 4：结束时间过晚 — 有日程结束时间晚于 23:00"""
    threshold = _time_to_minutes(LATE_END_THRESHOLD)
    reminders = []
    for s in schedules:
        if s["status"] not in ("pending", "in_progress"):
            continue
        if _time_to_minutes(s["end_time"]) > threshold:
            reminders.append({
                "type": "late_end",
                "priority": 4,
                "message": f"「{s['title']}」预计 {s['end_time']} 结束",
                "suggestion": "是否需要调整到明天",
                "related_schedule_ids": [s["id"]]
            })
    return reminders


def _check_long_focus(schedules: list[dict]):
    """规则 5：连续学习过久 — 连续学习类日程总时长超过 90 分钟"""
    # 按 start_time 排序，合并连续的学习任务
    sorted_schedules = sorted(schedules, key=lambda s: s["start_time"])
    study_ids = []
    study_total = 0

    for s in sorted_schedules:
        if s["status"] not in ("pending", "in_progress"):
            continue
        if _is_study_task(s["title"]):
            study_ids.append(s["id"])
            study_total += s["duration"]

    if study_total >= LONG_FOCUS_THRESHOLD and study_ids:
        hours = study_total // 60
        mins = study_total % 60
        duration_str = f"{hours}小时{mins}分钟" if hours > 0 else f"{mins}分钟"
        return [{
            "type": "long_focus",
            "priority": 5,
            "message": f"学习类任务已累计 {duration_str}",
            "suggestion": "建议安排一次休息",
            "related_schedule_ids": study_ids
        }]
    return []


def _check_move_pending(schedules: list[dict]):
    """规则 6：未完成任务 — 当天有 pending 任务且当前时间已过其结束时间"""
    now = _time_to_minutes(
        __import__("datetime").datetime.now().strftime("%H:%M")
    )
    reminders = []
    for s in schedules:
        if s["status"] != "pending":
            continue
        if _time_to_minutes(s["end_time"]) < now:
            reminders.append({
                "type": "move_pending",
                "priority": 6,
                "message": f"「{s['title']}」未完成",
                "suggestion": "点击顺延到明天",
                "related_schedule_ids": [s["id"]]
            })
    return reminders


# ---------------------------------------------------------------------------
# 对外 API
# ---------------------------------------------------------------------------

# 规则检测函数注册表（按优先级排序）
_ALL_RULES = [
    _check_time_conflict,
    _check_evening_p0,
    _check_overtime,
    _check_late_end,
    _check_long_focus,
    _check_move_pending,
]


def check_reminders(date: str) -> dict:
    """
    检测指定日期的所有 AI 提醒。
    遍历当天日程，按 6 条规则逐条检测，汇总后按 priority 升序返回。

    返回示例：
    {
      "ok": true,
      "data": [
        {
          "type": "evening_p0",
          "priority": 2,
          "message": "今晚有高优先级任务「Deadline」",
          "suggestion": "需要的话可以开启专注模式",
          "related_schedule_ids": [9]
        }
      ]
    }
    """
    result = db.get_schedules_by_date(date)
    if not result["ok"]:
        return result

    schedules = result["data"]
    if not schedules:
        return {"ok": True, "data": []}

    all_reminders = []
    for rule_fn in _ALL_RULES:
        all_reminders.extend(rule_fn(schedules))

    # 按 priority 升序排列（数值越小越严重）
    all_reminders.sort(key=lambda r: r["priority"])

    return {"ok": True, "data": all_reminders}
