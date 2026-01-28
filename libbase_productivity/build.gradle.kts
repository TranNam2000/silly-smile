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
            // Splash ads
            buildConfigField("String", "_101_spl_inter", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "_101_spl_inter_high", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "_101_spl_inter_medium", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "_102_spl_native", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_102_spl_native_high", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_103_spl_banner", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "_103_spl_banner_high", "\"ca-app-pub-3940256099942544/6300978111\"")

// Language/onboarding flow ads
            buildConfigField("String", "_201_lfo_native", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_201_lfo_native_high", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_202_lfo_native", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_202_lfo_native_max", "\"\"")
            buildConfigField("String", "_202_lfo_native_high", "\"ca-app-pub-3940256099942544/2247696110\"")

// Onboarding ads
            buildConfigField("String", "_301_onb_native", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_301_onb_native_high", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_302_onb_native", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_302_onb_native_high", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_302_onb_native_max", "\"\"")
            buildConfigField("String", "_303_onb_native", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_303_onb_native_high", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_304_onb_native", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_304_onb_native_high", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_305_onb_native", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_305_onb_native_high", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_306_onb_inter", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "_306_onb_inter_high", "\"ca-app-pub-3940256099942544/1033173712\"")

// Home and general ads
            buildConfigField("String", "_401_home_native", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_402_click_inter", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "_403_resume_open", "\"ca-app-pub-3940256099942544/5354046379\"")
            buildConfigField("String", "_404_home_native_banner", "\"ca-app-pub-3940256099942544/2247696110\"")
            buildConfigField("String", "_405_premium_reward", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "_406_unlock_reward", "\"ca-app-pub-3940256099942544/5224354917\"")

            // ss2 splash
            buildConfigField("String", "_101_v2_spl_inter", "\"\"")
            buildConfigField("String", "_102_v2_spl_native", "\"\"")
            buildConfigField("String", "_103_v2_spl_banner", "\"\"")
            buildConfigField("String", "_101_v2_spl_inter_high", "\"\"")
            buildConfigField("String", "_102_v2_spl_native_high", "\"\"")
            buildConfigField("String", "_103_v2_spl_banner_high", "\"\"")

            buildConfigField("Boolean", "env_dev", "true")
        }

        create("appProd") {
            buildConfigField("String", "key_max", "\"pmaJXAUhzJmbd_UelyvIXNuTfnk5SrmGRbENaSg47iJv8ETOKgTGVbrihsAOVaIF8csgk6LbGUf51PA5HuMpBO\"")
// Splash ads
            buildConfigField("String", "_101_spl_inter", "\"ca-app-pub-3886186147480382/3956285785\"")
            buildConfigField("String", "_101_spl_inter_high", "\"ca-app-pub-3886186147480382/5779846806\"")
            buildConfigField("String", "_101_spl_inter_medium", "\"ca-app-pub-3886186147480382/4275193442\"")
            buildConfigField("String", "_102_spl_native", "\"ca-app-pub-3886186147480382/2643204110\"")
            buildConfigField("String", "_102_spl_native_high", "\"ca-app-pub-3886186147480382/8329988520\"")
            buildConfigField("String", "_103_spl_banner", "\"\"")
            buildConfigField("String", "_103_spl_banner_high", "\"\"")

// Language/onboarding flow ads
            buildConfigField("String", "_201_lfo_native", "\"ca-app-pub-3886186147480382/9998949951\"")
            buildConfigField("String", "_201_lfo_native_high", "\"ca-app-pub-3886186147480382/5703825181\"")
            buildConfigField("String", "_202_lfo_native", "\"ca-app-pub-3886186147480382/1649030101\"")
            buildConfigField("String", "_202_lfo_native_max", "\"\"")
            buildConfigField("String", "_202_lfo_native_high", "\"ca-app-pub-3886186147480382/9527520124\"")

// Onboarding ads
            buildConfigField("String", "_301_onb_native", "\"ca-app-pub-3886186147480382/7398489734\"")
            buildConfigField("String", "_301_onb_native_high", "\"ca-app-pub-3886186147480382/7703959105\"")
            buildConfigField("String", "_302_onb_native", "\"ca-app-pub-3886186147480382/4493124794\"")
            buildConfigField("String", "_302_onb_native_high", "\"ca-app-pub-3886186147480382/3507346184\"")
            buildConfigField("String", "_302_onb_native_max", "\"\"")
            buildConfigField("String", "_303_onb_native", "\"\"")
            buildConfigField("String", "_303_onb_native_high", "\"\"")
            buildConfigField("String", "_304_onb_native", "\"ca-app-pub-3886186147480382/7372786617\"")
            buildConfigField("String", "_304_onb_native_high", "\"ca-app-pub-3886186147480382/5965988565\"")
            buildConfigField("String", "_305_onb_native", "\"ca-app-pub-3886186147480382/7007607490\"")
            buildConfigField("String", "_305_onb_native_high", "\"ca-app-pub-3886186147480382/6825335168\"")
            buildConfigField("String", "_306_onb_inter", "\"ca-app-pub-3886186147480382/3180043126\"")
            buildConfigField("String", "_306_onb_inter_high", "\"ca-app-pub-3886186147480382/8550136918\"")

// Home and general ads
            buildConfigField("String", "_401_home_native", "\"ca-app-pub-3886186147480382/7255019506\"")
            buildConfigField("String", "_402_click_inter", "\"ca-app-pub-3886186147480382/2770540084\"")
            buildConfigField("String", "_403_resume_open", "\"ca-app-pub-3886186147480382/9553879783\"")
            buildConfigField("String", "_404_home_native_banner", "\"ca-app-pub-3886186147480382/4743881518\"")
            buildConfigField("String", "_405_premium_reward", "\"ca-app-pub-3886186147480382/1124378313\"")
            buildConfigField("String", "_406_unlock_reward", "\"ca-app-pub-3886186147480382/8251881682\"")

// ss2 splash
            buildConfigField("String", "_101_v2_spl_inter", "\"\"")
            buildConfigField("String", "_102_v2_spl_native", "\"\"")
            buildConfigField("String", "_103_v2_spl_banner", "\"\"")
            buildConfigField("String", "_101_v2_spl_inter_high", "\"\"")
            buildConfigField("String", "_102_v2_spl_native_high", "\"\"")
            buildConfigField("String", "_103_v2_spl_banner_high", "\"\"")

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