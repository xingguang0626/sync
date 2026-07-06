"""P2 阶段新功能测试：清空数据 / 操作历史 / 设置"""
import db
import json

# 0. 重建数据库
import os
db_path = os.path.join(os.path.dirname(__file__), "sync.db")
if os.path.exists(db_path):
    os.remove(db_path)

r = db.init_db()
assert r["ok"], f"init_db 失败: {r}"
print("0. init_db: OK")

# 1. 默认设置
r = db.get_settings()
assert r["ok"]
assert r["data"]["default_priority"] == "P1"
assert r["data"]["default_duration"] == "60"
print(f"1. default settings: {json.dumps(r['data'], ensure_ascii=False)}")

# 2. 创建日程 → 自动记入操作历史
r = db.create_schedule({"title": "深度学习", "date": "2026-07-05", "start_time": "10:00", "duration": 90, "priority": "P0"})
assert r["ok"], f"create 失败: {r}"
sid1 = r["data"]["id"]
print(f"2. create: OK, id={sid1}")

r = db.create_schedule({"title": "午休", "date": "2026-07-05", "start_time": "12:30", "duration": 30, "priority": "P2"})
assert r["ok"], f"create2 失败: {r}"
sid2 = r["data"]["id"]
print(f"   create2: OK, id={sid2}")

# 3. 更新 → 记录 old + new
r = db.update_schedule(sid1, {"title": "深度学习(强化)", "duration": 120})
assert r["ok"], f"update 失败: {r}"
print(f"3. update: OK, title={r['data']['title']}, duration={r['data']['duration']}")

# 4. 标记完成
r = db.mark_complete(sid2)
assert r["ok"], f"complete 失败: {r}"
print(f"4. complete: OK, status={r['data']['status']}")

# 5. 顺延
r = db.postpone_schedule(sid1)
assert r["ok"], f"postpone 失败: {r}"
print(f"5. postpone: OK, new_date={r['data']['date']}")

# 6. 删除
r = db.delete_schedule(sid2)
assert r["ok"], f"delete 失败: {r}"
print(f"6. delete: OK")

# 7. 操作历史应有 6 条（2 create + 1 update + 1 complete + 1 postpone + 1 delete）
r = db.get_history()
assert r["ok"]
print(f"7. history: {len(r['data'])} 条记录（期望 6）")
assert len(r["data"]) == 6, f"期望 6 条，实际 {len(r['data'])}"
for h in r["data"]:
    print(f"   [{h['operation']:8s}] sid={str(h['schedule_id']):4s}  {h['created_at'][:19]}")

# 8. 修改设置
r = db.update_settings({"default_priority": "P0", "default_duration": "90"})
assert r["ok"]
assert r["data"]["default_priority"] == "P0"
assert r["data"]["default_duration"] == "90"
print(f"8. update_settings: {json.dumps(r['data'], ensure_ascii=False)}")

# 9. 清空数据
r = db.clear_all()
assert r["ok"], f"clear_all 失败: {r}"
print(f"9. clear_all: OK")

# 10. 清空后设置仍保留
r = db.get_settings()
assert r["data"]["default_priority"] == "P0"
assert r["data"]["default_duration"] == "90"
print(f"10. settings after clear: {json.dumps(r['data'], ensure_ascii=False)} — 保留了!")

# 11. 清空后日程和历史均为空
r = db.get_schedules_by_date("2026-07-05")
assert len(r["data"]) == 0
r = db.get_history()
assert len(r["data"]) == 0
print("11. schedules & history: both empty — OK")

print()
print("=" * 40)
print("ALL 11 TESTS PASSED")
print("=" * 40)
