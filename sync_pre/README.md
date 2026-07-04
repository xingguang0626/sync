# sync_pre · Life MVP 第一阶段前端代码

> 这是 `后端开发规划.md` + `第一阶段-前端1-主界面开发任务.md` 的代码落地。
> **第一阶段目标**：首页能显示今日时间轴（含冲突卡片）+ 优先级色块 + AI 提醒卡片 + 底部输入栏。

---

## 工程结构

```
sync_pre/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml         # version catalog，集中管理版本
│   └── wrapper/
│       └── gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/life/app/
        │   ├── LifeApplication.kt        # @HiltAndroidApp 入口
        │   ├── MainActivity.kt           # setContent { LifeTheme { LifeApp() } }
        │   ├── domain/model/             # 纯 Kotlin data class + enum
        │   │   ├── Schedule.kt
        │   │   ├── Priority.kt
        │   │   ├── Reminder.kt
        │   │   ├── TimelineItem.kt
        │   │   └── HomePageData.kt
        │   ├── data/
        │   │   ├── remote/               # Ktor HTTP 客户端
        │   │   │   ├── HomeApi.kt        # 接口
        │   │   │   ├── KtorHomeApi.kt    # 真接口实现（第二阶段启用）
        │   │   │   ├── KtorClientFactory.kt
        │   │   │   ├── ApiResult.kt      # sealed: Loading/Success/Failure
        │   │   │   ├── dto/              # 网络 DTO（snake_case 字段名）
        │   │   │   │   ├── ApiResponseDto.kt
        │   │   │   │   ├── TimelineDto.kt
        │   │   │   │   └── ReminderDto.kt
        │   │   │   └── mapper/
        │   │   │       └── TimelineMapper.kt
        │   │   ├── mock/
        │   │   │   └── MockApi.kt        # 第一阶段使用的假数据
        │   │   └── repository/
        │   │       └── HomeRepository.kt
        │   ├── di/
        │   │   └── AppModule.kt          # Hilt Module
        │   └── ui/
        │       ├── theme/
        │       │   ├── Color.kt
        │       │   ├── Type.kt
        │       │   ├── Theme.kt          # LifeTheme（Material 3）
        │       │   └── PriorityColors.kt # P0/P1/P2 蓝色系色板
        │       └── home/
        │           ├── HomeScreen.kt     # 页面入口
        │           ├── HomeViewModel.kt
        │           ├── HomeUiState.kt
        │           └── components/
        │               ├── HomeTopBar.kt        # Life 标题 + 日期 + L/M 入口
        │               ├── ReminderCard.kt      # AI 提醒卡片
        │               ├── ScheduleCard.kt      # 日程卡片（含进行中、P0、P1、P2 视觉）
        │               ├── ConflictPairCard.kt  # 2 日程冲突
        │               ├── ConflictGroupCard.kt # 3+ 日程冲突折叠
        │               ├── TimelineSection.kt   # 时间轴主容器
        │               ├── HomeInputBar.kt      # 底部输入栏
        │               ├── EmptyState.kt
        │               ├── BottomTabBar.kt      # 5 个 tab
        │               └── ScheduleIcons.kt     # title → icon 映射
        └── res/
            ├── values/
            │   ├── strings.xml
            │   └── themes.xml
            └── values-night/
                └── themes.xml
```

---

## 怎么跑起来

1. **Android Studio** → `File` → `Open` → 选择 `sync_pre` 目录
2. 等 Gradle 同步完成（首次会下载 Compose BOM / Ktor / Hilt 等依赖）
3. 接上手机或启动模拟器（API 26+）
4. 点 ▶ Run

> ⚠️ **首次打开需要在 Android Studio 里执行一次** `File` → `Sync with Gradle Files`，会自动生成 `gradle-wrapper.jar` 和 `gradlew` 脚本。
> 如果不想装 Android Studio，也可以命令行跑：
> ```bash
> cd sync_pre
> ./gradlew assembleDebug
> # 或：gradle assembleDebug（如果有系统 gradle）
> ```

---

## 第一阶段交付物（按 `第一阶段-前端1-主界面开发任务.md`）

| 验收项 | 状态 |
| --- | --- |
| 首页正常打开，无 ANR / Crash | ✅ Mock 数据 150ms 延迟 |
| 5+ 条 Mock 日程按时间排序展示 | ✅ 含 2 条 P0（深度学习 + Deadline 项目交付） |
| P0/P1/P2 蓝色系梯度区分 | ✅ 深蓝→中蓝→浅灰（`PriorityColors.kt`） |
| AI 提醒卡片 | ✅ 浅蓝背景 + sparkles + 时间戳 + 两个动作按钮 |
| 冲突卡片渲染 | ✅ ConflictPairCard（左右并列）+ ConflictGroupCard（折叠可展开） |
| 底部 + 跳转 | ⚠️ TODO：第二阶段接入 NewSchedulePage |
| 底部输入栏发送 | ⚠️ TODO：第三阶段接入 nlu.parse |
| Loading / Error / Empty / Content 四态 | ✅ `ApiResult` 三态分发 |
| 沉浸式状态栏 | ✅ `enableEdgeToEdge()` + `statusBarsPadding()` |

---

## 怎么从 Mock 切换到真后端

`app/src/main/java/com/life/app/di/AppModule.kt`：

```kotlin
// 第一阶段（默认）
@Provides
@Singleton
@Named("homeApi")
fun provideHomeApi(): HomeApi = MockApi()

// ⚠️ 第二阶段启用：注释掉上面那段，改成下面这段
// @Provides
// @Singleton
// @Named("homeApi")
// fun provideHomeApi(client: HttpClient): HomeApi = KtorHomeApi(client)
```

切到真后端时，**改这一行就够了**，Repository / ViewModel / UI 一行不改。

模拟器访问 host 机器默认用 `10.0.2.2:8800`（在 `app/build.gradle.kts` 的 `buildConfigField` 配置）。
真机调试改成电脑局域网 IP 后重 build。

---

## 还没做（按阶段划分）

- [ ] **第二阶段**：手动新建日程页 / 详情弹窗 / 编辑删除 / 冲突二级菜单 / 切真后端
- [ ] **第三阶段**：自然语言输入接入（`nlu.parse`）/ AI 提醒二级菜单
- [ ] **第四阶段**：预设模式 / 生活管家 / 设置页 / 其它 4 个 Tab

这些都在文档里详细列了，照着 `HomeUiEvent` 各个分支的 TODO 实现就行。

---

## 调试常见问题

| 问题 | 解决 |
| --- | --- |
| `gradle-wrapper.jar` 找不到 | Android Studio 自动生成；或命令行执行 `gradle wrapper --gradle-version 8.5` |
| Compose 编译报 Kotlin 版本不兼容 | 确认 `libs.versions.toml` 里的 `kotlin = "1.9.22"` 和 `composeCompiler = "1.5.8"` 匹配 |
| Ktor 解析 JSON 失败 | 确认后端返回的字段名是 snake_case（`start_time` 不是 `startTime`） |
| 中文显示乱码 | 确认 `gradle.properties` 里有 `org.gradle.jvmargs=-Dfile.encoding=UTF-8` |