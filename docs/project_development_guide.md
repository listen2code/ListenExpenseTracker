# ListenExpenseTracker - 项目开发与架构设计指南

本指南汇集了 Listen 系列软件工程哲学、架构规范与质量标准，作为 `ListenExpenseTracker`、`ListenArch` 与 `ListenUiComponent` 未来的编码与演进指导方针。

---

## 1. 软件工程哲学 (Software Philosophy)

1. **真实可维护优先**：优先保证 App 在无网、弱网、高频记账场景下的最高稳定性与可维护性。
2. **可观测性高于炫技**：优先打磨 TraceId 链路跟踪、崩溃保护 (Crash Safe Mode)、日志监控与离线缓存降级机制。
3. **“实事求是”的文档规则**：
   - `README.md` **只描述已落地落地的能力**（或明确标注的目标态）。
   - 所有的设计 Spec、架构规范与未实现的方案**统一下沉至 `docs/` 目录**。
4. **零告警工程质量**：代码需通过 Custom Lint 规则检查，保持 `No issues found!` 质量基线。

---

## 2. 软件架构设计 (MVI + Clean Architecture)

系统基于 Android Native (Kotlin 2.x + Jetpack Compose) 的 **MVI (Model-View-Intent)** 单向数据流架构：

```mermaid
graph LR
    UI[View / Compose Screen] -- 1. 发送 User Intent --> VM[ViewModel]
    VM -- 2. 更新 ViewState Flow --> UI
    VM -- 3. 发送一次性 ViewEffect SharedFlow --> UI
    VM -- 4. 调用 UseCase / Repository --> Data[Data Layer (Room / Google Drive)]
    Data -- 5. 返回 Either<Failure, T> --> VM
```

### 2.1 表现层规范 (Presentation Layer)
- **ViewModel 规范**：继承自 `ListenArch` 提供的 `BaseViewModel<State, Intent>`。单次副作用 (Effect) 已在基类层面被统一固化为泛型无关的 `CommonUiEffect`。
- **状态收口**：全局 UI 状态必须通过不可变的 `StateFlow<State>` 驱动，页面重绘完全依赖 ViewModel 的 `ViewState` 状态快照。
- **单次事件 (Side Effects)**：页面导航、Toast 提示、弹窗触发等单次动作必须通过基类方法 `emitEffect(CommonUiEffect)` 发送，防止配置变更（如屏幕旋转）导致的重复触发。
- **生命周期感知**：通过重写 `toLifecycleIntent(event: LifecycleEvent)` 接入生命周期回调，无需在 UI 层写样板式的生命周期观察代码。
- **状态分离与容器**：复杂页面需引入 `StateHolder` 模式（例如 `SettingsStateHolder`）管理框架级事件契约与 UI 动画/滚动状态，严禁将此类状态与业务逻辑混杂在 ViewModel 中。
- **组件粒度控制**：Compose View 层代码需保持短小精悍，单个文件行数必须控制在 **200 ~ 250 行** 以内。复杂或可复用的子 UI 模块必须按单一职责抽取至组件文件（如 `features/**/components/`）。

### 2.2 数据与网络层规范 (Data & Network Layer)
- **错误收敛**：所有数据源操作（Room 读写、Google Drive REST API 请求）统一使用 Kotlin 官方原生的 `Result<T>`（结合 `safeCall {}` 或 `Flow.asResult()`）封装，严禁在 UI 层直接抛出未捕获的 Exception。
- **二级缓存与降级策略**：
  1. 优先读取本地 Room SQLite / DataStore 数据库。
  2. 离线/无网络时使用本地快照兜底。
  3. 联网且用户授权时触发 Google Drive REST API 增量同步（操作与配置详见 [Google 登录与 Drive 同步全指南](google_auth_and_drive_sync_guide.md)）。

---

## 3. 可观测性与调试系统 (APM & Observability)

为了打造极致稳定的工具应用，系统内置可观测性体系：

### 3.1 TraceId 全链路追踪 (Tracing System)
每个记账 Action / Intent 自动生成唯一的 `traceId`（如 `trace-10293847`），贯穿 ViewModel 调度、Room 读写及 Google Drive API 请求全过程，方便日志排查。

### 3.2 APM 日志浮窗 (Log Overlay Inspector)
APP 内置可唤起的调试面板，包含 4 大频道：
- **APP Log**：业务逻辑与 ViewModel 意图日志。
- **Sync Log**：Google Drive REST API 请求与响应日志。
- **DB Log**：Room 数据库读写耗时与 SQL 跟踪。
- **Crash Log**：捕获的未处理异常信息。

### 3.3 崩溃安全保护 (Crash Safe Mode)
当 APP 发生极度罕见的未捕获崩溃时：
1. 捕获系统全局 Exception 堆栈。
2. 优先保存至本地 `crash_logs.txt` 文件。
3. 调起 Crash Safe Mode 急救界面，允许用户清理异常缓存或导出日志，防止 App 进入无休止崩溃循环。

---

## 4. 国际化与主题规范 (i18n & Design System)

1. **多语言规范**：
   - 界面所有展示字符串必须通过 `ListenArch` 提供的 `LocaleManager` 统一调度。
   - 严禁在 Compose 控件中硬编码原始中文/英文/日文字符串。
2. **主题与配色规范**：
   - 必须使用 `MaterialTheme.colorScheme` 或 `ListenUiComponent` 提供的 `AccentColor` Token。
   - 严禁在 UI 控件中硬编码 Color 值（如 `Color(0xFF123456)`）。

---

## 5. 编码质量与 Lint 红线

- ❌ **禁止 ViewModel 持有 View 或 Context 引用**。
- ❌ **禁止在 UI 线程执行阻塞的 I/O 或数据库读写**。
- ❌ **禁止忽略 Gradle 编译 Warning**。
- ✅ **所有扩展函数必须编写单元测试**。
