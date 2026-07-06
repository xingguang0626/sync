# Life MVP 第一阶段 · 前端2（float）正式任务清单

> 角色：前端2 — 辅助页面与交互层
> 负责人：float
> 队友：前端1（星光赠予依）负责首页主界面；后端D 负责 Python 数据层
> 阶段：Life MVP 第 1 阶段
> 目标平台：Android（Kotlin + Jetpack Compose）
> 架构：MVVM + Repository + StateFlow 单向数据流

---

## 一、工作边界

| 你做（前端2） | 不是你做 |
|-------------|----------|
| 手动新建日程页面（表单 UI） | 首页主界面布局 → 前端1 |
| 新建表单字段校验 | 首页时间轴渲染 → 前端1 |
| 冲突二级菜单（底部弹窗） | 首页日程卡片/冲突卡片 → 前端1 |
| NLU 解析确认卡片 | 优先级色板 → 前端1 |
| AI 提醒二级建议菜单 | 优先级判定逻辑 → 后端D |
| 底部导航 5 个 Tab 中除"日程"外的 4 个页面 | 自然语言解析引擎 → 后端D |
| 编辑/删除/标记完成（第二阶段） | 数据存储/排序/冲突检测 → 后端D |
| 预设模式页面（L/M 入口跳转） | Mock 数据/数据契约 → 前端1 |

---

## 二、技术栈

| 维度 | 选型 |
|------|------|
| 语言 | Kotlin 1.9.22+ |
| UI | Jetpack Compose (BOM 2024.02+) |
| 设计系统 | Material 3 |
| HTTP | Ktor Client 2.3.7+ |
| JSON | kotlinx-serialization 1.6.2+ |
| DI | Hilt 2.50+ |
| 导航 | Navigation Compose 2.7.6+ |
| 架构 | MVVM + Repository + StateFlow |

---

## 三、Task 清单

### Task 1：手动新建日程页面（NewSchedulePage）— P0

**页面路由**：首页点击"+" → 跳转到此页

**表单字段与校验**：

| 字段 | 组件 | 校验规则 |
|------|------|---------|
| 标题 | TextField | 不能为空 |
| 日期 | DatePicker / TextField | 不能为空，格式 YYYY-MM-DD |
| 开始时间 | TimePicker | 不能为空，格式 HH:MM |
| 持续时间 | TextField / 下拉选择 | 必须为正整数（分钟） |
| 优先级 | SegmentedButton / RadioGroup | P0 / P1 / P2 三选一 |
| 备注 | TextField（可选） | 无校验 |

**交互流程**：
```
用户填写表单 → 点"保存" → 客户端校验字段
  ├── 校验失败 → 字段高亮 + 错误提示
  └── 校验成功 → 调 create_schedule(data) → 返回 id
       ├── 成功 → 关闭页面 → 通知前端1刷新首页时间轴
       └── 失败 → Toast 错误信息
```

**产出文件**：
- `ui/newschedule/NewScheduleScreen.kt`
- `ui/newschedule/NewScheduleViewModel.kt`
- `ui/newschedule/NewScheduleUiState.kt`

**验收**：
- 首页点"+"跳转，能完整填写并保存
- 缺字段时不能保存，有明确提示
- 保存后返回首页，前端1的时间轴出现新日程

---

### Task 2：底部导航 4 个占位 Tab 页 — P0

首页底部导航有 5 个 Tab：日程、计划、统计、日记、我的。
- "日程"Tab → 前端1的首页
- 其余 4 个 Tab → 你做（第一阶段占位即可）

每个占位页只需：页面标题 + 简单占位文案（如"即将上线"）

**产出文件**：
- `ui/plan/PlanScreen.kt` — 计划页
- `ui/stats/StatsScreen.kt` — 统计页
- `ui/diary/DiaryScreen.kt` — 日记页
- `ui/mine/MineScreen.kt` — 我的页
- `ui/navigation/BottomNavBar.kt` — 底部导航组件（5 个 Tab 切换）
- `ui/navigation/NavGraph.kt` — 导航图配置

**验收**：
- 底部导航可切换 5 个 Tab
- "日程"Tab 显示前端1的首页
- 其余 4 个 Tab 显示占位内容

---

### Task 3：冲突二级菜单（ConflictBottomSheet）— P1

**触发**：用户在首页点击冲突组卡片（conflict_pair / conflict_group）

**交互**：
```
点击冲突组 → 底部弹出 BottomSheet
  ├── 展示冲突日程列表（每个日程：标题 + 时间 + 优先级）
  ├── 展示调整建议（如"将低优先级日程顺延30分钟"）
  └── 用户操作：
       ├── 采纳建议 → 调后端 postpone/update → 刷新首页
       ├── 手动调整 → 跳转编辑页
       └── 仍然保留 → 关闭菜单
```

**产出文件**：
- `ui/home/components/ConflictBottomSheet.kt`

**验收**：
- 点击冲突卡片弹出底部菜单
- 冲突日程列表展示正确
- 三个操作按钮均可点击（第一阶段先 toast 占位）

---

### Task 4：NLU 确认卡片（NluConfirmCard）— P1

**触发**：后端 NLU 解析完成，前端1的底部输入栏发送后 → 前端1调 NLU 解析 → 返回草稿 → 你展示确认卡片

**页面状态**：

| 置信度 | 卡片表现 |
|--------|---------|
| ≥ 0.85 | 直接展示完整信息 + [确认添加] [修改] 按钮 |
| 0.60~0.85 | 展示信息 + 高亮不确定字段 + 允许直接修改 |
| < 0.60 | 不生成卡片，提示"请补充更多信息" + 引导去手动新建 |

**卡片展示内容**：
```
我理解为：
日程：[标题]
时间：[日期] [开始时间] - [结束时间]
优先级：[P0/P1/P2]
重复：[无/每天/每周...]

[确认添加]  [修改一下]
```

**产出文件**：
- `ui/home/components/NluConfirmCard.kt`

---

### Task 5：AI 提醒二级菜单（ReminderBottomSheet）— P1

**触发**：用户点击首页 AI 提醒卡片

**交互**：
```
点击 AI 提醒卡片 → 底部弹出 BottomSheet
  ├── 展示提醒详情（完整 message + suggestion）
  ├── 展示关联日程列表
  └── 用户操作：
       ├── 采纳建议 → 执行调整 → 刷新首页
       ├── 手动调整 → 跳转相关编辑页
       └── 稍后处理 → 关闭菜单
```

**产出文件**：
- `ui/home/components/ReminderBottomSheet.kt`

---

### Task 6：日程编辑/删除/标记完成 — P2

**编辑**：点击日程卡片 → 进入编辑页（复用新建表单，预填现有数据）→ 保存 → 刷新首页

**删除**：点击日程卡片 → 弹出确认弹窗 → 确认 → 调 delete_schedule(id) → 刷新首页

**标记完成**：点击日程卡片 → 调 mark_complete(id) → 更新卡片状态为"已完成"（视觉弱化）

**产出文件**：
- `ui/scheduledetail/ScheduleDetailScreen.kt`
- `ui/scheduledetail/ScheduleDetailViewModel.kt`
- 删除确认弹窗组件
- 标记完成交互逻辑

---

### Task 7：预设模式页面（PresetModePage）— P2

**触发**：首页顶部 M 按钮 / 计划 Tab

**三种模式**：

| 模式 | 默认时长 | 页面内容 |
|------|---------|---------|
| 学习 | 90 分钟 | 目标设置、倒计时、结束按钮 |
| 放松 | 30 分钟 | 轻量休息、倒计时 |
| 小憩 | 20 分钟 | 倒计时、结束提醒 |

**交互**：
```
选择模式 → 设置时长 → 点击开始 → 倒计时页面
  ├── 倒计时结束 → 显示正向反馈文案
  └── 手动结束 → 显示正向反馈文案
       └── 询问是否顺延后续安排
```

**产出文件**：
- `ui/modes/ModeSelectScreen.kt`
- `ui/modes/ModeTimerScreen.kt`
- `ui/modes/ModeFeedbackScreen.kt`

---

### Task 8：生活管家 + 设置页 — P2 后续

**生活管家页**（点击首页 L 按钮进入）：
- MVP 只做简单展示：今日状态卡、最近活动、生活建议占位
- P2 再补充实际功能

**我的/设置页**：
- 通知设置（开关）
- 默认日程设置（默认优先级、默认时长）
- 数据管理（清空本地数据）
- 关于 Life（版本号、产品说明）

---

## 四、与前端1的接口约定

**导航回调**（前端1的 HomeScreen 暴露给你的入口）：
```kotlin
HomeScreen(
    onNavigateToNewSchedule: () -> Unit,            // → Task 1 新建日程
    onNavigateToLifestyle: () -> Unit,               // → Task 8 生活管家
    onNavigateToPreset: () -> Unit,                  // → Task 7 预设模式
    onNavigateToScheduleDetail: (Long) -> Unit,      // → Task 6 日程详情
    onNavigateToConflictMenu: (List<Long>) -> Unit,  // → Task 3 冲突菜单
)
```

**保存成功后通知前端1刷新**：
- 保存成功后直接 pop BackStack，前端1 的 HomeScreen 在 `onResume` 时自动刷新

**数据模型复用**：
- 统一使用 `domain/model/` 下的 Schedule、Reminder、TimelineItem 等
- 统一使用 `data/repository/` 下的 HomeRepository
- 复用 `ApiResult<T>` 三态处理

---

## 五、第一阶段验收清单（P0）

- [ ] 点击首页"+"能跳转到新建日程页
- [ ] 新建表单字段完整（标题/日期/时间/时长/优先级/备注）
- [ ] 缺必填字段时保存按钮不生效 + 有错误提示
- [ ] 保存成功后返回首页，首页时间轴刷新显示新日程
- [ ] 底部导航 5 个 Tab 可切换
- [ ] "计划""统计""日记""我的"4 个 Tab 显示占位内容
- [ ] 页面切换无 ANR/Crash

---

## 六、全部完成（原"明确不做"清单已全部消化）

| 原计划不做 | 原定阶段 | 实际完成时间 |
|------|------|------|
| 冲突二级菜单真实逻辑 | 第二阶段 | 2026-07-04 ✅ |
| NLU 确认卡片 | 第二阶段 | 2026-07-04 ✅ |
| AI 提醒二级菜单 | 第二阶段 | 2026-07-04 ✅ |
| 编辑/删除/标记完成 | 第三阶段 | 2026-07-04 ✅ |
| 预设模式完整功能 | 第三阶段 | 2026-07-04 ✅ |
| 生活管家实质内容 | 第四阶段 | 2026-07-05 ✅ |
| 语音识别 | P3 | 2026-07-05 ✅ |
| 首页主界面任何改动 | 前端1负责 | — |

---

## 七、空壳功能补全阶段（2026-07-05 新增）

> 背景：P0～P2 的 Task 1-8 已全部完成，但以下功能的 UI 已存在却无实际实现——按钮点下去只关弹窗不做事，或者页面只有"即将上线"。

### Task 9：ConflictBottomSheet 按钮接入 postpone API — P1

**现状**：「采纳建议」按钮只调 `DismissConflictSheet`，没有实际顺延日程。

**但后端已就绪**：`POST /api/schedules/{id}/postpone` 路由存在，`HomeRepository.postponeSchedule()` 已封装，只是没人接。

**改动**：
- `HomeViewModel` 新增 `AcceptConflictSuggestion` 事件
- 事件处理：取 `conflictSchedules` 中最低优先级的日程，调 `repository.postponeSchedule(id, null)`
- 成功后刷新首页 + 关闭弹窗；失败则 `errorMessage` 提示

**产出文件**：
- `HomeUiState.kt` — 新增 `AcceptConflictSuggestion` 事件
- `HomeViewModel.kt` — 实现 postpone 调用
- `HomeScreen.kt` — 将 `onAcceptSuggestion` 从 `DismissConflictSheet` 改为 `AcceptConflictSuggestion`

---

### Task 10：ReminderBottomSheet「采纳建议」接入真实操作 — P2

**现状**：三个按钮都是关闭弹窗，无实际操作。

**依赖后端**：需 regeonchen 新增 `POST /api/reminders/{id}/adopt` 端点：
- 接收 reminder type + related_schedule_ids
- 根据 type 执行对应操作（如 time_conflict → 顺延低优先级日程）
- 返回操作结果

**前端改动**（后端就绪后）：
- `HomeApi` 新增 `adoptReminder(type, scheduleIds)` 方法
- `HomeRepository` / `MockApi` / `KtorHomeApi` 同步新增
- `HomeViewModel` 新增 `AcceptReminderSuggestion` 事件
- `HomeScreen` 将 `onAcceptSuggestion` 从 `DismissReminderSheet` 改为真实调用

**产出文件**（前端）：
- `HomeApi.kt` / `KtorHomeApi.kt` / `MockApi.kt` / `HomeRepository.kt` — 新增 adopt 方法
- `HomeUiState.kt` / `HomeViewModel.kt` / `HomeScreen.kt` — 接入

---

### Task 11：ModeFeedbackScreen「顺延后续安排」真实执行 — P2

**现状**：「顺延后续安排」按钮只 `popBackStack`，不操作任何日程。

**改动**：
- `ModeFeedbackScreen` 需要知道今天有哪些日程需要顺延
- 方案 A：传入 `List<Schedule>`，批量调 `repository.postponeSchedule()`
- 方案 B：简化——仅顺延模式结束时尚未开始的 pending 日程
- ViewModel 新增 `postponeRemainingSchedules(date)` 逻辑

**产出文件**：
- `ModeFeedbackScreen.kt` — 注入 ViewModel 或接收 postpone 回调
- 新建 `ModeFeedbackViewModel.kt` — 管理顺延逻辑

---

### Task 12：语音输入 — P3

> 详细提示词见 `语音输入实现提示词.md`

**改动清单**：
- `AndroidManifest.xml` — 加 `RECORD_AUDIO` 权限
- 新建 `VoiceInputHelper.kt` — 封装 `SpeechRecognizer`
- `HomeUiState.kt` — 新增 `isVoiceListening`、`voicePartialText`
- `HomeViewModel.kt` — 处理 `OnVoiceClick` / `DismissVoice`
- `HomeInputBar.kt` — 录音中脉冲动画
- `HomeScreen.kt` — 底部识别预览条 + 权限请求

---

### Task 13：占位 Tab 页补充 — P3

**现状**：PlanScreen / StatsScreen / DiaryScreen 只显示"即将上线"。

**MVP 级别补充方案**（不过度设计）：

| 页面 | MVP 内容 |
|------|---------|
| **计划** | 展示所有重复日程（`repeat != none`），按类型分组（每天/每周/工作日/周末/每月） |
| **统计** | 展示本周完成率（completed / total）、日均专注时长（sum duration）/ 最活跃时段 |
| **日记** | 按日期列表展示已完成日程，附带备注摘要，可点击查看详情 |

> 以上数据均可从现有 API（`getTimeline` + `getSchedule`）获取，无需后端新增接口。

**产出文件**：
- `PlanScreen.kt` — 重写，重复日程列表
- `StatsScreen.kt` — 重写，统计卡片
- `DiaryScreen.kt` — 重写，已完成日程时间线

---

### Task 14：LifestyleScreen 生活建议卡片 — P4

**现状**：第三张卡片"生活建议"是硬编码占位文案。

**方案**：根据今日数据动态生成建议文案（纯客户端逻辑，不调后端）：
- 今日无 P0 日程 → "今天没有紧急任务，适合安排学习或运动"
- 有晚间日程（end_time > 20:00）→ "今晚有安排，注意留出休息时间"
- 完成率 100% → "今天全部完成，了不起！"
- 有冲突日程 → "今天有日程冲突，建议打开首页查看"

**产出文件**：
- `LifestyleScreen.kt` — 新增 `generateSuggestion(HomePageData)` 逻辑

---

## 八、空壳补全验收清单

- [ ] Task 9：冲突弹窗"采纳建议"→ 实际顺延低优先级日程 → 首页刷新
- [ ] Task 10：提醒弹窗"采纳建议"→ 后端 adopt 端点 + 前端调用
- [ ] Task 11：模式结束"顺延后续安排"→ 批量 postpone 当天 pending 日程
- [ ] Task 12：点击麦克风 → 权限请求 → 语音识别 → 文本填入输入栏
- [ ] Task 13：Plan/Stats/Diary 三个 Tab 显示真实数据
- [ ] Task 14：LifestyleScreen 生活建议动态生成
