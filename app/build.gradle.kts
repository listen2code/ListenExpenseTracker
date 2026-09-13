import java.io.FileInputStream
import java.util.Properties

/**
 * 模块级插件配置
 */
plugins {
    id("com.android.application")           // Android 应用核心插件
    id("org.jetbrains.kotlin.plugin.compose") // Kotlin Compose 编译器插件
    id("com.google.devtools.ksp")           // KSP 高性能注解处理器
    id("jacoco")                            // 单元测试覆盖率统计工具
    alias(libs.plugins.aboutlibraries)      // 第三方库许可自动生成工具
}

/**
 * 读取签名配置：从本地 keystore.properties 加载密钥信息
 */
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore/keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    // 资源包名映射
    namespace = "com.listen.expensetracker"
    
    // 编译时使用的 SDK 版本 (Compile SDK)
    // 作用：定义编译器在编译代码时使用的 API。设置为 release(36) minor 1 允许使用 Android 16 的全部正式 API。
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    /**
     * 基础配置：ID、SDK 限制和版本管理
     */
    defaultConfig {
        applicationId = "com.listen.expensetracker" // 应用在商店的唯一 ID
        minSdk = 24                               // 最低支持系统：Android 7.0 (Nougat)
        
        // 目标 SDK 版本 (Target SDK)
        // 作用：告知系统应用已在哪个版本上进行了充分测试。
        // 影响：系统会根据此值决定是否开启新的行为变更（Behavior Changes）。
        // 说明：36 为 Android 16 正式版本，已稳定适配主流生产环境。
        targetSdk = 36

        val vName = "0.0.42"
        versionName = vName

        // 自动计算版本号：major.minor.patch -> major * 10000 + minor * 100 + patch (例如 0.0.40 -> 40)
        versionCode = try {
            val parts = vName.split(".")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
            major * 10000 + minor * 100 + patch
        } catch (_: Exception) {
            1
        }

        println(">>> Calculated Auto-Increment versionCode: $versionCode (versionName: $versionName)")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /**
     * 签名配置：定义 Release 版的数字签名，确保应用身份安全
     */
    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"] as String?
                keyPassword = keystoreProperties["keyPassword"] as String?

                val storeFileName = keystoreProperties["storeFile"] as String?
                if (storeFileName != null) {
                    storeFile = rootProject.file("keystore/$storeFileName")
                }

                storePassword = keystoreProperties["storePassword"] as String?
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    /**
     * 构建类型：定义 Debug 和 Release 环境的不同策略
     */
    buildTypes {
        debug {
            isMinifyEnabled = false // Debug 版不混淆，方便调试
        }
        release {
            isMinifyEnabled = true    // 开启代码混淆，减小体积并提高逆向难度
            isShrinkResources = true  // 自动删除无用资源
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 只有存在签名配置时才应用签名
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    /**
     * Java 编译选项说明：
     * Java 版本经历了 1.8 (Java 8) -> 11 -> 17 -> 21 的演进。
     * 1.8 以前习惯叫 1.x，之后直接叫整数。17 和 21 是目前的长期支持版 (LTS)。
     */
    compileOptions {
        // 源码兼容性：决定了你在写代码时，可以使用哪些 Java 版本的语法特性。
        sourceCompatibility = JavaVersion.VERSION_17
        // 目标兼容性：决定了生成的字节码文件符合哪个 Java 虚拟机的规范。
        // 由于现代 Android 插件 (AGP 8+) 强制要求 JDK 17 以上，建议在此统一设置为 17。
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true // 开启 Jetpack Compose 支持
    }

    testOptions {
        unitTests.isReturnDefaultValues = true // 单元测试中对未 Mock 的 Android API 返回默认值
    }
}

/**
 * Kotlin 编译目标设置：
 * 针对 Kotlin 2.0+ 使用新的 compilerOptions DSL。
 * 统一设置所有 Kotlin 任务的 JVM 目标版本，确保与 Java 编译选项 (17) 一致。
 */
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

/**
 * 依赖管理
 */
dependencies {
    // --- 核心复合架构 SDK (Composite Build) ---
    // 这两个是本地库项目，分别负责基础架构和 UI 组件，直接引用源代码进行编译
    implementation("com.listen:listen-arch")
    implementation("com.listen:listen-uicomponent")

    // --- Compose 系列依赖 (Jetpack Compose UI 框架) ---
    // 使用 BOM (Bill of Materials) 来统一管理 Compose 相关库的版本，确保它们相互兼容
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)                  // 让 Activity 支持 Compose
    implementation(libs.androidx.compose.material3)               // 现代化的 Material Design 3 组件库
    implementation(libs.androidx.compose.material.icons.core)     // 核心图标集 (如返回、菜单)
    implementation(libs.androidx.compose.material.icons.extended) // 扩展图标集 (包含数千个额外图标)

    // --- Room 数据库 (本地持久化存储) ---
    implementation(libs.androidx.room.runtime) // 数据库运行时核心
    implementation(libs.androidx.room.ktx)     // 让 Room 支持 Kotlin 协程和扩展
    ksp(libs.androidx.room.compiler)          // 注解处理器，负责在编译时生成数据库实现代码

    // --- 认证与凭据 (Google Auth / 登录与云端同步) ---
    implementation(libs.androidx.credentials)               // 统一凭据管理器 (指纹、密码、通行密钥)
    implementation(libs.androidx.credentials.play.services.auth) // 让凭据管理器支持 Google 账号登录
    implementation(libs.google.android.libraries.identity.googleid) // 获取 Google ID 信息的专用库
    implementation(libs.play.services.auth.base)            // GoogleAuthUtil 底层轻量授权核心 (不包含废弃的 GoogleSignInClient)

    // --- 核心库与 UI 增强工具 ---
    implementation(libs.androidx.compose.ui)           // Compose UI 布局引擎
    implementation(libs.androidx.compose.ui.graphics)  // 图形绘制工具 (颜色、画笔等)
    implementation(libs.androidx.core.ktx)             // 基础 Kotlin 扩展，简化 Android 原生 API 调用
    implementation(libs.androidx.biometric)            // 生物识别支持 (指纹、面部识别)
    implementation(libs.androidx.core.splashscreen) // Android 12+ 官方启动页适配方案
    implementation(libs.coil.compose)    // 高性能图片异步加载库 (Compose 专用版)
    implementation(libs.aboutlibraries.compose.m3)     // 自动从 Gradle 依赖中提取并显示第三方许可信息的 UI 库
    
    // --- 生命周期管理 (Lifecycle & ViewModel) ---
    implementation(libs.androidx.lifecycle.runtime.ktx)    // 监控 Activity/Fragment 生命周期并支持协程挂起
    implementation(libs.androidx.lifecycle.viewmodel.compose) // 让 Compose 能够轻松获取和持有 ViewModel 实例

    // --- 单元测试 (Local Unit Tests) ---
    testImplementation(libs.junit)                         // Java 测试框架标准
    testImplementation(libs.json)                         // 用于在测试环境下处理和解析 JSON 数据
    testImplementation(libs.kotlinx.coroutines.test)      // 专门用于测试 Kotlin 协程的工具
    testImplementation(libs.mockito.core)                 // 模拟对象框架，用于隔离测试
    testImplementation(libs.mockito.kotlin)               // Mockito 的 Kotlin 友好封装版本

    // --- UI 测试 (Android Instrumentation Tests) ---
    androidTestImplementation(platform(libs.androidx.compose.bom))  // 确保 UI 测试使用同样的 Compose 版本
    androidTestImplementation(libs.androidx.compose.ui.test.junit4) // Compose 布局测试工具
    androidTestImplementation(libs.androidx.espresso.core)          // 经典的点击、滑动等 UI 交互测试框架
    androidTestImplementation(libs.androidx.junit)                  // 针对 Android 环境优化的 JUnit
    
    // --- 调试与开发辅助工具 ---
    debugImplementation(libs.androidx.compose.ui.test.manifest) // 只有调试版才需要的 UI 测试声明
    debugImplementation(libs.androidx.compose.ui.tooling)        // 开启 IDE 内的预览 (Preview) 功能支持
}

/**
 * Jacoco 任务：生成单元测试覆盖率报告
 */
tasks.register<JacocoReport>("jacocoTestReport") {
    description = "生成本地单元测试的代码覆盖率报告"
    dependsOn("testDebugUnitTest") // 依赖于运行测试任务
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }

    // 过滤掉自动生成的类和资源文件，避免污染覆盖率统计
    val fileFilter = listOf("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*Test*.*", "android/**/*.*", "**/*_Impl*.*")
    
    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
        exclude(fileFilter)
    }
    val javacClasses = fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug/compileDebugJavaWithJavac/classes") {
        exclude(fileFilter)
    }
    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(kotlinClasses, javacClasses))
    executionData.setFrom(fileTree(layout.buildDirectory.get()) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec", "jacoco/testDebugUnitTest.exec")
    })
}
