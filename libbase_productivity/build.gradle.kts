plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.jrm"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    viewBinding {
        enable = true
    }

    dataBinding {
        enable = true
        addKtx = true
    }
    
    buildFeatures {
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }
    
    flavorDimensions("adIds")
    productFlavors {
        create("appDev") {
            // Development Ad IDs (test IDs)
            buildConfigField("String", "key_max", "\"pmaJXAUhzJmbd_UelyvIXNuTfnk5SrmGRbENaSg47iJv8ETOKgTGVbrihsAOVaIF8csgk6LbGUf51PA5HuMpBO\"")
            buildConfigField("String", "_102_spl_native", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("Boolean", "env_dev", "true")
        }

        create("appProd") {
            buildConfigField("String", "key_max", "\"pmaJXAUhzJmbd_UelyvIXNuTfnk5SrmGRbENaSg47iJv8ETOKgTGVbrihsAOVaIF8csgk6LbGUf51PA5HuMpBO\"")
            buildConfigField("String", "_102_spl_native", "\"ca-app-pub-3886186147480382/2643204110\"")

            buildConfigField("Boolean", "env_dev", "false")
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // UI Libraries
    implementation(libs.google.material)
    implementation(libs.intuit.sdp)
    implementation(libs.airbnb.lottie)
    implementation(libs.bumptech.glide)
    implementation(libs.dotsindicator)
    
    // Navigation
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.messaging)
    
    // Ads SDK (from libs folder) - use compileOnly to avoid packaging in AAR
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    
    // Other SDKs
    implementation(libs.sdk.android)
    implementation(libs.gson)
    implementation(libs.androidx.work)
    
    // AdMob Mediation
    implementation(libs.unity.ads)
    implementation(libs.unity.mediation)
    implementation(libs.vungle.mediation)
    implementation(libs.facebook.mediation)
    implementation(libs.applovin.mediation)
    implementation(libs.pangle.mediation)
    implementation(libs.mintegral.mediation)
    implementation(libs.inmobi.mediation)
    implementation(libs.ironsource.mediation)
    implementation(libs.moloco.mediation)
    
    // AppLovin MAX
    implementation(libs.applovin.sdk)
    implementation(libs.applovin.google.adapter)
    implementation(libs.applovin.facebook.adapter)
    implementation(libs.applovin.bytedance.adapter)
    implementation(libs.applovin.mintegral.adapter)
    implementation(libs.applovin.unity.adapter)
    implementation(libs.facebook.sdk)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}