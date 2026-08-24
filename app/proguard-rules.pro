# ============================================================================
# ListenExpenseTracker - ProGuard & R8 混淆规则配置文件
# ============================================================================

# 1. 基础通用保留配置
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**

# 2. Kotlin 协程与反射
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# 3. Room 数据库实体与 DAO 接口（防止 SQL 字段映射被混淆破坏）
-keepclassmembers class * {
    @androidx.room.Entity *;
    @androidx.room.Dao *;
    @androidx.room.Database *;
    @androidx.room.TypeConverter *;
}
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class com.listen.arch.data.db.** { *; }

# 4. 数据模型与 JSON / CSV 序列化实体（保留字段名称以保障导入导出一致性）
-keep class com.listen.arch.data.db.TransactionEntity { *; }
-keep class com.listen.expensetracker.data.model.** { *; }
-keep class com.listen.arch.sync.SyncState { *; }
-keep class com.listen.uicomponent.apm.LogEntryUi { *; }
-keep class com.listen.uicomponent.charts.** { *; }
-keep class com.listen.uicomponent.components.** { *; }

# 5. Google Play Services Auth & Credential Manager SDK
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.api.** { *; }
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.googleid.**
-dontwarn com.google.android.gms.**

# 6. Jetpack Compose 运行时与状态
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# 7. Android 桌面小组件 (AppWidget)
-keep class com.listen.expensetracker.widget.** { *; }
