# ProGuard rules for RealPredict AI
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class com.realpredict.ai.domain.model.** { *; }
-keep class com.realpredict.ai.data.local.entity.** { *; }
-dontwarn org.apache.poi.**
