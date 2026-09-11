# ListenExpenseTracker - Google 账号登录与 Drive 云端同步全景开发指南
(Google Identity & Google Drive REST API v3 Integration Guide)

本文档系统性阐述 **ListenExpenseTracker (lExpense)** 中集成 **Google 原生身份凭据登录（AndroidX Credential Manager）** 与 **轻量级 Google Drive REST API v3 云端硬盘备份恢复** 的架构设计、底层通信实现细节、Google Play App Signing 双层密钥体系以及全流程故障排查诊断方案。

---

## 1. 架构总览与分层设计哲学 (Architecture & Design Philosophy)

系统采用 **Google Identity (身份鉴权) + Google Drive REST API v3 (云端存储) + MVI 状态机** 的三层解耦架构：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                  【用户交互层 (UI Layer)】                               │
│      SettingsScreen -> SettingsDataCenterSection -> GoogleAccountProfileCard            │
└────────────────────────────────────────────┬────────────────────────────────────────────┘
                                             │ 用户点击「登录 Google」 / 「立即备份」 / 「从云端恢复」
                                             ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           【状态机与调度层 (ViewModel & Delegate)】                      │
│             SettingsViewModel  <───>  SettingsSyncDelegate  <───>  SettingsEffects      │
│  - 监听与派发 MVI Intent (TriggerGoogleSignIn / TriggerCloudBackup / TriggerCloudRestore)│
│  - 调度 CommonUiEffect (ShowToast / ShowSnackbar) 与 UI 进度指示器                      │
└───────────────────────┬─────────────────────────────────────────┬───────────────────────┘
                        │ 1. 登录与身份凭据获取                    │ 2. 账单备份与还原
                        ▼                                         ▼
┌───────────────────────────────────────┐ ┌───────────────────────────────────────────────┐
│   【Google 身份认证 (Google Identity)】 │ │      【云端存储服务 (Google Drive REST v3)】    │
│            GoogleAuthManager          │ │      GoogleDriveService & AutoBackupManager   │
│ ───────────────────────────────────── │ │ ───────────────────────────────────────────── │
│ - AndroidX CredentialManager          │ │ - GoogleAuthUtil.getToken() 获取 Bearer Token │
│ - GetGoogleIdOption (Web Client ID)   │ │ - 捕获 UserRecoverableAuthException 自愈提权  │
│ - 解析 GoogleIdTokenCredential        │ │ - Multipart/related 新建 / Media PATCH 更新   │
│ - 提取 Email, Name, Avatar            │ │ - SHA-256 脏数据校验 + Wi-Fi 限制 + 5s 协程防抖│
└───────────────────┬───────────────────┘ └───────────────────────┬───────────────────────┘
                    │                                             │
                    │ 携带签名 SHA-1 与包名                       │ 携带 OAuth 2.0 Bearer Token
                    ▼                                             ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                         【Google 云端服务 (Google Cloud Platform)】                     │
│         Google Identity Services (OAuth 2.0)  +  Google Drive API v3 REST Endpoints     │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 核心业务时序交互图 (Sequence Flow)

```text
【阶段一：Google 官方原生登录 (Google Identity)】
用户 (User)               设置页 (UI)           SettingsViewModel       GoogleAuthManager        Google Cloud
    │                         │                         │                       │                     │
    ├─ 1. 点击「登录 Google」 ─>│                         │                       │                     │
    │                         ├─ 2. 派发 Intent ───────>│                       │                     │
    │                         │                         ├─ 3. 请求凭据获取 ─────>│                     │
    │                         │                         │                       ├─ 4. IPC 验证指纹 ──>│
    │<────────────────────────┼─────────────────────────┼───────────────────────┼── 5. 弹出账号面板 ──┤
    ├─ 6. 选择 Google 账号 ───┼─────────────────────────┼───────────────────────┼────────────────────>│
    │                         │                         │                       │<─ 7. 返回 ID Token ─┤
    │                         │                         │<─ 8. 解析 UserProfile ┤                     │
    │                         │<─ 9. 更新 UI 账号卡片 ──┤ (持久化至 DataStore)  │                     │
    ▼                         ▼                         ▼                       ▼                     ▼

【阶段二：备份至 Google Drive 云端硬盘】
用户 (User)               设置页 (UI)           SettingsViewModel       GoogleDriveService       Google Drive v3
    │                         │                         │                       │                     │
    ├─ 1. 点击「备份至云端」 ─>│                         │                       │                     │
    │                         ├─ 2. 派发备份 Intent ───>│                       │                     │
    │                         │                         ├─ 3. 获取 AccessToken ─>│                     │
    │<────────────────────────┼─────────────────────────┼── (首次) 弹提权面板 ──┤                     │
    ├─ 4. 点击「允许」授权 ───┼─────────────────────────┼──────────────────────>│                     │
    │                         │                         │                       ├─ 5. 检索备份文件 ──>│
    │                         │                         │                       │<─ 6. 返回查询结果 ──┤
    │                         │                         │                       ├─ 7. PATCH/POST ────>│
    │                         │                         │                       │<─ 8. 上传成功 200 OK┤
    │                         │<─ 9. Toast 提示备份成功 ┤<─ 记录 APM 链路日志 ──┤                     │
    ▼                         ▼                         ▼                       ▼                     ▼

【阶段三：从 Google Drive 云端恢复】
用户 (User)               设置页 (UI)           SettingsViewModel       GoogleDriveService       Google Drive v3
    │                         │                         │                       │                     │
    ├─ 1. 点击「从云端恢复」 ─>│                         │                       │                     │
    │                         ├─ 2. 派发恢复 Intent ───>│                       │                     │
    │                         │                         ├─ 3. downloadBackup ──>│                     │
    │                         │                         │                       ├─ 4. GET ?alt=media ─>│
    │                         │                         │                       │<─ 5. 返回备份 JSON ─┤
    │                         │                         ├─ 6. 反序列化写入 Room │                     │
    │                         │<─ 7. 提示成功恢复 N 条 ─┤                       │                     │
    ▼                         ▼                         ▼                       ▼                     ▼
```

### 1.2 核心设计哲学与技术权衡

#### 1. 为何弃用旧版 `GoogleSignInClient`，拥抱 AndroidX `CredentialManager`？
- **历史包袱彻底清空**：旧版 `GoogleSignInClient` 与 `Play Services Auth` 已被 Google 官方明确列为 **Deprecated（已废弃）**。旧 API 强依赖 `Activity.startActivityForResult`，破坏了现代 Compose/MVI 架构的单向数据流与无状态组件设计；
- **凭据聚合体验**：`AndroidX CredentialManager` 是 Google 与 Android 官方统一的凭据认证入口，不仅支持 Google 账号密码登录，还无缝兼容 **Passkeys (通行密钥)** 与三方密码管理器；
- **系统底层通信**：直接与 Android 14+ 系统的凭据提供程序框架（Credential Provider Framework）交互，无额外 Activity 中转，动画更丝滑。

#### 2. 为何不引入官方 `google-api-services-drive` SDK，而是基于 REST API v3 原生接入？
- **APK 体积控制**：官方 Drive Java Client 强依赖 Guava、Jackson、Apache HTTP Client 等臃肿的重量级依赖，引入后 APK 体积将暴增 **4 ~ 6 MB**，并额外引入数千个 DEX 方法数；
- **原生极速调用**：记账应用的云端备份本质上只是单文件的“查询、创建、更新、下载”。采用原生 `HttpURLConnection` 自行封装 REST API v3，**零三方依赖，APK 增量为 0 KB**，冷启动零延迟，网络性能极致轻快；
- **透明度与可控性**：所有 HTTP 请求头（`Authorization: Bearer`、`Content-Type: multipart/related`）与 Boundary 边界分隔符完全可控，便于直接与内部 APM（`TraceManager`）深度集成。

#### 3. 为何设计变动驱动防抖与 SHA-256 脏数据校验？
- **防抖机制 (Debouncing)**：用户连续记录多笔账单或导入测试数据时，通过 `CoroutineScope` 延时 5000ms 自动防抖，避免触发高频网络上传，极大降低网络耗电与 Google API 配额消耗；
- **脏数据哈希指纹校验**：通过 SHA-256 计算账单数据快照指纹，如果内容未发生实质变更（`currentHash == lastHash`），则直接跳过上传。

---

## 2. Google Cloud Console 凭据全矩阵与签名体系

### 2.1 凭据全景配置矩阵表 (The 4+1 Matrix)

为了保证在 **本地开发直跑、CI 自动化打包构建、Google Play 内部测试分发、Google Play 线上正式分发** 4 大场景下 Google 登录均 100% 成功，必须在 [Google Cloud Console 凭据控制台](https://console.cloud.google.com/apis/credentials) 的**同一个 GCP 项目**下配置以下完整的凭据矩阵：

| 客户端名称 | 应用类型 | 包名 (Package Name) | SHA-1 证书指纹 (Certificate Fingerprint) | 对应生效环境与场景 |
| :--- | :--- | :--- | :--- | :--- |
| **Android - Play 当前密钥** | Android | `com.listen.expensetracker` | `B7:DD:48:E4:59:98:8C:B4:7B:42:B8:D7:D9:50:61:14:75:A5:45:08` | Google Play 商店正式版本 / 线上版本 / 升级后版本 |
| **Android - Play 历史密钥** | Android | `com.listen.expensetracker` | `31:3A:36:A4:C4:60:30:31:06:AF:95:CE:6D:81:6B:7B:BB:8C:35:A1` | Google Play 内部测试 (Internal Testing) / 内部应用分享 / 存量旧机型 |
| **Android - Release 上传密钥** | Android | `com.listen.expensetracker` | `38:71:09:AA:CE:E2:54:5B:9E:3A:F8:1F:54:38:99:CD:CD:E1:E9:93` | 本地 Release 打包 / GitHub Actions CI 直装 APK (`keystore/lExpense.jks`) |
| **Android - Debug 本地调试** | Android | `com.listen.expensetracker` | `D3:FC:90:5E:5D:05:C5:F8:0B:63:70:DD:C4:11:71:72:D3:02:3B:09` | Android Studio 开发者电脑直接 Run (`debug.keystore`) |
| **Web - 核心签发受众** | Web 应用程序 | - (无需填写) | - (用于获取 Audience / ID Token) | 代码中 `GoogleAuthManager` 填入此 ID: `1069102462195-...apps.googleusercontent.com` |

### 2.2 核心机制解密：为什么代码中仅需要配置 Web 客户端 ID？

很多 Android 开发者在此处容易产生混淆：**“为什么 Android 平台的客户端 ID 不用写在代码里？”**

1. **Android 客户端 ID 的作用（身份鉴权门禁）**：
   - 手机端安装的 Google Play Services（GMS 核心）在处理客户端请求时，会直接通过底层 Linux 内核与 Android PackageManager IPC 提取当前应用的 **真实包名** 与 **真实签名证书的 SHA-1 指纹**；
   - GMS 将此 `(包名, SHA-1)` 组合上报给 Google 认证服务器；Google 云端校验其是否在 GCP 控制台已注册的 Android 客户端列表中。若匹配则放行，否则直接返回 `16: No credentials available`；
   - 因此，**Android 客户端 ID 完全由系统层底层提取，无需在代码中硬编码**。
2. **Web 客户端 ID 的作用（令牌签发受众 Audience）**：
   - 在 OAuth 2.0 / OpenID Connect 协议中，客户端向 Google 请求签发 JWT ID Token 时，必须指明这个 Token 的**受众（Audience, `aud`）**是谁；
   - 在 `GoogleAuthManager.kt` 中，`DEFAULT_WEB_CLIENT_ID` 被传入 `GetGoogleIdOption.Builder().setServerClientId(...)`，用于指导 Google 认证中心将 ID Token 签发给该 Web 凭证，从而返回包含邮箱、昵称与头像的有效凭证。

### 2.3 Google Play App Signing 双层签名机制深度解析

Google Play 采用**上传密钥 (Upload Key)** 与 **应用签名密钥 (App Signing Key)** 相互隔离的双层密钥体系：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                      【阶段一：开发者编译与上传 (Developer / CI)】                       │
│    本地 Android Studio / GitHub Actions CI 使用 keystore/lExpense.jks 进行打包签名      │
│    - 证书指纹 (SHA-1): 38:71:09:AA:CE:E2:54:5B:9E:3A:F8:1F:54:38:99:CD:CD:E1:E9:93      │
└────────────────────────────────────────────┬────────────────────────────────────────────┘
                                             │ 上传 Release AAB (Android App Bundle)
                                             ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                    【阶段二：Google Play 托管与签名重构 (Google Cloud)】                 │
│  1. Google Play 服务端校验 Upload Key 签名合法性                                        │
│  2. 剥离上传签名，从 Google 硬件安全模块 (HSM) 提取受保护的【应用签名密钥】重新签名    │
│     - 线上正式版密钥指纹: B7:DD:48:E4:59:98:8C:B4:7B:42:B8:D7:D9:50:61:14:75:A5:45:08   │
│     - 历史/内测版密钥指纹: 31:3A:36:A4:C4:60:30:31:06:AF:95:CE:6D:81:6B:7B:BB:8C:35:A1   │
└────────────────────────────────────────────┬────────────────────────────────────────────┘
                                             │ 生成针对具体机型优化的 Split APKs
                                             ▼
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                        【阶段三：用户手机安装与 OAuth 运行时校验】                      │
│  用户从 Google Play 下载安装，底层 Google Play Services 提取当前 APK 真实 SHA-1 与包名:  │
│  - 匹配成功：弹出账号选择面板，顺利颁发 ID Token                                        │
│  - 匹配失败：控制台报错 16: No credentials available                                    │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

> [!IMPORTANT]
> 绝不能仅配置本地 keystore 的 SHA-1！一旦发布到 Google Play，终端用户手机上的安装包签名会被替换为 Google Play 管理的 App Signing Key。如果 GCP 控制台缺少 Google Play 签名密钥的 SHA-1，线上用户点击 Google 登录将必然报出 `No credentials available` 致命错误。

---

## 3. 核心重点与难点代码实现深度剖析 (Implementation Walkthrough)

### 3.1 难点一：AndroidX Credential Manager 原生身份凭据调用与解析

源码位于 [`GoogleAuthManager.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/auth/GoogleAuthManager.kt)。

#### 🔑 设计思路
- 将凭证初始化、请求参数组装、结果解析与登出清理封装为纯单例；
- 运用现代 `GetGoogleIdOption` 构建器，精确控制“自动静默选择”与“账号过滤策略”；
- 安全捕获数据包并转换成不可变模型 [`GoogleUserProfile`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/auth/GoogleAuthManager.kt#L14-L19)。

#### 💻 教学源码实现（含逐行中文注释）

```kotlin
package com.listen.expensetracker.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * 用户经过 Google 身份认证后的核心信息实体模型 (Clean Data Model)
 *
 * @property email 用户 Google 账号邮箱 (如 user@gmail.com)
 * @property displayName 用户的公开昵称/姓名 (可能为 null)
 * @property avatarUrl 用户的个人高清头像在线链接 (可能为 null)
 * @property idToken Google 签发的 OpenID Connect JWT 令牌，供后端或受众方校验合法性
 */
data class GoogleUserProfile(
    val email: String,
    val displayName: String?,
    val avatarUrl: String?,
    val idToken: String?
)

/**
 * 基于新一代 AndroidX Credential Manager 的 Google 身份认证引擎
 *
 * 核心设计优势：
 * 1. 零废弃 API：全面替代已被 Google 标记废弃的 GoogleSignInClient 与 Legacy Auth API；
 * 2. 凭证聚合：无缝融合 Google ID、Passkeys (通行密钥)、密码管理器；
 * 3. 极速响应：轻量化架构，不拉起重型 GoogleSignInActivity，直接通过系统服务进行 IPC 认证交互。
 */
object GoogleAuthManager {

    // 在 Google Cloud Console 创建的 Web 应用程序 (Web application) 类型的 OAuth 客户端 ID
    // 用于告知 Google Identity 服务本次认证的受众方 (Audience)
    private const val DEFAULT_WEB_CLIENT_ID = "1069102462195-rjdheb5uqeb64o02ucan0lc65r0ammn6.apps.googleusercontent.com"

    /**
     * 获取官方 AndroidX CredentialManager 单例句柄
     */
    fun getCredentialManager(context: Context): CredentialManager {
        return CredentialManager.create(context)
    }

    /**
     * 构建 Google Identity 专用认证凭据请求选项 (GetGoogleIdOption)
     *
     * @param serverClientId 可选的 Web Client ID，默认缺省使用预置的标准 Web Client ID
     * @return 配置完毕的 GetGoogleIdOption 选项
     */
    fun buildGoogleIdOption(serverClientId: String = ""): GetGoogleIdOption {
        val clientId = serverClientId.ifBlank { DEFAULT_WEB_CLIENT_ID }
        return GetGoogleIdOption.Builder()
            // 关闭仅过滤已授权账号，确保首次登录的用户也能在列表里看到自己手机上的所有 Google 账号
            .setFilterByAuthorizedAccounts(false)
            // 设定签发受众方客户端 ID (Web Client ID)
            .setServerClientId(clientId)
            // 关闭自动静默选择，弹出账号选择底面供用户主动确认，保障知情权
            .setAutoSelectEnabled(false)
            .build()
    }

    /**
     * 构建聚合的凭据请求对象 (GetCredentialRequest)
     */
    fun buildGetCredentialRequest(serverClientId: String = ""): GetCredentialRequest {
        return GetCredentialRequest.Builder()
            .addCredentialOption(buildGoogleIdOption(serverClientId))
            .build()
    }

    /**
     * 解析 AndroidX CredentialManager 返回的凭证数据流
     *
     * @param response CredentialManager.getCredential() 异步成功返回的响应对象
     * @return 包含 GoogleUserProfile 的 Result 容器；若解析格式不兼容则返回 failure
     */
    fun parseGoogleIdCredential(response: GetCredentialResponse): Result<GoogleUserProfile> {
        return try {
            val credential = response.credential
            // 从 Bundle 数据流中反序列化出 Google 专用的 GoogleIdTokenCredential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val profile = GoogleUserProfile(
                email = googleIdTokenCredential.id,
                displayName = googleIdTokenCredential.displayName,
                avatarUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                idToken = googleIdTokenCredential.idToken
            )
            Result.success(profile)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * 清理所有已缓存的凭据状态（用于用户主动登出账号）
     */
    suspend fun clearCredentials(context: Context) {
        try {
            getCredentialManager(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Throwable) {
            // 静默处理清理失败，不阻塞用户登出体验
        }
    }
}
```

---

### 3.2 难点二：零 SDK 依赖的 Google Drive REST API v3 客户端与提权自愈

源码位于 [`GoogleDriveService.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/data/cloud/GoogleDriveService.kt)。

#### 🔑 设计思路与技术难点
1. **OAuth 2.0 访问令牌获取与自愈弹窗**：
   - 传统方案在用户未给应用授权 Google Drive 权限时直接崩溃或报错；
   - `GoogleDriveService` 通过 `GoogleAuthUtil.getToken(context, account, "oauth2:...drive.file")` 请求 Token；
   - **自愈机制**：当首次访问云盘抛出 `UserRecoverableAuthException` 时，从中提取官方授权 Activity 的 `Intent`，加上 `FLAG_ACTIVITY_NEW_TASK` 自动拉起系统授权弹窗，用户点击允许即可无缝恢复；
2. **RFC 2387 `multipart/related` 表单组装**：
   - Google Drive 上传新文件要求同时上传元数据（文件名、MIME 类型）与二进制内容；
   - 手动组装多部分边界 `===lExpenseBoundary...===`，第一段写入元数据 JSON，第二段写入账单数据 JSON；
3. **PATCH 增量媒体更新**：
   - 如果云盘已存在同名备份文件，直接调用 `PATCH /drive/v3/files/{id}?uploadType=media`，只更新文件正文，大幅提升同步效率。

#### 💻 教学源码实现（含逐行中文注释）

```kotlin
package com.listen.expensetracker.data.cloud

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.listen.arch.apm.ApmLogChannel
import com.listen.arch.apm.ApmLogger
import com.listen.arch.apm.TraceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 原生 Google Drive REST API v3 云端同步服务 (轻量化无 SDK 架构)
 *
 * 核心设计优势：
 * 1. 零 SDK 依赖：彻底摒弃体积达数兆的 Google API Client，基于原生 HttpURLConnection 极速交互；
 * 2. 自愈鉴权守卫：拦截 UserRecoverableAuthException 并自动拉起系统授权面板，实现故障自愈；
 * 3. 增量覆盖与多部分上传：首次创建走 multipart/related，后续同步走媒体流式 PATCH，极致节省流量；
 * 4. 全链路 APM 追踪：在 SYNC 频道记录每次云端读写的 Trace ID、文件 ID 与耗时。
 */
object GoogleDriveService {

    private const val BACKUP_FILE_NAME = "lexpense_backup.json"
    private const val DRIVE_API_FILES = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_MULTIPART = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
    private const val DRIVE_UPLOAD_MEDIA = "https://www.googleapis.com/upload/drive/v3/files/%s?uploadType=media"
    private const val OAUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file"

    /**
     * 获取具有 drive.file 作用域的有效 OAuth 2.0 访问令牌 (Bearer Token)
     * 若遇到权限未授予异常，自动拉起 Google 系统授权面板
     */
    suspend fun getAccessToken(context: Context, accountEmail: String): String = withContext(Dispatchers.IO) {
        val account = Account(accountEmail, "com.google")
        try {
            GoogleAuthUtil.getToken(context, account, OAUTH_SCOPE)
        } catch (e: UserRecoverableAuthException) {
            // 提权自愈：利用异常携带的 Intent 调起原生权限授予面板
            e.intent?.let { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            throw IllegalStateException("请在弹出的 Google 授权窗口中点击「允许」，完成后再次点击备份")
        }
    }

    /**
     * 将序列化后的 JSON 账单快照上传至 Google Drive
     */
    suspend fun uploadBackup(
        accessToken: String,
        jsonContent: String,
        traceId: String = TraceManager.newTraceId()
    ): Result<String> = withContext(Dispatchers.IO) {
        TraceManager.trace(ApmLogChannel.SYNC, "GoogleDrive", "UploadBackup", traceId) {
            try {
                val existingFileId = findBackupFileId(accessToken)
                val fileId = if (existingFileId != null) {
                    // 已存在备份文件 -> 执行媒体覆盖更新
                    updateExistingFile(existingFileId, accessToken, jsonContent)
                    existingFileId
                } else {
                    // 首次备份 -> 执行多部分新建文件
                    createNewFile(accessToken, jsonContent)
                }
                ApmLogger.sync("GoogleDrive", "已成功上传备份至 Google Drive ($BACKUP_FILE_NAME, FileID: $fileId)", traceId)
                Result.success(fileId)
            } catch (e: Throwable) {
                ApmLogger.sync("GoogleDrive", "Google Drive 上传异常: ${e.message}", traceId)
                Result.failure(e)
            }
        }
    }

    /**
     * 从 Google Drive 下载最新的 JSON 备份数据流
     */
    suspend fun downloadBackup(
        accessToken: String,
        traceId: String = TraceManager.newTraceId()
    ): Result<String> = withContext(Dispatchers.IO) {
        TraceManager.trace(ApmLogChannel.SYNC, "GoogleDrive", "DownloadBackup", traceId) {
            try {
                val fileId = findBackupFileId(accessToken)
                    ?: return@trace Result.failure(IllegalStateException("未在 Google 云端硬盘中找到 $BACKUP_FILE_NAME 备份文件"))

                // 请求 alt=media 直接下载原始二进制/文本正文
                val downloadUrl = "$DRIVE_API_FILES/$fileId?alt=media"
                val connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException("Google Drive 下载失败 (${connection.responseCode}): ${readStream(connection.errorStream)}")
                }

                val jsonPayload = readStream(connection.inputStream)
                ApmLogger.sync("GoogleDrive", "成功从 Google Drive 下载快照 ($BACKUP_FILE_NAME, ${jsonPayload.length} 字节)", traceId)
                Result.success(jsonPayload)
            } catch (e: Throwable) {
                ApmLogger.sync("GoogleDrive", "Google Drive 下载异常: ${e.message}", traceId)
                Result.failure(e)
            }
        }
    }

    /**
     * 检索用户 Drive 中是否存在未被移入回收站的有效备份文件
     */
    private fun findBackupFileId(accessToken: String): String? {
        val query = "name='$BACKUP_FILE_NAME' and trashed=false"
        val urlStr = "$DRIVE_API_FILES?q=${URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,modifiedTime)"
        val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
            connectTimeout = 15000
            readTimeout = 15000
        }

        if (connection.responseCode in 200..299) {
            val responseText = readStream(connection.inputStream)
            val files = JSONObject(responseText).optJSONArray("files")
            if (files != null && files.length() > 0) {
                return files.getJSONObject(0).optString("id")
            }
        }
        return null
    }

    /**
     * 通过 multipart/related 方式在 Google Drive 中创建新备份文件
     */
    private fun createNewFile(accessToken: String, jsonContent: String): String {
        val boundary = "===lExpenseBoundary" + System.currentTimeMillis() + "==="
        val connection = (URL(DRIVE_UPLOAD_MULTIPART).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            connectTimeout = 15000
            readTimeout = 20000
        }

        // 第 1 部分：元数据描述 (JSON)
        val metadata = JSONObject().apply {
            put("name", BACKUP_FILE_NAME)
            put("mimeType", "application/json")
        }.toString()

        // 第 2 部分：文件正文内容 (JSON Payload)
        val outputStream: OutputStream = connection.outputStream
        outputStream.bufferedWriter().use { writer ->
            writer.write("--$boundary
")
            writer.write("Content-Type: application/json; charset=UTF-8

")
            writer.write(metadata)
            writer.write("
--$boundary
")
            writer.write("Content-Type: application/json

")
            writer.write(jsonContent)
            writer.write("
--$boundary--
")
            writer.flush()
        }

        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Google Drive 创建文件失败 (${connection.responseCode}): ${readStream(connection.errorStream)}")
        }

        return JSONObject(readStream(connection.inputStream)).getString("id")
    }

    /**
     * 通过 PATCH uploadType=media 覆盖更新已存在的 Google Drive 备份文件
     */
    private fun updateExistingFile(fileId: String, accessToken: String, jsonContent: String) {
        val urlStr = DRIVE_UPLOAD_MEDIA.format(fileId)
        val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 15000
            readTimeout = 20000
        }

        connection.outputStream.bufferedWriter().use { writer ->
            writer.write(jsonContent)
            writer.flush()
        }

        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Google Drive 更新文件失败 (${connection.responseCode}): ${readStream(connection.errorStream)}")
        }
    }

    /**
     * 安全读取字节流并转换为 UTF-8 文本
     */
    private fun readStream(inputStream: InputStream?): String {
        if (inputStream == null) return ""
        return BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
    }
}
```

---

### 3.3 难点三：变动驱动与网络感知的智能自动备份编排器

源码位于 [`GoogleDriveAutoBackupManager.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/data/cloud/GoogleDriveAutoBackupManager.kt)。

#### 🔑 设计思路
```text
用户新增/编辑/删除账单 (TransactionDao Mutation)
   │
   ▼
GoogleDriveAutoBackupManager.scheduleAutoBackup(delayMs = 5000L)
   │
   ├── 5 秒内再次触发记账？──>【是】──> 取消旧 Job (cancel)，重置 5 秒倒计时
   │
   └──【否：5秒倒计时平稳到期】
         │
         ▼
     【前置守卫检测】
     ├── 是否登录 Google 账号？ ──────>【未登录】──> 中止 (Skip)
     ├── 是否启用“自动备份”开关？ ────>【未启用】──> 中止 (Skip)
     └── 是否开启“仅 Wi-Fi”且当前是移动网络？ ──>【是】──> 中止 (Skip)
         │
         ▼【前置条件全满足】
     【脏数据对比 (Dirty Checking)】
     1. 从 Room 读取全量账单并序列化为 JSON
     2. 计算当前 JSON 字符串的 SHA-256 哈希值
     3. 比对 DataStore 中存储的 lastBackupHash:
        ├── currentHash == lastHash ──>【数据无变动】──> 记录日志并跳过上传 (节省流量与配额)
        └── currentHash != lastHash ──>【数据已变更】──> 执行云端增量上传
                                                              │
                                                              ▼
                                               GoogleDriveService.uploadBackup()
                                                              │
                                                              ▼
                                               成功后更新 DataStore:
                                               - lastBackupHash = currentHash
                                               - lastSyncTimestamp = System.currentTimeMillis()
```

#### 💻 教学源码实现（含逐行中文注释）

```kotlin
/**
 * 智能 Google Drive 自动备份编排调度器 (Intelligent Auto-Backup Orchestrator)
 *
 * 核心机制：
 * 1. 数据变动防抖 (Mutation-Driven Debouncing)：高频记账操作合并，延时 5 秒静默触发，杜绝网络风暴；
 * 2. 脏数据哈希校验 (SHA-256 Dirty Checking)：通过比对全量账单 JSON 哈希值，无变动则直接跳过上传；
 * 3. 网络守卫 (Network Guarding)：严格检测 Wi-Fi 开关配置，防止非 Wi-Fi 环境下消耗用户蜂窝流量；
 * 4. APM 全链路追踪：记录备份各阶段耗时、文件 ID 与异常详情。
 */
object GoogleDriveAutoBackupManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingDebounceJob: Job? = null

    /**
     * 调度防抖自动备份任务（默认防抖时长：5000ms）
     * 若在防抖倒计时期间再次发生账单变动，自动取消前序任务并重置倒计时
     */
    fun scheduleAutoBackup(context: Context, delayMs: Long = 5000L) {
        pendingDebounceJob?.cancel()
        pendingDebounceJob = scope.launch {
            delay(delayMs.milliseconds)
            performAutoBackup(context.applicationContext, force = false)
        }
    }

    /**
     * 执行 Google Drive 自动备份主干链路（带前置条件守卫）
     */
    suspend fun performAutoBackup(context: Context, force: Boolean = false): Result<String> {
        val traceId = TraceManager.newTraceId()
        val prefManager = ExpenseDataStoreManager(context)

        // 守卫 1：账号登录状态校验
        val isLoggedIn = prefManager.isLoggedInFlow.first()
        val email = prefManager.userEmailFlow.first()
        if (!isLoggedIn || email.isBlank()) {
            ApmLogger.sync(tag = "AutoBackup", message = "未登录 Google 账号，跳过自动备份", traceId = traceId)
            return Result.failure(IllegalStateException("User not logged into Google"))
        }

        // 守卫 2：自动备份全局开关校验
        val autoBackupEnabled = prefManager.autoBackupDriveFlow.first()
        if (!autoBackupEnabled && !force) {
            ApmLogger.sync(tag = "AutoBackup", message = "用户已禁用 Google Drive 自动备份开关，跳过备份", traceId = traceId)
            return Result.failure(IllegalStateException("Auto backup disabled"))
        }

        // 守卫 3：网络环境守卫（仅在 Wi-Fi 下自动备份）
        val wifiOnly = prefManager.autoBackupWifiOnlyFlow.first()
        if (wifiOnly && !isWifiConnected(context)) {
            ApmLogger.sync(tag = "AutoBackup", message = "已启用仅 Wi-Fi 自动备份，当前为蜂窝移动网络，跳过备份", traceId = traceId)
            return Result.failure(IllegalStateException("Wi-Fi not connected"))
        }

        return try {
            val db = AppDatabase.getInstance(context)
            val allList = db.transactionDao().getAllTransactions()
            val jsonPayload = TransactionBackupManager.exportToJson(allList)

            // 脏数据哈希校验：若本次序列化内容与上次成功备份哈希完全一致，则无需重复向云端上传
            val currentHash = computeHash(jsonPayload)
            val lastHash = prefManager.lastBackupHashFlow.first()

            if (!force && currentHash == lastHash && lastHash.isNotBlank()) {
                ApmLogger.sync(tag = "AutoBackup", message = "账单数据无变动 (Hash 一致: $currentHash)，无需上传，节省流量", traceId = traceId)
                return Result.success("Skipped: data unchanged")
            }

            ApmLogger.sync(tag = "AutoBackup", message = "检测到数据变更，正在向 Google Drive 上传 ${allList.size} 条账单...", traceId = traceId)

            // 获取 OAuth 2.0 Bearer Token 并上传
            val token = GoogleDriveService.getAccessToken(context, email)
            val uploadRes = GoogleDriveService.uploadBackup(token, jsonPayload, traceId)

            uploadRes.onSuccess { fileId ->
                val now = System.currentTimeMillis()
                // 更新最后同步时间与数据指纹哈希
                prefManager.setLastSyncTimestamp(now)
                prefManager.setLastBackupHash(currentHash)
                CloudSyncManager.backupToCloud(jsonPayload, allList.size, email, traceId)
                ApmLogger.sync(tag = "AutoBackup", message = "Google Drive 自动备份成功 (FileID: $fileId)", traceId = traceId)
            }.onFailure { err ->
                CloudSyncManager.backupToCloud(jsonPayload, allList.size, email, traceId)
                ApmLogger.sync(tag = "AutoBackup", message = "Google Drive 自动备份上传失败: ${err.message}", traceId = traceId)
            }

            uploadRes
        } catch (e: Throwable) {
            ApmLogger.sync(tag = "AutoBackup", message = "自动备份过程抛出未捕获异常: ${e.message}", traceId = traceId)
            Result.failure(e)
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun computeHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
```

---

### 3.4 难点四：MVI 架构下的设置与同步调度代理

源码位于 [`SettingsSyncDelegate.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/features/settings/viewmodel/SettingsSyncDelegate.kt) 与 [`SettingsEffects.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/features/settings/ui/SettingsEffects.kt)。

#### 🔑 双层容灾机制
当 Google Drive 出现由于网络超时、凭据需刷新等异常时，`SettingsSyncDelegate` 不会直接造成应用崩溃，而是会自动将序列化快照备份至本地缓存快照（`CloudSyncManager` 本地内存/沙盒备份），并通过 Toast 提示用户真实原因，实现业务连续性（Business Continuity）。

```kotlin
// SettingsEffects.kt 中的单次即逝事件调度
@Composable
fun SettingsEffects(
    viewModel: SettingsViewModel?,
    context: Context,
    listState: LazyListState
) {
    val currentListState by rememberUpdatedState(listState)
    LaunchedEffect(viewModel) {
        // 使用 collectLatest 防止用户多次快速点击触发重复的账号选择弹窗
        viewModel?.viewEffect?.filterIsInstance<SettingsEffect>()?.collectLatest { effect ->
            when (effect) {
                is SettingsEffect.LaunchGoogleSignIn -> viewModel.launchGoogleAccountPicker(context)
                is SettingsEffect.ScrollToTop -> currentListState.animateScrollToItem(0)
            }
        }
    }
}
```

---

## 4. 常见排错指南与故障自愈手册 (Troubleshooting & Diagnostics)

### 4.1 故障一：`16: No credentials available` 深度诊断 5 步法

这是 Google 账号登录最常见的核心故障。当用户点击登录提示该错误时，请严格按以下步骤依次排查：

#### 步骤 1：确认真机 APK 的真实 SHA-1 证书指纹
- **错误根源**：Google Play Services 底层只认可 **`当前运行 APK 的 SHA-1`**。很多开发者在控制台配置了 `debug.keystore` 的 SHA-1，但手机上安装的是 Release 签名包；
- **排查命令**：
  ```powershell
  # 提取连接设备上安装包的路径并拉取至电脑
  adb shell pm path com.listen.expensetracker
  adb pull <extracted-package-path>/base.apk app_dumped.apk
  # 打印该安装包的真实签名 SHA-1 指纹
  apksigner verify --print-certs app_dumped.apk
  ```
  核对输出的 SHA-1 是否已 100% 完整录入到 GCP 控制台的 Android 客户端凭据中。

#### 步骤 2：检查 Google Play Console 托管密钥
- 进入 **Google Play Console** $ightarrow$ **【设置】** $ightarrow$ **【应用完整性】** $ightarrow$ **【应用签名】**：
  - 复制 **【应用签名密钥证书】** 下的 SHA-1 指纹；
  - 若应用经过密钥升级或属于旧版本迁移，务必同时复制 **【之前的应用签名密钥 (Previous key)】** 下的 SHA-1 指纹；
  - 必须将上述 SHA-1 均录入到 Google Cloud Console。

#### 步骤 3：清除手机端 Google Play 服务本地失败缓存
- **问题根源**：Google Play Services 内部对“鉴权失败”的结果有长达 15 ~ 30 分钟的本地内存与磁盘二级缓存。即便开发者在控制台更正了 SHA-1，手机端仍会继续报错；
- **解决步骤**：
  1. 手机打开系统 `设置` $ightarrow$ `应用管理` $ightarrow$ 搜索找到 `Google Play 服务` (Google Play Services)；
  2. 点击 `存储和缓存` $ightarrow$ 点击 **「清除缓存」**；
  3. 在多任务后台划掉杀死 `lExpense` 应用，重新打开即可瞬间生效。

#### 步骤 4：检查 OAuth 同意屏幕的“测试用户”名单
- **问题根源**：当 GCP 控制台中的 OAuth 同意屏幕发布状态处于 **“测试中 (Testing)”** 时，Google 强制要求只有列入白名单的 Google 账号才被允许登录。非名单内账号调用登录会被直接静默拦截；
- **解决步骤**：进入 GCP 控制台 $ightarrow$ **【OAuth 同意屏幕】** $ightarrow$ 点击 **「添加测试用户」**，填入你的测试 Gmail 邮箱并保存。

#### 步骤 5：等待 Google 全球认证分布式节点同步生效
- 在 Google Cloud Console 新建或更新 Client ID 后，Google 全球分布式鉴权服务器需要 **5 ~ 15 分钟** 进行缓存刷新同步。更新后请稍候再行测试。

---

### 4.2 故障二：`UserRecoverableAuthException` 动态提权弹窗处理

- **触发场景**：用户已登录 Google 账号，但在首次点击“备份至云端”时，尚未向应用授予 `drive.file`（管理由本应用创建的 Google 云端硬盘文件）权限；
- **底层自愈**：
  - `GoogleDriveService.getAccessToken()` 捕获到此异常后，自动通过 `context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))` 调起 Google 原生系统授权页；
  - **用户操作**：用户只需在弹出的原生系统界面上勾选并点击“允许”，授权状态将由 Google Play Services 自动落盘；
  - 随后再次点击备份即可秒级完成。

---

### 4.3 故障三：HTTP 401 Unauthorized / Token 失效

- **触发场景**：Bearer Token 已经过期（OAuth 2.0 访问令牌通常具有 1 小时有效期）；
- **应对方案**：
  - Google Play Services 底层的 `GoogleAuthUtil.getToken()` 会自动根据刷新令牌进行静默续期；
  - 若云端密码被修改或撤销了授权，可调用 `GoogleAuthUtil.clearToken(context, oldToken)` 清理本地令牌缓存，随后重新请求获取新令牌。

---

## 5. 开发者实用调试命令与操作速查 (Developer Cheat Sheet)

```powershell
# 1. 打印本地 debug.keystore 的证书 SHA-1
keytool -list -v -keystore $env:USERPROFILE\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android

# 2. 打印生产环境 keystore (lExpense.jks) 的证书 SHA-1
keytool -list -v -keystore keystore\lExpense.jks -alias lExpenseKey -storepass <your-password>

# 3. 查看应用内 APM 同步链路实时日志
adb logcat -s "lExpense_APM:SYNC" "GoogleDrive" "AutoBackup"

# 4. 模拟断网以验证网络守卫功能
adb shell svc wifi disable
adb shell svc data disable

# 5. 恢复网络
adb shell svc wifi enable
adb shell svc data enable
```
