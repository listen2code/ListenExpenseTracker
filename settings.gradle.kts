/**
 * 插件管理：定义 Gradle 插件的搜索仓库和过滤规则
 */
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

/**
 * 设置插件：引入用于自动解析和下载所需 JDK 版本的插件
 */
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

/**
 * 依赖解析管理：统一配置项目中所有模块（如 :app）的依赖仓库
 */
@Suppress("UnstableApiUsage") // 忽略 Gradle 孵化期 API (Incubating) 的警告，这些 API 在未来版本可能会有变动
dependencyResolutionManagement {
    // 强制要求所有模块使用此处定义的仓库，禁止在子模块中单独定义，确保依赖来源唯一可控
    // 提示：repositoriesMode 及其相关配置在当前 Gradle 版本中仍被标记为 Incubating
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// 定义根项目名称
rootProject.name = "ListenExpenseTracker"
// 包含主应用模块
include(":app")

/**
 * 复合构建 (Composite Build)：
 * 将外部独立的库项目（位于项目根目录之外）包含在当前构建中。
 * 这样做的好处是：当你修改这些库的代码时，主应用会立即生效，无需发布到远程仓库。
 */
// 引入核心架构库
includeBuild("../ListenArch") {
    dependencySubstitution {
        // 将远程/模块依赖替换为本地项目，实现无缝调试
        substitute(module("com.listen:listen-arch")).using(project(":listen-arch"))
    }
}
// 引入 UI 组件库
includeBuild("../ListenUiComponent") {
    dependencySubstitution {
        substitute(module("com.listen:listen-uicomponent")).using(project(":listen-uicomponent"))
    }
}
