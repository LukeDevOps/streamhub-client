# Kotlinx Serialization — keep @Serializable data classes and their generated serializers
-keepattributes *Annotation*, InnerClasses
-keep @kotlinx.serialization.Serializable class ** { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** { *; }
-keep class **$$serializer { *; }

# Ktor — reflection-based internals used by the Android engine and content negotiation
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Coil — OkHttp disk-cache and image-pipeline internals
-dontwarn okio.**
-dontwarn com.squareup.okhttp3.**

# Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
