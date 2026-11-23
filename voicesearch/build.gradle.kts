import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    `maven-publish`
}

group = "com.voicesearch"
version = "0.1.0"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        publishLibraryVariants("release")
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "VoiceSearch"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidUnitTest.dependencies {
            implementation(libs.kotlin.testJunit)
        }
    }
}

android {
    namespace = "com.voicesearch"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("Voice Search")
            description.set(
                "Compose Multiplatform SDK for hold-to-talk voice search: speech capture on " +
                    "Android and iOS, a review/edit sheet, and themable strings and colors.",
            )
            url.set("https://github.com/krajarajan96/Voice-Search")
            licenses {
                license {
                    name.set("MIT")
                    url.set("https://github.com/krajarajan96/Voice-Search/blob/main/LICENSE")
                }
            }
        }
    }
}
