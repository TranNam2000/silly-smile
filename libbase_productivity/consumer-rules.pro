# Consumer ProGuard rules for onboarding module

# Keep public API classes that will be used by the app module
-keep public class com.yeknom.aitranslate.onboarding.** { *; }

# Keep Firebase and Ads related classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.ads.nomyek_admob.** { *; }
-keep class co.ab180.airbridge.** { *; }




