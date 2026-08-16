# ListenExpenseTracker - 演进路线图与任务追踪 (Roadmap & Todo)

本文档记录 `ListenExpenseTracker` 的功能迭代路线、目标态与待沉淀规范。

---

## 阶段一：核心应用与库架构落地 (Completed Stage)

- [x] **前期功能设计与 UI 画面规划**
- [x] **PROMPTS.md 针对 Android 原生架构的改版**
- [x] **创建根目录 README.md 与模块依赖说明**
- [x] **创建 ListenArch 独立 SDK 项目并配置 Composite Build**
- [x] **创建 ListenUiComponent 独立 SDK 项目并配置 Composite Build**
- [x] **沉淀项目开发指南 (docs/project_development_guide.md)**
- [x] **维护 Git 提交与版本控制记录**

---

## 阶段二：底层 SDK 核心与通用组件实装 (P0 Task 1 & Task 2 - Completed)

- [x] **Room 数据库实装 (ListenArch)**
  - [x] `TransactionEntity` 账单数据表定义
  - [x] `TransactionDao` Flow 响应式 SQL 增删改查与日期区间查询
  - [x] `AppDatabase` Room 单例数据库
- [x] **DataStore 偏好设置实装 (ListenArch)**
  - [x] `DataStoreManager` 语言 (zh/en/ja)、主题模式 (Light/Dark/System)、AccentColor 持久化 Flow
- [x] **MVI 架构基类与错误收敛实装 (ListenArch)**
  - [x] `BaseViewModel<ViewState, UserIntent, ViewEffect>` 核心基类
  - [x] `Either<Failure, T>` 单向函数式异常处理模型
  - [x] `LocaleManager` 多语言调度器
- [x] **通用 UI 组件与设计系统实装 (ListenUiComponent)**
  - [x] `AccentColor` & `Color.kt` 6+ 动态 Accent 调色盘 Token
  - [x] `ListenTheme` Material 3 主题包装器
  - [x] `CustomKeypad` 自定义算术数字键盘组件
  - [x] `CategoryDonutChart` Canvas 环形占比图

---

## 阶段三：主 App 记账核心功能与页面实装 (P0 Task 3 - Next)

- [ ] **记账与流水明细页面功能组装 (ListenExpenseTracker)**
  - [ ] 接入 `ListenArch` 数据库与 `ListenUiComponent` 控件
  - [ ] 明细列表按日期分组与 Swipe-to-Delete 滑动删除
- [ ] **多维统计图表与排行榜**
  - [ ] Canvas 环形占比图交互高亮与收支对比柱状图
  - [ ] 统计长图渲染与系统分享 Intent 调起

---

## 阶段四：可观测性与云端同步 (P1 Stage)

- [ ] **APM 日志浮窗与调试面板 (ListenArch / Debug Module)**
  - [ ] 浮动调试小窗组件
  - [ ] App/DB/Sync/Crash 分频道日志过滤与导出
- [ ] **TraceId 链路追踪系统**
  - [ ] ViewModel Intent 到 Room/Drive API 请求全链路耗时打点
- [ ] **Google Drive 云端同步实装**
  - [ ] Google Credential Manager 账号授权集成
  - [ ] Google Drive REST API (`appDataFolder`) 数据库序列化备份与增量恢复

---

## 📚 文档矩阵路线图

- [x] `docs/project_development_guide.md`：项目开发与架构设计指南
- [x] `docs/todo.md`：演进路线图与任务追踪
- [ ] `docs/apm_performance_monitoring_design.md`：APM 性能监控与日志浮窗设计
- [ ] `docs/repository_caching_strategy.md`：数据源缓存与云端同步降级规范
- [ ] `docs/error_codes_reference.md`：错误码与 Failure 统一收敛规范
