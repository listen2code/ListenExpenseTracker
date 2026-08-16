# ListenExpenseTracker - 演进路线图与任务追踪 (Roadmap & Todo)

本文档记录 `ListenExpenseTracker` 的功能迭代路线、目标态与待沉淀规范。

---

## 阶段一：核心应用与库架构落地 (Current Stage)

- [x] **前期功能设计与 UI 画面规划**
- [x] **PROMPTS.md 针对 Android 原生架构的改版**
- [x] **创建根目录 README.md 与模块依赖说明**
- [x] **创建 ListenArch 独立 SDK 项目并配置 Composite Build**
- [x] **创建 ListenUiComponent 独立 SDK 项目并配置 Composite Build**
- [x] **沉淀项目开发指南 (docs/project_development_guide.md)**
- [x] **维护 Git 提交与版本控制记录**

---

## 阶段二：应用核心功能实装与数据持久化 (P0 Stage)

- [ ] **Room 数据库实装 (ListenArch)**
  - [ ] `TransactionEntity` 与 `CategoryEntity` 表定义
  - [ ] `TransactionDao` 增删改查与日期区间 Flow 查询
  - [ ] `DatabaseMigrator` 数据库升级与迁移策略
- [ ] **DataStore 偏好设置实装 (ListenArch)**
  - [ ] 语言 (zh/en/ja)、主题模式 (Light/Dark/System)、AccentColor 持久化
- [ ] **记账与流水明细页面功能补齐 (ListenExpenseTracker)**
  - [ ] 实装自定义计算器键盘逻辑与高精度计算
  - [ ] 明细列表按日期分组与 Swipe-to-Delete 滑动删除
- [ ] **多维统计图表与排行榜 (ListenUiComponent)**
  - [ ] Canvas 环形占比图交互高亮
  - [ ] 收支对比柱状图与趋势折线图
  - [ ] 统计长图渲染与系统分享 Intent 调起

---

## 阶段三：可观测性与云端同步 (P1 Stage)

- [ ] **APM 日志浮窗与调试面板 (ListenArch / Debug Module)**
  - [ ] 浮动调试小窗组件
  - [ ] App/DB/Sync/Crash 分频道日志过滤与导出
- [ ] **TraceId 链路追踪系统**
  - [ ] ViewModel Intent 到 Room/Drive API 请求全链路耗时打点
- [ ] **Google Drive 云端同步实装**
  - [ ] Google Credential Manager 账号授权集成
  - [ ] Google Drive REST API (`appDataFolder`) 数据库序列化备份与增量恢复
  - [ ] 同步冲突检测机制

---

## 阶段四：数据安全与高级扩展功能 (P2 Stage)

- [ ] **FIDO2 / 指纹生物识别安全锁**
  - [ ] 启动 App 与查看隐私结余时的生物识别解锁
- [ ] **桌面挂件 (Launcher Widgets)**
  - [ ] 快捷记账挂件与本月收支总览挂件
- [ ] **数据导入与导出**
  - [ ] CSV / Excel 账单数据导入导出与解析器
- [ ] **应用内版本更新流 (In-App Update)**
  - [ ] 基于 GitHub Release 的版本检查与 Changelog 展示

---

## 📚 文档矩阵路线图

- [x] `docs/project_development_guide.md`：项目开发与架构设计指南
- [x] `docs/todo.md`：演进路线图与任务追踪
- [ ] `docs/apm_performance_monitoring_design.md`：APM 性能监控与日志浮窗设计
- [ ] `docs/repository_caching_strategy.md`：数据源缓存与云端同步降级规范
- [ ] `docs/error_codes_reference.md`：错误码与 Failure 统一收敛规范
- [ ] `docs/custom_lint_rules.md`：Custom Lint 规则与质量基线说明
