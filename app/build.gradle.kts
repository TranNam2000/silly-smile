plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.nomyek.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "silly.smile.wallpaper.live"
        minSdk = 24
        targetSdk = 36
        versionCode = 1030
        versionName = "1.0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
        }
    }

    flavorDimensions += listOf("adIds")
    productFlavors {
        create("appDev") {
            // Use test ID during development
            manifestPlaceholders["ad_app_id"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "key_max", "\"pmaJXAUhzJmbd_UelyvIXNuTfnk5SrmGRbENaSg47iJv8ETOKgTGVbrihsAOVaIF8csgk6LbGUf51PA5HuMpBO\"")

            buildConfigField("String", "mail_app_key", "\"${project.findProperty("MAIL_APP_KEY_TEST")}\"")
            buildConfigField("String", "mail_app_address", "\"${project.findProperty("MAIL_APP_ADDRESS_TEST")}\"")
            buildConfigField("Boolean", "env_dev", "true")
        }

        create("appProd") {
            // Add your production ad IDs here
            manifestPlaceholders["ad_app_id"] = "ca-app-pub-3886186147480382~1288376955"
            buildConfigField("String", "key_max", "\"pmaJXAUhzJmbd_UelyvIXNuTfnk5SrmGRbENaSg47iJv8ETOKgTGVbrihsAOVaIF8csgk6LbGUf51PA5HuMpBO\"")

            buildConfigField("String", "mail_app_key", "\"${project.findProperty("MAIL_APP_KEY_LIVE")}\"")
            buildConfigField("String", "mail_app_address", "\"${project.findProperty("MAIL_APP_ADDRESS_LIVE")}\"")
            buildConfigField("Boolean", "env_dev", "true")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/NOTICE.md"
        }
    }
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                val timestamp = System.currentTimeMillis() / 1000
                outputFileName =
                    "Silly Smile${variant.flavorName}_${variant.buildType.name}_${variant.versionCode}_${timestamp}.apk"
            }
        }
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
}

dependencies {
    implementation(project(":libbase_productivity"))
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.messaging)

    // Ads SDK (required by libbase_productivity)
    implementation(files("../libbase_productivity/libs/nomyek_admob-release.aar"))
    implementation(files("../libbase_productivity/libs/pageindicatorview-release.aar"))
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.fragment.ktx)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    kapt(libs.hilt.android.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Gson
    implementation(libs.gson)

    // Glide
    implementation(libs.bumptech.glide)

    // GIF Encoder
    implementation("com.squareup:gifencoder:0.10.0")

    // Lottie
    implementation("com.airbnb.android:lottie:6.1.0")

    // Shimmer
    implementation(libs.shimmer)

    // Airbridge
    implementation(libs.sdk.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    /// mediation admob
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

    //mediation max
    implementation(libs.applovin.sdk)
    implementation(libs.applovin.google.adapter)
    implementation(libs.applovin.facebook.adapter)
    implementation(libs.applovin.bytedance.adapter)
    implementation(libs.applovin.mintegral.adapter)
    implementation(libs.applovin.unity.adapter)
    implementation(libs.facebook.sdk)
}   