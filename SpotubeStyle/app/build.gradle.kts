import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.application")
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    jvm("desktop") { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            implementation("io.ktor:ktor-client-core:2.3.12")
            implementation("io.coil-kt.coil3:coil-compose:3.0.4")
        }
        androidMain.dependencies {
            implementation(project(":newpipe-bridge"))
            implementation("androidx.activity:activity-compose:1.9.3")
            implementation("androidx.media3:media3-exoplayer:1.4.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
            implementation("io.ktor:ktor-client-okhttp:2.3.12")
            implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
        }
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(project(":newpipe-bridge"))
            implementation(compose.desktop.currentOs)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
            implementation("io.ktor:ktor-client-okhttp:2.3.12")
            implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
            implementation("uk.co.caprica:vlcj:4.8.3")
        }
    }
}

android {
    namespace = "app.spotubestyle"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.spotubestyle"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}", "META-INF/DEPENDENCIES", "META-INF/INDEX.LIST",
            "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        )
    }
}

dependencies { coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2") }

compose.desktop {
    application {
        mainClass = "app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi)
            packageName = "SpotubeStyle"
            packageVersion = "1.0.0"
            modules("java.net.http", "jdk.httpserver", "jdk.unsupported")
        }
    }
}
