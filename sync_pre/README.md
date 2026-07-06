# Sync（随刻）MVP — 项目总览

> Android · Kotlin + Jetpack Compose + Material 3 · Python 数据层 · 暖琥珀设计系统

---

## 一、团队分工

| 角色 | 代号 | 负责范围 |
|------|------|---------|
| **前端1** | xingguangzengyuyi | 首页主界面、数据契约（domain 层）、MockApi、时间轴渲染、AI 提醒卡片、冲突卡片 |
| **前端2** | mengyiguang | 新建/编辑日程表单、底部导航占位页、冲突二级菜单、NLU 确认卡片、AI 提醒二级菜单、预设模式、日程详情页、设置页、生活管家、UI 设计系统 |
| **后端** | regeonchen | Python 数据层：SQLite 本地存储、日程 CRUD API、时间轴排序/冲突检测、自然语言解析引擎、AI 提醒检测 |

---

## 二、工程结构

```
sync/
├── README.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       └── gradle-wrapper.properties
├── app/
│   ├── build.gradle.kts              # BASE_URL → 10.181.148.115:8800（真机）/ 10.0.2.2:8800（模拟器）
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/life/app/
│       │   ├── SyncApplication.kt            # @HiltAndroidApp 入口
│       │   ├── MainActivity.kt               # NavHost + 路由 + Tab 切换
│       │   │
│       │   ├── domain/model/                 # 纯 Kotlin 数据模型
│       │   │   ├── Schedule.kt               # 日程：id/标题/日期/时间/时长/优先级/状态/重复/备注
│       │   │   ├── Priority.kt               # 优先级：P0/P1/P2
│       │   │   ├── Reminder.kt               # AI 提醒：类型/优先级/文案/建议/关联日程
│       │   │   ├── TimelineItem.kt           # 时间轴条目：Single/ConflictPair/ConflictGroup
│       │   │   └── HomePageData.kt           # 首页聚合数据
│       │   │
│       │   ├── data/
│       │   │   ├── remote/
│       │   │   │   ├── HomeApi.kt            # 接口定义（7 个方法）
│       │   │   │   ├── KtorHomeApi.kt        # 真后端实现（调用 8800 端口）
│       │   │   │   ├── KtorClientFactory.kt  # Ktor HttpClient 工厂
│       │   │   │   ├── ApiResult.kt          # Loading/Success/Failure 三态
│       │   │   │   ├── dto/                  # 网络 DTO（snake_case）
│       │   │   │   │   ├── ApiResponseDto.kt # 通用响应 {"ok":bool,"data":T,"error":str}
│       │   │   │   │   ├── CreateScheduleDto.kt
│       │   │   │   │   ├── TimelineDto.kt
│       │   │   │   │   └── ReminderDto.kt
│       │   │   │   └── mapper/
│       │   │   │       └── TimelineMapper.kt # DTO → Domain 转换
│       │   │   ├── mock/
│       │   │   │   └── MockApi.kt            # 第一阶段假数据（9 条日程 + 1 条提醒）
│       │   │   └── repository/
│       │   │       └── HomeRepository.kt     # 数据仓库（7 个方法）
│       │   │
│       │   ├── di/
│       │   │   └── AppModule.kt              # Hilt：切换 MockApi ↔ KtorHomeApi
│       │   │
│       │   └── ui/
│       │       ├── home/                     # 首页
│       │       │   ├── HomeScreen.kt
│       │       │   ├── HomeViewModel.kt
│       │       │   ├── HomeUiState.kt
│       │       │   └── components/
│       │       │       ├── HomeTopBar.kt             # Sync 标题 + 日期 + 生活管家/预设模式入口
│       │       │       ├── ReminderCard.kt           # AI 提醒卡片（琥珀色调）
│       │       │       ├── ScheduleCard.kt           # 日程卡片（左侧色条 + 进行中呼吸光晕）
│       │       │       ├── ConflictPairCard.kt       # 2 日程冲突并列
│       │       │       ├── ConflictGroupCard.kt      # 3+ 日程冲突折叠
│       │       │       ├── TimelineSection.kt        # 时间轴容器
│       │       │       ├── HomeInputBar.kt           # 底部输入栏（Psychology 图标）
│       │       │       ├── EmptyState.kt             # 空状态
│       │       │       ├── BottomTabBar.kt           # 5 个 Tab 底部导航
│       │       │       ├── ScheduleIcons.kt          # 标题→图标映射
│       │       │       ├── ConflictBottomSheet.kt    # 冲突弹窗（暖琥珀主题）
│       │       │       ├── NluConfirmCard.kt         # NLU 确认卡片（琥珀色调）
│       │       │       └── ReminderBottomSheet.kt    # AI 提醒弹窗（暖琥珀主题）
│       │       ├── scheduledetail/           # 日程详情页（前端2）
│       │       │   ├── ScheduleDetailScreen.kt
│       │       │   └── ScheduleDetailViewModel.kt
│       │       ├── newschedule/              # 新建/编辑日程表单（前端2）
│       │       │   ├── NewScheduleScreen.kt  # 支持新建 + 编辑双模式
│       │       │   ├── NewScheduleViewModel.kt
│       │       │   └── NewScheduleUiState.kt
│       │       ├── voice/                   # Vosk 离线语音识别
│       │       │   ├── VoskVoiceHelper.kt    # 录音 + 识别封装
│       │       │   └── VoskModelManager.kt   # 模型下载管理
│       │       ├── plan/                    # 计划页（占位）
│       │       ├── stats/                   # 统计页（占位）
│       │       ├── diary/                   # 日记页（已完成日程列表）
│       │       │   ├── DiaryScreen.kt
│       │       │   └── DiaryViewModel.kt
│       │       ├── mine/                    # 我的/设置 + 生活管家
│       │       │   ├── MineScreen.kt        # 通知/默认设置/数据管理/关于
│       │       │   └── LifestyleScreen.kt   # 今日状态/最近活动/生活建议
│       │       ├── modes/                   # 预设模式（选择→倒计时→反馈）
│       │       │   ├── ModeSelectScreen.kt
│       │       │   ├── ModeTimerScreen.kt   # 琥珀渐变背景
│       │       │   ├── ModeFeedbackScreen.kt
│       │       │   └── ModeType.kt
│       │       └── theme/                   # 暖琥珀设计系统
│       │           ├── Color.kt             # 琥珀主色 + 薄暮蓝辅助 + 暖灰中性 + AI 卡片色
│       │           ├── Type.kt              # 排版层级（Display/Headline/Title/Body/Label）
│       │           ├── Theme.kt             # 亮色/暗色双主题 + M3 全部角色映射
│       │           └── PriorityColors.kt    # P0/P1/P2 暖琥珀系色板
│       └── res/
│           ├── values/strings.xml
│           ├── values/themes.xml
│           └── values-night/themes.xml
│
└── sync-logic/                              # Python 后端（regeonchen 负责）
    ├── server.py                            # HTTP 服务入口（端口 8800）
    ├── db.py                                # SQLite 数据库操作
    ├── timeline.py                          # 时间轴排序 + 冲突检测
    ├── nlu.py                               # 自然语言解析引擎（P1）
    ├── reminders.py                         # AI 提醒检测引擎（P1）
    ├── test_db.py                           # 数据库测试
    ├── test_timeline.py                     # 冲突检测测试
    ├── test_nlu.py                          # NLU 20 条用例测试
    └── sync.db                              # SQLite 数据库文件（自动生成）
```

---

## 三、怎么跑起来

1. **启动后端**：
   ```bash
   cd sync-logic
   python server.py
   # 服务运行在 http://localhost:8800
   ```

2. **跑后端测试**（可选）：
   ```bash
   python test_db.py    # 16 条 CRUD 用例
   python test_nlu.py   # 20 条 NLU 用例
   python demo_e2e.py   # 端到端流程（自动启 server + 模拟前端调用）
   ```

3. **启动前端**（Android Studio）：
   - `File` → `Open` → 选择 `sync_pre1` 目录
   - 等 Gradle 同步完成
   - 接上手机或启动模拟器（API 26+）
   - 点 ▶ Run

4. **从 Mock 切换到真实后端**：当前默认 `AppModule.kt` 绑定的就是 `KtorHomeApi`。如果要切回 Mock（无后端演示）：
   ```kotlin
   // AppModule.kt 中改回：
   fun provideHomeApi(): HomeApi = MockApi()
   ```

---

## 四、前后端数据契约

### 统一响应格式

```json
{"ok": true, "data": {...}}
{"ok": false, "error": "错误原因"}
```

### 字段名约定

全部使用 **snake_case**——JSON 传输和 SQLite 存储都是，前端 Kotlin 类通过 `@SerialName` 映射到驼峰：

| JSON / DB 字段 | Kotlin 字段 |
|---------------|-------------|
| `start_time` | `startTime` |
| `end_time` | `endTime` |
| `related_schedule_ids` | `relatedScheduleIds` |

### HomeApi 接口（7 个方法）

| 方法 | 用途 | 状态 |
|------|------|------|
| `getTimeline(date)` | 获取当天时间轴（含冲突分组） | ✅ Mock |
| `checkReminders(date)` | 检测当天 AI 提醒 | ✅ Mock |
| `createSchedule(dto)` | 创建新日程 | ✅ Mock |
| `getSchedule(id)` | 查询单条日程 | ✅ Mock |
| `updateSchedule(id, dto)` | 更新日程字段 | ✅ Mock |
| `deleteSchedule(id)` | 删除日程 | ✅ Mock |
| `markComplete(id)` | 标记日程为已完成 | ✅ Mock |

---

## 五、当前开发进度

### 前端（已完成）

| 模块 | 内容 | 责任人 | 状态 |
|------|------|--------|------|
| 设计系统 | 暖琥珀色板 + M3 双主题 + 排版层级 + 优先级色板 | mengyiguang | ✅ |
| 首页主界面 | HomeScreen、时间轴渲染、日程卡片、冲突卡片、AI 提醒卡片、空状态、输入栏、Tab 导航 | xingguangzengyuyi | ✅ |
| 数据模型 | Schedule、Priority、Reminder、TimelineItem、HomePageData | xingguangzengyuyi | ✅ |
| Mock 数据 | 9 条日程 + 1 条 AI 提醒 + 完整 CRUD Mock | xingguangzengyuyi | ✅ |
| HTTP 客户端 | Ktor + Hilt DI + MockApi/KtorHomeApi 切换 | xingguangzengyuyi | ✅ |
| 新建日程表单 | 表单 UI + 客户端校验 + 保存 | mengyiguang | ✅ |
| 编辑日程 | 复用新建表单 + 预填数据 + 调用 update | mengyiguang | ✅ |
| 日程详情页 | 信息展示 + 标记完成 + 编辑入口 + 删除确认 | mengyiguang | ✅ |
| 底部导航占位 | 计划/统计/日记/我的 4 个 Tab | mengyiguang | ✅ |
| 冲突弹窗 | ConflictBottomSheet（暖琥珀主题 + 呼吸光晕） | mengyiguang | ✅ |
| NLU 确认卡片 | NluConfirmCard（琥珀色调 + 置信度分层） | mengyiguang | ✅ |
| AI 提醒弹窗 | ReminderBottomSheet（暖琥珀主题 + 三级按钮） | mengyiguang | ✅ |
| 预设模式 | 选择→倒计时→反馈 三页面（琥珀色调） | mengyiguang | ✅ |
| 设置页 + 生活管家 | MineScreen（通知/默认设置/数据管理/关于）+ LifestyleScreen | mengyiguang | ✅ |
| UI 组件润色 | ReminderCard、ScheduleCard、HomeInputBar、HomeTopBar、ModeTimerScreen 琥珀主题 | mengyiguang | ✅ |
| BottomSheet 接入 | ConflictBottomSheet / ReminderBottomSheet 在首页内嵌弹窗 | mengyiguang | ✅ |
| NLU 确认接入 | NluConfirmCard 接入输入栏发送流程（Mock 解析） | mengyiguang | ✅ |

### 前端（已完成）

| 内容 | 状态 |
|------|------|
| 新建/编辑/删除 对接真实 API | ✅ 第 4 阶段已完成 |
| NLU 确认卡片接入真实解析 | ✅ 第 3 阶段已完成 |
| AI 提醒建议执行（顺延接口） | ✅ 第 4 阶段已完成（`POST /api/schedules/{id}/postpone`） |
| 离线语音识别 | ✅ Vosk 引擎 + 模型下载管理 + 实时识别 + 语音预览条 + NLU 自动发送 |
| NLU 修改预填 | ✅ "修改一下"自动填入解析字段（标题/日期/时间/优先级/时长） |

### 后端（已完成 — 第 4 阶段全部上线）

| 内容 | 状态 |
|------|------|
| HTTP 服务（8800 端口） | ✅ |
| SQLite 数据库 + 建表 | ✅ |
| `GET /api/timeline?date=` | ✅ |
| `POST /api/schedules`（创建） | ✅ |
| `GET /api/schedules/{id}`（查单条） | ✅ |
| `PUT /api/schedules/{id}`（更新） | ✅ |
| `DELETE /api/schedules/{id}`（删除） | ✅ |
| `POST /api/schedules/{id}/complete`（标记完成） | ✅ |
| `POST /api/schedules/{id}/postpone`（顺延） | ✅ 第 4 阶段新增 |
| `GET /api/reminders/check?date=` | ✅ |
| `POST /api/nlu/parse`（自然语言解析） | ✅ 第 3 阶段新增 |

### 测试覆盖

| 测试文件 | 覆盖范围 | 状态 |
|---|---|---|
| `sync-logic/test_db.py` | db.py 16 条 CRUD 用例（init / create / get / update / delete / mark_complete / postpone） | ✅ 16/16 通过 |
| `sync-logic/test_nlu.py` | nlu.py 20 条 MVP 用例（《本地规则.md》§17） | ✅ 20/20 通过 |
| `sync-logic/demo_e2e.py` | 端到端 HTTP 流程（创建 / 时间轴 / 标记完成 / 顺延 / 编辑 / 删除 / NLU） | ✅ 全流程通过 |

---

## 六、设计系统速览

### 暖琥珀主色

| 色值 | 名称 | 用途 |
|------|------|------|
| `#F5A623` | AmberBase | 主按钮、进行中状态、焦点色 |
| `#C17D11` | AmberDark | P0 背景 |
| `#835500` | AmberDeep | 文字强调、P0 徽章 |
| `#FFF1E6` | AmberLight | 卡片底色、柔和背景 |
| `#FFF5EB` | AiCardBg | AI 提醒卡片专用底色 |

### 优先级色板

| 优先级 | 背景 | 色条 | 文字 |
|--------|------|------|------|
| P0 | `AmberDark` | `AmberDeep` | 白色 |
| P1 | `AmberLight` | `AmberBase` | 深色 |
| P2 | `WarmSurface` | `WarmOutline` | 深色 |

### 关键动画

- **进行中日程**：`InfiniteTransition` 琥珀呼吸光晕（alpha 0.03 ↔ 0.10，周期 1.5s）
- **冲突/提醒建议区**：同动画，吸引注意但不刺眼

---

## 七、调试指南

| 问题 | 解决 |
|------|------|
| `gradle-wrapper.jar` 找不到 | Android Studio 自动生成；命令行 `gradle wrapper --gradle-version 8.5` |
| Compose 编译 Kotlin 版本不兼容 | `libs.versions.toml` 中 `kotlin = "1.9.22"` 与 compose compiler 需匹配 |
| Ktor JSON 解析失败 | 确认后端返回字段名是 snake_case（`start_time` 不是 `startTime`） |
| 中文显示乱码 | `gradle.properties` 加 `org.gradle.jvmargs=-Dfile.encoding=UTF-8` |
| 模拟器连不上后端 | 确认后端在 `0.0.0.0:8800` 监听，模拟器用 `10.0.2.2` |
| 后端 8800 端口被占用 | `netstat -ano \| findstr 8800` 查看并关闭占用进程 |
| Vosk 模型下载失败 | 检查网络连接，模型约 42MB 从 alphacephei.com 下载；也可手动放入 `vosk_models/vosk-model-small-cn-0.22/` |
| 语音识别无反应 | 确认麦克风权限已授予；检查 `voskModelReady` 是否为 true（模型已下载且初始化成功） |
