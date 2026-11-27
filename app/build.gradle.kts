plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "om.sstvsuite"
    compileSdk = 36

    defaultConfig {
        applicationId = "om.sstvsuite"
        minSdk = 23
        targetSdk = 36
        versionCode = 8
        versionName = "1.1.3"
    }

    signingConfigs {
        create("release") {
            // 密钥不进仓库。本地把 sstv-mochi.jks 放在仓库根目录即可（.gitignore 已排除）；
            // CI 从 secret 还原到 runner 临时目录，再通过 SSTV_KEYSTORE_PATH 指过来。
            val envPath = System.getenv("SSTV_KEYSTORE_PATH")
            val ksFile = if (envPath != null) file(envPath) else rootProject.file("sstv-mochi.jks")
            storeFile = ksFile
            storePassword = System.getenv("SSTV_KEYSTORE_PASSWORD") ?: "sstvmochi"
            keyAlias = System.getenv("SSTV_KEY_ALIAS") ?: "sstv"
            keyPassword = System.getenv("SSTV_KEY_PASSWORD") ?: "sstvmochi"
            if (!ksFile.exists()) {
                logger.warn("签名密钥不存在: ${ksFile.absolutePath} —— assembleRelease 会失败")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    lint { abortOnError = false; checkReleaseBuilds = false }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    // Robot36 的高质量解码器（FFT 解调），由 dex2jar 从 Robot36 2.16 的 dex 反编译打包而来
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("io.github.feice11:mochi-compose:0.2.0")
    implementation("io.github.feice11:mochi-icons:0.2.0")
}
