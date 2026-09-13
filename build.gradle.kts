/**
 * 根级构建脚本：在此处统一声明所有子模块（如 :app）共用的插件及其版本。
 * 这里使用 'apply false' 是为了仅统一定义版本，而不在根项目运行这些逻辑，具体的启用由各子模块负责。
 */
plugins {
    // Android 应用插件：构建 Android 应用程序的核心插件。
    // 它负责处理 APK 的打包、签名、资源编译（AAPT2）以及所有 Android 特有的构建任务。
    id("com.android.application") version "9.2.1" apply false

    // Kotlin Compose 编译器插件：专门为 Jetpack Compose 准备的 Kotlin 编译器扩展。
    // 从 Kotlin 2.0 开始，Compose 编译器集成在 Kotlin 插件中，负责 @Composable 的转换和性能优化。
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false

    // KSP (Kotlin Symbol Processing) 插件：现代化的 Kotlin 符号处理工具。
    // 它是 kapt 的高性能替代者。常用于 Room、Moshi、Dagger 等框架的代码生成，能显著提升编译效率。
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}
