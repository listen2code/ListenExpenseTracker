# ListenExpenseTracker - 架构决策记录 (Architecture Decision Records - ADR)

本文档系统性记录 **ListenExpenseTracker** 及其关联架构库在全生命周期演进过程中的关键架构决策、技术权衡、设计思路、核心难点攻坚以及关键代码的逐行中文深度解析。

---

## 目录索引 (ADR Index)

| 编号 | 决策主题 | 核心分类 | 状态 |
| :--- | :--- | :--- | :--- |
| [ADR-001](#adr-001-采用-mvi-model-view-intent-作为核心展示层架构) | 采用 MVI 作为核心展示层架构 | 基础架构 / 展示层 | **Accepted** |
| [ADR-002](#adr-002-本地优先-local-first-与-room-sqlite-结合) | 本地优先 (Local-First) 与 Room SQLite 结合 | 数据持久化 | **Accepted** |
| [ADR-003](#adr-003-gradle-composite-build-多仓库模块解耦) | Gradle Composite Build 多仓库模块解耦 | 工程构建 / 模块化 | **Accepted** |
| [ADR-004](#adr-004-apm-性能监控与-500-条环形内存日志) | APM 性能监控与 500 条环形内存日志 | 运维稳定性 / APM | **Accepted** |
| [ADR-005](#adr-005-真实-google-账户连携与无服务器云端快照同步体系) | 真实 Google 账户连携与无服务器云端快照同步体系 | 云同步 / 鉴权 | **Accepted** |
| [ADR-006](#adr-006-账单滑动删除软删除撤销通道-undo-snackbar-pattern) | 账单滑动删除“软删除撤销通道” | 交互模式 / 副作用 | **Accepted** |
| [ADR-007](#adr-007-专属年月网格选择器与日聚合高信息密度投影) | 专属年月网格选择器与日聚合高信息密度投影 | 交互视觉 / 列表优化 | **Accepted** |
| [ADR-008](#adr-008-listen-系列多-app-架构边界与通用库业务解耦) | Listen 系列多 App 架构边界与通用库业务解耦 | 领域设计 / 代码边界 | **Accepted** |
| [ADR-009](#adr-009-资产账户多维分层与破坏性操作确认规范) | 资产账户多维分层与破坏性操作确认规范 (Rule 15) | 交互设计 / 资产安全 | **Accepted** |
| [ADR-010](#adr-010-ui-组件深度拆分解耦与单文件行数控制标准) | UI 组件深度拆分解耦与单文件行数控制标准 (Rule 3) | 代码整洁 / 架构治理 | **Accepted** |
| [ADR-011](#adr-011-cicd-自动化构建与发布成功邮件通知机制) | CI/CD 自动化构建与发布成功邮件通知机制 | 持续交付 / DevOps | **Accepted** |
| [ADR-012](#adr-012-触觉反馈系统与组件级平滑动效体系) | 触觉反馈系统与组件级平滑动效体系 | 交互体验 / 动效体系 | **Accepted** |
| [ADR-013](#adr-013-release-pipeline-工业级上线交付与多轨道发布规范) | Release Pipeline 工业级上线交付与多轨道发布规范 | 应用商店 / 交付治理 | **Accepted** |
| [ADR-014](#adr-014-设置页信息架构现代化重组与数据中心收拢) | 设置页信息架构现代化重组与数据中心收拢 | 信息架构 / 设置体系 | **Accepted** |
| [ADR-015](#adr-015-全局月份状态联动与统计排行榜视觉体系升级) | 全局月份状态联动与统计排行榜视觉体系升级 | 数据分析 / 跨屏联动 | **Accepted** |
| [ADR-016](#adr-016-状态树持久化保持与图表动态刷新动效协议) | 状态树持久化保持与图表动态刷新动效协议 | Compose 重组 / 动效 | **Accepted** |
| [ADR-017](#adr-017-引入-stateholder-模式管理-ui-框架状态) | 引入 StateHolder 模式管理 UI 框架状态 | Compose 设计模式 | **Accepted** |
| [ADR-018](#adr-018-统一合并新增与编辑弹窗-unified-transaction-sheet) | 统一合并新增与编辑弹窗 (Unified Transaction Sheet) | 组件重用 / DRY 原则 | **Accepted** |
| [ADR-019](#adr-019-分类维度精细化预算与动态权重换算体系) | 分类维度精细化预算与动态权重换算体系 | 领域逻辑 / 财务预算 | **Accepted** |
| [ADR-020](#adr-020-泛型路由双重生命周期派发适配-commonroute) | 泛型路由双重生命周期派发适配 (`CommonRoute`) | Compose / 生命周期 | **Accepted** |
| [ADR-021](#adr-021-桌面小部件-20-4x2-智能双模看板与闪电分类直达记账) | 桌面小部件 2.0 智能双模看板与闪电分类直达记账 | 系统集成 / 小部件 | **Accepted** |
| [ADR-022](#adr-022-生物识别应用锁单调时钟防作弊与多任务防窥治理) | 生物识别应用锁、单调时钟防作弊与多任务防窥治理 | 隐私安全 / 逆向防御 | **Accepted** |
| [ADR-023](#adr-023-纯函数式智能财务诊断引擎与-9-大核心规则策略) | 纯函数式智能财务诊断引擎与 9 大核心规则策略 | 智能算法 / 纯计算 | **Accepted** |
| [ADR-024](#adr-024-记账表单金额展示规范与双小数位输入防溢流) | 记账表单金额展示规范与双小数位输入防溢流 | 输入控制 / 格式化 | **Accepted** |
| [ADR-025](#adr-025-跨-tab-年月视图双向联动与统计下钻保持机制) | 跨 Tab 年月视图双向联动与统计下钻保持机制 | 跨屏编排 / 复杂状态机 | **Accepted** |
| [ADR-026](#adr-026-开发者模式-5-击隐秘唤醒与真实数据防脏守卫) | 开发者模式 5 击隐秘唤醒与真实数据防脏守卫 | 调试通道 / 数据防护 | **Accepted** |
| [ADR-027](#adr-027-版本在线校验与-google-play-更新闭环) | 版本在线校验与 Google Play 更新闭环 | 应用生命周期 / 更新 | **Accepted** |
| [ADR-028](#adr-028-底部导航双击置顶与智能归位当前周期) | 底部导航双击置顶与智能归位当前周期 | 导航人体工学 / 交互 | **Accepted** |
| [ADR-029](#adr-029-设置画面-switch-触控热区全行标准化) | 设置画面 Switch 触控热区全行标准化 | 人体工学 / 无障碍 | **Accepted** |
| [ADR-030](#adr-030-流水画面筛选按钮大拇指黄金热区化与长按快速重置) | 流水画面筛选按钮大拇指黄金热区化与长按快速重置 | 人体工学 / 复合手势 | **Accepted** |
| [ADR-031](#adr-031-账单-excelcsv-导出与-utf-8-bom-防乱码双轨分享机制) | 账单 Excel/CSV 导出与 UTF-8 BOM 防乱码双轨分享机制 | 数据交互 / 互操作性 | **Accepted** |
| [ADR-032](#adr-032-本地智能通知预警与多渠道分流提醒中枢) | 本地智能通知预警与多渠道分流提醒中枢 | 系统通知 / 权限闭环 | **Accepted** |
| [ADR-033](#adr-033-全功能组件与屏幕-preview-可视化覆盖架构规范) | 全功能组件与屏幕 @Preview 可视化覆盖架构规范 | 开发体验 / 隔离调试 | **Accepted** |
| [ADR-034](#adr-034-设置中心高紧凑布局重塑与智能通知单开关整合) | 设置中心高紧凑布局重塑与智能通知单开关整合 | 视觉交互 / 信息紧凑化 | **Accepted** |

---

## ADR-001: 采用 MVI (Model-View-Intent) 作为核心展示层架构

### 背景 (Context)
在现代复杂的财务管理应用中，记账流水、日历筛选、按月/按年统计、收支 Tab 切换等操作高度密集。传统 MVVM 模式中，ViewModel 经常暴露大量散碎的 `MutableLiveData` 或 `MutableStateFlow` 变量（例如 `selectedMonth`、`filterType`、`isLoading`、`listData`）。这种状态碎片化模式极易导致事件竞态条件（Race Condition），造成 Compose 组合过程中的状态撕裂（State Inconsistency）与无意义的过度重组（Excessive Recomposition）。

### 决策与设计思路 (Decision & Rationale)
全工程统一步调，全量采用严格的 **纯 Kotlin MVI (Model-View-Intent)** 单向响应式架构：
1. **单一真实源不可变快照 (UiState)**：页面仅观察单一只读 `StateFlow<ViewState>`，状态更新必须通过原子化 `copy()` 产出全新快照，杜绝在外部或多线程中直接修改属性；
2. **唯一意图派发入口 (UiIntent)**：所有用户事件（点击、滑动、输入、切换时间）抽象为强类型密封接口（Sealed Interface），统一由 `handleIntent(intent)` 驱动，天然便于自动化测试与事件溯源追踪；
3. **隔离瞬态单次事件 (ViewEffect)**：Toast、Snackbar 撤销条、页面跳转等瞬态事件绝不保存在持久状态中，而是通过底层缓冲通道 `Channel<ViewEffect>` 独立发射，彻底杜绝屏幕旋转或配置变更时重复触发弹窗。

### 重点代码实现细节 (Code Walkthrough)

```kotlin
// 文件位置: ListenArch/listen-arch/.../mvi/BaseViewModel.kt
abstract class BaseViewModel<S : Any, I : Any>(initialState: S) : ViewModel() {

    // 1. 持久状态流：使用 MutableStateFlow 缓存当前状态，对外仅暴露只读 StateFlow
    private val _viewState = MutableStateFlow(initialState)
    val viewState: StateFlow<S> = _viewState.asStateFlow()

    // 2. 瞬态单次事件通道：使用 Channel.BUFFERED 确保事件不阻塞且被且仅被消费一次
    // 转换为 Flow 供 UI 层的 CollectCommonUiEffects 订阅
    private val _viewEffect = Channel<CommonUiEffect>(Channel.BUFFERED)
    val viewEffect: Flow<CommonUiEffect> = _viewEffect.receiveAsFlow()

    // 3. 意图派发核心入口：强类型抽象，所有业务逻辑流向的唯一起点
    abstract fun handleIntent(intent: I)

    // 4. 原子状态更新函数：通过 Kotlin 扩展 Lambda 执行纯函数式状态转换
    // update 具有线程安全的 CAS (Compare-And-Swap) 保障，彻底避免并发竞争
    protected fun updateState(reducer: S.() -> S) {
        _viewState.update { current -> current.reducer() }
    }

    // 5. 瞬态副作用发射：启动协程将单次事件写入通道
    protected fun emitEffect(effect: CommonUiEffect) {
        viewModelScope.launch {
            _viewEffect.send(effect)
        }
    }
}
```

### 影响与权衡 (Consequences)
- **收益**：状态流向单一透明，重绘逻辑严密；UI 具备确定性（给定状态 S 必得固定渲染 UI）；单元测试只需向 `handleIntent` 喂入输入，校验 `viewState` 快照即可。
- **成本**：对于简单交互（如临时文本输入）需要声明对应的 Intent 实体，代码样板相对增加，需结合 `StateHolder` 做局部平衡。

---

## ADR-002: 本地优先 (Local-First) 与 Room SQLite 结合

### 背景 (Context)
记账具有强烈的即时性与碎片化特征（如在地下车库扫码缴费、在超市无信号区域付款）。如果每次写操作均依赖云端 API 往返响应，网络延迟或超时会导致用户严重受挫，甚至直接放弃记账。

### 决策与设计思路 (Decision & Rationale)
全面践行 **Local-First** 架构哲学：
1. **本地为主存储源 (Primary Storage)**：所有账单记账、编辑、删除均 100% 优先写入手机本地的 Room SQLite 数据库，主线程无感异步落盘；
2. **响应式自动刷新**：Room DAO 方法返回 `Flow<List<TransactionEntity>>`，底层数据一旦发生变更，自动触发 Flow 推送并驱动 Compose 界面重构；
3. **非阻塞异步云同步**：网络请求退居二线，由后台守护服务（`GoogleDriveAutoBackupManager`）在手机闲置、切后台或指定时机以增量/快照方式异步同步至 Google 云端。

---

## ADR-003: Gradle Composite Build 多仓库模块解耦

### 背景 (Context)
`ListenArch`（核心架构）与 `ListenUiComponent`（通用设计系统）是 Listen 系列多款 App（记账、待办、习惯打卡）共同依赖的公共技术资产。如果将它们作为子模块置于单一巨石仓库，极易由于误导入造成业务与底座的隐式耦合；而如果完全拆分成远端 Maven 依赖，每次修改底层代码都需要发版，极大削弱开发联调效率。

### 决策与设计思路 (Decision & Rationale)
采用 Gradle **复合构建 (Composite Build)** 机制（`includeBuild`）：
- 物理上代码各自存放在独立的 Git 仓库中，保持仓库边界纯洁；
- 在宿主工程的 `settings.gradle.kts` 中声明依赖替换规则（Dependency Substitution）；
- 开发者在 Android Studio 中打开 `ListenExpenseTracker` 时，底座 SDK 会作为本地工程平滑挂载，享受代码跳转、实时断点调试与联动重构能力。

```kotlin
// settings.gradle.kts
// 采用 Gradle 复合构建实现独立 SDK 本地零延迟联调
includeBuild("../ListenArch") {
    dependencySubstitution {
        substitute(module("com.listen:listen-arch")).using(project(":listen-arch"))
    }
}
includeBuild("../ListenUiComponent") {
    dependencySubstitution {
        substitute(module("com.listen:listen-uicomponent")).using(project(":listen-uicomponent"))
    }
}
```

---

## ADR-004: APM 性能监控与 500 条环形内存日志

### 背景 (Context)
在线上环境中，用户偶发的卡顿、数据不同步或非崩溃逻辑异常极难复现。引入诸如 Firebase Performance、Bugly 等重型第三方 APM SDK 会给轻量记账应用增加数兆体积和冷启动耗时，且存在个人财务数据上传第三方合规风险。

### 决策与设计思路 (Decision & Rationale)
自研零第三方依赖、完全本地化的 `ApmLogger` 与 `TraceManager`：
1. **固定容量环形内存缓冲区 (Ring Buffer)**：在内存中维护最大容量为 500 条的日志链表，超出时自动淘汰最旧日志，内存开销恒定在百 KB 级别，0 额外 Disk I/O 损耗；
2. **多通道日志分流**：划分 `APP`、`DB`、`SYNC`、`CRASH` 四大诊断频道；
3. **开箱即用可视化排查**：内置 `LogInspectorSheet` 调试面板，技术人员可现场长按复制、分级过滤，或一键导出完整日志链路。

```kotlin
// ListenArch 中的轻量环形链表日志核心机制
object ApmLogger {
    private const val MAX_LOG_SIZE = 500
    // 使用线程安全的 ArrayDeque 充当定长环形队列
    private val buffer = ArrayDeque<LogEntry>(MAX_LOG_SIZE)
    private val lock = Any()

    fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        synchronized(lock) {
            // 当达到容量上限时，抛弃队首最老的数据，始终保留最近 500 条高价值上下文
            if (buffer.size >= MAX_LOG_SIZE) {
                buffer.removeFirst()
            }
            buffer.addLast(LogEntry(System.currentTimeMillis(), tag, message, level))
        }
    }
}
```

---

## ADR-005: 真实 Google 账户连携与无服务器云端快照同步体系

### 背景 (Context)
用户在跨设备换机、重装系统或日常使用中存在资产账目丢失风险。建立专属服务器需要投入数据库运维、安全合规成本，且用户对将个人财务敏感数据上传至第三方自建服务器心存顾虑。

### 决策与设计思路 (Decision & Rationale)
全面拥抱 **Google Identity + Google Drive REST API v3** 无服务器直连方案：
1. **现代单键凭据鉴权**：采用 Android 官方最新 `androidx.credentials.CredentialManager`，一键拉起系统 Google 账号授信，获得安全的 ID Token；
2. **直连个人私有云盘**：基于轻量 HTTP 直连用户个人 Google Drive，将账单与配置以加密 JSON 结构（`lexpense_backup.json`）存储在用户自有的云盘根目录下；
3. **动态 OAuth2 提权补偿**：针对 Drive API 权限不足边界，系统捕获 `UserRecoverableAuthException` 并调起系统专属授权面板，获得授权后无缝重试。

---

## ADR-006: 账单滑动删除“软删除撤销通道” (Undo Snackbar Pattern)

### 背景 (Context)
在列表流水管理中，滑动删除 (Swipe-to-Delete) 交互极其高效，但极易因单手手滑发生误删；如果每次滑动删除都强行弹出模态确认对话框（Modal Dialog），又会极大破坏流畅的交互体验。

### 决策与设计思路 (Decision & Rationale)
设计并引入 **双层防误触 + 瞬态软删除撤销** 机制：
1. **70% 高滑动阈值防御**：仅当用户大幅度划动超过条目宽度的 70% 时才判定为有意删除，规避轻微横划造成的误触；
2. **带 Action 的 Snackbar 瞬态撤销**：删除发生时，数据库立即删除，但 ViewModel 内存中暂存该条目快照，并通过 `emitEffect(ShowSnackbar("已删除账单", actionLabel="撤销"))` 唤起悬浮条；
3. **用户点击“撤销”瞬间逆转**：用户点击撤销时，直接调用 `dao.insertTransaction(cachedItem)` 无损恢复。

---

## ADR-007: 专属年月网格选择器与日聚合高信息密度投影

### 背景 (Context)
Android 原生 `DatePickerDialog` 强迫用户逐层选择日、月、年，切换查看数月前的账单操作路径冗长；此外，若流水按单条账单平铺，用户无法一眼获知当天的净支出总额。

### 决策与设计思路 (Decision & Rationale)
1. **专属双模年月网格选择器 (`MonthPickerDialog`)**：
   - 顶部提供 `< 2026年 >` 极速切换年份箭头；
   - 内部主体采用 3x4 的 12 个月份方块网格直选，任何月份 1 次点击直达；
2. **日维度聚合与紧凑投影 (Daily Projection)**：
   - 在数据流层通过 `groupBy { formatDayGroupHeader(it.timestamp) }` 将流水按天聚合成组；
   - 每个日分组 Header 醒目显示“X月X日 星期几”，右侧清晰汇总“当天总支出 ¥XX / 总收入 ¥XX”；
   - 列表条目图标与内边距紧凑化设计，一屏展示账单量提升 100%。

---

## ADR-008: Listen 系列多 App 架构边界与通用库业务解耦

### 背景 (Context)
在系统演进早期，`ListenArch` 与 `ListenUiComponent` 中混入了特定记账业务的 Room 实体（`TransactionEntity`）、分类枚举、固定“记账”多语言文案，导致开发新的 Listen 系列 App 时无法复用底层库。

### 决策与设计思路 (Decision & Rationale)
确立 **严格零业务耦合 (Zero Business Coupling)** 边界红线：
- **`ListenArch` 彻底去业务化**：移除所有 Room 实体与数据库依赖，仅保留泛型状态机、APM、副作用、多语言调度；
- **`ListenUiComponent` 纯视觉化**：所有 UI 控件的文案与操作均参数化（如键盘“完成”文案由调用方传入），移除任何特定业务模型；
- **业务宿主闭环**：所有账单、分类、资产账户实体全量收拢在 `ListenExpenseTracker/app/` 内。

---

## ADR-009: 资产账户多维分层与破坏性操作确认规范 (Rule 15)

### 背景 (Context)
用户除了标准现金账户外，还会创建微信、支付宝、各类银行卡、信用卡及自定义理财账户。若资产账户被误删，将导致与之关联的账目账户归属失效。

### 决策与设计思路 (Decision & Rationale)
1. **双层账户结构**：系统级预置账户 (`CASH`、`ALIPAY`、`WECHAT`、`BANK`、`CREDIT`) 与动态持久化自定义账户 (`ACC_*`) 分离治理；
2. **破坏性确认标准 (Rule 15)**：
   - 删除账户必须弹出带红色警告色彩的二次确认弹窗；
   - 确认按钮强制采用 `CommonButtonStyle.Danger` 醒目红，取消按钮弱化为 Text 按钮，文案明确告知“删除账户不会删除已有账单数据”。

---

## ADR-010: UI 组件深度拆分解耦与单文件行数控制标准 (Rule 3)

### 背景 (Context)
随着业务复杂度提升，部分核心弹窗（如 `AccountManageDialog.kt`、`TransactionSheet.kt`）行数逐渐突破 400~500 行，代码混杂了模型转换、列表渲染、输入表单与状态维护，维护成本高企。

### 决策与设计思路 (Decision & Rationale)
全面执行 `PROMPTS.md` 规定的 **单文件 $\le 250$ 行架构红线**：
1. **单一职责拆分**：将超长组件解耦为独立的细粒度子组件（例如将卡片渲染、录入表单、确认弹窗各自分离至 `components/` 目录）；
2. **规范参数签名 (Rule 13)**：独立 Composable 组件首个可选参数统一为 `modifier: Modifier = Modifier`；
3. **架构体检保护**：工程在编译与提交前会自动校验 Kotlin 源文件行数，杜绝任何“巨石文件”产生。

---

## ADR-011: CI/CD 自动化构建与发布成功邮件通知机制

### 背景 (Context)
日常开发中的常规代码 Push 和 PR 测试构建非常频繁，如果每次构建都发送提醒邮件，会严重造成信息噪音；而当且仅当正式发版到 Google Play 生产轨道成功时，团队需要第一时间获知确认。

### 决策与设计思路 (Decision & Rationale)
在 GitHub Actions 部署工作流中加入条件邮件触发：
- 配置 `if: success()` 限制，仅在 AAB 生成、R8 缩减、Play App Signing 签名校验且通过 API 成功推送至 Google Play Console 时，向 `listen2code@gmail.com` 发送带版本号、Commit 摘要与流水线运行链接的高优先级邮件。

---

## ADR-012: 触觉反馈系统与组件级平滑动效体系

### 背景 (Context)
移动端纯触屏输入缺乏实体键盘的物理确认感，连续按键极易输入错误；同时，图表在月份切换时的突兀跳变会削弱界面的精致度。

### 决策与设计思路 (Decision & Rationale)
1. **分级触觉系统 (Compose LocalHapticFeedback)**：
   - **高频操作 (Tap)**：`NumericKeypad` 按键、筛选胶囊切换采用 `TextHandleMove` 清脆微触感；
   - **关键提交 (Confirm)**：「完成记账」按钮与危险删除确认采用 `LongPress` 脉冲触感；
2. **渐进式平滑动效**：
   - 图表展开接入 600~650ms `FastOutSlowInEasing` 阻尼曲线，折线与圆环自底向上/顺时针优雅扫开；
   - 月份与收支切换采用 `AnimatedContent` 交叉淡入淡出（Crossfade 300ms）。

---

## ADR-013: Release Pipeline 工业级上线交付与多轨道发布规范

### 背景 (Context)
面向 Google Play 全球多机型发布，需保障极小包体积、混淆安全性与渐进式发布防护。

### 决策与设计思路 (Decision & Rationale)
1. **自动化构建与版本语义化**：`versionCode` 基于构建序号自增，`versionName` 遵循 SemVer 标准；
2. **深度 R8 优化**：Release 构建强制开启 `minifyEnabled true` 与 `shrinkResources true`，剔除未引用资源并归档 `mapping.txt`；
3. **双重密钥隔离**：本地/CI 仅使用 Upload Key 签名，发布由 Google Play 生产密钥重签名；
4. **渐进式灰度流转**：推行 `10% -> 20% -> 50% -> 100%` 灰度流转，依托 Crash/ANR 指标守护质量红线。

---

## ADR-014: 设置页信息架构现代化重组与数据中心收拢

### 背景 (Context)
早期设置页零散放置云端备份、文件导出和账户管理，层级逻辑割裂，不符合直觉。

### 决策与设计思路 (Decision & Rationale)
重组为两大高内聚核心板块：
1. **记账规则中枢 (`SettingsFinanceSection`)**：聚合月度预算、分类管理与资产账户入口；
2. **统一数据中心 (`SettingsDataCenterSection`)**：将 Google Drive 自动备份与本地 JSON 导出/导入合并为一体化操作面板。

---

## ADR-015: 全局月份状态联动与统计排行榜视觉体系升级

### 背景 (Context)
用户在流水页选定历史月份后切到统计页需重新选择；统计页排行榜此前为无序小卡片，缺少竞技感与分类图标。

### 决策与设计思路 (Decision & Rationale)
1. **全局双向月份联动**：`ExpenseAppState` 自动同步跨 Tab 月份偏移量；
2. **榜单视觉升级**：聚合为统一大卡片，赋予金/银/铜领奖台勋章（🥇/🥈/🥉），搭配透明光晕气泡与平滑补间进度条。

---

## ADR-016: 状态树持久化保持与图表动态刷新动效协议

### 背景 (Context)
1. 切换底部 Tab 时默认由于 Composable 卸载导致列表滚动位置重置；
2. 基础图表内部若缺少数据特征签名，会导致 `LazyColumn` 上下滑动子项回收复用时错误重播进场动画。

### 决策与设计思路 (Decision & Rationale)
1. **多 Tab 状态保持**：引入 `rememberSaveableStateHolder()` 托管全局 Tab 切换，结合 `SaveableStateProvider(tab)` 保持离开时的滑动快照；
2. **数据签名驱动动效 (Data Signature Guard)**：
   - 引入 `dataSignature = remember(data) { data.hashCode().toString() }`；
   - 只有当数据签名发生实质改变时才触发动画重置，上下滑动复用时直接以满格状态渲染，杜绝动画闪烁干扰。

```kotlin
// 图表动效数据签名防抖核心模式
@Composable
fun DonutChart(data: List<ChartSlice>, modifier: Modifier = Modifier) {
    // 1. 基于核心数据计算哈希指纹
    val dataSignature = remember(data) { data.hashCode().toString() }
    var lastPlayedSignature by rememberSaveable { mutableStateOf("") }

    // 2. 如果指纹相同，说明只是滚动复用，直接满格渲染；仅在真实数据变更时从 0f 扫开
    val initialProgress = if (lastPlayedSignature == dataSignature) 1f else 0f
    val animProgress = remember { Animatable(initialProgress) }

    LaunchedEffect(dataSignature) {
        if (lastPlayedSignature != dataSignature) {
            animProgress.snapTo(0f)
            animProgress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            lastPlayedSignature = dataSignature
        }
    }
}
```

---

## ADR-017: 引入 StateHolder 模式管理 UI 框架状态

### 背景 (Context)
`ActivityResultLauncher`、滚动位置 `LazyListState` 强依赖 Activity 与 Compose 框架生命周期。如果强行注入 ViewModel，会在屏幕旋转或配置变更时丢失引用，造成严重内存泄露。

### 决策与设计思路 (Decision & Rationale)
推行 **StateHolder 模式**：
- **`ViewModel`**：纯净专注于数据流、领域逻辑与 DataStore 持久化；
- **`StateHolder` (如 `SettingsStateHolder.kt`)**：在 `@Composable` 作用域内声明，专职持有 `ActivityResultLauncher`、`LazyListState` 等框架级对象。

---

## ADR-018: 统一合并新增与编辑弹窗 (Unified Transaction Sheet)

### 背景 (Context)
“记一笔”与“编辑账单”拥有 90% 以上相同的输入要素（键盘、分类选择、备注输入、账户切换）。早期拆分为两个组件导致维护极为冗余，极易出现两边输入逻辑不一致的问题。

### 决策与设计思路 (Decision & Rationale)
1. **单一核心组件**：收拢至 `TransactionSheet.kt`；
2. **可选参数区分状态**：通过 `transaction: TransactionEntity? = null` 参数自适应新增与编辑态；
3. **状态提升与防残留**：通过 `remember(transaction)` 绑定局部状态，彻底防止不同账单连续点击时遗留上一笔的旧数据。

```kotlin
@Composable
fun TransactionSheet(
    transaction: TransactionEntity? = null,
    onConfirm: (TransactionEntity) -> Unit,
    onDismiss: () -> Unit
) {
    // 关键难点：通过 remember(transaction) 确保当外部切换编辑目标时，局部状态被安全重置
    var amountText by remember(transaction) {
        mutableStateOf(transaction?.amount?.let { Double.formatAmount(it) } ?: "")
    }
    var selectedCategoryId by remember(transaction) {
        mutableStateOf(transaction?.categoryId ?: "")
    }
    // ... 输入过程中仅更新本地 mutableState，点击“保存”时才整体打包回调
}
```

---

## ADR-019: 分类维度精细化预算与动态权重换算体系

### 背景 (Context)
单一的全局月度总预算无法针对重点花销品类（如“餐饮美食”）进行超支防控。

### 决策与设计思路 (Decision & Rationale)
1. **分类权重动态映射**：采用分类预算占比权重或独立限额模型；
2. **三态健康度状态机**：
   - **`NORMAL` (正常)**：支出 $< 80\%$ 预算额度；
   - **`WARNING` (警戒)**：支出 $80\% \sim 100\%$；
   - **`OVERBUDGET` (超支)**：支出 $\ge 100\%$，进度条标红警示。

---

## ADR-020: 泛型路由双重生命周期派发适配 (`CommonRoute`)

### 背景 (Context)
在 Compose MVI 架构中，数据同步与状态刷新需要精准感知生命周期。然而，Android 系统前后台切换（Activity `ON_RESUME`/`ON_PAUSE`）与 Compose 跨 Tab 切换（Composable 组合挂载与 `onDispose` 卸载）是两套独立的生命周期通道。

### 决策与设计思路 (Decision & Rationale)
实现全工程唯一的泛型胶水组件 `CommonRoute`：
- 通过 `reified VM` 配合 `inline` 消除反射与闭包开销；
- 使用 `crossinline` 阻止非局部返回；
- 采用 **双重派发 (Dual Dispatch)** 策略，统一收口系统生命周期与 Compose 生命周期，作为纯正的 MVI Intent 派发至 `BaseViewModel`。

```kotlin
// 详见 core/route/CommonRoute.kt
@Composable
inline fun <S : Any, I : Any, reified VM : BaseViewModel<S, I>> CommonRoute(
    viewModel: VM = viewModel(),
    crossinline content: @Composable (state: S, onIntent: (I) -> Unit) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        // 1. 监听系统级生命周期 (Activity 切前后台)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_APPEAR)
                Lifecycle.Event.ON_PAUSE  -> viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_DISAPPEAR)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // 2. Compose 树首次挂载
        viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_APPEAR)

        // 3. Compose 树卸载 (如 Tab 切离)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_DISAPPEAR)
        }
    }

    val state by viewModel.viewState.collectAsState()
    content(state, viewModel::handleIntent)
}
```

---

## ADR-021: 桌面小部件 2.0 (4x2 智能双模看板与闪电分类直达记账)

### 背景 (Context)
碎片化即时记账（如出入地铁、超市结账）如果需要解锁后翻找 App，平均耗时长达 5~10 秒，极易导致用户放弃记账。

### 决策与设计思路 (Decision & Rationale)
1. **智能双模 4x2 看板**：左侧展示结余与健康度进度条，右侧内置 4 大高频分类快捷记账按钮；
2. **DeepLink 零延迟穿透**：绑定专属 PendingIntent（`lexpense://quick_add?category={catId}&type=EXPENSE`），点击桌面小部件分类直达预填弹窗，2 秒闪电记账；
3. **零轮询节能机制**：小部件 `updatePeriodMillis="0"` 彻底禁用系统定时唤醒，仅在数据库变动时响应式推送到 `RemoteViews`。

---

## ADR-022: 生物识别应用锁、单调时钟防作弊与多任务防窥治理

### 背景 (Context)
财务应用涉及用户高敏感度的资产信息。系统切换多任务卡片时会对界面截图存底；此外，如果使用手机系统时钟来计算退后台锁定超时，恶意用户可通过将手机时钟往回调来绕过锁屏超时判定。

### 决策与设计思路 (Decision & Rationale)
构建 **三重主动防御矩阵**：
1. **单调硬件时钟防篡改**：采用 `SystemClock.elapsedRealtime()` 计算退后台时长，该时钟为 CPU 硬件时钟，不受用户调系统时间影响；
2. **多任务界面防窥 (`FLAG_SECURE`)**：动态注入 `WindowManager.LayoutParams.FLAG_SECURE`，阻止系统截取多任务快照并抵御录屏刺探；
3. **顶层遮罩阻断 (`BiometricLockOverlay`)**：未通过指纹/面容或设备密码认证前，最高 Z-Index 全屏遮罩阻断一切下层触控。

```kotlin
// core/security/AppSecurityCoordinator.kt 中的安全防作弊逻辑
class AppSecurityCoordinator(private val onShakeTriggered: () -> Unit) {
    var isAppLocked by mutableStateOf(false)
    private var backgroundTimestamp = 0L

    // 记录退后台时刻：必须使用单调时钟，抵御修改系统时钟攻击
    fun onStop() {
        backgroundTimestamp = SystemClock.elapsedRealtime()
    }

    fun onStart(activity: FragmentActivity, biometricEnabled: Boolean, timeoutSeconds: Int) {
        if (!biometricEnabled) return

        val elapsedSeconds = if (backgroundTimestamp == 0L) {
            Long.MAX_VALUE / 1000 // 首次启动，立即强制锁定
        } else {
            (SystemClock.elapsedRealtime() - backgroundTimestamp) / 1000
        }

        if (elapsedSeconds >= timeoutSeconds) {
            isAppLocked = true // 超过设定宽限期，激活锁屏遮罩
        }
    }
}
```

---

## ADR-023: 纯函数式智能财务诊断引擎与 9 大核心规则策略

### 背景 (Context)
复杂的财务诊断（如月环比暴涨、预测超支日、特定单项突增）若与 UI 或 ViewModel 混杂，极难进行回归测试与策略调优。

### 决策与设计思路 (Decision & Rationale)
1. **纯计算引擎架构**：封装纯 Kotlin 单例 `FinancialInsightEngine`，输入为不可变的原始账单列表与周期参数，输出为结构化诊断卡片实体；
2. **纯 JVM 极速测试**：脱离 Android 运行环境，支持在毫秒级内完成 100% 分支覆盖的单元测试。

---

## ADR-024: 记账表单金额展示规范与双小数位输入防溢流

### 背景 (Context)
编辑整数金额账单时若显示 `"78.00"` 违背了全局去零视觉规范（Rule 21）；且软键盘在用户输入两位小数后若无拦截，会导致输入类似 `"12.345"` 的非法金额。

### 决策与设计思路 (Decision & Rationale)
1. **对齐 Rule 21**：初始化时统一使用 `Double.formatAmount()` 剥离无意义的 `.00`；
2. **键盘按键源头拦截**：在 `NumericKeypad` 按键回调中实时校验小数点后字符位数，达到 2 位后自动阻断后续数字键输入。

---

## ADR-025: 跨 Tab 年月视图双向联动与统计下钻保持机制

### 背景 (Context)
流水与统计页面支持月/年视图联动。然而存在一个高频交互冲突：用户在统计年视图中点击某月柱状图下钻到流水月视图查看明细后，点击底部导航返回统计页时，常规同步会把统计页也改写为月视图，破坏用户的年视图浏览上下文。

### 决策与设计思路 (Decision & Rationale)
设计 **下钻保护锁状态机**：
1. 从统计年视图点击月份调用 `navigateToTransactionsMonth` 时，置位 `preserveStatisticsYearOnReturn = true`；
2. 用户从流水切回统计时，拦截同步逻辑，**保持统计页的年视图不变**，并自动重置保护锁；
3. 引入 `lastTimeTab`，防止无时间概念的 Settings Tab 污染时间同步源。

```kotlin
// 详见 core/state/ExpenseAppState.kt
private fun syncTimeState(fromTab: NavTab, toTab: NavTab) {
    if (fromTab == NavTab.TRANSACTIONS && toTab == NavTab.STATISTICS) {
        val stats = statisticsViewModel.viewState.value
        val tx = transactionsViewModel.viewState.value

        // ---- 关键难点：拦截下钻返回，守护统计页年视图 ----
        if (preserveStatisticsYearOnReturn &&
            stats.period == StatisticsPeriod.YEAR &&
            tx.period == TransactionPeriod.MONTH
        ) {
            preserveStatisticsYearOnReturn = false // 消费保护锁
            return // 终止反向覆写，保持年视图！
        }
        // ... 正常双向同步逻辑
    }
}
```

---

## ADR-026: 开发者模式 5 击隐秘唤醒与真实数据防脏守卫

### 背景 (Context)
“清除所有数据”按钮若暴露在常规设置中极易引发焦虑与误触；而“生成模拟数据”若在用户已有真实账单的月份触发，会导致测试数据与真实账目混杂且无法清理。

### 决策与设计思路 (Decision & Rationale)
1. **5 击彩蛋手势**：版本号区域连续点击 5 次激活开发者面板，激活后状态持久化；
2. **真实数据防脏守卫**：在写入模拟数据前，先执行 `dao.getTransactionCountInRange`，若当月已有真实账单，立即拦截并提示错误，严禁污染真实资产账目。

---

## ADR-027: 版本在线校验与 Google Play 更新闭环

### 背景 (Context)
应用需要具备轻量无侵入的在线版本检测能力，引导用户体验最新功能。

### 决策与设计思路 (Decision & Rationale)
1. 异步请求 GitHub Pages 静态托管的 `version.json`，比对本地与远端 `versionCode` 与 SemVer；
2. 检测到新版本呼出 `UpdateAvailableDialog` 展示更新日志（Changelog），点击“立即更新”直接拉起 Google Play 商店对应应用页。

---

## ADR-028: 底部导航双击置顶与智能归位当前周期

### 背景 (Context)
翻看多年历史账单后回到当月/当年往往需要连续回退，路径冗长。

### 决策与设计思路 (Decision & Rationale)
1. 在底部导航捕获连续点击间隔 $< 350\text{ms}$ 的双击手势；
2. **两段式归位**：未置顶时优先平滑滚动到顶部；已置顶时将月份/年份偏移量重置为 0（归位当月/当年）；
3. 采用 `SharedFlow(replay = 0, extraBufferCapacity = 1)` 传递置顶信号，彻底避免切 Tab 时误触重复滚动。

---

## ADR-029: 设置画面 Switch 触控热区全行标准化

### 背景 (Context)
部分开关仅右侧小控件可点，部分整行可点，交互手感分裂，小控件难以单手精准触控。

### 决策与设计思路 (Decision & Rationale)
全量统一迁移至 `CommonSwitchRow`：点击标题、副标题说明文字或开关自身均可平滑切换状态，全行触控热区高度对齐 Material 无障碍标准。

---

## ADR-030: 流水画面筛选按钮大拇指黄金热区化与长按快速重置

### 背景 (Context)
右上角筛选按钮在大屏单手握持时无法触达，且缺乏极速清空筛选的便捷路径。

### 决策与设计思路 (Decision & Rationale)
1. **大拇指黄金操作区**：在右下角主记账按钮旁设立次级筛选 FAB（40dp），并配备激活数量徽标；
2. **双重手势交互**：
   - **单击**：唤起多维筛选抽屉；
   - **长按（带震动）**：一键瞬间清除所有生效筛选，手势时长约 400ms 天然杜绝误触。

```kotlin
// 流水 FAB 复合手势实现模式
Surface(
    modifier = Modifier.combinedClickable(
        onClick = { onIntent(TransactionsIntent.OpenFilterSheet) },
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress) // 扎实震动确认
            onIntent(TransactionsIntent.ResetAllFilters)              // 瞬间清空
            Toast.makeText(context, "已清除全部筛选", Toast.LENGTH_SHORT).show()
        }
    )
)
```

---

## ADR-031: 账单 Excel/CSV 导出与 UTF-8 BOM 防乱码双轨分享机制

### 背景 (Context)
传统 Apache POI 库体积过大（20MB+），而普通纯文本 CSV 在 Windows Excel 中直接打开会出现严重的中文乱码。

### 决策与设计思路 (Decision & Rationale)
1. **轻量原生方案**：不引入任何第三方库，纯原生流式写入 **UTF-8 BOM (`0xEF, 0xBB, 0xBF`)** 头，全平台 Excel/WPS 即开即显，0 乱码，0 APK 体积增量；
2. **RFC 4180 规范转义**：对含有换行或逗号的备注实施双引号转义；
3. **双轨交付动线**：通过 Storage Access Framework (SAF) 保存至本地存储，或通过 `FileProvider` 调起系统分享面板直发第三方应用。

```kotlin
// 写入 UTF-8 BOM 确保 Windows Excel 识别为 UTF-8 编码
val bomHeader = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
outputStream.write(bomHeader)
```

---

## ADR-032: 本地智能通知预警与多渠道分流提醒中枢

### 背景 (Context)
1. **预算越界被动盲区**：应用内三态健康度无法在用户离开 App 时主动警示；
2. **周期账单静默履约**：后台到期自动扣款后用户不知情；
3. **新版本感知滞后**：用户不进入设置页手动检查便错失新特性与修复。

### 决策与设计思路 (Decision & Rationale)
构建统一的本地通知与提醒中枢体系：
1. **渠道分流矩阵 (Channel Matrix)**：
   - `channel_budget_alerts` (HIGH)：总预算与分类预算 80% 警戒与 100% 超支强提醒；
   - `channel_recurring_bills` (DEFAULT)：周期账单履约到账温和提醒；
   - `channel_app_updates` (DEFAULT/LOW)：新版本发布轻提醒。
2. **去重防打扰状态机 (Anti-Spam Dedup)**：
   - 基于 `yyyy_MM:targetId:LEVEL` 维度持久化已提醒记录，同月同级仅提醒 1 次，支持 80% $\to$ 100% 单向升级。
3. **DeepLink 深度穿透**：点击通知精准呼出 `CategoryBudgetModalDialog`、流水详情或版本更新弹窗。
4. **Android 13+ 运行时权限**：规范检测 `POST_NOTIFICATIONS` 权限，未授权时优雅提示并支持跳转系统通知设置。

---

## ADR-033: 全功能组件与屏幕 @Preview 可视化覆盖架构规范

### 背景 (Context)
在 Compose 开发中，若所有 UI 组件强绑定 ViewModel 或真实数据库实例，开发者每次微调 UI 都必须在真机/模拟器上全量编译安装运行（耗时 1~2 分钟），严重降低 UI 迭代效率。

### 决策与设计思路 (Decision & Rationale)
推行 **无状态 Screen + 独立 Preview** 架构规范：
1. **状态与渲染解耦**：Screen Composable 仅依赖纯数据 State 和 Intent 回调 Lambda，不直接引用任何 ViewModel；
2. **专属 Preview 文件分离**：对于复杂页面，设立独立的 `XxxScreenPreview.kt`，准备逼真的 Mock 数据与预览外壳，严格符合单文件 $\le 250$ 行规范；
3. **深浅色全维度覆盖**：每个核心组件统一生成 Light 与 Dark 两套 `@Preview`，在 Android Studio 检查器中实现即写即看，0 编译等待。

```kotlin
// 典型预览分离架构模式
@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TransactionsScreenPreview() {
    ListenTheme {
        TransactionsContent(
            state = TransactionsViewState(
                transactions = sampleMockTransactions,
                totalExpense = 3580.0,
                totalIncome = 12000.0
            ),
            onIntent = {} // 预览模式下传空 Lambda，完全解耦真实业务
        )
    }
}
```

---

## ADR-034: 设置中心高紧凑布局重塑与智能通知单开关整合

### 背景 (Context)
在应用功能持续迭代（数据中心、安全防窥、APM 运维、通知中心）的过程中，设置主页的内容密度与纵向高度不断膨胀。尤其在通知模块引入后：
1. **Header 与 Master Switch 分离**：导致卡片头部占用一行，总开关独立占用一行，未启用时浪费过多垂直屏幕空间；
2. **预算通知按钮冗余**：原本将“100% 超支”与“80% 警戒线”拆为两个微调开关，且文案均提及 80%，导致用户心智困惑，占据大量屏幕空间；
3. **卡片内外间距偏大**：导致整个设置列表在常规屏幕上需要频繁大幅滚动。

### 决策与设计思路 (Decision & Rationale)
1. **Master Switch 融入 Header**：
   将全局通知开关直接内嵌在卡片 Header 顶栏右侧，与左侧铃铛 Icon、标题及单行描述并列为同一行。在关闭通知时，整张卡片仅高约 44dp，彻底消除无用留白。
2. **预算预警双开关合二为一 (2-in-1 Unified Switch)**：
   消除独立的 80% 二级微调开关，将其与超支警报合并为单个 **「预算预警与超支提醒」** 开关。底层状态机（`BudgetAlertGuard`）在用户开启后自动在 80% 触发警戒提醒、100% 触发超支警报，完全自动化闭环。
3. **极简三项展开体系**：
   通知卡片展开后仅展示 3 个业务开关（预算预警、周期账单自动入账、新版本发布更新），高度缩减 60% 以上。
4. **全卡片间距统一收紧**：
   设置页 LazyColumn 卡片间距统一收紧至 `6.dp` (`AppDimens.SpaceMedium`)，所有卡片内部内衬统一收拢为 `8.dp` (`AppDimens.SpaceStandard`)，文案采用单行精简描述。

