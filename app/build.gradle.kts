import org.gradle.kotlin.dsl.implementation
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("androidx.navigation.safeargs.kotlin")
    id("jacoco")
}

// Define the helper function at the top level of the script
fun getSecret(key: String): String {
    // 1. Check for command line property (-PSTAGING_URL=...)
    val projectProp = project.findProperty(key)?.toString()
    if (!projectProp.isNullOrBlank()) return projectProp

    // 2. Check local.properties (mostly for local development)
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
        val localProp = localProperties.getProperty(key)
        if (!localProp.isNullOrBlank()) return localProp
    }

    // 3. Fallback
    return "0.0.0.0"
}

android {
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    namespace = "iss.nus.edu.sg.webviews.binitrightmobileapp"
    compileSdk = 36


    defaultConfig {
        applicationId = "iss.nus.edu.sg.webviews.binitrightmobileapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions.add("env")
    productFlavors {
        create("local") {
            dimension = "env"
            val ip = getSecret("LOCAL_IP") // 10.0.2.2
            buildConfigField("String", "BASE_URL", "\"http://$ip:8080/\"") }
        create("staging") {
            dimension = "env"
            val domain = getSecret("STAGING_URL") // e.g., staging.yourdomain.me
            // Now we include the ports explicitly with https
            buildConfigField("String", "BASE_URL", "\"https://$domain/\"") }
        create("production") {
            dimension = "env"
            val domain = getSecret("PROD_URL") // e.g., api.yourdomain.me
            buildConfigField("String", "BASE_URL", "\"https://$domain/\"")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.ink.geometry)
    val cameraxVersion = "1.3.1"
    val nav_version = "2.8.5"
    val retrofitVersion = "2.11.0"
    val okhttpVersion = "4.12.0"

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Navigation and Safe Args
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")

    // Retrofit & Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // OkHttp (Required for JWT Interceptors)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Image Loading (For those Unsplash recycling images)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Kotlin Coroutines for non-blocking API calls
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Material Design
    implementation(libs.material)
    
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")


    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")

    // CameraX
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-video:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Retrofit & Networking
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    implementation(libs.gson)

    // Google Play Services
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // ONNX Runtime
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.14.0")

    // Image Loading
    implementation(libs.coil)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.register<JacocoReport>("jacocoLocalDebugUnitTestReport") {
    group = "verification"
    description = "Generate JaCoCo coverage report for localDebug unit tests"

    dependsOn("testLocalDebugUnitTest")

    val excludes = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*\$Lambda\$*.*",
        "**/*\$inlined\$*.*"
    )

    val kotlinClasses = fileTree("$buildDir/tmp/kotlin-classes/localDebug") {
        exclude(excludes)
    }
    val javaClasses = fileTree("$buildDir/intermediates/javac/localDebug/compileLocalDebugJavaWithJavac/classes") {
        exclude(excludes)
    }

    classDirectories.setFrom(files(kotlinClasses, javaClasses))
    sourceDirectories.setFrom(files("src/main/java"))
    executionData.setFrom(
        fileTree(buildDir) {
            include("jacoco/testLocalDebugUnitTest.exec")
            include("outputs/unit_test_code_coverage/localDebugUnitTest/*.exec")
            include("outputs/unit_test_code_coverage/localDebugUnitTest/testLocalDebugUnitTest.exec")
        }
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}