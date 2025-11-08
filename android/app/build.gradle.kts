plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.mindlog"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mindlog"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.example.mindlog.HiltTestRunner"

        buildConfigField("String", "API_BASE_URL", "\"http://ec2-15-164-239-56.ap-northeast-2.compute.amazonaws.com:3000/api/v1/\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://ec2-15-164-239-56.ap-northeast-2.compute.amazonaws.com:3000/api/v1/\"")
            // buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3000/api/v1/\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"http://ec2-15-164-239-56.ap-northeast-2.compute.amazonaws.com:3000/api/v1/\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
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
    // ---- Runtime (app) ----
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.fragment:fragment-ktx:1.7.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // ✅ SavedState 추가 (Fragment / Nav 최신 버전 호환)
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")
    androidTestImplementation("androidx.savedstate:savedstate-ktx:1.2.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Dependency for visualization
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.patrykandpatrick.vico:views:1.14.0")
    implementation("com.patrykandpatrick.vico:compose:1.14.0")
    implementation("androidx.webkit:webkit:1.10.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")

    // Hilt Navigation
    implementation("androidx.hilt:hilt-navigation-fragment:1.2.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // ---- Unit Test (Local JVM) ----
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")

    // ---- Instrumented Test + UI (androidTest) ----
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.7")
    androidTestImplementation("com.google.truth:truth:1.4.4")

    // Hilt Testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.52")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.52")

    androidTestImplementation(libs.androidx.fragment.testing)
    androidTestImplementation(libs.androidx.espresso.contrib)

    // Network Integration Test
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    testImplementation(kotlin("test"))
}

kapt {
    correctErrorTypes = true
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    }
}