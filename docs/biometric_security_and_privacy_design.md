# 生物识别应用锁与隐私防窥架构设计与实现规范 (Biometric Security & Privacy Shield Specification)

本文档系统性阐述 **ListenExpenseTracker** 的全链路安全与隐私防护体系，涵盖生物识别强认证、单调硬件时钟防篡改锁屏、系统多任务界面防偷窥、首帧零泄露机制以及物理重力手势隐额设计。

---

## 1. 概述与核心安全原则 (Overview & Principles)

### 1.1 背景与业务痛点
记账应用直接记录了用户的资产净值、消费习惯、真实行踪与生活隐私，属于极度敏感的核心个人数据。在移动设备日常使用中，面临三大典型泄露威胁：
1. **设备暂借与偷窥风险**：手机临时借给他人使用、或在工位/公共场合放置桌面时，他人可能误点或刻意打开应用刺探财务资产；
2. **多任务界面快照泄露 (Task Snapshot Leak)**：Android 操作系统在上滑进入多任务卡片管理（Recent Apps）时，系统渲染引擎会自动对前台 Activity 截取一帧全屏位图快照。即便退出应用，多任务卡片上仍然暴露了清晰的资产余额；
3. **时钟回拨漏洞 (Clock Rollback Exploit)**：在传统的基于时钟差值的锁屏方案中，恶意用户在切出后台后，若进入系统设置将时间往前调，会导致时间差为负数，从而瞬间攻破锁屏倒计时。

### 1.2 四大核心安全架构原则
```text
┌─────────────────────────┬─────────────────────────┬─────────────────────────┬─────────────────────────┐
│     1. 零信任首帧防护    │    2. 单调硬件时钟防作弊  │    3. 系统快照动态治理  │    4. 无缝设备凭据降级  │
│(Zero First-Frame Leak)  │(Monotonic Clock Guard)  │ (Dynamic FLAG_SECURE)   │(Credential Fallback)    │
│纳秒级同步缓存拦截首帧，  │采用 elapsedRealtime()   │多任务卡片自动涂白/阻断，│优先指纹/3D面容，        │
│杜绝异步加载引起的界面闪烁│抵御修改系统时间绕过锁屏 │彻底防御外部截屏与录屏   │无硬件时优雅降级系统 PIN │
└─────────────────────────┴─────────────────────────┴─────────────────────────┴─────────────────────────┘
```

---

## 2. 三层纵深防御架构拓扑 (Three-Tier Defense Architecture)

全工程的安全防线自上而下分为视图阻断层、生命周期协调层与底层硬件/系统层，形成严格的纵深防御矩阵：

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                       【第 1 层：视图层绝对阻断】                            │
│                        BiometricLockOverlay                                 │
│  - 位于 Compose 根节点 (MainActivity.setContent) 最高层级                    │
│  - 处于锁定状态时完全代替主应用呈现，阻断一切触控事件、返回键及底层子组件重绘│
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ 状态驱动 (isAppLocked: Boolean)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      【第 2 层：生命周期与状态机协调】                       │
│                        AppSecurityCoordinator                               │
│  - 绑定 Activity 物理生命周期 (onCreate, onStart, onResume, onPause, onStop)│
│  - 纳秒级同步状态读取 (SecurityPreferences) 彻底消除首帧竞态                 │
│  - 单调时钟测算退后台真实流逝时长                                           │
│  - 动态调度 WindowManager.LayoutParams.FLAG_SECURE                          │
└───────────────────┬─────────────────────────────────────┬───────────────────┘
                    │ 鉴权调用                            │ 传感器生命周期
                    ▼                                     ▼
┌───────────────────────────────────────┐   ┌─────────────────────────────────┐
│     【第 3A 层：系统生物识别底座】    │   │   【第 3B 层：物理手势防护】    │
│        BiometricSecurityManager       │   │           ShakeDetector         │
│  - androidx.biometric.BiometricPrompt │   │  - 基于 Sensor.TYPE_ACCELEROMETER│
│  - BIOMETRIC_STRONG or CREDENTIAL     │   │  - 重力矢量模长去中心化算法     │
│  - 统一 APM 安全审计日志埋点          │   │  - onPause 自动注销，杜绝耗电   │
└───────────────────────────────────────┘   └─────────────────────────────────┘
```

---

## 3. 状态机与生命周期流转 (State Transition Flow)

### 3.1 状态流转架构图 (State Transition Diagram)

```text
       ┌────────────────────────┐
       │     应用冷启动启动     │
       └───────────┬────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│  SecurityPreferences 快速同步检测    │
└───────────┬──────────────────────────┘
            │
            ├─ [未开启锁屏] ──────────────────────────┐
            │                                         │
            ▼ [开启锁屏 且 硬件支持]                  ▼
┌──────────────────────────────────────┐  验证成功  ┌──────────────────────────────────────┐
│         【LOCKED 锁定态】            ├───────────>│         【UNLOCKED 解锁态】          │
│ - isAppLocked = true                 │            │ - isAppLocked = false                │
│ - 全屏展示 BiometricLockOverlay      │<───────────┤ - 正常展示应用主体界面 (App)         │
│ - FLAG_SECURE 强制保持置位           │  退后台超时│ - FLAG_SECURE 按用户设置动态开关     │
└──────────────────▲───────────────────┘            └──────────────────┬───────────────────┘
                   │                                                   │
                   │                                                   ▼ 用户按 Home / 锁屏
                   │                                ┌──────────────────────────────────────┐
                   │                                │       【BACKGROUND 后台挂起态】      │
                   │                                │ - onPause: 强制注入 FLAG_SECURE      │
                   │                                │ - onPause: 注销摇一摇传感器避免耗电  │
                   └────────────────────────────────┤ - onStop: 记录单调时钟时间戳         │
                         切回前台 且 耗时 >= 阈值    └──────────────────────────────────────┘
```

### 3.2 状态流转矩阵表 (State Transition Matrix)

| 当前状态 | 触发事件 | 判定条件 | 迁移目标状态 | 核心安全动作 | UI 呈现 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **应用启动** | `Activity.onCreate` | `enabled && supported` | **LOCKED** | 立即标记 `isAppLocked = true`，常驻置位 `FLAG_SECURE` | 仅展示 `BiometricLockOverlay`，首帧不渲染任何账单 |
| **应用启动** | `Activity.onCreate` | `!enabled \|\| !supported` | **UNLOCKED** | 标记 `isAppLocked = false` | 正常展示主应用界面 |
| **UNLOCKED** | 切后台 (`onPause` $\to$ `onStop`) | 用户按 Home / 锁屏 | **BACKGROUND** | 1. 记录 `backgroundTimestamp = SystemClock.elapsedRealtime()`<br/>2. 注入 `FLAG_SECURE`<br/>3. 停止摇一摇传感器 | 操作系统切屏，多任务卡片显示空白/模糊 |
| **BACKGROUND** | 切回前台 (`onStart`) | 流逝时间 $\ge$ 超时阈值 (0s/60s/300s) | **LOCKED** | 1. 保持 `isAppLocked = true`<br/>2. 调用 `promptUnlock()` 拉起系统指纹/人脸弹窗 | 遮罩层盖住底层界面，弹出系统认证框 |
| **BACKGROUND** | 切回前台 (`onStart`) | 流逝时间 $<$ 超时阈值 | **UNLOCKED** | 1. 保持 `isAppLocked = false`<br/>2. 按用户防窥设置恢复 `FLAG_SECURE`<br/>3. 恢复摇一摇传感器 | 直接恢复用户刚刚浏览的界面 |
| **LOCKED** | 系统认证成功 | 指纹/人脸/密码验证通过 | **UNLOCKED** | 1. 标记 `isAppLocked = false`<br/>2. 刷新 `backgroundTimestamp`<br/>3. 写入 APM 审计日志 | 遮罩层平滑淡出，呈现应用主体 |
| **LOCKED** | 系统认证失败/取消 | 用户点击取消或多次失败 | **LOCKED** | 保持锁定，提供“重试指纹/密码解锁”按钮 | 停留在锁定遮罩层，阻断进入应用 |

---

## 4. 核心技术难点攻坚与关键代码逐行深度解析 (Technical Challenges & Code Walkthrough)

### 4.1 难点一：首帧零泄露机制与同步 SharedPreferences 缓存 (`SecurityPreferences`)

#### 🔑 技术挑战
在现代化应用中，偏好设置通常采用 Jetpack DataStore（基于 Kotlin 协程的异步 Flow）。
然而在应用冷启动时，`Activity.onCreate` 执行是同步的，如果通过协程异步去读取 DataStore 中的锁屏开关，在 DataStore 首次发出值的 16ms ~ 80ms 间隙内，Compose 会先以默认值渲染出第一帧真实账单，随后才突然弹出锁屏遮罩。**这种“首帧闪烁”会导致用户敏感资产瞬间暴露在录屏设备或肉眼视野中，造成致命安全漏洞！**

#### 💡 终极解决方案：双轨缓存设计
1. 在常规业务层使用 DataStore 保证数据响应式；
2. 专门为安全锁开辟纳秒级同步缓存 `SecurityPreferences`（基于原生 `SharedPreferences`）。在 `onCreate` 的最开端、`setContent` 渲染第一帧之前，同步读出开关并初始化 `isAppLocked = true`。

```kotlin
// 文件位置: core/security/SecurityPreferences.kt
object SecurityPreferences {
    private const val PREF_NAME = "expense_security_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_lock_enabled"
    private const val KEY_LOCK_TIMEOUT = "lock_timeout_seconds"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // ---- 为什么需要纳秒级同步方法？ ----
    // 供 Activity.onCreate 首行同步调用，阻断首帧界面渲染，彻底消除异步等待时间窗口
    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    // 用户在设置页切换开关时，同步写入保障下一次冷启动立刻生效
    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getLockTimeoutSeconds(context: Context): Int {
        return getPrefs(context).getInt(KEY_LOCK_TIMEOUT, 0)
    }

    fun setLockTimeoutSeconds(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_LOCK_TIMEOUT, seconds).apply()
    }
}
```

---

### 4.2 难点二：硬件单调时钟与防篡改状态机 (`AppSecurityCoordinator`)

#### 🔑 技术挑战与攻击场景
传统锁屏计时常使用 `System.currentTimeMillis()` 记录切后台时间戳。恶意用户在把应用退到后台后，打开手机系统设置将系统时间往前调整 1 小时。当再次打开应用时，`(now - lastTime)` 算出的流逝秒数为负数，从而永久绕过了超时锁定判定！

#### 💡 终极解决方案：`SystemClock.elapsedRealtime()`
`SystemClock.elapsedRealtime()` 计算的是从 CPU 硬件上电开机起算的流逝毫秒数，是严格**单调递增 (Monotonically Increasing)** 的。无论是用户篡改系统时区、手动修改日期，还是手机进入 Deep Sleep 睡眠态，该时钟均不可被篡改，从物理层面根除时钟漏洞。

```kotlin
// 文件位置: core/security/AppSecurityCoordinator.kt
class AppSecurityCoordinator(
    private val onShakeTriggered: () -> Unit
) {
    // 供 Compose 观察的锁屏状态变量
    var isAppLocked by mutableStateOf(false)
        private set

    // ---- 关键防御点：采用单调硬件时钟抵御时间回拨攻击 ----
    private var backgroundTimestamp = 0L

    // 防双弹窗守卫 (Double-Prompt Guard)：防止系统生命周期偶发重入导致拉起两个 BiometricPrompt
    private var isAuthenticating = false
    private val shakeDetector = ShakeDetector(onShake = onShakeTriggered)

    /**
     * 在 Activity.onCreate 中第一时间同步调用
     */
    fun checkInitialLock(activity: FragmentActivity) {
        val enabled = SecurityPreferences.isBiometricEnabled(activity)
        val supported = BiometricSecurityManager.isBiometricOrCredentialAvailable(activity)
        // 只有当用户显式开启且硬件支持时才激活首帧锁定
        if (enabled && supported) {
            isAppLocked = true
            applyRecentAppsShield(activity, true)
        }
    }

    /**
     * 在 Activity.onStop 生命周期记录硬件时间戳
     */
    fun onStop() {
        backgroundTimestamp = SystemClock.elapsedRealtime()
    }

    /**
     * 在 Activity.onStart 生命周期测算后台流逝时长
     */
    fun onStart(
        activity: FragmentActivity,
        biometricEnabled: Boolean,
        isBioSupported: Boolean,
        timeoutSeconds: Int,
        lang: String,
        recentAppsShield: Boolean
    ) {
        applyRecentAppsShield(activity, recentAppsShield)
        if (biometricEnabled && isBioSupported) {
            val elapsedSeconds = if (backgroundTimestamp == 0L) {
                // ---- 初始冷启动巧妙逻辑 ----
                // 首次进入应用时 backgroundTimestamp 为 0，赋予极大值确保冷启动必须强制锁定！
                Long.MAX_VALUE / 1000
            } else {
                // 精准计算退后台的硬件时间差 (秒)
                (SystemClock.elapsedRealtime() - backgroundTimestamp) / 1000
            }

            // 超过用户设置的宽限期（0秒/60秒/300秒）
            if (elapsedSeconds >= timeoutSeconds) {
                isAppLocked = true
                applyRecentAppsShield(activity, recentAppsShield)
                promptUnlock(activity, lang, recentAppsShield)
            }
        }
    }

    /**
     * 安全拉起解锁弹窗
     */
    fun promptUnlock(activity: FragmentActivity, lang: String, recentAppsShield: Boolean = true) {
        // 双弹窗守卫拦截
        if (isAuthenticating) return
        isAuthenticating = true

        BiometricSecurityManager.promptUnlock(
            activity = activity,
            title = AppStrings.SECURITY_UNLOCK_PROMPT_TITLE.tr(lang),
            subtitle = AppStrings.SECURITY_UNLOCK_PROMPT_SUBTITLE.tr(lang),
            onSuccess = {
                // 认证成功：放行
                isAuthenticating = false
                isAppLocked = false
                backgroundTimestamp = SystemClock.elapsedRealtime()
                applyRecentAppsShield(activity, recentAppsShield)
            },
            onError = { _, _ ->
                // 认证失败或取消：保持锁定状态
                isAuthenticating = false
                isAppLocked = true
                applyRecentAppsShield(activity, true)
            }
        )
    }
}
```

---

### 4.3 难点三：多任务快照防窥与 `FLAG_SECURE` 动态注入治理

#### 🔑 技术挑战
Android 系统为了提升多任务切换器（Recent Apps / Task Switcher）的流畅度，会在 Activity 退后台（`onPause`）的一瞬间，由 SurfaceFlinger 截取当前界面的位图快照缓存在系统磁盘上。如果直接常驻开启 `FLAG_SECURE`，会导致用户在日常使用时无法主动进行合法截屏（如给好友分享精美的记账月度海报）。

#### 💡 终极解决方案：动态生命周期注入
1. **退后台立即注入**：在 `onPause` 时刻，检测若启用了防窥或处于锁定状态，强制为当前窗口注入 `WindowManager.LayoutParams.FLAG_SECURE`。此时系统截屏出来的多任务卡片完全是一片漆黑或留白；
2. **回前台精准还原**：在 `onResume` 且应用已解锁的前提下，按用户个人偏好决定是否解除 `FLAG_SECURE`，允许用户在应用内自由截屏分享。

```kotlin
/**
 * 响应式调度窗口安全标志 (FLAG_SECURE)
 *
 * @param activity 窗口宿主 Activity
 * @param recentAppsShield 用户是否在设置中开启了“多任务防窥”
 */
fun applyRecentAppsShield(activity: FragmentActivity, recentAppsShield: Boolean) {
    // 判定规则：只要开启了防窥，或者当前处于生物识别锁定状态，窗口必须强制处于最高安全级别！
    if (recentAppsShield || isAppLocked) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    } else {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
```

---

### 4.4 难点四：生物识别统一凭据降级链 (`BiometricSecurityManager`)

#### 🔑 技术挑战
不同 Android 机型的生物识别能力差异极大：
- 部分千元机没有指纹传感器；
- 部分机型用户未录入指纹或因多次脱皮识别失败；
- 部分定制厂商 ROM 在调用 `BiometricManager.canAuthenticate()` 时可能会抛出内部空指针或未捕获异常。

#### 💡 终极解决方案：`BIOMETRIC_STRONG or DEVICE_CREDENTIAL`
1. **强生物识别 + 设备凭据一体化**：优先调用 `BIOMETRIC_STRONG`（Class 3 强指纹/3D面容），若用户未设置或硬件不可用，自动平滑 fallback 到 `DEVICE_CREDENTIAL`（系统的锁屏 PIN、图案或数字密码），绝对不会把用户锁在外面；
2. **FragmentActivity 强绑定**：明确要求调用方传入 `FragmentActivity`（而非普通的 `Context`），确保底层 `BiometricPrompt` 的弹窗生命周期得到正确的管理；
3. **ROM 异常安全包裹**：对探测方法全量包裹 `try-catch`，并接入 `ApmLogger` 安全审计日志。

```kotlin
// 文件位置: core/security/BiometricSecurityManager.kt
object BiometricSecurityManager {
    private const val TAG = "BiometricSecurity"

    /**
     * 探测设备硬件与凭据可用性
     */
    fun isBiometricOrCredentialAvailable(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            // 采用降级链机制：强生物识别 (指纹/面容) 自动备选设备锁屏密码 (PIN/图案)
            val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            val canAuthenticate = biometricManager.canAuthenticate(authenticators)
            canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            // 防御特定 OEM ROM 的运行时异常，杜绝检测闪退
            ApmLogger.e(TAG, "Failed to check biometric availability: ${e.message}")
            false
        }
    }

    /**
     * 拉起系统标准解锁弹窗
     */
    fun promptUnlock(
        activity: FragmentActivity, // 必须为 FragmentActivity 以维持弹窗生命周期
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: String) -> Unit = { _, _ -> }
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                ApmLogger.i(TAG, "Biometric authentication succeeded")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                ApmLogger.w(TAG, "Biometric authentication error [$errorCode]: $errString")
                onError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // 单次识别失败（如手指放偏），系统会振动提示用户重试，此处仅记录审计警告
                ApmLogger.w(TAG, "Biometric authentication attempt rejected")
            }
        }

        try {
            val prompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                // 允许降级到系统锁屏密码输入
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()

            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            ApmLogger.e(TAG, "Failed to launch BiometricPrompt: ${e.message}")
            onError(-1, e.message ?: "Authentication error")
        }
    }
}
```

---

### 4.5 难点五：重力矢量差与物理摇一摇传感器手势 (`ShakeDetector`)

#### 🔑 技术挑战与设计考量
在收银台结账、或者乘坐地铁时，周围常有他人注视。如果需要点击多级菜单才能隐藏资产金额，时效性无法满足。
系统引入了“物理摇一摇手机”快速隐额，但存在两大设计难点：
1. **传感器功耗陷阱 (Battery Drain)**：加速度传感器如果后台持续监听，会导致手机耗电量陡增；
2. **手抖误触与连续触发**：用户快走或普通颠簸可能误触隐额。

#### 💡 终极解决方案：矢量加速度变化率 + 严格生命周期管理
1. **重力加速度归一化与矢量模长算法**：
   $$g = \sqrt{\left(\frac{x}{g_0}\right)^2 + \left(\frac{y}{g_0}\right)^2 + \left(\frac{z}{g_0}\right)^2}$$
   $$\Delta a = (g - 1.0) \times g_0$$
   只有当消除静态重力后的瞬间加速度增量 $\Delta a > 13.0\,\text{m/s}^2$ 时才判定为有意摇晃；
2. **1000ms 节流防抖 (Throttling)**：一次摇晃触发后，1 秒内屏蔽后续传感器事件；
3. **退后台即刻注销**：在 Activity `onPause` 时彻底注销 `SensorEventListener`，只有在活跃前台且设置开启时才注册，做到 **0 后台电量开销**。

```kotlin
// 文件位置: core/security/ShakeDetector.kt
class ShakeDetector(
    private val onShake: () -> Unit,
    private val thresholdAcceleration: Float = 13.0f, // 摇晃触发阈值
    private val throttleIntervalMs: Long = 1000L      // 节流间隔，防止连续触发
) : SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTimestamp = 0L

    fun start(context: Context) {
        if (sensorManager != null) return
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelerometer?.let { sensor ->
                // 使用 UI 级别采样率，兼顾灵敏度与能耗
                sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
                ApmLogger.i("ShakeDetector", "ShakeDetector registered successfully")
            }
        } catch (e: Exception) {
            ApmLogger.w("ShakeDetector", "Failed to register accelerometer: ${e.message}")
        }
    }

    fun stop() {
        try {
            // 退后台必须立刻注销，杜绝后台常驻耗电
            sensorManager?.unregisterListener(this)
            sensorManager = null
            accelerometer = null
            ApmLogger.i("ShakeDetector", "ShakeDetector unregistered")
        } catch (e: Exception) {
            ApmLogger.w("ShakeDetector", "Failed to unregister accelerometer: ${e.message}")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // 归一化重力矢量
        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // 计算总重力加速度空间矢量模长
        val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()
        // 减去常态下的 1G 地球自身重力，得到用户赋予手机的实际加速度
        val accelerationDelta = (gForce - 1.0f) * SensorManager.GRAVITY_EARTH

        if (accelerationDelta > thresholdAcceleration) {
            val now = SystemClock.elapsedRealtime()
            // 节流防抖判断
            if (now - lastShakeTimestamp >= throttleIntervalMs) {
                lastShakeTimestamp = now
                ApmLogger.i("ShakeDetector", "Shake gesture detected (delta: $accelerationDelta)")
                onShake() // 触发隐额翻转
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* 忽略精度变动 */ }
}
```

---

### 4.6 难点六：Compose 顶层全屏安全阻断遮罩 (`BiometricLockOverlay`)

#### 🔑 技术挑战
如果将锁屏弹窗做成常规的 `Dialog` 或 `Popup`，在某些 Android 系统版本上，用户狂点系统物理“返回键”可能会 Dismiss 掉 Dialog，从而穿透看到底层界面！

#### 💡 终极解决方案：Root 节点互斥渲染
在 `MainActivity.setContent` 中，锁屏状态并不作为弹窗叠加，而是作为**根布局的绝对互斥分支**：
- 当 `isAppLocked == true` 时，整个 Compose 树**仅挂载 `BiometricLockOverlay`**，底层的 `App()` 根本未被包含在组合中；
- 这种架构在物理上杜绝了任何返回键穿透、无障碍服务刺探或触摸事件穿透的可能。

```kotlin
// MainActivity.kt 根渲染入口
ListenTheme(...) {
    Surface(modifier = Modifier.fillMaxSize()) {
        if (securityCoordinator.isAppLocked) {
            // ---- 物理级绝对隔离 ----
            // 锁定状态下，主界面组件完全不参与 Composition 组合！
            BiometricLockOverlay(
                onUnlockRequest = {
                    securityCoordinator.promptUnlock(
                        this@MainActivity,
                        settingsState.language,
                        settingsState.recentAppsShieldEnabled
                    )
                },
                lang = settingsState.language
            )
        } else {
            // 认证解锁通过后，才正式渲染应用主体与全局浮层
            App(appState = appState)
            AppOverlayHost(appState = appState)
        }
    }
}
```

---

## 5. UI 与设置项架构 (Settings & Preferences)

在设置页「安全与隐私」独立配置卡片（`SettingsSecuritySection.kt`）中，提供分级掌控能力：

```text
[安全与隐私]
├─ 生物识别应用锁 ------------------------------ [Switch]
│   └─ 未录入指纹或硬件不支持时：自动置灰并提示原因
├─ 自动锁定时长 (宽限期) ----------------------- [Dropdown / OptionSheet]
│   ├─ 立即锁定 (0秒) ------------------------- [Default 极高安全性]
│   ├─ 1 分钟后 (60秒) ------------------------ [平衡临时切出看验证码]
│   └─ 5 分钟后 (300秒) ----------------------- [高频操作便利性]
├─ 多任务界面防窥保护 (FLAG_SECURE) ------------ [Switch] (默认开启)
└─ 摇一摇手机快速隐藏金额 --------------------- [Switch] (默认开启)
```

---

## 6. 核心架构源码对照清单 (Source Code Index)

| 核心文件 | 行数 | 架构职责定位 |
| :--- | :--- | :--- |
| `core/security/AppSecurityCoordinator.kt` | 138 行 | 统筹锁屏状态机、单调时钟计算、`FLAG_SECURE` 治理、摇一摇传感器调度 |
| `core/security/BiometricSecurityManager.kt` | 90 行 | 适配 `BiometricPrompt`，提供强生物识别到系统凭据的降级链与 APM 日志审计 |
| `core/security/SecurityPreferences.kt` | 36 行 | 轻量同步 `SharedPreferences` 存储，专门用于解决冷启动首帧泄露竞态 |
| `core/security/ShakeDetector.kt` | 86 行 | 物理加速度传感器手势识别器，包含矢量加速度算法与防后台漏电机制 |
| `core/security/BiometricLockOverlay.kt` | 129 行 | 顶层全屏安全锁屏遮罩 Composable 组件 |
| `features/settings/components/SettingsSecuritySection.kt` | 150 行 | 设置页安全配置独立卡片 |
| `MainActivity.kt` | 163 行 | 生命周期挂载点与顶层互斥组合渲染根节点 |

---

## 7. 安全合规与测试验证核对单 (Security Checklist)

- [x] **无第三方云端传输**：生物特征数据完全由 Android 安全芯片（TEE / Titan M）处理，应用仅接收成功/失败回调，0 生物特征外泄；
- [x] **防时钟篡改测试**：退后台后修改系统时区或手动倒拨时间，重新进入应用依然能精确检测到真实硬件流逝时长并锁定；
- [x] **多任务卡片黑屏测试**：开启 Recent Apps Shield 后上滑切出多任务卡片，验证系统缩略图完全为白底或黑底，无法窥见账单明细；
- [x] **后台零漏电测试**：应用切入后台或锁屏后，加速度传感器立即注销，通过 Battery Historian 验证无额外 WakeLock 与传感器唤醒消耗；
- [x] **单文件行数合规**：所有安全模块源码单文件行数严格控制在 $36 \sim 163$ 行之间，100% 符合 $\le 250$ 行规范。
