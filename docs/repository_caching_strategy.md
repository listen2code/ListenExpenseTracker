# ListenExpenseTracker - Repository 缓存与数据流控规范

本文档定义 `ListenExpenseTracker` 与基础架构 `ListenArch` 的 **Local-First 本地优先数据流架构、二级缓存机制与云端同步降级策略**。

---

## 1. Local-First 本地优先核心原则

1. **零延迟响应**：用户的任何记账、修改或删除操作必须立即可见，UI 层通过 Room Flow / DataStore 响应式驱动，绝不在主线程或 UI 渲染前等待网络同步响应。
2. **全离线可用**：App 必须在无任何网络连接或弱网环境下保证 100% 功能完好，所有数据持久化保存在本地 SQLite 数据库中。
3. **安全同步降级**：云端同步（如 Google Drive `appDataFolder` 备份）作为后台次级任务运行，网络失败时静默重试或提示用户，绝不阻塞本地业务流程。

---

## 2. 数据层架构分层

```mermaid
graph TD
    UI[Compose UI / ViewModel] --> Flow[Reactive Flow Query]
    Flow --> Room[Room SQLite Database (Single Source of Truth)]
    Room --> Disk[(Local Storage / App Sandbox)]
    
    SyncEngine[Background Sync Engine] -.-> Room
    SyncEngine -. Local-First Sync .-> Cloud[(Google Drive REST API)]
```

---

## 3. 缓存与数据流控策略

### 3.1 单一事实来源 (Single Source of Truth)
- 本地 Room 数据库是整个 App 的 **唯一数据事实来源**。
- ViewModel 只观察 DAO 暴露的响应式 `Flow<List<TransactionEntity>>`。
- 新增账单通过 `dao.insertTransaction(...)` 写入数据库，Room 自动派发最新的数据列表触发 UI 重绘。

### 3.2 离线测试与数据运维
- **一键填充测试数据 (`seedDemoData`)**：支持快速向本地数据库注入涵盖多类目、多账户的典型记账明细。
- **一键清空重置 (`clearAllData`)**：支持彻底清空数据库以便进行干净的功能回归与测试。
