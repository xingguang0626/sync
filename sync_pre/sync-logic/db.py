"""
db.py — SQLite 数据库操作模块
===============================
负责 schedules / operation_history / settings 三张表的建表与增删改查。
所有函数遵循统一返回格式：{"ok": bool, "data": ...} 或 {"ok": bool, "error": str}
"""

import json
import sqlite3
import os
from datetime import datetime, timedelta

# ---------------------------------------------------------------------------
# 全局配置
# ---------------------------------------------------------------------------

# 数据库文件路径：与本文件同目录下的 sync.db
DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "sync.db")

# 合法的枚举值（与前端 domain/model 中的枚举对齐）
VALID_PRIORITIES = {"P0", "P1", "P2"}
VALID_STATUSES   = {"pending", "in_progress", "completed", "cancelled"}
VALID_REPEATS    = {"none", "daily", "weekly", "weekdays", "weekends", "monthly"}

# ---------------------------------------------------------------------------
# 内部工具
# ---------------------------------------------------------------------------

def _get_conn():
    """获取数据库连接（自动创建文件）。调用方负责关闭。"""
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row  # 让查询结果支持按列名访问
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def _now():
    """返回当前时间的 ISO8601 字符串"""
    return datetime.now().isoformat()


def _tomorrow():
    """返回明天的日期字符串 YYYY-MM-DD"""
    return (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")


def _calc_end_time(start_time: str, duration_minutes: int) -> str:
    """根据开始时间(HH:MM)和持续时间(分钟)计算结束时间(HH:MM)"""
    h, m = map(int, start_time.split(":"))
    total = h * 60 + m + duration_minutes
    return f"{total // 60 % 24:02d}:{total % 60:02d}"


def _validate_schedule_fields(data: dict, is_update: bool = False) -> str | None:
    """
    校验日程字段。返回 None 表示通过，否则返回错误信息字符串。
    is_update=True 时允许部分字段为空（编辑场景）。
    """
    if not is_update:
        title = data.get("title", "").strip()
        if not title:
            return "title 不能为空"

    date = data.get("date")
    if date is not None:
        date = date.strip()
        if not date:
            return "date 不能为空"

    start_time = data.get("start_time")
    if start_time is not None:
        start_time = start_time.strip()
        if not start_time:
            return "start_time 不能为空"

    duration = data.get("duration")
    if duration is not None:
        if not isinstance(duration, int) or duration <= 0:
            return "duration 必须是正整数（分钟）"

    priority = data.get("priority")
    if priority is not None and priority not in VALID_PRIORITIES:
        return f"priority 必须是 {', '.join(sorted(VALID_PRIORITIES))} 之一"

    status = data.get("status")
    if status is not None and status not in VALID_STATUSES:
        return f"status 必须是 {', '.join(sorted(VALID_STATUSES))} 之一"

    repeat = data.get("repeat")
    if repeat is not None and repeat not in VALID_REPEATS:
        return f"repeat 必须是 {', '.join(sorted(VALID_REPEATS))} 之一"

    return None


def _row_to_dict(row: sqlite3.Row) -> dict:
    """将 sqlite3.Row 转为普通字典"""
    return dict(row)


# ---------------------------------------------------------------------------
# 对外 API
# ---------------------------------------------------------------------------

def init_db() -> dict:
    """
    初始化数据库：创建 schedules 和 operation_history 两张表（如不存在）。
    App 启动时调用一次，幂等安全。
    """
    conn = _get_conn()
    try:
        conn.executescript("""
            CREATE TABLE IF NOT EXISTS schedules (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                title       TEXT    NOT NULL,
                date        TEXT    NOT NULL,
                start_time  TEXT    NOT NULL,
                end_time    TEXT    NOT NULL,
                duration    INTEGER NOT NULL DEFAULT 60,
                priority    TEXT    NOT NULL DEFAULT 'P1',
                status      TEXT    NOT NULL DEFAULT 'pending',
                repeat      TEXT    NOT NULL DEFAULT 'none',
                note        TEXT    DEFAULT '',
                created_at  TEXT    NOT NULL,
                updated_at  TEXT    NOT NULL
            );

            CREATE INDEX IF NOT EXISTS idx_schedules_date
                ON schedules(date);

            CREATE TABLE IF NOT EXISTS operation_history (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                schedule_id INTEGER,
                operation   TEXT    NOT NULL,
                old_value   TEXT,
                new_value   TEXT,
                created_at  TEXT    NOT NULL
            );

            CREATE TABLE IF NOT EXISTS settings (
                key   TEXT PRIMARY KEY,
                value TEXT NOT NULL
            );

            -- 写入默认设置（如已存在则跳过）
            INSERT OR IGNORE INTO settings (key, value) VALUES ('default_priority', 'P1');
            INSERT OR IGNORE INTO settings (key, value) VALUES ('default_duration', '60');
        """)
        conn.commit()
        return {"ok": True}
    finally:
        conn.close()


def create_schedule(data: dict) -> dict:
    """
    创建日程。
    必填字段：title, date, start_time
    可选字段：duration(默认60), priority(默认P1), repeat(默认none), note(默认"")
    自动计算 end_time 并写入 created_at/updated_at。
    """
    # 校验
    error = _validate_schedule_fields(data, is_update=False)
    if error:
        return {"ok": False, "error": error}

    title      = data["title"].strip()
    date       = data["date"].strip()
    start_time = data["start_time"].strip()
    duration   = data.get("duration", 60)
    priority   = data.get("priority", "P1")
    repeat     = data.get("repeat", "none")
    note       = data.get("note", "")
    now        = _now()
    end_time   = _calc_end_time(start_time, duration)

    conn = _get_conn()
    try:
        cur = conn.execute(
            """INSERT INTO schedules
               (title, date, start_time, end_time, duration, priority, status, repeat, note, created_at, updated_at)
               VALUES (?, ?, ?, ?, ?, ?, 'pending', ?, ?, ?, ?)""",
            (title, date, start_time, end_time, duration, priority, repeat, note, now, now)
        )
        conn.commit()
        new_id = cur.lastrowid

        # 读取完整记录返回
        row = conn.execute("SELECT * FROM schedules WHERE id = ?", (new_id,)).fetchone()
        record = _row_to_dict(row)

        # 记录操作历史
        log_operation(new_id, "create", None, json.dumps(record, ensure_ascii=False))

        return {"ok": True, "data": record}
    finally:
        conn.close()


def get_schedules_by_date(date: str) -> dict:
    """按日期查询所有日程，按 start_time 升序排列"""
    conn = _get_conn()
    try:
        rows = conn.execute(
            "SELECT * FROM schedules WHERE date = ? ORDER BY start_time",
            (date,)
        ).fetchall()
        return {"ok": True, "data": [_row_to_dict(r) for r in rows]}
    finally:
        conn.close()


def get_schedule(schedule_id: int) -> dict:
    """按 id 查询单条日程"""
    conn = _get_conn()
    try:
        row = conn.execute(
            "SELECT * FROM schedules WHERE id = ?", (schedule_id,)
        ).fetchone()
        if row is None:
            return {"ok": False, "error": "日程不存在"}
        return {"ok": True, "data": _row_to_dict(row)}
    finally:
        conn.close()


def update_schedule(schedule_id: int, data: dict) -> dict:
    """
    编辑日程。只更新传入的非空字段。
    如果 start_time 或 duration 有变化，自动重算 end_time。
    自动更新 updated_at。
    """
    conn = _get_conn()
    try:
        existing = conn.execute(
            "SELECT * FROM schedules WHERE id = ?", (schedule_id,)
        ).fetchone()
        if existing is None:
            return {"ok": False, "error": "日程不存在"}

        old_record = _row_to_dict(existing)

        # 合并现有值和新值
        merged = dict(existing)
        for key in ("title", "date", "start_time", "duration", "priority", "status", "repeat", "note"):
            if key in data and data[key] is not None:
                merged[key] = data[key]

        # 校验合并后的数据
        error = _validate_schedule_fields(merged, is_update=False)
        if error:
            return {"ok": False, "error": error}

        # 重算 end_time
        merged["end_time"] = _calc_end_time(merged["start_time"], merged["duration"])
        merged["updated_at"] = _now()

        conn.execute(
            """UPDATE schedules
               SET title=?, date=?, start_time=?, end_time=?, duration=?,
                   priority=?, status=?, repeat=?, note=?, updated_at=?
               WHERE id=?""",
            (merged["title"], merged["date"], merged["start_time"], merged["end_time"],
             merged["duration"], merged["priority"], merged["status"], merged["repeat"],
             merged["note"], merged["updated_at"], schedule_id)
        )
        conn.commit()

        row = conn.execute("SELECT * FROM schedules WHERE id = ?", (schedule_id,)).fetchone()
        new_record = _row_to_dict(row)

        # 记录操作历史
        log_operation(schedule_id, "update",
                      json.dumps(old_record, ensure_ascii=False),
                      json.dumps(new_record, ensure_ascii=False))

        return {"ok": True, "data": new_record}
    finally:
        conn.close()


def delete_schedule(schedule_id: int) -> dict:
    """删除日程"""
    conn = _get_conn()
    try:
        # 删除前读取完整记录，供操作历史使用
        existing = conn.execute("SELECT * FROM schedules WHERE id = ?", (schedule_id,)).fetchone()
        if existing is None:
            return {"ok": False, "error": "日程不存在"}

        old_record = _row_to_dict(existing)
        cur = conn.execute("DELETE FROM schedules WHERE id = ?", (schedule_id,))
        conn.commit()

        # 记录操作历史
        log_operation(schedule_id, "delete",
                      json.dumps(old_record, ensure_ascii=False), None)

        return {"ok": True}
    finally:
        conn.close()


def mark_complete(schedule_id: int) -> dict:
    """标记日程为已完成（status → 'completed'）"""
    conn = _get_conn()
    try:
        existing = conn.execute("SELECT * FROM schedules WHERE id = ?", (schedule_id,)).fetchone()
        if existing is None:
            return {"ok": False, "error": "日程不存在"}

        old_record = _row_to_dict(existing)
        now = _now()
        conn.execute(
            "UPDATE schedules SET status = 'completed', updated_at = ? WHERE id = ?",
            (now, schedule_id)
        )
        conn.commit()

        row = conn.execute("SELECT * FROM schedules WHERE id = ?", (schedule_id,)).fetchone()
        new_record = _row_to_dict(row)

        # 记录操作历史
        log_operation(schedule_id, "complete",
                      json.dumps(old_record, ensure_ascii=False),
                      json.dumps(new_record, ensure_ascii=False))

        return {"ok": True, "data": new_record}
    finally:
        conn.close()


def postpone_schedule(schedule_id: int, new_date: str | None = None) -> dict:
    """顺延日程到指定日期（默认明天）"""
    conn = _get_conn()
    try:
        existing = conn.execute("SELECT * FROM schedules WHERE id = ?", (schedule_id,)).fetchone()
        if existing is None:
            return {"ok": False, "error": "日程不存在"}

        old_record = _row_to_dict(existing)
        target = new_date or _tomorrow()
        now = _now()
        conn.execute(
            "UPDATE schedules SET date = ?, updated_at = ? WHERE id = ?",
            (target, now, schedule_id)
        )
        conn.commit()

        row = conn.execute("SELECT * FROM schedules WHERE id = ?", (schedule_id,)).fetchone()
        new_record = _row_to_dict(row)

        # 记录操作历史
        log_operation(schedule_id, "postpone",
                      json.dumps(old_record, ensure_ascii=False),
                      json.dumps(new_record, ensure_ascii=False))

        return {"ok": True, "data": new_record}
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 操作历史
# ---------------------------------------------------------------------------

def log_operation(schedule_id: int | None, operation: str,
                  old_value: str | None = None, new_value: str | None = None) -> dict:
    """
    记录一次操作到 operation_history 表。
    供后续"撤销"、"操作记录查看"功能使用。
    由 db 内部函数调用，也对外暴露为独立函数以便测试。
    """
    conn = _get_conn()
    try:
        conn.execute(
            """INSERT INTO operation_history (schedule_id, operation, old_value, new_value, created_at)
               VALUES (?, ?, ?, ?, ?)""",
            (schedule_id, operation, old_value, new_value, _now())
        )
        conn.commit()
        return {"ok": True}
    finally:
        conn.close()


def get_history(limit: int = 20) -> dict:
    """获取最近的操作历史记录"""
    conn = _get_conn()
    try:
        rows = conn.execute(
            "SELECT * FROM operation_history ORDER BY created_at DESC LIMIT ?",
            (limit,)
        ).fetchall()
        return {"ok": True, "data": [_row_to_dict(r) for r in rows]}
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 清空数据
# ---------------------------------------------------------------------------

def clear_all() -> dict:
    """清空所有日程和操作历史（settings 表保留）"""
    conn = _get_conn()
    try:
        conn.execute("DELETE FROM schedules")
        conn.execute("DELETE FROM operation_history")
        conn.commit()
        return {"ok": True}
    finally:
        conn.close()


# ---------------------------------------------------------------------------
# 设置
# ---------------------------------------------------------------------------

def get_settings() -> dict:
    """获取所有设置项，返回键值对字典"""
    conn = _get_conn()
    try:
        rows = conn.execute("SELECT key, value FROM settings").fetchall()
        return {"ok": True, "data": {r["key"]: r["value"] for r in rows}}
    finally:
        conn.close()


def update_settings(data: dict) -> dict:
    """
    批量更新设置项。
    传入 {"default_priority": "P0", "default_duration": "90"}
    返回更新后的完整设置。
    """
    conn = _get_conn()
    try:
        for key, value in data.items():
            conn.execute(
                "INSERT INTO settings (key, value) VALUES (?, ?) "
                "ON CONFLICT(key) DO UPDATE SET value = excluded.value",
                (key, str(value))
            )
        conn.commit()
        # 重新读取全部设置返回
        rows = conn.execute("SELECT key, value FROM settings").fetchall()
        return {"ok": True, "data": {r["key"]: r["value"] for r in rows}}
    finally:
        conn.close()
