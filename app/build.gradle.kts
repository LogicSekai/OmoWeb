// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Logic Sekai and OmoWeb contributors. See NOTICE.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Release and beta builds are signed with <root>/keystore.properties (scripts\signing-setup.bat).
// Without it they fall back to the debug key: fine for sideloading, not for Google Play.
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasReleaseKeystore = keystoreProps.getProperty("storeFile")?.let { rootProject.file(it).exists() } == true

// ---- Distribution ----------------------------------------------------------------------------
// "github" (default): APKs published on GitHub Releases. They check for updates, can download and
// install new versions, and show the donation links.
// "play": bundles for Google Play (scripts\bundle-*.bat pass -Pomo.distribution=play). The store
// handles updates, so self-update and its install permission are removed, and donation links are
// hidden because Play's payments policy does not allow them outside Play billing.
val playBuild = (findProperty("omo.distribution") as String?) == "play"
fun quoted(value: String) = "\"$value\""

android {
    namespace = "com.logicsekai.omoweb"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.logicsekai.omoweb"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = (findProperty("omo.versionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("omo.versionName") as String?) ?: "0.0.0"

        // Links shown on the phone screen. Leave a value empty to hide it.
        buildConfigField("String", "UPDATE_REPO", quoted(if (playBuild) "" else "LogicSekai/OmoWeb"))
        buildConfigField("String", "SOURCE_URL", quoted("https://github.com/LogicSekai/OmoWeb"))
        buildConfigField("String", "COMMUNITY_URL", quoted("https://logicsekai.com/omo/omoweb"))
        buildConfigField("String", "SPONSOR_URL", quoted(if (playBuild) "" else "https://github.com/sponsors/LogicSekai"))
        buildConfigField("String", "KOFI_URL", quoted(if (playBuild) "" else "https://ko-fi.com/logicsekai"))
        buildConfigField("String", "TRAKTEER_URL", quoted(if (playBuild) "" else "https://trakteer.id/kwaii"))
        // Release builds from GitHub download and install updates themselves; debug builds
        // (different package) only link to the release page.
        buildConfigField("boolean", "SELF_UPDATE", "false")
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
            buildConfigField("boolean", "SELF_UPDATE", (!playBuild).toString())
        }
        create("beta") {
            initWith(getByName("release"))
            versionNameSuffix = "-beta"
            // Beta bundles only go to Play testing tracks.
            buildConfigField("String", "UPDATE_REPO", quoted(""))
            buildConfigField("boolean", "SELF_UPDATE", "false")
        }
    }

    // Play bundles: drop the self-update install permission (see src/play/AndroidManifest.xml).
    sourceSets.getByName("beta").manifest.srcFile("src/play/AndroidManifest.xml")
    if (playBuild) {
        sourceSets.getByName("release").manifest.srcFile("src/play/AndroidManifest.xml")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    bundle {
        language { enableSplit = false }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    // Background update checks (update/UpdateCheckWorker.kt).
    implementation(libs.androidx.work.runtime.ktx)
    // Not called from code: its manifest entries (car-app host query, permission activity) are
    // part of what Android Auto checks before listing a phone app in Customize launcher.
    implementation(libs.androidx.car.app)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.test)
}
