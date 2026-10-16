# Add project specific ProGuard rules here.
-keep class org.tensorflow.lite.** { *; }
-keep class com.google.android.odml.image.** { *; }
-keep class coil.** { *; }
-dontwarn okhttp3.**
-dontwarn com.google.auto.value.**
