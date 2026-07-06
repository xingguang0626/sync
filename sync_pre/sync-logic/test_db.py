"""
test_db.py — db.py 单元测试
===========================
直接调 db.py 函数验证 CRUD，覆盖第 4 阶段所有新功能。
零依赖，python test_db.py 直接跑。

测试流程：
  1. 备份现有 sync.db（如有）
  2. 删除 sync.db 用全新库测试
  3. 跑所有用例
  4. 恢复备份
"""

import sys
import os
import shutil
import traceback

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import db


# 用例：(name, callable, expected_ok, expected_keyword_in_error_or_None)
TEST_CASES = []


def case(name: str):
    """装饰器：注册一个测试用例"""
    def wrap(fn):
        TEST_CASES.append((name, fn))
        return fn
    return wrap


# ============================================================================
# 测试用例
# ============================================================================

@case("1. init_db 建表")
def t_init_db():
    r = db.init_db()
    assert r["ok"], f"init_db 失败: {r}"
    # 验证 sync.db 文件存在
    assert os.path.exists(db.DB_PATH), "sync.db 文件未生成"
    return True

@case("2. create_schedule 合法字段")
def t_create_ok():
    r = db.create_schedule({
        "title": "深度学习",
        "date": "2026-07-04",
        "start_time": "15:00",
        "duration": 90,
        "priority": "P0",
        "repeat": "none",
        "note": "Kotlin + Compose"
    })
    assert r["ok"], f"创建失败: {r}"
    assert "id" in r["data"], "返回数据缺少 id"
    assert r["data"]["title"] == "深度学习", "title 不匹配"
    assert r["data"]["end_time"] == "16:30", f"end_time 自动计算错误: {r['data']['end_time']}"
    return True

@case("3. create_schedule 空标题")
def t_create_empty_title():
    r = db.create_schedule({"title": "", "date": "2026-07-04", "start_time": "08:00", "duration": 60, "priority": "P1"})
    assert not r["ok"], f"应该报错: {r}"
    assert "title" in r["error"], f"错误信息应提到 title: {r['error']}"
    return True

@case("4. create_schedule 非法优先级")
def t_create_invalid_priority():
    r = db.create_schedule({"title": "x", "date": "2026-07-04", "start_time": "08:00", "duration": 60, "priority": "P99"})
    assert not r["ok"], f"应该报错: {r}"
    assert "priority" in r["error"]
    return True

@case("5. create_schedule 时长非法")
def t_create_invalid_duration():
    r = db.create_schedule({"title": "x", "date": "2026-07-04", "start_time": "08:00", "duration": 0, "priority": "P1"})
    assert not r["ok"]
    assert "duration" in r["error"]
    return True

@case("6. get_schedules_by_date 查询当天")
def t_get_by_date():
    db.create_schedule({"title": "晨练", "date": "2026-07-04", "start_time": "06:00", "duration": 30, "priority": "P2"})
    db.create_schedule({"title": "午餐", "date": "2026-07-04", "start_time": "12:00", "duration": 45, "priority": "P2"})
    r = db.get_schedules_by_date("2026-07-04")
    assert r["ok"]
    assert len(r["data"]) >= 2, f"应至少 2 条: {r['data']}"
    # 按 start_time 升序
    times = [s["start_time"] for s in r["data"]]
    assert times == sorted(times), f"未按 start_time 升序: {times}"
    return True

@case("7. get_schedule 查单条")
def t_get_one():
    c = db.create_schedule({"title": "测试", "date": "2026-07-04", "start_time": "10:00", "duration": 60, "priority": "P1"})
    sid = c["data"]["id"]
    r = db.get_schedule(sid)
    assert r["ok"]
    assert r["data"]["id"] == sid
    return True

@case("8. get_schedule 不存在的 id")
def t_get_one_not_found():
    r = db.get_schedule(999999)
    assert not r["ok"]
    assert "不存在" in r["error"]
    return True

@case("9. update_schedule 改标题")
def t_update_title():
    c = db.create_schedule({"title": "原标题", "date": "2026-07-04", "start_time": "10:00", "duration": 60, "priority": "P1"})
    sid = c["data"]["id"]
    r = db.update_schedule(sid, {"title": "新标题"})
    assert r["ok"]
    assert r["data"]["title"] == "新标题"
    # 验证数据库里确实改了
    g = db.get_schedule(sid)
    assert g["data"]["title"] == "新标题"
    return True

@case("10. update_schedule 改时间 → 自动重算 end_time")
def t_update_recalc_end():
    c = db.create_schedule({"title": "test", "date": "2026-07-04", "start_time": "10:00", "duration": 60, "priority": "P1"})
    sid = c["data"]["id"]
    r = db.update_schedule(sid, {"start_time": "14:00", "duration": 30})
    assert r["ok"]
    assert r["data"]["end_time"] == "14:30", f"end_time 应为 14:30，实际 {r['data']['end_time']}"
    return True

@case("11. delete_schedule 删除后查不到")
def t_delete():
    c = db.create_schedule({"title": "to_delete", "date": "2026-07-04", "start_time": "10:00", "duration": 60, "priority": "P1"})
    sid = c["data"]["id"]
    r = db.delete_schedule(sid)
    assert r["ok"]
    g = db.get_schedule(sid)
    assert not g["ok"]
    return True

@case("12. delete_schedule 不存在的 id")
def t_delete_not_found():
    r = db.delete_schedule(999999)
    assert not r["ok"]
    return True

@case("13. mark_complete 改 status")
def t_mark_complete():
    c = db.create_schedule({"title": "to_complete", "date": "2026-07-04", "start_time": "10:00", "duration": 60, "priority": "P1"})
    sid = c["data"]["id"]
    r = db.mark_complete(sid)
    assert r["ok"]
    assert r["data"]["status"] == "completed"
    return True

@case("14. postpone_schedule 默认顺延到明天")
def t_postpone_default():
    c = db.create_schedule({"title": "to_postpone", "date": "2026-07-04", "start_time": "10:00", "duration": 60, "priority": "P1"})
    sid = c["data"]["id"]
    r = db.postpone_schedule(sid)
    assert r["ok"]
    from datetime import date, timedelta
    expected = (date.today() + timedelta(days=1)).isoformat()
    assert r["data"]["date"] == expected, f"应顺延到 {expected}，实际 {r['data']['date']}"
    return True

@case("15. postpone_schedule 指定日期")
def t_postpone_specific():
    c = db.create_schedule({"title": "to_postpone2", "date": "2026-07-04", "start_time": "10:00", "duration": 60, "priority": "P1"})
    sid = c["data"]["id"]
    r = db.postpone_schedule(sid, "2026-07-10")
    assert r["ok"]
    assert r["data"]["date"] == "2026-07-10"
    return True

@case("16. postpone_schedule 不存在的 id")
def t_postpone_not_found():
    r = db.postpone_schedule(999999)
    assert not r["ok"]
    return True


# ============================================================================
# 运行器
# ============================================================================

def main() -> int:
    # 备份现有 sync.db（如有）
    backup_path = db.DB_PATH + ".backup"
    had_existing = os.path.exists(db.DB_PATH)
    if had_existing:
        shutil.copy2(db.DB_PATH, backup_path)
        os.remove(db.DB_PATH)

    print("=" * 70)
    print("db.py 单元测试（第 4 阶段 CRUD 完整覆盖）")
    print("=" * 70)

    passed = 0
    failed = 0
    for name, fn in TEST_CASES:
        try:
            fn()
            print(f"  ✅ PASS  {name}")
            passed += 1
        except AssertionError as e:
            print(f"  ❌ FAIL  {name} — {e}")
            failed += 1
        except Exception as e:
            print(f"  ❌ ERROR {name} — {e}")
            traceback.print_exc()
            failed += 1

    print("=" * 70)
    print(f"结果：{passed} 通过 / {failed} 失败 / 共 {len(TEST_CASES)} 条")
    print("=" * 70)

    # 恢复备份
    if had_existing and os.path.exists(backup_path):
        os.remove(db.DB_PATH)
        shutil.move(backup_path, db.DB_PATH)
        print(f"[清理] 已恢复原 sync.db")
    elif not had_existing and os.path.exists(db.DB_PATH):
        os.remove(db.DB_PATH)
        print(f"[清理] 已删除测试用 sync.db")

    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())