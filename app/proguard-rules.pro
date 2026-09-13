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

# 8. Coil 图片加载库
# 保证图片异步加载和转换逻辑不被混淆
-keep class coil.** { *; }
-dontwarn coil.**

# 9. AboutLibraries (开源许可声明库)
# 必须保留元数据，否则应用内的开源许可页面将无法显示内容
-keep class com.mikepenz.aboutlibraries.** { *; }

# 10. Jetpack Lifecycle & ViewModel
# 显式保留 ViewModel 的构造函数，防止 Compose 或 Hilt 反射创建实例时失败
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# 11. Kotlin 运行时通用安全规则
# 处理 Kotlin 反射、默认构造函数标记等
-keep class kotlin.reflect.jvm.internal.** { *; }
-keep class kotlin.Metadata { *; }
-keep class kotlin.jvm.internal.DefaultConstructorMarker { *; }
-dontwarn kotlin.**

# 12. 常见序列化库通用保护 (针对 Google Drive 同步可能的 JSON 转换)
# 如果你使用了 kotlinx.serialization 或 Gson，保留相关的注解和标记
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
