"""
test_nlu.py — nlu.py 的 20 条 MVP 用例验证
============================================
对应《本地规则.md》§17 的 20 条测试用例。
零依赖，直接 python test_nlu.py 即可运行。
"""

import sys
import os

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import nlu

# 测试用例：(输入, today_date, 期望的 intent, 期望的 title 子串或 None)
TEST_CASES = [
    # 1
    ("明天早上8点跑步",          "2026-07-04", "create_schedule", "跑步"),
    # 2
    ("今天下午3点学习90分钟",    "2026-07-04", "create_schedule", "学习"),
    # 3
    ("晚上10点deadline",        "2026-07-04", "create_schedule", "Deadline"),
    # 4
    ("今晚有deadline，晚点睡",   "2026-07-04", "create_schedule", "Deadline"),
    # 5
    ("每天晚上读书45分钟",       "2026-07-04", "create_schedule", "读书"),
    # 6
    ("周五下午4点小组会议",      "2026-07-04", "create_schedule", "会议"),
    # 7
    ("下午2点到4点复习数学",     "2026-07-04", "create_schedule", "复习数学"),
    # 8
    ("半小时后休息",             "2026-07-04", "create_schedule", "休息"),
    # 9
    ("明天考试",                 "2026-07-04", "create_schedule", "考试"),
    # 10
    ("取消今晚跑步",             "2026-07-04", "delete_schedule", None),
    # 11
    ("把学习改到晚上8点",        "2026-07-04", "update_schedule", None),
    # 12
    ("今天太累了，晚读明天再做", "2026-07-04", "postpone_schedule", None),
    # 13
    ("开启学习模式",             "2026-07-04", "start_mode", None),
    # 14
    ("小憩20分钟",               "2026-07-04", "start_mode", None),
    # 15
    ("明天晚上写作业1小时",      "2026-07-04", "create_schedule", "写作业"),
    # 16
    ("每周一晚上跑步",           "2026-07-04", "create_schedule", "跑步"),
    # 17
    ("有空再整理书桌",           "2026-07-04", "create_schedule", "整理"),
    # 18
    ("下午提交作业",             "2026-07-04", "create_schedule", "作业"),
    # 19
    ("睡前背单词30分钟",         "2026-07-04", "create_schedule", "背单词"),
    # 20
    ("晚点弄一下那个",           "2026-07-04", "unknown",          None),
]


def run_test(idx: int, text: str, today: str, expected_intent: str, expected_title_substr: str | None) -> bool:
    result = nlu.parse(text, today)
    if not result.get("ok"):
        print(f"  [{idx:2d}] ❌ FAIL 解析失败: {result.get('error')}")
        return False
    data = result["data"]
    intent = data.get("intent")
    draft = data.get("draft", {})
    title = draft.get("title", "") if isinstance(draft, dict) else ""

    ok = True
    if intent != expected_intent:
        print(f"  [{idx:2d}] ❌ FAIL intent 期望={expected_intent}, 实际={intent}")
        ok = False
    if expected_title_substr and expected_title_substr not in title:
        print(f"  [{idx:2d}] ❌ FAIL title 期望含 '{expected_title_substr}', 实际='{title}'")
        ok = False

    if ok:
        conf = data.get("confidence", 0)
        print(f"  [{idx:2d}] ✅ PASS  intent={intent:18s} title={title:12s} conf={conf:.2f}")
    return ok


def main() -> int:
    print("=" * 70)
    print("nlu.py 20 条 MVP 用例测试（《本地规则.md》§17）")
    print("=" * 70)

    passed = 0
    failed = 0
    for idx, (text, today, exp_intent, exp_title) in enumerate(TEST_CASES, 1):
        if run_test(idx, text, today, exp_intent, exp_title):
            passed += 1
        else:
            failed += 1

    print("=" * 70)
    print(f"结果：{passed} 通过 / {failed} 失败 / 共 {len(TEST_CASES)} 条")
    print("=" * 70)
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())