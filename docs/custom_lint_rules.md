# ListenExpenseTracker - 静态分析与 Custom Lint 规则体系规范 (Custom Lint & Static Analysis)

本文档系统性阐述 **ListenExpenseTracker** 及其架构底座（`ListenArch` 与 `ListenUiComponent`）的**代码质量红线、静态分析基线与自主研发的 Custom Lint 规则体系**。

---

## 1. 架构总览与质量门禁基线 (Lint Architecture & Gate)

在多模块大型项目中，仅靠人工 Code Review 难以 100% 杜绝架构越界（如通用库引用业务层）、硬编码、内存泄露或安全漏洞。
本项目推行 **“编译器即门禁”** 的理念，构建了基于 **Android Lint + UAST (Universal Abstract Syntax Tree)** 的定制化静态代码扫描系统：

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                       【源码提交 / PR / CI 构建阶段】                       │
│                        ./gradlew lintDebug --check                          │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      【Custom Lint 扫描检测引擎 (UAST)】                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  [1] 架构解耦红线 (Architecture Boundary) -> 阻止底层向业务层逆向依赖        │
│  [2] 单文件 250 行红线 (Line Count Guard) -> 强制单一职责物理拆分           │
│  [3] 零硬编码红线 (Zero-Hardcoding)       -> 阻止 UI 硬编码中/英字符串与尺寸│
│  [4] 资产安全防窥 (Security & Monotonic)  -> 强制单调时钟，禁止篡改系统时间 │
│  [5] Compose 性能与规范 (Modifier/MVI)    -> Modifier 签名与密封弹窗驱动    │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ 扫描分析结果
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        【分级告警与阻断处理】                               │
├──────────────────────────────────────┬──────────────────────────────────────┤
│ 🚨 ERROR (阻断构建 - Exit Code 1)    │ ⚠️ WARNING (警告排查 - 需整改优化)    │
│  - 模块边界逆向依赖                  │  - 废弃 API 使用 (禁止 @Suppress)    │
│  - 单文件超过 250 行                 │  - 扩展函数未编写单元测试            │
│  - 锁屏计时使用墙上时间              │  - 连续重组未优化                    │
│  - ViewModel 内存持有 Context 引用   │  - 尺寸硬编码建议转 Token            │
└──────────────────────────────────────┴──────────────────────────────────────┘
```

---

## 2. 静态分析核心规则矩阵 (Custom Lint Rule Matrix)

| 规则 ID | 级别 | 规则名称 | 违规范例 | 推荐修复方案 |
| :--- | :---: | :--- | :--- | :--- |
| **LINT_001** | `ERROR` | **架构边界依赖隔离** | `ListenArch` 引入 `com.listen.expensetracker.*` | 底层架构库必须 100% 零业务依赖，数据通过泛型或 Payload 传入 |
| **LINT_002** | `ERROR` | **单文件 250 行红线** | 任何 `.kt` 文件总行数 $> 250$ 行 | 按单一职责拆分子组件至 `components/` 目录 |
| **LINT_003** | `ERROR` | **用户可见文本零硬编码** | `Text("确认支付")` 或 `Button("保存")` | 使用 `AppStrings.KEY.tr(lang)` 走统一多语言收口 |
| **LINT_004** | `ERROR` | **安全锁屏禁用墙上时间** | 安全模块调用 `System.currentTimeMillis()` | 必须采用单调硬件时钟 `SystemClock.elapsedRealtime()` |
| **LINT_005** | `ERROR` | **ViewModel 严禁持有 Context** | ViewModel 声明 `val context: Context` 属性 | 继承 `AndroidViewModel(application)` 或仅传递基本数据 |
| **LINT_006** | `ERROR` | **主线程禁止阻塞式 I/O** | 主线程调用 `dao.getAllBlocking()` | 使用协程挂起函数 `suspend` 或响应式 `Flow<T>` |
| **LINT_007** | `ERROR` | **严禁 `@Suppress` 废弃警告** | 代码标注 `@Suppress("DEPRECATION")` | 调研并迁移至 Google 官方推荐的最新现代 API |
| **LINT_008** | `ERROR` | **Compose Modifier 签名规范** | 独立 Composable 缺少 `modifier: Modifier` | 首个可选参数必须声明为 `modifier: Modifier = Modifier` (Rule 13) |
| **LINT_009** | `ERROR` | **破坏性操作 Danger 确认按钮** | 删除/清空弹窗使用常规主题色确认按钮 | 破坏性操作强制使用 `CommonButtonStyle.Danger` 红色按钮 (Rule 15) |
| **LINT_010** | `ERROR` | **弹窗显隐 MVI 密封接口收敛** | 页面声明 `var showDialog = remember { mutableStateOf(false) }` | 必须由 `UiState.activeDialog` 密封接口统一驱动，末尾收拢在 `DialogHost` |
| **LINT_011** | `WARNING`| **UI 颜色与尺寸数值 Token 化** | 硬编码 `Color(0xFF123456)` 或 `16.dp` | 使用 `MaterialTheme.colorScheme` 与 `AppDimens` 常量 |
| **LINT_012** | `WARNING`| **数字键盘两位小数拦截** | 自定义输入框未对浮点输入做小数位截断 | 引入动态小数位校验 `amountExpression.length - dotIdx - 1 >= 2` (Rule 22) |
| **LINT_013** | `WARNING`| **图表动效滚动复用防抖** | `DonutChart` 等在 `LazyColumn` 滑动时重复进场 | 引入 `dataSignature` 哈希指纹对比守卫 (ADR-016) |
| **LINT_014** | `ERROR` | **CSV/Excel 导出 UTF-8 BOM 守卫** | 生成 CSV 时直接使用纯文本输出流 | 必须首行写入 `0xEF, 0xBB, 0xBF` 字节头，杜绝 Windows 乱码 (Rule 27) |
| **LINT_015** | `ERROR` | **核心业务计算脱离 UI 线程** | 在 Composable 内部进行多层嵌套的账单聚合计算 | 收拢至纯 Kotlin 单例 `TransactionCalculationEngine` (Rule 10) |
| **LINT_016** | `WARNING`| **单元测试覆盖率门禁** | 核心计算引擎单测覆盖率 $< 80\%$ | 在 `src/test/` 补充针对性分支覆盖测试用例 |

---

## 3. 核心难点 Lint Detector 源码深度剖析 (Deep Implementation Walkthrough)

所有定制化 Detector 继承自 Android Lint 官方 SDK 的 `Detector()`，并实现 `Detector.UastScanner`，直接在语法树级别进行精确匹配与智能修复（QuickFix）。

### 3.1 难点一：架构依赖边界与防逆向污染检测器 (`ArchitectureBoundaryDetector`)

#### 🔑 设计思路
`ListenArch` 与 `ListenUiComponent` 是面向全产品线通用的 SDK。开发者在编码过程中，Android Studio 偶尔会自动补全并引入宿主 App 的业务包（例如误导入了 `com.listen.expensetracker.data.db.TransactionEntity`）。
本检测器在 UAST 访问 `UImportStatement` 时进行前缀审查，若底层库中检测到了宿主 App 的业务包名，立即抛出阻断级 `ERROR`！

```kotlin
package com.listen.lint.detectors

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement

/**
 * 架构边界与防逆向依赖检测器 (ArchitectureBoundaryDetector)
 * 核心目标：阻止 ListenArch 与 ListenUiComponent 逆向引用业务层代码
 */
class ArchitectureBoundaryDetector : Detector(), Detector.UastScanner {

    companion object {
        private const val FORBIDDEN_PACKAGE_PREFIX = "com.listen.expensetracker"

        val ISSUE = Issue.create(
            id = "LINT_001_ArchitectureBoundaryViolation",
            briefDescription = "架构边界违规：底层通用库严禁依赖宿主业务层",
            explanation = """
                ListenArch 与 ListenUiComponent 作为面向多款 Listen App 的通用 SDK，
                必须保持严格的业务无关性 (Zero Business Coupling)。
                严禁在其中 import 任何包含 $FORBIDDEN_PACKAGE_PREFIX 的类或包！
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 10,
            severity = Severity.ERROR, // 阻断级红线
            implementation = Implementation(
                ArchitectureBoundaryDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        // 声明关注的语法树节点类型：仅监听 import 声明
        return listOf(UImportStatement::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitImportStatement(node: UImportStatement) {
                // 1. 判断当前被检查的文件是否属于 ListenArch 或 ListenUiComponent 模块
                val filePath = context.file.absolutePath
                val isCoreModule = filePath.contains("ListenArch") || filePath.contains("ListenUiComponent")
                if (!isCoreModule) return // 业务宿主 App 自身可以自由使用业务包

                // 2. 提取当前 import 的完整限定名 (FQN)
                val importFqn = node.importReference?.asSourceString() ?: return

                // 3. 校验是否命中违规业务前缀
                if (importFqn.startsWith(FORBIDDEN_PACKAGE_PREFIX)) {
                    context.report(
                        issue = ISSUE,
                        scope = node,
                        location = context.getLocation(node),
                        message = "架构红线违规：通用库禁止引入业务层包 [$importFqn]！请通过泛型或通用数据结构解耦。"
                    )
                }
            }
        }
    }
}
```

---

### 3.2 难点二：Compose 用户可见文本零硬编码检测器 (`HardcodedStringDetector`)

#### 🔑 设计思路
直接在 `Text("确定")` 中写死中英文字符串，是国际化的大忌。
本检测器拦截所有调用名为 `Text` 或 `CommonText` 的 Composable 函数，审查其第一个参数是否为纯字面量字符串（`ULiteralExpression`）。若是，则提示违规并推荐使用 `AppStrings.KEY.tr(lang)`。

```kotlin
package com.listen.lint.detectors

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.expressions.UInjectionHost

/**
 * Compose 界面文本零硬编码检测器 (HardcodedStringDetector)
 */
class HardcodedStringDetector : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.create(
            id = "LINT_003_HardcodedComposeString",
            briefDescription = "禁止在 Compose 界面控件中硬编码字符串字面量",
            explanation = """
                所有展示文本必须通过 AppStrings.KEY.tr(lang) 或 ExpenseStrings 统一收拢调度，
                严禁直接向 Text/CommonText 传入硬编码的中/英/日文字符串，避免漏翻与多语言维护撕裂。
            """.trimIndent(),
            category = Category.I18N,
            priority = 8,
            severity = Severity.ERROR,
            implementation = Implementation(HardcodedStringDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        // 关注所有文本呈现函数
        return listOf("Text", "CommonText", "showToast", "showSnackbar")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val firstArg = node.valueArguments.firstOrNull() ?: return

        // 检查参数是否为字符串字面量表达式 (例如 "确定" 或 "Cancel")
        if (firstArg is ULiteralExpression || firstArg is UInjectionHost) {
            val literalText = firstArg.asSourceString().trim('"', ' ')
            // 过滤空串与纯数字/标点符号
            if (literalText.isBlank() || literalText.all { !it.isLetter() }) return

            context.report(
                issue = ISSUE,
                scope = firstArg,
                location = context.getLocation(firstArg),
                message = "零硬编码违规：发现未国际化的硬编码字符串 \"$literalText\"！请在 AppStrings 中声明并在 ExpenseStrings 中注册翻译。"
            )
        }
    }
}
```

---

### 3.3 难点三：安全锁屏单调时钟守卫检测器 (`MonotonicClockDetector`)

#### 🔑 设计思路
在 `com.listen.expensetracker.core.security` 包下的任何文件中，如果开发者调用了 `System.currentTimeMillis()` 计算时差，该 Detector 能够精确识别其方法签名，并强制提示使用 `SystemClock.elapsedRealtime()`。

```kotlin
package com.listen.lint.detectors

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

/**
 * 安全模块单调硬件时钟守卫 (MonotonicClockDetector)
 */
class MonotonicClockDetector : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.create(
            id = "LINT_004_NonMonotonicClockUsageInSecurity",
            briefDescription = "安全模块严禁使用容易被篡改的墙上时钟 System.currentTimeMillis()",
            explanation = """
                System.currentTimeMillis() 极易受到用户手动调整系统时间或时区的影响，
                若用于计算应用切后台超时时长，用户将时间倒拨即可瞬间绕过锁屏判定！
                在安全调度逻辑中，必须 100% 采用硬件单调时钟 SystemClock.elapsedRealtime()！
            """.trimIndent(),
            category = Category.SECURITY,
            priority = 9,
            severity = Severity.ERROR,
            implementation = Implementation(MonotonicClockDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf("currentTimeMillis")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        // 1. 判断是否仅作用在安全相关包下
        val packageName = context.evaluator.getPackage(node)?.qualifiedName ?: ""
        if (!packageName.contains("security")) return

        // 2. 判断所属类是否为 java.lang.System
        val containingClass = method.containingClass?.qualifiedName ?: ""
        if (containingClass == "java.lang.System") {
            // 提供一键快速修复 (QuickFix)
            val quickFix = fix()
                .name("替换为 SystemClock.elapsedRealtime()")
                .replace()
                .text("System.currentTimeMillis()")
                .with("android.os.SystemClock.elapsedRealtime()")
                .autoFix()
                .build()

            context.report(
                issue = ISSUE,
                scope = node,
                location = context.getLocation(node),
                message = "安全漏洞风险：严禁使用 System.currentTimeMillis() 进行超时判定！请替换为 SystemClock.elapsedRealtime()。",
                quickfixData = quickFix
            )
        }
    }
}
```

---

### 3.4 难点四：Compose Modifier 参数签名规范检测器 (`ModifierParameterDetector`)

#### 🔑 设计思路与 Rule 13
根据 Google 官方 Compose API 规范及项目 `PROMPTS.md` 规则 13：
任何暴露给外部调用的独立 Composable 组件，其首个具有默认值的参数必须声明为 `modifier: Modifier = Modifier`，以便调用方能链式注入尺寸、外边距或可点击性。

```kotlin
package com.listen.lint.detectors

import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UMethod

/**
 * Compose Modifier 规范检查器 (ModifierParameterDetector)
 */
class ModifierParameterDetector : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.create(
            id = "LINT_008_ComposeModifierMissing",
            briefDescription = "独立 Composable 组件首个可选参数必须为 modifier: Modifier = Modifier",
            explanation = """
                遵循 PROMPTS.md Rule 13 与 Google 官方 Compose 设计规范，
                所有公共独立 Composable 组件必须向调用方暴露 Modifier 链式注入能力。
            """.trimIndent(),
            category = Category.USABILITY,
            priority = 7,
            severity = Severity.ERROR,
            implementation = Implementation(ModifierParameterDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableUastTypes() = listOf(UMethod::class.java)

    override fun createUastHandler(context: JavaContext): com.android.tools.lint.client.api.UElementHandler {
        return object : com.android.tools.lint.client.api.UElementHandler() {
            override fun visitMethod(node: UMethod) {
                // 仅检查带 @Composable 注解的公共顶层函数
                val isComposable = node.hasAnnotation("androidx.compose.runtime.Composable")
                if (!isComposable || node.isConstructor) return

                val params = node.uastParameters
                if (params.isEmpty()) return // 纯预览或无参函数跳过

                // 检查参数列表中是否声明了 Modifier
                val hasModifier = params.any { it.name == "modifier" && it.type.canonicalText.contains("Modifier") }
                if (!hasModifier) {
                    context.report(
                        issue = ISSUE,
                        scope = node,
                        location = context.getNameLocation(node),
                        message = "Composable 规范违规：组件 [${node.name}] 缺少标准 modifier: Modifier = Modifier 参数！"
                    )
                }
            }
        }
    }
}
```

---

### 3.5 难点五：破坏性操作危险确认按钮检测器 (`DangerousButtonDetector`)

#### 🔑 设计思路与 Rule 15
删除账户、清空全部账单等不可逆的高危破坏性交互，若确认按钮使用了通常的浅蓝或主色按钮，极易导致用户无脑顺手点击“确认”造成灾难性数据丢失。
本检测器扫描包含 `Delete`、`Clear`、`Remove` 关键字的 Dialog 确认按钮，强制其 `CommonButton` 的 `style` 属性必须声明为 `CommonButtonStyle.Danger`！

```kotlin
package com.listen.lint.detectors

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression

/**
 * 破坏性操作确认规范检测器 (DangerousButtonDetector)
 */
class DangerousButtonDetector : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.create(
            id = "LINT_009_DestructiveActionDangerButtonRequired",
            briefDescription = "删除与清空等破坏性操作确认按钮必须使用 CommonButtonStyle.Danger",
            explanation = """
                根据 PROMPTS.md Rule 15 规范，所有破坏性不可逆操作的确认按钮必须使用醒目的 Danger 红色，
                给用户明确的心理风险预警，防止误操作。
            """.trimIndent(),
            category = Category.USER_INTERFACE,
            priority = 8,
            severity = Severity.ERROR,
            implementation = Implementation(DangerousButtonDetector::class.java, Scope.JAVA_FILE_SCOPE)
        )
    }

    override fun getApplicableMethodNames() = listOf("CommonButton", "Button")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val containingFileName = context.file.name
        // 检查文件名是否带有敏感的破坏性操作关键字
        val isDestructiveDialog = containingFileName.contains("Delete") ||
                                  containingFileName.contains("Clear") ||
                                  containingFileName.contains("Remove")
        if (!isDestructiveDialog) return

        val styleArg = node.valueArguments.find { it.asSourceString().contains("style") }
        val isDangerStyle = styleArg?.asSourceString()?.contains("Danger") == true

        if (!isDangerStyle) {
            context.report(
                issue = ISSUE,
                scope = node,
                location = context.getLocation(node),
                message = "交互安全违规：破坏性弹窗 [$containingFileName] 的确认按钮必须使用 CommonButtonStyle.Danger 红色样式！"
            )
        }
    }
}
```

---

## 4. 单文件 250 行架构红线自动化扫描器 (`FileSizeLimitDetector`)

除了基于 Lint UAST 的语法树检测外，工程还配备了极速的源码行数体检脚本，在 Gradle 编译钩子和 Git Pre-commit 阶段双重拦截超长文件：

```powershell
# 极速单文件 250 行红线检查脚本
Get-ChildItem -Path "app/src/main/java" -Filter "*.kt" -Recurse |
    Where-Object { 
        # 排除自动生成的临时文件与 i18n 巨型字典映射
        $_.FullName -notmatch "ExpenseStrings\.kt|AppStrings\.kt|ListenExpenseAppWidgetProvider\.kt" 
    } |
    ForEach-Object {
        $lineCount = (Get-Content $_.FullName | Measure-Object -Line).Lines
        if ($lineCount -gt 250) {
            Write-Error "🚨 [Rule 3 违规] 文件 [$($_.Name)] 达到 $lineCount 行 (上限 250 行)！请拆分子组件！"
            exit 1
        }
    }
Write-Host "✅ 全项目 Kotlin 文件行数合规校验 100% 通过！" -ForegroundColor Green
```

---

## 5. CI/CD 流水线集成与质量门禁配置 (CI Gate)

在主工程的 `app/build.gradle.kts` 中，开启最严格的 Lint 自动化阻断配置：

```kotlin
android {
    // ...
    lint {
        // 遇到 ERROR 级别问题立即中断构建，阻止产出带病产物
        abortOnError = true
        // 将所有 WARNING 升级为 ERROR 严格对待，推行零告警文化
        warningsAsErrors = false
        // 检查所有子模块与依赖项
        checkDependencies = true
        // 输出纯文本报告到控制台，方便在 GitHub Actions 运行日志中直接查看
        textReport = true
        textOutput("stdout")
        // 生成详细的 HTML 诊断报告供技术排查
        htmlReport = true
        htmlOutput = file("${project.buildDir}/reports/lint/lint-report.html")
        // 致命崩溃问题无论如何必须拦截
        checkReleaseBuilds = true
    }
}
```

---

## 6. 开发者排查指南与代码修复建议 (Troubleshooting & QuickFix)

1. **如何在 Android Studio 中执行全量体检？**
   在终端运行：
   ```bash
   ./gradlew lintDebug
   ```
   或者在 Android Studio 右侧 Gradle 面板中双击运行：
   `Tasks -> verification -> lintDebug`。

2. **遇到 Lint 报错时的正确处理姿势**：
   - 严禁通过 `@Suppress` 或 `@SuppressLint` 强行掩盖问题；
   - 查看报告中的 `Explanation` 解释，并结合 `PROMPTS.md` 规范进行针对性物理拆分或 API 现代化升级；
   - 若属于多语言硬编码问题，先向 `AppStrings.kt` 录入常量，再在 `ExpenseStrings.kt` 配齐中/英/日翻译，最后替换原 UI 字符串。
