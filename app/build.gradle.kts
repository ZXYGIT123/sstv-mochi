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
        versionCode = 4
        versionName = "1.0.3-mochi"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("sstv-mochi.jks")
            storePassword = "sstvmochi"
            keyAlias = "sstv"
            keyPassword = "sstvmochi"
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
