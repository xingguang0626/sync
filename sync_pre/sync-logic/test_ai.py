"""AI 生活建议端点快速测试"""
import db
import ai_advice

db.init_db()

# 创建几条测试日程
db.create_schedule({"title": "晨跑", "date": "2026-07-05", "start_time": "07:00", "duration": 30, "priority": "P2"})
db.create_schedule({"title": "深度学习", "date": "2026-07-05", "start_time": "09:00", "duration": 120, "priority": "P0", "note": "Kotlin + Compose 实战"})
db.create_schedule({"title": "午休", "date": "2026-07-05", "start_time": "12:30", "duration": 30, "priority": "P2"})
db.create_schedule({"title": "小组会议", "date": "2026-07-05", "start_time": "14:00", "duration": 60, "priority": "P1"})

# 标记一条完成
schedules = db.get_schedules_by_date("2026-07-05")
if schedules["ok"] and schedules["data"]:
    sid = schedules["data"][0]["id"]
    db.mark_complete(sid)

print("测试数据已创建。正在调用 AI...\n")
result = ai_advice.get_lifestyle_advice(db)

if result["ok"]:
    print("=" * 50)
    print("AI 智能生活建议")
    print("=" * 50)
    print(result["data"]["advice"])
    print("=" * 50)
    print(f"模型：{result['data']['model']}")
else:
    print(f"调用失败：{result['error']}")

# 清理测试数据
db.clear_all()
