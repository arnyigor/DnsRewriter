plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.arny.dnsrewriter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.arny.dnsrewriter"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // --- ОБНОВИТЕ ДО JAVA 17 ---
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        // --- ОБНОВИТЕ ДО "17" ---
        jvmTarget = "17"
    }
    composeOptions {
        // Указываем компилятору Kotlin, какую версию Compose использовать
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // Jetpack Compose (оставляем как было)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.security.crypto)

    implementation(libs.okhttp.core)
    implementation(libs.okhttp.loggingInterceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.urlconnection)
    implementation(libs.jsoup)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler) // Используем ksp вместо kapt

    // ViewModel для Compose
    implementation(libs.lifecycle.viewmodel.compose)

    // DNS-парсер
    implementation(libs.slf4j.nop)

    // Тестовые зависимости (оставляем как было)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}