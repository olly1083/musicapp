import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins { kotlin("jvm") }
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
dependencies {
    api("com.github.TeamNewPipe:NewPipeExtractor:v0.24.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
