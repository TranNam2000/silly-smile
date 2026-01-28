# Keep all data classes and their properties
-keep class com.nomyek.myapplication.data.** { *; }
-keep class com.nomyek.myapplication.ui.**.* { *; }

# Keep all Entity classes (Room)
-keep @androidx.room.Entity class * { *; }

# Keep all Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
    *;
}

# Gson specific rules
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class * extends com.google.gson.TypeAdapter

# Keep Gson annotations
-keepattributes *Annotation*
-keep class com.google.gson.annotations.** { *; }

# Keep classes with @SerializedName
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep generic signature of TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep classes used with Gson
-keep class * {
    @com.google.gson.annotations.Expose <fields>;
}

# Application classes that will be serialized/deserialized over Gson
-keep class com.nomyek.myapplication.data.model.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# Keep all attributes for reflection
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Don't warn about missing classes
-dontwarn **

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}