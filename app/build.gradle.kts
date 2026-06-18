plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.firebase.crashlytics)

    id("com.google.gms.google-services")
}

import java.util.Properties

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { localProps.load(it) }
}

val releaseStoreFile = providers.gradleProperty("SAFAR_RELEASE_STORE_FILE")
    .orElse(providers.environmentVariable("SAFAR_RELEASE_STORE_FILE"))
    .orElse(providers.provider { localProps.getProperty("SAFAR_RELEASE_STORE_FILE") })
val releaseStorePassword = providers.gradleProperty("SAFAR_RELEASE_STORE_PASSWORD")
    .orElse(providers.environmentVariable("SAFAR_RELEASE_STORE_PASSWORD"))
    .orElse(providers.provider { localProps.getProperty("SAFAR_RELEASE_STORE_PASSWORD") })
val releaseKeyAlias = providers.gradleProperty("SAFAR_RELEASE_KEY_ALIAS")
    .orElse(providers.environmentVariable("SAFAR_RELEASE_KEY_ALIAS"))
    .orElse(providers.provider { localProps.getProperty("SAFAR_RELEASE_KEY_ALIAS") })
val releaseKeyPassword = providers.gradleProperty("SAFAR_RELEASE_KEY_PASSWORD")
    .orElse(providers.environmentVariable("SAFAR_RELEASE_KEY_PASSWORD"))
    .orElse(providers.provider { localProps.getProperty("SAFAR_RELEASE_KEY_PASSWORD") })
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it.isPresent }

fun normalizeBaseUrl(raw: String): String {
    var trimmed = raw.trim()
    if (trimmed.isEmpty()) return trimmed
    if (!trimmed.endsWith("/")) trimmed += "/"
    // Retrofit uses BASE_URL + "plans/..." — must end with api/
    if (!trimmed.endsWith("api/", ignoreCase = true)) {
        trimmed += "api/"
    }
    return trimmed
}

gradle.taskGraph.whenReady {
    // Only "real" release APK/AAB tasks need keystore — not intermediate `bundle*Release*Jar`
    // tasks (e.g. `bundleQaReleaseClassesToRuntimeJar`) that also match `bundle*`.
    val requiresReleaseSigning = allTasks.any { task ->
        task.path.startsWith(":app:") &&
            task.name.contains("Release") &&
            !task.name.contains("UnitTest") &&
            (task.name.startsWith("assemble") || task.name.startsWith("bundle")) &&
            task.name.endsWith("Release")
    }
    if (requiresReleaseSigning && !hasReleaseSigning) {
        logger.warn(
            "Release signing is not configured. Falling back to debug signing for this release build."
        )
    }
}

android {
    namespace = "com.safarparmar.app"
    compileSdk = 35
    val defaultApiRoot = "https://safar.parmarssc.in/"
    val apiBaseUrl = normalizeBaseUrl(defaultApiRoot)
    val qaBaseUrl = normalizeBaseUrl(
        providers.gradleProperty("SAFAR_QA_BASE_URL").orNull
            ?: providers.environmentVariable("SAFAR_QA_BASE_URL").orNull
            ?: localProps.getProperty("SAFAR_QA_BASE_URL")
            ?: defaultApiRoot,
    )
    val prodBaseUrl = apiBaseUrl
    val aiSyllabusImportEnabled = providers.gradleProperty("AI_SYLLABUS_IMPORT_ENABLED")
        .map { it.equals("true", ignoreCase = true).toString() }
        .orElse("true")
        .get()

    defaultConfig {
        applicationId = "com.safarparmar.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.5.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // KAVACH (FocusShield) accessibility service is a digital wellbeing feature.
        // It MUST remain in the prod manifest so Google can review & whitelist it.
        // Note: sideloaded installs may trigger Play Protect warnings — this is expected
        // for accessibility services from unknown sources and resolves after Play Store review.
        buildConfigField("boolean", "KAVACH_ACCESSIBILITY_ENABLED", "false")
    }

    flavorDimensions += "env"

    productFlavors {
        create("qa") {
            dimension = "env"
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            buildConfigField("String", "BASE_URL", "\"$qaBaseUrl\"")
            buildConfigField("boolean", "KAVACH_ACCESSIBILITY_ENABLED", "true")
            buildConfigField("boolean", "AI_SYLLABUS_IMPORT_ENABLED", aiSyllabusImportEnabled)
            manifestPlaceholders["allowBackup"] = "false"
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            resValue("string", "app_name", "Safar QA")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"$prodBaseUrl\"")
            buildConfigField("boolean", "KAVACH_ACCESSIBILITY_ENABLED", "false")
            buildConfigField("boolean", "AI_SYLLABUS_IMPORT_ENABLED", aiSyllabusImportEnabled)
            manifestPlaceholders["allowBackup"] = "false"
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            resValue("string", "app_name", "Safar")
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.google.material)

    //socket
    implementation("io.socket:socket.io-client:2.1.0")
    implementation("org.json:json:20231013")

    //animation
    implementation("androidx.compose.animation:animation:1.7.0")

    //video
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    //di
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    //networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.okhttp.urlconnection)

    //coroutine
    implementation(libs.kotlinx.coroutines.android)

    //offline store
    implementation(libs.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    //image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation("androidx.compose.ui:ui-text-google-fonts")

    //work manager
    implementation(libs.androidx.work.runtime.ktx)

    //firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.crashlytics)

    //system
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.permissions)
    
    //payments
    implementation("com.razorpay:checkout:1.6.38")

    //testing
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

val copyApksToOutputs = tasks.register<Copy>("copyApksToOutputs") {
    group = "build"
    description = "Copy built APKs into Safar_Android/Outputs/"
    from(layout.buildDirectory.dir("outputs/apk"))
    include("**/*.apk")
    into(rootProject.file("Outputs"))
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

listOf(
    "assembleProdRelease",
    "assembleQaRelease",
    "assembleProdDebug",
    "assembleQaDebug",
).forEach { taskName ->
    tasks.matching { it.name == taskName }.configureEach {
        finalizedBy(copyApksToOutputs)
    }
}
