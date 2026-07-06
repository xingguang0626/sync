"""
ai_advice.py — AI 智能生活建议模块
===================================
调用 vivo AI API，基于用户近期日程和操作历史生成智能生活建议。

依赖：requests 库（pip install requests）
API：POST https://api-ai.vivo.com.cn/v1/chat/completions
模型：Doubao-Seed-2.0-mini（默认开启深度思考）
"""

import json
import uuid
from datetime import datetime, timedelta

# ---------------------------------------------------------------------------
# 配置
# ---------------------------------------------------------------------------

APP_KEY = "sk-xuanji-2026241446-amRxSXBYT3BWUHFVR1lGdA=="
API_URL = "https://api-ai.vivo.com.cn/v1/chat/completions"
MODEL_NAME = "Doubao-Seed-2.0-mini"
TIMEOUT = 30  # 请求超时（秒）

# ---------------------------------------------------------------------------
# 内部工具
# ---------------------------------------------------------------------------

def _last_7_days():
    """返回最近 7 天的日期列表 YYYY-MM-DD（从旧到新）"""
    today = datetime.now().date()
    return [(today - timedelta(days=i)).isoformat() for i in range(6, -1, -1)]


def _format_schedule(s: dict) -> str:
    """格式化一条日程为 prompt 文本"""
    status_label = {
        "pending": "待办", "in_progress": "进行中",
        "completed": "已完成", "cancelled": "已取消"
    }.get(s.get("status", ""), s.get("status", ""))
    priority_label = {"P0": "高", "P1": "中", "P2": "低"}.get(s.get("priority", ""), "")
    return (
        f"「{s['title']}」{s['date']} {s['start_time']}-{s['end_time']} "
        f"({s['duration']}分钟) 优先级{priority_label} 状态{status_label}"
        + (f" 备注：{s['note']}" if s.get("note") else "")
    )


def _format_history(h: dict) -> str:
    """格式化一条操作记录为 prompt 文本"""
    op_labels = {
        "create": "创建", "update": "修改", "delete": "删除",
        "complete": "标记完成", "postpone": "顺延"
    }
    op = op_labels.get(h.get("operation", ""), h.get("operation", ""))
    return f"{h['created_at'][:19]} {op}了日程 #{h.get('schedule_id', '?')}"


def _build_prompt(schedules_by_day: dict, total_schedules: int,
                   completed_count: int, deleted_count: int,
                   recent_history: list, settings: dict,
                   today_str: str) -> str:
    """构建发送给 AI 的 user prompt"""
    parts = []

    # 近期日程概要
    parts.append(f"今天是 {today_str}。以下是用户最近 7 天的日程数据：\n")
    for day in sorted(schedules_by_day.keys()):
        day_schedules = schedules_by_day[day]
        parts.append(f"【{day}】共 {len(day_schedules)} 条：")
        for s in day_schedules:
            parts.append(f"  - {_format_schedule(s)}")
        parts.append("")

    # 统计
    parts.append(
        f"## 统计\n"
        f"- 最近 7 天总计 {total_schedules} 条日程\n"
        f"- 已完成 {completed_count} 条\n"
        f"- 被删除 {deleted_count} 条\n"
        f"- 完成率 {completed_count / max(total_schedules, 1) * 100:.0f}%\n"
    )

    # 操作记录
    if recent_history:
        parts.append(f"\n## 最近 {len(recent_history)} 条操作记录")
        for h in recent_history:
            parts.append(f"- {_format_history(h)}")

    # 用户偏好
    parts.append(
        f"\n## 用户偏好\n"
        f"- 默认优先级：{settings.get('default_priority', 'P1')}\n"
        f"- 默认持续时间：{settings.get('default_duration', '60')} 分钟\n"
    )

    return "\n".join(parts)


SYSTEM_PROMPT = (
    "你是 Sync（随刻）智能日程规划 App 的 AI 生活管家。"
    "你的职责是基于用户近期的日程数据、完成情况和操作习惯，给出温暖、实用、个性化的生活建议。"
    "规则：\n"
    "1. 用中文回答，语气亲切自然，像朋友建议一样\n"
    "2. 给出恰好 3 条具体建议，每条 20-40 个字\n"
    "3. 格式为：1. xxx\n2. xxx\n3. xxx（每条一行，编号+空格+内容）\n"
    "4. 建议方向：时间管理、优先级调整、休息提醒、习惯养成、效率提升\n"
    '5. 不要说"作为 AI 助手"之类的客套话，不要加开头总结和结尾总结，直接给出 3 条建议\n'
    "6. 如果用户数据较少，就鼓励性地给出通用建议"
)

# ---------------------------------------------------------------------------
# 对外 API
# ---------------------------------------------------------------------------

def get_lifestyle_advice(db_module) -> dict:
    """
    调用 vivo AI 生成智能生活建议。

    参数：
        db_module: db 模块引用（避免循环导入）

    返回：
        {"ok": True, "data": {"advice": "...", "model": "..."}}
        或 {"ok": False, "error": "..."}
    """
    try:
        import requests
    except ImportError:
        return {"ok": False, "error": "缺少 requests 库，请执行 pip install requests"}

    # 1. 收集数据
    today_str = datetime.now().strftime("%Y-%m-%d")
    days = _last_7_days()

    schedules_by_day = {}
    total_schedules = 0
    completed_count = 0
    deleted_count = 0

    for day in days:
        result = db_module.get_schedules_by_date(day)
        if result["ok"]:
            day_schedules = result["data"]
            schedules_by_day[day] = day_schedules
            total_schedules += len(day_schedules)
            completed_count += sum(1 for s in day_schedules if s.get("status") == "completed")

    # 统计被删除的（从操作历史中提取）
    history_result = db_module.get_history(limit=50)
    recent_history = history_result.get("data", []) if history_result["ok"] else []
    deleted_count = sum(1 for h in recent_history if h.get("operation") == "delete")

    # 设置
    settings_result = db_module.get_settings()
    settings = settings_result.get("data", {}) if settings_result["ok"] else {}

    # 2. 构建 prompt
    user_prompt = _build_prompt(
        schedules_by_day, total_schedules, completed_count,
        deleted_count, recent_history[:20], settings, today_str
    )

    # 3. 调用 AI
    request_id = str(uuid.uuid4())
    headers = {
        "Content-Type": "application/json; charset=utf-8",
        "Authorization": f"Bearer {APP_KEY}",
    }
    payload = {
        "model": MODEL_NAME,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_prompt}
        ],
        "temperature": 0.7,
        "max_tokens": 512,
        "stream": False,
        "thinking": {"type": "enabled"}
    }

    try:
        response = requests.post(
            API_URL,
            headers=headers,
            params={"request_id": request_id},
            json=payload,
            timeout=TIMEOUT
        )
        response.raise_for_status()
        response_data = response.json()

        content = response_data["choices"][0]["message"]["content"]
        return {
            "ok": True,
            "data": {
                "advice": content.strip(),
                "model": response_data.get("model", MODEL_NAME)
            }
        }

    except requests.exceptions.Timeout:
        return {"ok": False, "error": "AI 服务响应超时，请稍后重试"}
    except requests.exceptions.ConnectionError:
        return {"ok": False, "error": "无法连接 AI 服务，请检查网络"}
    except Exception as e:
        error_msg = str(e)
        # 尝试提取 API 返回的错误信息
        try:
            if "response" in locals() and response is not None:
                error_detail = response.text[:200]
                error_msg = f"AI 服务错误：{error_detail}"
        except:
            pass
        return {"ok": False, "error": error_msg}


# ---------------------------------------------------------------------------
# 本地调试入口
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    import db as dummy_db

    dummy_db.init_db()
    print("正在请求 AI 生活建议...\n")
    result = get_lifestyle_advice(dummy_db)
    if result["ok"]:
        print(f"模型：{result['data']['model']}")
        print(f"建议：\n{result['data']['advice']}")
    else:
        print(f"失败：{result['error']}")
