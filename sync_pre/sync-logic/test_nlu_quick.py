import requests
tests = ["晚上跑步", "晚上8点跑步", "早上8点", "明天开会", "跑步"]
for t in tests:
    r = requests.post("http://192.168.2.103:8800/api/nlu/parse",
                      json={"text": t, "today_date": "2026-07-05"}, timeout=5)
    d = r.json()["data"]
    title = ""
    if d.get("draft") and isinstance(d["draft"], dict):
        title = d["draft"].get("title", "")
    print(f"{t!r:20s} -> intent={d['intent']:18s} title={title}")
