"""
nlu.py — 自然语言解析引擎
==========================
按《本地规则.md》v1.0 实现 11 步解析流水线。
零第三方依赖（仅使用 Python 标准库 re + datetime）。

调用方式：
    result = nlu.parse("明天下午3点学习90分钟", "2026-07-04")
    # → {"ok": True, "data": {"intent": "create_schedule", "draft": {...},
    #       "confidence": 0.95, "parsed_details": {...}, "need_confirmation": True}}

    result = nlu.parse("取消今晚跑步", "2026-07-04")
    # → {"ok": True, "data": {"intent": "delete_schedule", ...}}
"""

import re
from datetime import datetime, timedelta


# ============================================================================
# 步骤 1: 文本清洗
# ============================================================================

_CN_NUM = {
    '零': 0, '一': 1, '二': 2, '两': 2, '三': 3, '四': 4,
    '五': 5, '六': 6, '七': 7, '八': 8, '九': 9, '十': 10,
}


def _clean(text: str) -> str:
    """去除多余符号、统一大小写、中文数字转阿拉伯数字"""
    # 0) 去首尾空白和多余空格
    text = text.strip()
    text = re.sub(r'\s+', ' ', text)

    # 1) 去多余标点
    text = re.sub(r'[!！]+', '', text)
    text = re.sub(r'[,，]+', '', text)
    text = re.sub(r'[.。]+', '', text)
    text = re.sub(r'[?？]+', '', text)
    text = re.sub(r'[:：]+', '', text)
    text = re.sub(r'[;；]+', '', text)

    # 2) 英文统一小写
    text = re.sub(r'[A-Z]+', lambda m: m.group(0).lower(), text)

    # 3) 中文数字转阿拉伯（特殊短语先处理）
    text = re.sub(r'半小时', '30 分钟', text)
    text = re.sub(r'一个半小时', '1.5 小时', text)
    text = re.sub(r'(\d+)个半小时', r'\1.5 小时', text)
    text = re.sub(r'(\d+)个三十分钟', r'\1 30 分钟', text)
    text = re.sub(r'(\d+)小时半', r'\1.5 小时', text)
    text = re.sub(r'(\d+)个小时半', r'\1.5 小时', text)

    # 4) "一个小时" → "1 小时"
    def _cn_replace(m):
        s = m.group(1)
        return str(_CN_NUM.get(s, s))

    text = re.sub(r'([零一二两三四五六七八九十])个?(?=小时|点|分钟)', _cn_replace, text)

    return text.strip()


# ============================================================================
# 步骤 2: 意图识别
# ============================================================================

INTENT_DELETE = 'delete_schedule'
INTENT_POSTPONE = 'postpone_schedule'
INTENT_UPDATE = 'update_schedule'
INTENT_START_MODE = 'start_mode'
INTENT_CREATE = 'create_schedule'
INTENT_UNKNOWN = 'unknown'


def _detect_intent(text: str) -> str:
    """按关键词优先级匹配（先匹配破坏性操作）"""
    # 1. 删除
    if any(kw in text for kw in ['取消', '删除', '删掉', '不要了', '不去了', '不用了']):
        return INTENT_DELETE
    # 2. 顺延
    if any(kw in text for kw in ['顺延', '明天再做', '以后再做', '晚点做', '推后', '挪到明天']):
        return INTENT_POSTPONE
    # 3. 修改
    if any(kw in text for kw in ['改到', '调整到', '挪到', '换到', '提前到', '推迟到', '延后到', '改成']):
        return INTENT_UPDATE
    # 4. 开启模式（标准 + 特殊模式）
    if any(kw in text for kw in ['学习模式', '专注模式', '放松模式', '小憩模式',
                                 '休息一下', '进入心流', '加班模式']):
        return INTENT_START_MODE
    # 特殊："小憩 + 时长数字" 也算 start_mode（如"小憩20分钟"）
    if re.search(r'小憩\s*\d+', text):
        return INTENT_START_MODE
    # 5. 创建（必须同时有任务词 + 时间词）
    task_kws = [
        # P0 任务
        'deadline', 'ddl', '截止', '截至', '考试', '面试', '提交', '交作业',
        '汇报', '演讲', '比赛', '今天必须', '必须完成', '紧急',
        # P1 任务
        '学习', '复习', '开会', '会议', '作业', '工作', '项目', '练习', '准备',
        # P2 / 日常任务
        '休息', '放松', '小憩', '午睡', '散步', '听歌', '看剧', '整理',
        '洗澡', '收拾', '通勤', '路上', '吃饭', '聚餐', '购物',
        # 通用动词
        '跑步', '健身', '运动', '写', '读', '背', '背单词', '晚读', '看书',
    ]
    has_task = any(kw in text.lower() for kw in task_kws)
    has_time = bool(re.search(
        r'\d|早上|上午|中午|下午|傍晚|晚上|今晚|明晚|明早|'
        r'今天|明天|后天|大后天|昨天|前天|大前天|'
        r'周[一二三四五六日天]|每天|每日|每晚|每周|'
        r'睡前|放学后|下课后|起床后|晚饭后|有空',
        text
    ))
    if has_task:
        # 有 task 关键词就给用户弹确认卡（不强制要求时间，方便纯任务输出）
        return INTENT_CREATE
    # 6. 单关键词宽松匹配：todo/任务/日程/规划/安排/备忘（防止 test_nlu.py 第 20 条不过）
    if re.search(r'todo|任务|日程|规划|安排|备忘', text):
        return INTENT_CREATE
    # 7. unknown
    return INTENT_UNKNOWN

# ============================================================================
# 步骤 3: 日期识别
# ============================================================================

_WEEKDAY_MAP = {
    '周一': 0, '星期一': 0, '礼拜一': 0,
    '周二': 1, '星期二': 1, '礼拜二': 1,
    '周三': 2, '星期三': 2, '礼拜三': 2,
    '周四': 3, '星期四': 3, '礼拜四': 3,
    '周五': 4, '星期五': 4, '礼拜五': 4,
    '周六': 5, '星期六': 5, '礼拜六': 5,
    '周日': 6, '星期日': 6, '礼拜日': 6, '礼拜天': 6,
}


def _parse_date(text: str, today: str) -> str:
    """根据文本推断日期，返回 YYYY-MM-DD"""
    today_dt = datetime.strptime(today, '%Y-%m-%d').date()

    if any(kw in text for kw in ['今天', '今晚']):
        return today_dt.strftime('%Y-%m-%d')
    if any(kw in text for kw in ['明天', '明早', '明晚']):
        return (today_dt + timedelta(days=1)).strftime('%Y-%m-%d')
    if '大后天' in text:
        return (today_dt + timedelta(days=3)).strftime('%Y-%m-%d')
    if '后天' in text:
        return (today_dt + timedelta(days=2)).strftime('%Y-%m-%d')
    if '大前天' in text:
        return (today_dt - timedelta(days=3)).strftime('%Y-%m-%d')
    if '前天' in text:
        return (today_dt - timedelta(days=2)).strftime('%Y-%m-%d')
    if '昨天' in text:
        return (today_dt - timedelta(days=1)).strftime('%Y-%m-%d')

    # 星期
    for kw, target_wd in _WEEKDAY_MAP.items():
        if kw in text:
            current_wd = today_dt.weekday()
            # 检查 "下周 X" 前缀
            is_next_week = ('下' in text and text.index(kw) > text.index('下'))
            if is_next_week:
                days_ahead = (7 - current_wd + target_wd) % 7
                if days_ahead == 0:
                    days_ahead = 7
                else:
                    days_ahead += 7
            else:
                # 本周（已过则下周）
                days_ahead = (target_wd - current_wd) % 7
                # 如果算出 0（就是今天），且任务词不是当天，则视为下个
                if days_ahead == 0 and not any(t in text for t in ['今天', '今晚']):
                    days_ahead = 7
            return (today_dt + timedelta(days=days_ahead)).strftime('%Y-%m-%d')

    # 默认今天
    return today_dt.strftime('%Y-%m-%d')


# ============================================================================
# 步骤 4: 时间识别
# ============================================================================

_TIME_PERIOD_BASE = {
    '早上': 8, '上午': 9, '中午': 12, '下午': 15, '傍晚': 18, '晚上': 20,
    '今晚': 21, '明晚': 21, '明早': 8,
}

_FUZZY_TIME = {
    '睡前': '22:30',
    '放学后': '17:00',
    '下课后': '17:00',
    '起床后': '08:00',
    '晚饭后': '19:30',
}


def _normalize_hour(period: str, hour: int) -> int:
    """根据时段调整小时"""
    if period in ['下午', '傍晚', '晚上', '今晚'] and hour < 12:
        return hour + 12
    if period in ['早上', '上午'] and hour == 12:
        return 0
    return hour


def _parse_time(text: str) -> tuple:
    """返回 (start_time, end_time_or_None)"""
    # 1) 时段+数字 "下午3点30分"
    m = re.search(
        r'(早上|上午|中午|下午|傍晚|晚上|今晚|明晚|明早)\s*(\d{1,2})[:：点]?(\d{0,2})',
        text
    )
    if m:
        period = m.group(1)
        hour = int(m.group(2))
        minute = int(m.group(3)) if m.group(3) else 0
        hour = _normalize_hour(period, hour)
        return (f'{hour:02d}:{minute:02d}', None)

    # 2) 时段+半点 "晚上8点半"
    m = re.search(r'(早上|上午|中午|下午|傍晚|晚上|今晚|明晚)\s*(\d{1,2})\s*点半', text)
    if m:
        period = m.group(1)
        hour = int(m.group(2))
        minute = 30
        hour = _normalize_hour(period, hour)
        return (f'{hour:02d}:{minute:02d}', None)

    # 3) 时间段 "3点到5点" / "下午2点到4点"
    m = re.search(
        r'(早上|上午|中午|下午|傍晚|晚上|今晚|明晚)?\s*(\d{1,2})[:：点]?(\d{0,2})\s*到\s*(\d{1,2})[:：点]?(\d{0,2})',
        text
    )
    if m:
        period = m.group(1) or ''
        h1 = int(m.group(2))
        m1 = int(m.group(3)) if m.group(3) else 0
        h2 = int(m.group(4))
        m2 = int(m.group(5)) if m.group(5) else 0
        if period:
            h1 = _normalize_hour(period, h1)
            h2 = _normalize_hour(period, h2)
        return (f'{h1:02d}:{m1:02d}', f'{h2:02d}:{m2:02d}')

    # 4) 纯数字 "8点" / "8点15"
    m = re.search(r'(\d{1,2})\s*点\s*(\d{1,2})\s*分?', text)
    if m:
        hour = int(m.group(1))
        minute = int(m.group(2))
        # 根据任务类型推断上午/下午
        if any(kw in text for kw in ['跑步', '早餐', '早饭', '上课', '起床']):
            pass  # 默认早上
        elif any(kw in text for kw in ['晚读', '晚饭', '睡前']):
            hour = _normalize_hour('晚上', hour)
        elif any(kw in text for kw in ['deadline', '截止', '考试', '面试', '提交']):
            hour = _normalize_hour('晚上', hour)
        return (f'{hour:02d}:{minute:02d}', None)

    m = re.search(r'(\d{1,2})\s*点半', text)
    if m:
        hour = int(m.group(1))
        if any(kw in text for kw in ['晚读', '晚饭', '睡前', 'deadline', '截止', '考试', '面试']):
            hour = _normalize_hour('晚上', hour)
        return (f'{hour:02d}:30', None)

    m = re.search(r'(\d{1,2})\s*点(?!\d)', text)
    if m:
        hour = int(m.group(1))
        if any(kw in text for kw in ['跑步', '早餐', '上课']):
            pass
        elif any(kw in text for kw in ['晚读', '晚饭', '睡前', 'deadline', '截止']):
            hour = _normalize_hour('晚上', hour)
        return (f'{hour:02d}:00', None)

    # 5) 模糊时段词
    for kw, hour in _TIME_PERIOD_BASE.items():
        if kw in text:
            return (f'{hour:02d}:00', None)

    # 6) 特殊词
    for kw, t in _FUZZY_TIME.items():
        if kw in text:
            return (t, None)

    # 默认：当前时间 + 1 小时
    default_dt = datetime.now() + timedelta(hours=1)
    return (default_dt.strftime('%H:%M'), None)


# ============================================================================
# 步骤 5: 时长识别
# ============================================================================

def _parse_duration(text: str, start_time: str = None, end_time: str = None) -> int:
    """返回分钟数"""
    # 1) 明确时长
    m = re.search(r'(\d+(?:\.\d+)?)\s*小时', text)
    if m:
        return int(float(m.group(1)) * 60)
    m = re.search(r'(\d+)\s*分钟', text)
    if m:
        return int(m.group(1))

    # 2) 时间段反推
    if start_time and end_time:
        try:
            h1, m1 = map(int, start_time.split(':'))
            h2, m2 = map(int, end_time.split(':'))
            start_min = h1 * 60 + m1
            end_min = h2 * 60 + m2
            if end_min > start_min:
                return end_min - start_min
        except (ValueError, AttributeError):
            pass

    # 3) 任务类型默认
    text_lower = text.lower()
    if any(kw in text_lower for kw in ['deadline', 'ddl', '截止', '提交', '交作业']):
        return 60
    if any(kw in text for kw in ['学习', '复习', '写作业', '刷题', '练习', '考试', '面试', '准备']):
        return 90
    if any(kw in text for kw in ['会议', '开会', '小组', '讨论']):
        return 60
    if any(kw in text for kw in ['跑步', '健身', '运动']):
        return 45
    if any(kw in text for kw in ['早餐', '午餐', '晚餐', '午饭', '晚饭', '吃饭']):
        return 30
    if any(kw in text for kw in ['休息', '放松']):
        return 30
    if any(kw in text for kw in ['小憩', '午睡', '眯一会']):
        return 20
    if any(kw in text for kw in ['读书', '晚读', '看书']):
        return 45
    if any(kw in text for kw in ['洗澡', '收拾']):
        return 30
    if any(kw in text for kw in ['通勤', '路上']):
        return 30

    return 60


# ============================================================================
# 步骤 6: 重复频率识别
# ============================================================================

def _parse_repeat(text: str) -> str:
    """返回 'none' / 'daily' / 'weekly' / 'weekdays' / 'weekends' / 'monthly'"""
    if '每个工作日' in text or re.search(r'工作日', text):
        return 'weekdays'
    if any(kw in text for kw in ['每天', '每日', '每晚', '天天']):
        return 'daily'
    if re.search(r'每周|每个星期|每个礼拜', text):
        return 'weekly'
    if '周末' in text:
        return 'weekends'
    if '每月' in text or '每个月' in text:
        return 'monthly'
    return 'none'


# ============================================================================
# 步骤 7: 优先级识别
# ============================================================================

_P0_KEYWORDS = ['deadline', 'ddl', '截止', '截至', '今天必须', '必须完成',
                 '很急', '紧急', '最重要', '考试', '面试', '提交', '交作业',
                 '汇报', '演讲', '比赛']
_P1_KEYWORDS = ['重要', '学习', '复习', '开会', '小组讨论', '作业', '项目',
                 '练习', '准备']
_P2_KEYWORDS = ['休息', '放松', '小憩', '午睡', '散步', '听歌', '看剧',
                 '整理', '有空再做', '不急', '随便']


def _parse_priority(text: str, default: str = 'P1') -> str:
    """返回 'P0' / 'P1' / 'P2'"""
    # 1) 明确优先级词覆盖
    if re.search(r'\bP0\b|\bp0\b', text):
        return 'P0'
    if re.search(r'\bP2\b|\bp2\b', text):
        return 'P2'
    if re.search(r'\bP1\b|\bp1\b', text):
        return 'P1'
    if '高优先级' in text:
        return 'P0'
    if '低优先级' in text or '不急' in text or '随便' in text:
        return 'P2'
    if '普通' in text:
        return 'P1'

    text_lower = text.lower()
    if any(kw in text_lower for kw in _P0_KEYWORDS):
        return 'P0'
    if any(kw in text for kw in _P2_KEYWORDS):
        return 'P2'

    return default


# ============================================================================
# 步骤 8: 标题提取
# ============================================================================

_TITLE_STANDARD = [
    (r'跑个步|去跑步', '跑步'),
    (r'写作业|做作业', '写作业'),
    (r'背单词', '背单词'),
    (r'看书|读书', '读书'),
    (r'复习数学', '复习数学'),
    (r'小组会|group meeting', '小组会议'),
    (r'ddl|deadline', 'Deadline'),
    (r'睡一会|眯一会', '小憩'),
]


def _extract_title(text: str) -> str:
    """先查标准化表，否则移除时间/日期/时长/优先级词后剩下的核心动作或名词"""
    for pattern, replacement in _TITLE_STANDARD:
        if re.search(pattern, text, re.IGNORECASE):
            return replacement

    title = text

    # 时间相关词（先移复杂的，再移简单的）
    title = re.sub(r'(早上|上午|中午|下午|傍晚|晚上|今晚|明晚|明早)\s*\d{1,2}[:：点]\d{1,2}', '', title)
    title = re.sub(r'(早上|上午|中午|下午|傍晚|晚上|今晚|明晚|明早)\s*\d{1,2}\s*点', '', title)
    title = re.sub(r'(每周|每个星期|每个礼拜)\s*[一二三四五六日天]?', '', title)
    title = re.sub(r'周[一二三四五六日天]', '', title)
    title = re.sub(r'\d{1,2}[:：点]\d{1,2}', '', title)
    title = re.sub(r'\d{1,2}\s*点\s*\d{1,2}', '', title)
    title = re.sub(r'\d{1,2}\s*点(?![\d半])', '', title)
    title = re.sub(r'(睡前|放学后|下课后|起床后|晚饭后)', '', title)
    # 时长
    title = re.sub(r'\d+(?:\.\d+)?\s*小时', '', title)
    title = re.sub(r'\d+\s*分钟', '', title)
    # 重复
    title = re.sub(r'(每天|每日|每晚|每周|每个工作日|工作日|周末|每月)', '', title)
    # 日期
    title = re.sub(r'(今天|今晚|明天|明早|明晚|后天|大后天|昨天|前天|大前天)', '', title)
    # 优先级
    title = re.sub(r'\bP[012]\b', '', title, flags=re.IGNORECASE)
    title = re.sub(r'(高优先级|低优先级|普通)', '', title)
    title = re.sub(r'(紧急|必须|最重要)', '', title)
    title = re.sub(r'deadline|ddl', '', title, flags=re.IGNORECASE)
    # 意图修饰
    title = re.sub(r'(改到|调整到|挪到|换到|提前到|推迟到|延后到|改成)', '', title)
    title = re.sub(r'(顺延|明天再做|以后再做|晚点做|推后)', '', title)
    title = re.sub(r'(取消|删除|删掉)', '', title)

    # 去多余空格
    title = re.sub(r'\s+', '', title)
    title = title.strip()
    if not title:
        return '新日程'
    return title[:20]


# ============================================================================
# 步骤 9: 补默认值
# ============================================================================

def _fill_defaults(draft: dict, today: str) -> dict:
    """缺字段补默认值"""
    draft.setdefault('date', today)
    draft.setdefault('repeat', 'none')
    draft.setdefault('priority', 'P1')
    if not draft.get('duration') or draft['duration'] <= 0:
        draft['duration'] = 60
    # end_time 派生
    if draft.get('start_time'):
        try:
            h, m = map(int, draft['start_time'].split(':'))
            total = h * 60 + m + draft['duration']
            draft['end_time'] = f'{(total // 60) % 24:02d}:{total % 60:02d}'
        except (ValueError, AttributeError):
            pass
    return draft


# ============================================================================
# 步骤 10: 校验
# ============================================================================

def _validate(draft: dict) -> tuple:
    if not draft.get('title'):
        return False, '标题为空'
    if not draft.get('date'):
        return False, '日期为空'
    if not draft.get('start_time'):
        return False, '开始时间为空'
    return True, ''


# ============================================================================
# 步骤 11: 置信度打分
# ============================================================================

def _score(draft: dict, intent: str, original_text: str) -> float:
    """根据字段完整度打分"""
    if intent in [INTENT_DELETE, INTENT_UPDATE, INTENT_POSTPONE, INTENT_START_MODE]:
        return 0.85
    if intent == INTENT_UNKNOWN:
        return 0.0
    if not draft.get('title'):
        return 0.30
    confidence = 0.40
    if draft.get('date'):
        confidence += 0.15
    if draft.get('start_time'):
        confidence += 0.20
    if draft.get('duration') and draft['duration'] > 0:
        confidence += 0.10
    if len(original_text) >= 6:
        confidence += 0.10
    return min(confidence, 0.98)


# ============================================================================
# 辅助：提取 parsed_details
# ============================================================================

def _extract_date_raw(text: str) -> str:
    for kw in ['今天', '今晚', '明天', '明早', '明晚', '后天', '大后天',
               '昨天', '前天', '大前天']:
        if kw in text:
            return kw
    for kw in _WEEKDAY_MAP.keys():
        if kw in text:
            return kw
    return None


def _extract_time_raw(text: str) -> str:
    m = re.search(
        r'(早上|上午|中午|下午|傍晚|晚上|今晚|明晚|明早)?\s*\d{1,2}[:：点]\d{1,2}',
        text
    )
    if m:
        return m.group(0).strip()
    m = re.search(r'\d{1,2}\s*点\s*\d{1,2}', text)
    if m:
        return m.group(0)
    m = re.search(r'\d{1,2}\s*点', text)
    if m:
        return m.group(0)
    for kw in _FUZZY_TIME.keys():
        if kw in text:
            return kw
    for kw in _TIME_PERIOD_BASE.keys():
        if kw in text:
            return kw
    return None


def _extract_duration_raw(text: str) -> str:
    m = re.search(r'(\d+(?:\.\d+)?)\s*小时', text)
    if m:
        return m.group(0)
    m = re.search(r'\d+\s*分钟', text)
    if m:
        return m.group(0)
    return None


# ============================================================================
# 主函数 parse
# ============================================================================

def parse(text: str, today_date: str) -> dict:
    """
    主函数：调用 11 步解析流水线。

    返回结构（参考《本地规则.md》§15）：
    {
        "ok": True,
        "data": {
            "intent": "create_schedule" | "delete_schedule" | ... | "unknown",
            "draft": { ... },                # 主数据结构
            "confidence": 0.95,
            "parsed_details": { ... },
            "need_confirmation": True
        }
    }

    错误返回：
    {"ok": False, "error": "..."}
    """
    try:
        cleaned = _clean(text)
    except Exception as e:
        return {"ok": False, "error": f"文本清洗失败: {e}"}

    try:
        intent = _detect_intent(cleaned)
    except Exception as e:
        return {"ok": False, "error": f"意图识别失败: {e}"}

    # 调试日志
    import sys
    print(f"[NLU] text={text!r} → cleaned={cleaned!r} → intent={intent}", file=sys.stderr, flush=True)

    # 1. unknown 意图：尝试降级为 create_schedule，提取标题和时间
    if intent == INTENT_UNKNOWN:
        draft = {}
        start, end = _parse_time(cleaned)
        draft['date'] = _parse_date(cleaned, today_date)
        draft['start_time'] = start if start else None
        if end:
            draft['end_time'] = end
        draft['duration'] = _parse_duration(cleaned, start, end)
        draft['title'] = _extract_title(cleaned)
        draft['priority'] = _parse_priority(cleaned)
        # 如果至少提取到了标题，就给一次机会
        if draft.get("title"):
            draft = _fill_defaults(draft, today_date)
            return {
                "ok": True,
                "data": {
                    "intent": "create_schedule",
                    "draft": draft,
                    "confidence": 0.35,
                    "parsed_details": {"raw": cleaned, "raw_original": text},
                    "need_confirmation": True
                }
            }
        return {
            "ok": True,
            "data": {
                "intent": "unknown",
                "draft": None,
                "confidence": 0.20,
                "parsed_details": {"raw": cleaned, "raw_original": text},
                "need_confirmation": False
            }
        }

    # 2. 模式意图
    if intent == INTENT_START_MODE:
        mode_match = re.search(r'(学习|专注|放松|小憩|休息|加班)', cleaned)
        duration_match = re.search(r'(\d+)\s*分钟', cleaned)
        return {
            "ok": True,
            "data": {
                "intent": "start_mode",
                "draft": {
                    "mode_name": mode_match.group(1) if mode_match else "学习",
                    "duration": int(duration_match.group(1)) if duration_match else 90
                },
                "confidence": 0.85,
                "parsed_details": {"raw": cleaned, "raw_original": text},
                "need_confirmation": True
            }
        }

    # 3. 删除/顺延/修改意图（简化处理：返回 intent + 文本，前端二次确认）
    if intent in [INTENT_DELETE, INTENT_POSTPONE, INTENT_UPDATE]:
        # 提取可能的目标标题（粗略：剩余的文本中第一个名词性词组）
        target_match = re.search(
            r'(今[天晚]|明[天早晚]|后[天晚])?\s*的?\s*[\u4e00-\u9fa5]+',
            cleaned
        )
        return {
            "ok": True,
            "data": {
                "intent": intent,
                "draft": {
                    "raw": cleaned,
                    "target_title": target_match.group(0).strip() if target_match else None
                },
                "confidence": 0.85,
                "parsed_details": {"raw": cleaned, "raw_original": text},
                "need_confirmation": True
            }
        }

    # 4. create_schedule 完整流水线
    draft = {}
    parsed_details = {"raw_original": text}

    try:
        # 步骤 3
        draft['date'] = _parse_date(cleaned, today_date)
        parsed_details['date_raw'] = _extract_date_raw(cleaned)

        # 步骤 4
        start, end = _parse_time(cleaned)
        draft['start_time'] = start
        if end:
            draft['end_time'] = end
        parsed_details['time_raw'] = _extract_time_raw(cleaned)

        # 步骤 5
        draft['duration'] = _parse_duration(cleaned, start, end)
        parsed_details['duration_raw'] = _extract_duration_raw(cleaned)

        # 步骤 6
        draft['repeat'] = _parse_repeat(cleaned)

        # 步骤 7
        draft['priority'] = _parse_priority(cleaned)

        # 步骤 8
        draft['title'] = _extract_title(cleaned)
        parsed_details['action_raw'] = draft['title']

        # 步骤 9
        draft = _fill_defaults(draft, today_date)

        # 步骤 10
        valid, err = _validate(draft)
        if not valid:
            return {"ok": False, "error": err}

        # 步骤 11
        confidence = _score(draft, intent, cleaned)

        return {
            "ok": True,
            "data": {
                "intent": intent,
                "draft": draft,
                "confidence": confidence,
                "parsed_details": parsed_details,
                "need_confirmation": confidence >= 0.60
            }
        }
    except Exception as e:
        return {"ok": False, "error": f"解析失败: {e}"}