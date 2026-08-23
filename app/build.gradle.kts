import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("jacoco")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore/keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.listen.expensetracker"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.listen.expensetracker"
        minSdk = 24
        targetSdk = 36

        val vName = "0.0.6"
        versionName = vName

        // Auto-generate versionCode: major.minor.patch -> major * 10000 + minor * 100 + patch (e.g., 0.0.1 -> 1, 1.2.3 -> 10203)
        versionCode = try {
            val parts = vName.split(".")
            val major = parts.getOrNull(0)?.toInt() ?: 0
            val minor = parts.getOrNull(1)?.toInt() ?: 0
            val patch = parts.getOrNull(2)?.filter { it.isDigit() }?.toInt() ?: 0
            major * 10000 + minor * 100 + patch
        } catch (e: Exception) {
            1
        }

        println(">>> Calculated Auto-Increment versionCode: $versionCode (versionName: $versionName)")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

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
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Core Composite SDKs
    implementation("com.listen:listen-arch")
    implementation("com.listen:listen-uicomponent")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }
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