"""
timeline.py — 时间轴排序与冲突检测模块
=====================================
从 db 获取当天日程 → 按 start_time 排序 → 检测时间重叠 → 返回结构化时间轴。
前端根据 type 字段决定渲染方式：single / conflict_pair / conflict_group。
"""

import db


def _is_overlap(a: dict, b: dict) -> bool:
    """
    判断两个日程的时间段是否重叠。
    规则：max(A.start, B.start) < min(A.end, B.end)
    注意：首尾相接（如 09:00-10:00 与 10:00-11:00）不算重叠。
    """
    return max(a["start_time"], b["start_time"]) < min(a["end_time"], b["end_time"])


def _group_conflicts(schedules: list[dict]) -> list[dict]:
    """
    输入：已按 start_time 排序的日程列表。
    输出：分组后的时间轴条目列表，每个条目的结构为：
      - {"type": "single",          "schedule": {...}}
      - {"type": "conflict_pair",   "schedules": [{...}, {...}]}
      - {"type": "conflict_group",  "schedules": [{...}, {...}, {...}]}

    算法说明：
      遍历排序后的日程列表，用一个贪心指针 i 逐项扫描。
      对每个 i，尝试向后扩展 j，只要 schedules[j] 的 start_time < 当前组的 max(end_time)，
      就将其纳入同一冲突组。最终根据组内元素数量决定 type。
    """
    if not schedules:
        return []

    items = []
    n = len(schedules)
    i = 0

    while i < n:
        group = [schedules[i]]
        group_max_end = schedules[i]["end_time"]
        j = i + 1

        while j < n:
            # 如果下一个日程的开始时间 < 当前组的最大结束时间，说明有重叠
            if schedules[j]["start_time"] < group_max_end:
                group.append(schedules[j])
                # 更新组内最大结束时间（新加入的日程可能更晚结束）
                if schedules[j]["end_time"] > group_max_end:
                    group_max_end = schedules[j]["end_time"]
                j += 1
            else:
                break

        # 根据组内数量决定 type
        if len(group) == 1:
            items.append({"type": "single", "schedule": group[0]})
        elif len(group) == 2:
            items.append({"type": "conflict_pair", "schedules": group})
        else:
            items.append({"type": "conflict_group", "schedules": group})

        i = j

    return items


# ---------------------------------------------------------------------------
# 对外 API
# ---------------------------------------------------------------------------

def get_timeline(date: str) -> dict:
    """
    获取指定日期的时间轴，包含冲突检测结果。
    返回示例：
    {
      "ok": true,
      "data": {
        "date": "2026-07-03",
        "items": [
          {"type": "single",         "schedule": {...}},
          {"type": "conflict_pair",  "schedules": [{...}, {...}]},
          {"type": "conflict_group", "schedules": [{...}, {...}, {...}]}
        ]
      }
    }
    """
    result = db.get_schedules_by_date(date)
    if not result["ok"]:
        return result

    schedules = result["data"]
    items = _group_conflicts(schedules)

    return {
        "ok": True,
        "data": {
            "date": date,
            "items": items
        }
    }
