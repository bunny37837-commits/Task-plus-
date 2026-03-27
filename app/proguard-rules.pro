-keep class com.taskpulse.app.** { *; }
-keepattributes *Annotation*
-keepclassmembers class ** {
    @dagger.hilt.android.AndroidEntryPoint *;
}
