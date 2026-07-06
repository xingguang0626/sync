"""
demo_e2e.py — 端到端演示脚本
=============================
启动 Python 后端服务（server.py），模拟 Android 前端的完整使用流程：
  1. 创建 5 条不同优先级的日程
  2. 查询时间轴
  3. 标记完成其中一条
  4. 顺延一条到明天
  5. 编辑一条标题
  6. 删除一条
  7. 再次查询时间轴，验证最终状态

零依赖。python demo_e2e.py 直接跑。
"""

import json
import multiprocessing
import sys
import time
import urllib.error
import urllib.request
from datetime import date, timedelta

BASE_URL = "http://localhost:8800"


# ---------------------------------------------------------------------------
# 后台启动 server
# ---------------------------------------------------------------------------

def _run_server():
    """在子进程里启 server.py"""
    import server as _server_mod  # noqa: F401
    # server.py 的 main 逻辑写在 __main__ 块，import 不会启动
    # 这里复制它的启动流程
    import db as _db
    init_result = _db.init_db()
    if not init_result["ok"]:
        print(f"init_db 失败: {init_result}", file=sys.stderr)
        sys.exit(1)
    import http.server
    server = http.server.HTTPServer(("127.0.0.1", 8800), _server_mod.SyncHandler)
    server.serve_forever()


def _http(method: str, path: str, body: dict | None = None) -> tuple[int, dict]:
    """HTTP 调用辅助"""
    url = f"{BASE_URL}{path}"
    data = json.dumps(body).encode("utf-8") if body else None
    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"} if data else {},
        method=method,
    )
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode("utf-8"))


# ---------------------------------------------------------------------------
# 测试流程
# ---------------------------------------------------------------------------

def step(title: str):
    print(f"\n{'=' * 60}\n  {title}\n{'=' * 60}")


def main() -> int:
    # 1. 启动 server
    step("启动后端服务")
    proc = multiprocessing.Process(target=_run_server, daemon=True)
    proc.start()
    time.sleep(1.5)
    if not proc.is_alive():
        print("server 启动失败")
        return 1

    try:
        # 2. 清空当日数据（按日期过滤，不删整个库）
        today = date.today().isoformat()
        # 删昨日创建的所有 today 数据
        existing = _http("GET", f"/api/timeline?date={today}")[1].get("data", {}).get("items", [])
        for item in existing:
            for s in item.get("schedules", [item.get("schedule")]):
                if s:
                    _http("DELETE", f"/api/schedules/{s['id']}")

        # 3. 创建 5 条日程
        step(f"1. 创建 5 条日程（{today}）")
        ids = []
        seeds = [
            {"title": "晨跑",      "date": today, "start_time": "07:00", "duration": 30, "priority": "P2", "note": "公园 5km"},
            {"title": "深度学习",   "date": today, "start_time": "15:00", "duration": 90, "priority": "P0", "note": "Kotlin + Compose"},
            {"title": "小组会议",   "date": today, "start_time": "10:00", "duration": 60, "priority": "P1"},
            {"title": "晚读",       "date": today, "start_time": "21:00", "duration": 45, "priority": "P1", "repeat": "daily"},
            {"title": "Deadline",   "date": today, "start_time": "22:30", "duration": 30, "priority": "P0", "note": "今晚必须提交"},
        ]
        for seed in seeds:
            status, resp = _http("POST", "/api/schedules", seed)
            assert status == 201 and resp["ok"], f"创建失败: {status} {resp}"
            ids.append(resp["data"]["id"])
            print(f"  ✓ 创建 id={resp['data']['id']:3d}  {seed['title']:8s}  {seed['start_time']}-{resp['data']['end_time']}  {seed['priority']}")
        print(f"\n  共创建 {len(ids)} 条")

        # 4. 查询时间轴
        step("2. 查询时间轴（应看到 5 条）")
        status, resp = _http("GET", f"/api/timeline?date={today}")
        assert status == 200 and resp["ok"], f"timeline 失败: {status} {resp}"
        timeline_items = resp["data"]["items"]
        print(f"  返回 {len(timeline_items)} 个 items")
        for it in timeline_items:
            t = it.get("type")
            if t == "single":
                s = it["schedule"]
                print(f"    - single  {s['start_time']}  {s['title']}  {s['priority']}")
            else:
                print(f"    - {t}  共 {len(it['schedules'])} 条")

        # 5. 标记完成 第 2 条（深度学习）
        step("3. 标记完成『深度学习』(id=" + str(ids[1]) + ")")
        status, resp = _http("POST", f"/api/schedules/{ids[1]}/complete")
        assert status == 200 and resp["ok"], f"complete 失败: {status} {resp}"
        assert resp["data"]["status"] == "completed"
        print(f"  ✓ status={resp['data']['status']}")

        # 6. 顺延 第 5 条（Deadline）到明天
        step(f"4. 顺延『Deadline』(id={ids[4]}) 到明天")
        tomorrow = (date.today() + timedelta(days=1)).isoformat()
        status, resp = _http("POST", f"/api/schedules/{ids[4]}/postpone", {"new_date": tomorrow})
        assert status == 200 and resp["ok"], f"postpone 失败: {status} {resp}"
        assert resp["data"]["date"] == tomorrow
        print(f"  ✓ date={resp['data']['date']}")

        # 7. 编辑 第 3 条（小组会议）标题
        step(f"5. 编辑『小组会议』(id={ids[2]}) 标题")
        status, resp = _http("PUT", f"/api/schedules/{ids[2]}", {"title": "项目周会"})
        assert status == 200 and resp["ok"], f"update 失败: {status} {resp}"
        assert resp["data"]["title"] == "项目周会"
        print(f"  ✓ title={resp['data']['title']}")

        # 8. 删除 第 4 条（晚读）
        step(f"6. 删除『晚读』(id={ids[3]})")
        status, resp = _http("DELETE", f"/api/schedules/{ids[3]}")
        assert status == 200 and resp["ok"], f"delete 失败: {status} {resp}"

        # 9. 再次查询时间轴（验证最终状态）
        # id=5 (Deadline) 已顺延到明天，不在今天的 timeline 里
        # 所以今天应剩 3 条：id=1 晨跑 / id=2 深度学习（completed）/ id=3 项目周会
        step("7. 最终查询时间轴（今天应剩 3 条，id=5 已顺延到明天）")
        status, resp = _http("GET", f"/api/timeline?date={today}")
        assert status == 200 and resp["ok"]
        remaining = []
        for it in resp["data"]["items"]:
            if it.get("type") == "single":
                remaining.append(it["schedule"])
            else:
                remaining.extend(it.get("schedules", []))
        print(f"\n  今天剩余 {len(remaining)} 条：")
        for s in remaining:
            mark = "✓" if s["status"] == "completed" else " "
            print(f"    [{mark}] id={s['id']:3d}  {s['start_time']}  {s['title']:8s}  {s['priority']}  ({s['status']})")
        assert len(remaining) == 3, f"今天应剩 3 条，实际 {len(remaining)}（注意：id=5 已顺延到明天）"

        # 10. 查明天 timeline（应该有 Deadline）
        step("8. 查明天 timeline（应看到 id=5 Deadline）")
        status, resp = _http("GET", f"/api/timeline?date={tomorrow}")
        assert status == 200 and resp["ok"]
        tomorrow_items = resp["data"]["items"]
        print(f"  明天剩余 {len(tomorrow_items)} 条")
        for it in tomorrow_items:
            s = it.get("schedule") if it.get("type") == "single" else it["schedules"][0]
            print(f"    - id={s['id']:3d}  {s['start_time']}  {s['title']}  ({s['date']})")

        # 10. 查 reminders
        step("9. 查询 AI 提醒")
        status, resp = _http("GET", f"/api/reminders/check?date={today}")
        assert status == 200 and resp["ok"]
        print(f"  返回 {len(resp['data'])} 条提醒")
        for r in resp["data"]:
            print(f"    - {r['type']:15s}  {r['message']}")

        # 11. 测 NLU
        step("10. 测 NLU 解析：『今晚 10 点 deadline』")
        status, resp = _http("POST", "/api/nlu/parse", {"text": "今晚 10 点 deadline", "today_date": today})
        assert status == 200 and resp["ok"], f"nlu 失败: {status} {resp}"
        data = resp["data"]
        print(f"  intent={data['intent']}  confidence={data['confidence']:.2f}")
        print(f"  draft={data.get('draft')}")

        print("\n" + "=" * 60)
        print("  端到端演示全部通过 ✅")
        print("=" * 60)
        return 0

    except AssertionError as e:
        print(f"\n❌ 断言失败: {e}")
        return 1
    except Exception as e:
        print(f"\n❌ 异常: {e}")
        import traceback
        traceback.print_exc()
        return 1
    finally:
        # 清理
        if proc.is_alive():
            proc.terminate()
            proc.join(timeout=3)
            if proc.is_alive():
                proc.kill()


if __name__ == "__main__":
    sys.exit(main())