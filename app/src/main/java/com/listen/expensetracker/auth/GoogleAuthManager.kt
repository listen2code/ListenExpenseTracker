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

