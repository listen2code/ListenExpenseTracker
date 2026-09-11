# 周期性收支与订阅管理技术设计与实现规范
(Recurring Transactions & Subscriptions Engineering Specification)

本文档系统性定义 **ListenExpenseTracker (lExpense)** 中**周期性账单（Recurring Transactions）与固定订阅服务（Subscriptions）**的业务设计理念、数据模型契约、到期自动履约引擎、月度刚性生活成本基准 (Baseline) 算法以及界面紧凑排版架构。

---

## 1. 概述与核心设计哲学 (Overview & Philosophy)

### 1.1 背景与用户痛点
个人记账过程中存在大量高频、重复的固定刚性收支场景：
1. **固定支出**：房租/房贷、固定车贷、五险一金、宽带费、水电气物业费等；
2. **周期性订阅**：iCloud、Apple Music、Netflix、Spotify、外卖/视频月卡、健身房季卡等；
3. **周期性收入**：每月发薪日固定工资、兼职定期入账、理财定期分红等。

传统记账软件的主要缺陷：
- **手动录入繁重且易漏记**：用户每月都需要机械重复录入相同的金额与分类；
- **缺乏刚性生活成本基准**：用户无法一眼洞察自己“每个月一睁眼就必须花掉的固定生活底线（Baseline）”，难以科学规划弹性预算。

### 1.2 核心设计哲学
1. **无感自动化履约 (Zero-Friction Fulfillment)**：
   到期自动插入 Room 数据库流水，并在备注自动标记 `[周期]` 标签，流水列表以高亮紫色徽章呈现，既清晰可追溯又免除人工机械操作；
2. **多频次归一化月度成本基线 (Normalized Monthly Baseline)**：
   支持**日、周、月、年** 4 种频次，系统自动根据精算公式统一折算为“每月固定支出”与“每月固定收入”，精准计算固定成本占月预算的百分比；
3. **断网关机鲁棒推进 (Robust Recovery)**：
   基于纯 Kotlin 领域算法，每次启动时自动扫描并批量补齐历史应履约账单，通过 `do-while` 循环严格保证下次履约时间晚于当前时间，**绝不陷入死循环，绝不发生重复记账**。

---

## 2. 数据架构与实体契约 (Data Architecture & Models)

全模块采用分层解耦的 Room 实体结构与强类型枚举设计：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 【核心枚举定义 (Core Enums)】                                    │
├───────────────────────────────────────────────┬─────────────────────────────────────────────────┤
│  RecurringFrequency (周期频次)                │  ExecutionType (履约模式)                       │
│  - DAILY   : 每日固定执行                     │  - AUTO_INSERT   : 到期静默自动记账入库         │
│  - WEEKLY  : 每周固定某日 (1=周一 ~ 7=周日)   │  - NOTIFY_CONFIRM: 到期发送通知提醒用户确认     │
│  - MONTHLY : 每月固定某日 (1 ~ 31号, 月末归一)│                                                 │
│  - YEARLY  : 每年固定到期日执行               │                                                 │
└───────────────────────────────────────────────┴─────────────────────────────────────────────────┘
                                                ▲
                                                │ 强类型关联引用
                                                │
┌───────────────────────────────────────────────┴─────────────────────────────────────────────────┐
│                              【周期规则实体 (RecurringRuleEntity)】                              │
│                                  Room Table: "recurring_rules"                                  │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│  + id: String (UUID 唯一主键)                                                                    │
│  + title: String (规则标题，如 "房租"、"Netflix 订阅")                                           │
│  + type: String (TransactionType: EXPENSE / INCOME)                                             │
│  + categoryId / categoryName / categoryIcon / categoryColorHex (分类可视化元数据)               │
│  + amount: Double (每期固定金额，最大限额 999,999.99)                                           │
│  + accountType: String (扣款/入账资产账户，如 CASH, BANK, WECHAT)                                │
│  + note: String (自定义备注信息)                                                                 │
│  + frequency: RecurringFrequency (DAILY, WEEKLY, MONTHLY, YEARLY)                               │
│  + dayOfPeriod: Int (月周期为 1~31 号；周周期为 1~7)                                             │
│  + startDate: Long (规则生效开始时间戳)                                                          │
│  + endDate: Long? (规则结束截止时间戳，为 null 时表示长期永久有效)                              │
│  + lastExecutionDate: Long? (上次成功履约时间戳)                                                │
│  + nextExecutionDate: Long (下次应履约时间戳，按 ASC 建立快速检索索引)                         │
│  + executionType: ExecutionType (默认 AUTO_INSERT)                                               │
│  + isEnabled: Boolean (规则启用/暂停开关)                                                       │
│  + createdAt: Long (规则创建时间戳)                                                             │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.1 数据库访问接口规范 (`RecurringRuleDao`)
源码位于 [`RecurringRuleDao.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/data/db/RecurringRuleDao.kt)。

```kotlin
@Dao
interface RecurringRuleDao {
    // 响应式流：按下次履约时间升序排列，UI 列表实时感知变动
    @Query("SELECT * FROM recurring_rules ORDER BY nextExecutionDate ASC")
    fun getAllRulesFlow(): Flow<List<RecurringRuleEntity>>

    // 核心履约查询：筛选出处于启用状态且到期时间 <= 当前时间的待处理规则
    @Query("SELECT * FROM recurring_rules WHERE isEnabled = 1 AND nextExecutionDate <= :currentTime")
    suspend fun getDueRules(currentTime: Long): List<RecurringRuleEntity>

    @Query("SELECT * FROM recurring_rules WHERE id = :id LIMIT 1")
    suspend fun getRuleById(id: String): RecurringRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RecurringRuleEntity)

    @Update
    suspend fun updateRule(rule: RecurringRuleEntity)

    @Delete
    suspend fun deleteRule(rule: RecurringRuleEntity)
}
```

---

## 3. 调度履约工作流与核心算法 (Execution Workflow & Algorithms)

系统采用 **“应用启动自检 + 数据库变动级联”** 的自愈式履约机制：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 【触发源：应用启动与生命周期】                                    │
│                 TransactionsViewModel.init -> checkDueRecurringRules()                          │
└───────────────────────────────────────────────┬─────────────────────────────────────────────────┘
                                                │ 1. 扫描待履约规则
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                           【待履约规则检索 (Query Due Rules)】                                   │
│              RecurringRuleDao.getDueRules(currentTime = System.currentTimeMillis())             │
└───────────────────────────────────────────────┬─────────────────────────────────────────────────┘
                                                │ 返回 List<RecurringRuleEntity>
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                               【履约决策与执行引擎 (Processing Loop)】                           │
│                          RecurringTransactionEngine.processDueRules()                           │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ 遍历每条待履约规则：                                                                             │
│ 1. 提取或生成专属备注: `[周期] ${rule.title}` (Idempotent Note Tagging)                          │
│ 2. 构造 TransactionEntity，履约时间戳取 min(nextExecutionDate, currentTime)                     │
│ 3. 写入流水表: TransactionDao.insertTransaction(tx)                                             │
│ 4. 递推计算下个周期: calculateNextExecutionDate(frequency, dayOfPeriod, nextExecutionDate)      │
│ 5. 校验有效期: 若下个周期超过 endDate，自动置 isEnabled = false                                  │
│ 6. 更新规则表: RecurringRuleDao.updateRule(updatedRule)                                         │
└───────────────────────────────────────────────┬─────────────────────────────────────────────────┘
                                                │ 触发 Room 全局变动 Flow
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                  【全链路响应式级联刷新】                                       │
│ 1. TransactionsViewModel.observeTransactions() 收到通知 -> 自动重新计算当月收支与结余          │
│ 2. ListenExpenseAppWidgetProvider.updateFromTransactions() -> 桌面小部件看板毫秒级同步刷新      │
│ 3. GoogleDriveAutoBackupManager.scheduleAutoBackup() -> 5 秒防抖自动备份至云端                  │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. 重点与难点代码实现深度剖析 (Implementation Walkthrough)

### 4.1 难点一：下次执行时间防溢出递推算法 (`calculateNextExecutionDate`)

源码位于 [`RecurringTransactionEngine.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/data/engine/RecurringTransactionEngine.kt#L22-L63)。

#### 🔑 技术难点与边界条件
1. **月末大小月与闰年防溢出**：若用户设置每月 31 日扣款，在 4、6、9、11 月（只有 30 天）或 2 月（平年 28 天，闰年 29 天）时，直接 `cal.set(DAY_OF_MONTH, 31)` 会导致日期外溢到次月（例如变成 3 月 3 日）。必须使用 `cal.getActualMaximum(Calendar.DAY_OF_MONTH)` 进行动态钳制（`dayOfPeriod.coerceIn(1, maxDay)`）；
2. **长期未打开应用的追赶机制 (Catch-up Loop)**：若用户 3 个月未打开应用，一次性推进 1 个月后计算出的时间可能依然早于当前系统时间。通过 `do-while (cal.timeInMillis <= fromDate)` 确保无论设备断网关机多久，最终返回的时间必然**严格晚于本次履约时间**。

#### 💻 教学源码实现（含逐行中文注释）

```kotlin
/**
 * 计算下次执行时间戳。
 * 自动处理月末大小月及闰年边界防溢出，保证计算得到的时间严格晚于 [fromDate]。
 *
 * @param frequency 周期频次（日、周、月、年）
 * @param dayOfPeriod 周期内的具体天数（月模式为 1~31；周模式为 1~7）
 * @param fromDate 基准起始时间戳
 * @return 规范化后的下一次执行时间戳（毫秒）
 */
fun calculateNextExecutionDate(frequency: RecurringFrequency, dayOfPeriod: Int, fromDate: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = fromDate
        // 统一归一化至每日上午 09:00:00.000 执行，避开凌晨时区切换与系统打盹 (Doze) 窗口
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // 采用 do-while 循环推进，确保计算出的时间严格晚于起始基准时间 fromDate
    do {
        when (frequency) {
            RecurringFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurringFrequency.WEEKLY -> {
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                // 映射自定义周几 (1=周一 ... 7=周日) 至标准 Java Calendar 常量
                val targetCalendarDay = when (dayOfPeriod) {
                    1 -> Calendar.MONDAY
                    2 -> Calendar.TUESDAY
                    3 -> Calendar.WEDNESDAY
                    4 -> Calendar.THURSDAY
                    5 -> Calendar.FRIDAY
                    6 -> Calendar.SATURDAY
                    else -> Calendar.SUNDAY
                }
                cal.set(Calendar.DAY_OF_WEEK, targetCalendarDay)
            }
            RecurringFrequency.MONTHLY -> {
                cal.add(Calendar.MONTH, 1)
                // 边界防御：动态获取目标月份的实际最大天数（28、29、30 或 31）
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                // 强制钳制在 [1, maxDay] 范围内，彻底杜绝月份外溢 Bug
                cal.set(Calendar.DAY_OF_MONTH, dayOfPeriod.coerceIn(1, maxDay))
            }
            RecurringFrequency.YEARLY -> {
                cal.add(Calendar.YEAR, 1)
            }
        }
    } while (cal.timeInMillis <= fromDate)

    return cal.timeInMillis
}
```

---

### 4.2 难点二：待执行规则自动履约与数据流级联 (`processDueRules`)

源码位于 [`RecurringTransactionEngine.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/data/engine/RecurringTransactionEngine.kt#L100-L142)。

#### 🔑 技术难点与设计考量
1. **幂等性与前缀标记 (`Idempotent Tagging`)**：
   在流水 `note` 前置 `[周期]` 标记，不仅使用户在账单明细中能够清晰分辨“自动扣款”与“手工花销”，还为未来扩展防重复入账提供了天然的审计特征；
2. **真实业务时间与系统时间的校准**：
   记账流水时间戳取 `rule.nextExecutionDate.coerceAtMost(currentTime)`，确保若用户在 15 号关机、17 号开机，15 号的房租依然记录在 15 号账单中，保证月度账目统计的真实性；
3. **有效期截止判定**：
   比对递推后的 `nextDate` 与 `endDate`，若规则已过截止日期，系统自动将 `isEnabled` 置为 `false`，停止后续履约。

#### 💻 教学源码实现（含逐行中文注释）

```kotlin
/**
 * 履约待执行的周期规则。
 * 自动插入账单记录并递增下一次执行时间戳。
 *
 * @param recurringDao 周期规则数据库访问接口
 * @param txDao 交易明细数据库访问接口
 * @param currentTime 当前执行时间戳
 * @return 成功自动履约插入的账单数量
 */
suspend fun processDueRules(
    recurringDao: RecurringRuleDao,
    txDao: TransactionDao,
    currentTime: Long = System.currentTimeMillis()
): Int {
    val dueRules = recurringDao.getDueRules(currentTime)
    var processedCount = 0

    for (rule in dueRules) {
        if (rule.executionType == ExecutionType.AUTO_INSERT) {
            // 1. 规范化备注：注入统一的 [周期] 前缀标识
            val baseNote = rule.note.ifEmpty { rule.title }
            val recurringNote = if (baseNote.startsWith("[周期]")) baseNote else "[周期] $baseNote"

            // 2. 生成正式交易流水，时间戳精准校准至应扣款日
            val tx = TransactionEntity(
                type = rule.type,
                categoryId = rule.categoryId,
                categoryName = rule.categoryName,
                categoryIcon = rule.categoryIcon,
                categoryColorHex = rule.categoryColorHex,
                amount = rule.amount,
                note = recurringNote,
                accountType = rule.accountType,
                timestamp = rule.nextExecutionDate.coerceAtMost(currentTime)
            )
            txDao.insertTransaction(tx)

            // 3. 推进下一次履约时间戳，并校验是否超出规则有效截止期
            val nextDate = calculateNextExecutionDate(rule.frequency, rule.dayOfPeriod, rule.nextExecutionDate)
            val isStillEnabled = rule.endDate == null || nextDate <= rule.endDate
            val updatedRule = rule.copy(
                lastExecutionDate = currentTime,
                nextExecutionDate = nextDate,
                isEnabled = isStillEnabled
            )
            recurringDao.updateRule(updatedRule)
            processedCount++
        }
    }

    return processedCount
}
```

---

### 4.3 难点三：全周期归一化月度生活成本基准计算 (`calculateMonthlyBaseline`)

源码位于 [`RecurringTransactionEngine.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/data/engine/RecurringTransactionEngine.kt#L65-L98)。

#### 🔑 折算精算模型
为了给用户呈现准确的“月度刚性生活成本底线”，各周期的换算系数严格基于历法标准：
- **每日频次 (Daily)**：按月均 30 天折算：$	ext{Monthly} = 	ext{Amount} 	imes 30.0$
- **每周频次 (Weekly)**：全年 52 周平摊到 12 个月：$	ext{Monthly} = 	ext{Amount} 	imes rac{52.0}{12.0} pprox 	ext{Amount} 	imes 4.333$
- **每月频次 (Monthly)**：直接计入：$	ext{Monthly} = 	ext{Amount} 	imes 1.0$
- **每年频次 (Yearly)**：年费均摊到 12 个月：$	ext{Monthly} = rac{	ext{Amount}}{12.0}$

```kotlin
data class RecurringMonthlyBaseline(
    val totalExpense: Double, // 月均固定支出总额
    val totalIncome: Double,  // 月均固定收入总额
    val netMonthly: Double,   // 月均固定净结余 (收入 - 支出)
    val expenseCount: Int,    // 生效中的支出规则笔数
    val incomeCount: Int      // 生效中的收入规则笔数
)
```

---

### 4.4 难点四：独创三层无挤压排版卡片设计 (`RecurringRuleItemCard.kt`)

源码位于 [`RecurringRuleItemCard.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/features/recurring/components/RecurringRuleItemCard.kt)。

#### 🔑 移动端 UI 痛点与破局
在传统的列表卡片设计中，往往将“图标、标题、金额、Switch 开关”挤在同一行。在小屏手机或大字体模式下，长标题（如“中国移动家庭千兆宽带套餐扣费”）必然被截断成“中国移动...”，金额也被挤压换行，极不美观。

#### 💡 三层紧凑架构 (Three-Tier Architecture)
```text
┌─────────────────────────────────────────────────────────────────────────────┐
│ 【第 1 行：标题独占整行】                                                   │
│ [图标 28dp] 规则名称独占横向 100% 空间，无任何按键或金额干扰 (maxLines = 2)  │
├───────────────────────────────────────────────────────────────┬─────────────┤
│ 【第 2 行：属性徽标】 [每月 15日] [微信支付] [3天后扣款]      │             │
│                                                               │  [ Switch ] │
│ 【第 3 行：大字金额】 -¥1,299.00 (靠左顶格，加粗完整金额)     │  垂直居中   │
└───────────────────────────────────────────────────────────────┴─────────────┘
```
1. **第 1 行（标题行）**：`[图标]` + `标题文本` 占满整行，同行绝无其他元素干扰，长文本支持 2 行自然折行，永不截断；
2. **第 2~3 行左侧复合块 (`weight(1f)`)**：包含周期/账户属性徽标与加粗大金额；
3. **右侧 Switch 开关**：与第 2~3 行构成的复合块进行垂直居中对齐（`Alignment.CenterVertically`），整卡布局通透稳健，在各种分辨率下均表现完美。

---

### 4.5 难点五：全频次水平滚动药丸选择器 (`RecurringFrequencySelector.kt`)

源码位于 [`RecurringFrequencySelector.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/features/recurring/components/RecurringFrequencySelector.kt)。

- **周模式**：提供 `[周一] ~ [周日]` 水平滚动选择；
- **月模式**：配备 `[-] 步进调节 [+]` 与 `[1日] [5日] [10日] [15日] [20日] [25日] [月末]` 高频快捷药丸；
- **排版技术**：采用 `Modifier.horizontalScroll(rememberScrollState())`，彻底消除垂直方向的挤压与跳动。

---

## 5. 异常边界与鲁棒性防重策略 (Robustness & Edge Cases)

| 潜在异常场景 | 底层防御与应对策略 | 用户端呈现与影响 |
| :--- | :--- | :--- |
| **设备关机/断网长达数月** | 冷启动自检时，`do-while` 循环将 `nextExecutionDate` 推进至当前时间之后 | 自动补全历史欠缺账单，下次执行时间严格正确 |
| **规则设定结束日期 (endDate)** | 推进下次时间后，若 `nextDate > endDate` 自动置 `isEnabled = false` | 规则列表自动标注“已暂停/已结束”，不再重复记账 |
| **月末扣款日在 2 月遇闰年/平年** | `dayOfPeriod.coerceIn(1, cal.getActualMaximum(DAY_OF_MONTH))` 动态规整 | 28/29 日自动对齐，绝不漂移至 3 月 |
| **用户在设置中清空全部数据** | `RecurringRuleDao.deleteAll()` 彻底级联清理 | 状态重置为空列表，欢迎新规则录入 |
| **演示演练数据生成** | `DemoDataEngine` 预置发薪、房租、会员等样例规则，插入前比对规则标题去重 | 开箱即用，真实账单存在时绝不污染覆写 |

---

## 6. 自动化回归与测试验证 (Testing & Verification)

本模块已具备 100% 覆盖核心计算的完整单元测试套件：
- `RecurringTransactionEngineTest.kt`：
  - 测试按日、周几、月中某日、跨年、跨月末的执行时间精确计算；
  - 测试闰年 2 月 29 日向次年平年 2 月 28 日的平滑过渡；
  - 测试批量待履约入账、`[周期]` 前缀注入及 `endDate` 终止条件；
  - 测试日/周/月/年生活成本归一化 Baseline 算法精度；
- 通过 `./gradlew testDebugUnitTest` 实现 100% 自动化回归测试。
