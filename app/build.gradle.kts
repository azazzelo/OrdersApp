plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compilerKsp)
}

android {
    namespace = "com.example.ordersapp"
    compileSdk = 35 // Убедись, что targetSdk тоже 35 или выше, если compileSdk 35

    defaultConfig {
        applicationId = "com.example.ordersapp"
        minSdk = 24
        targetSdk = 35 // Должно соответствовать или быть <= compileSdk
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
        // Для AGP 8.x рекомендуется использовать JavaVersion.VERSION_17 или выше,
        // но оставим 11, если у тебя все собирается.
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
    // Убираем packagingOptions/packaging, так как iText удален
}

dependencies {
    // Основные зависимости
    implementation(libs.glide)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.room) // Room KTX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity) // Activity KTX
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle) // Lifecycle LiveData KTX

    // KSP для Room
    ksp(libs.androidx.room.compiler)

    // --- Unit Тесты ---
    testImplementation(libs.junit) // JUnit 4 для src/test
    // testImplementation(kotlin("test")) // Можно убрать, если не используешь kotlin.test напрямую

    // --- Инструментальные Тесты (androidTest) ---
    androidTestImplementation(libs.androidx.junit) // AndroidX Test - JUnit4 extensions
    androidTestImplementation(libs.androidx.espresso.core) // Espresso Core

    // AndroidX Test Runner и Rules (УБЕДИСЬ, ЧТО ВЕРСИИ АКТУАЛЬНЫ)
    androidTestImplementation("androidx.test:runner:1.5.2") // Версия из твоего libs.versions.toml может быть другой, если ты ее там определил
    androidTestImplementation("androidx.test:rules:1.5.0")   // Версия из твоего libs.versions.toml может быть другой

    // Espresso Contrib для RecyclerView и др. (УБЕДИСЬ, ЧТО ВЕРСИЯ АКТУАЛЬНА)
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1") // Используем версию, совместимую с espresso-core (libs.androidx.espresso.core)

    // --- Зависимости для тестов Room (если будешь писать тесты DAO) ---
    // androidTestImplementation(libs.androidx.room.testing) // Alias из libs.versions.toml для androidx.room:room-testing
    // androidTestImplementation("androidx.test:core-ktx:1.5.0") // Для ApplicationProvider

    // --- Зависимости для тестов корутин (если будешь писать тесты DAO) ---
    // androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}