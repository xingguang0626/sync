"""
server.py — HTTP 服务入口
==========================
提供 RESTful API 供 Android 前端调用。
监听 0.0.0.0:8800，零第三方依赖（仅使用 Python 标准库）。

端点一览：
  GET    /api/timeline?date=YYYY-MM-DD     → 时间轴（含冲突检测）
  POST   /api/schedules                     → 创建日程
  GET    /api/schedules/{id}                → 查询单条
  PUT    /api/schedules/{id}                → 编辑日程
  DELETE /api/schedules/{id}                → 删除日程
  POST   /api/schedules/{id}/complete       → 标记完成
  POST   /api/schedules/{id}/postpone       → 顺延到指定日期
  GET    /api/reminders/check?date=...      → AI 提醒
  POST   /api/nlu/parse                     → 自然语言解析
  DELETE /api/schedules/clear               → 清空所有数据
  GET    /api/history?limit=20              → 操作历史记录
  GET    /api/settings                      → 获取设置
  PUT    /api/settings                      → 更新设置
  POST   /api/ai/lifestyle-advice           → AI 智能生活建议

启动方式：
  python server.py
"""

import json
import re
import sys
import os
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

# 确保能 import 同目录下的模块
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import db
import timeline
import reminders
import nlu
import ai_advice

# ---------------------------------------------------------------------------
# 配置
# ---------------------------------------------------------------------------
HOST = "0.0.0.0"
PORT = 8800


# ---------------------------------------------------------------------------
# 工具
# ---------------------------------------------------------------------------

def _json_bytes(data) -> bytes:
    """将 Python 对象序列化为 UTF-8 JSON 字节串（ensure_ascii=False 保证中文可读）"""
    return json.dumps(data, ensure_ascii=False, indent=2).encode("utf-8")


# ---------------------------------------------------------------------------
# 路由处理
# ---------------------------------------------------------------------------

def handle_timeline(params: dict, body: dict) -> tuple[int, dict]:
    """GET /api/timeline?date=YYYY-MM-DD"""
    date = params.get("date", [None])[0]
    if not date:
        return 400, {"ok": False, "data": None, "error": "缺少 date 参数"}
    result = timeline.get_timeline(date)
    return 200, result


def handle_create_schedule(params: dict, body: dict) -> tuple[int, dict]:
    """POST /api/schedules"""
    result = db.create_schedule(body)
    if result["ok"]:
        return 201, result
    return 400, result


def handle_get_schedule(params: dict, body: dict) -> tuple[int, dict]:
    """GET /api/schedules/{id}"""
    schedule_id = int(params["id"])
    result = db.get_schedule(schedule_id)
    if result["ok"]:
        return 200, result
    return 404, result


def handle_update_schedule(params: dict, body: dict) -> tuple[int, dict]:
    """PUT /api/schedules/{id}"""
    schedule_id = int(params["id"])
    result = db.update_schedule(schedule_id, body)
    if result["ok"]:
        return 200, result
    if result.get("error") == "日程不存在":
        return 404, result
    return 400, result


def handle_delete_schedule(params: dict, body: dict) -> tuple[int, dict]:
    """DELETE /api/schedules/{id}"""
    schedule_id = int(params["id"])
    result = db.delete_schedule(schedule_id)
    if result["ok"]:
        return 200, result
    return 404, result


def handle_mark_complete(params: dict, body: dict) -> tuple[int, dict]:
    """POST /api/schedules/{id}/complete"""
    schedule_id = int(params["id"])
    result = db.mark_complete(schedule_id)
    if result["ok"]:
        return 200, result
    return 404, result


def handle_postpone_schedule(params: dict, body: dict) -> tuple[int, dict]:
    """POST /api/schedules/{id}/postpone

    请求体可空（默认顺延到明天）或 {"new_date": "YYYY-MM-DD"}
    """
    schedule_id = int(params["id"])
    new_date = body.get("new_date") if body else None
    if new_date is not None:
        if not re.match(r"^\d{4}-\d{2}-\d{2}$", new_date):
            return 400, {"ok": False, "data": None, "error": "new_date 格式应为 YYYY-MM-DD"}
    result = db.postpone_schedule(schedule_id, new_date)
    if result["ok"]:
        return 200, result
    return 404, result


def handle_reminders(params: dict, body: dict) -> tuple[int, dict]:
    """GET /api/reminders/check?date=YYYY-MM-DD"""
    date = params.get("date", [None])[0]
    if not date:
        return 400, {"ok": False, "data": None, "error": "缺少 date 参数"}
    result = reminders.check_reminders(date)
    return 200, result


def handle_nlu_parse(params: dict, body: dict) -> tuple[int, dict]:
    """POST /api/nlu/parse

    请求体：{"text": "...", "today_date": "YYYY-MM-DD"}
    返回：  {"ok": true, "data": {intent, draft, confidence, parsed_details, ...}}
    """
    text = body.get("text", "")
    today_date = body.get("today_date", "")
    if not text:
        return 400, {"ok": False, "data": None, "error": "缺少 text 参数"}
    if not today_date:
        from datetime import date as _date
        today_date = _date.today().isoformat()
    result = nlu.parse(text, today_date)
    return 200, result


def handle_clear_all(params: dict, body: dict) -> tuple[int, dict]:
    """DELETE /api/schedules/clear — 清空所有日程和操作历史"""
    result = db.clear_all()
    return 200, result


def handle_get_history(params: dict, body: dict) -> tuple[int, dict]:
    """GET /api/history?limit=20 — 获取操作历史"""
    limit_str = params.get("limit", ["20"])[0]
    try:
        limit = int(limit_str)
    except (ValueError, TypeError):
        limit = 20
    result = db.get_history(limit)
    return 200, result


def handle_get_settings(params: dict, body: dict) -> tuple[int, dict]:
    """GET /api/settings — 获取所有设置项"""
    result = db.get_settings()
    return 200, result


def handle_update_settings(params: dict, body: dict) -> tuple[int, dict]:
    """PUT /api/settings — 更新设置项"""
    if not body:
        return 400, {"ok": False, "data": None, "error": "请求体为空，请提供要更新的设置项"}
    result = db.update_settings(body)
    return 200, result


def handle_lifestyle_advice(params: dict, body: dict) -> tuple[int, dict]:
    """POST /api/ai/lifestyle-advice — AI 智能生活建议"""
    result = ai_advice.get_lifestyle_advice(db)
    if result["ok"]:
        return 200, result
    return 500, result


# ---------------------------------------------------------------------------
# 路由表（支持路径参数）
# ---------------------------------------------------------------------------

# 每条规则：(HTTP 方法, 正则 pattern, 处理函数, 是否需要 body, 参数名列表)
ROUTE_PATTERNS = [
    # GET 类
    ("GET",    re.compile(r"^/api/timeline$"),                 handle_timeline,           False, []),
    ("GET",    re.compile(r"^/api/schedules/(\d+)$"),          handle_get_schedule,       False, ["id"]),
    ("GET",    re.compile(r"^/api/reminders/check$"),         handle_reminders,          False, []),
    ("GET",    re.compile(r"^/api/history$"),                  handle_get_history,        False, []),
    ("GET",    re.compile(r"^/api/settings$"),                 handle_get_settings,       False, []),

    # POST 类（需要 body）
    ("POST",   re.compile(r"^/api/schedules$"),                 handle_create_schedule,     True,  []),
    ("POST",   re.compile(r"^/api/schedules/(\d+)/complete$"),  handle_mark_complete,       True,  ["id"]),
    ("POST",   re.compile(r"^/api/schedules/(\d+)/postpone$"),  handle_postpone_schedule,   True,  ["id"]),
    ("POST",   re.compile(r"^/api/nlu/parse$"),                 handle_nlu_parse,           True,  []),
    ("POST",   re.compile(r"^/api/ai/lifestyle-advice$"),       handle_lifestyle_advice,    True,  []),

    # PUT / DELETE
    ("PUT",    re.compile(r"^/api/schedules/(\d+)$"),          handle_update_schedule,     True,  ["id"]),
    ("PUT",    re.compile(r"^/api/settings$"),                 handle_update_settings,     True,  []),
    ("DELETE", re.compile(r"^/api/schedules/clear$"),          handle_clear_all,           False, []),
    ("DELETE", re.compile(r"^/api/schedules/(\d+)$"),          handle_delete_schedule,     False, ["id"]),
]


def _resolve_route(method: str, path: str):
    """根据 method + path 匹配路由，返回 (handler, params_dict, need_body) 或 None"""
    for m, pattern, handler, need_body, param_names in ROUTE_PATTERNS:
        if m != method:
            continue
        m_obj = pattern.match(path)
        if m_obj:
            params = {name: value for name, value in zip(param_names, m_obj.groups())}
            return handler, params, need_body
    return None


# ---------------------------------------------------------------------------
# HTTP Handler
# ---------------------------------------------------------------------------

class SyncHandler(BaseHTTPRequestHandler):
    """处理所有 HTTP 请求"""

    def _send(self, status_code: int, data: dict):
        """发送 JSON 响应"""
        body = _json_bytes(data)
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def _log_request(self, method: str, path: str, status: int):
        """控制台日志"""
        print(f"[{method}] {path} → {status}")

    def _dispatch(self, method: str):
        """统一分发：解析路径 → 读 body → 调 handler → 发响应"""
        parsed = urlparse(self.path)
        path = parsed.path.rstrip("/") or "/"
        query_params = parse_qs(parsed.query)

        result = _resolve_route(method, path)
        if result is None:
            self._send(404, {"ok": False, "data": None, "error": f"未知路由: {method} {path}"})
            self._log_request(method, self.path, 404)
            return

        handler, path_params, need_body = result
        body = {}
        if need_body:
            content_length = int(self.headers.get("Content-Length", 0))
            raw_body = self.rfile.read(content_length) if content_length > 0 else b"{}"
            try:
                body = json.loads(raw_body) if raw_body else {}
            except json.JSONDecodeError:
                self._send(400, {"ok": False, "data": None, "error": "请求体不是合法的 JSON"})
                self._log_request(method, self.path, 400)
                return

        # 合并 query params 和 path params
        merged_params = {**query_params, **path_params}

        try:
            # 统一签名：所有 handler 都是 (params, body)，params 含 path 和 query 参数
            merged_params = {**query_params, **path_params}
            status, data = handler(merged_params, body)
        except Exception as e:
            status, data = 500, {"ok": False, "data": None, "error": f"服务器内部错误: {e}"}

        self._send(status, data)
        self._log_request(method, self.path, status)

    # ---- HTTP 方法分发 ----

    def do_GET(self):
        self._dispatch("GET")

    def do_POST(self):
        self._dispatch("POST")

    def do_PUT(self):
        self._dispatch("PUT")

    def do_DELETE(self):
        self._dispatch("DELETE")

    def do_OPTIONS(self):
        """预检请求（CORS）"""
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    # 禁用请求日志（改用自定义 _log_request）
    def log_message(self, format, *args):
        pass


# ---------------------------------------------------------------------------
# 启动入口
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    print("=" * 56)
    print("  Sync（随刻）后端服务 — P2 阶段（补完整度）")
    print(f"  监听地址: http://{HOST}:{PORT}")
    print("=" * 56)
    print()
    print("已注册的路由：")
    for m, pattern, handler, need_body, _ in ROUTE_PATTERNS:
        body_mark = " [body]" if need_body else ""
        print(f"  {m:6s} {pattern.pattern:32s} → {handler.__name__}{body_mark}")
    print()

    # 启动时自动初始化数据库
    init_result = db.init_db()
    if init_result["ok"]:
        print(f"[初始化] 数据库已就绪 → {db.DB_PATH}")
    else:
        print(f"[初始化] 数据库初始化失败: {init_result.get('error')}")
        sys.exit(1)

    print()

    server = HTTPServer((HOST, PORT), SyncHandler)
    try:
        print("服务已启动，按 Ctrl+C 停止\n")
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n服务已停止")
        server.server_close()