# Voice by Spark — keep essential classes for assistant integration
-keep class com.feathertech.voice.assistant.** { *; }
-keepclassmembers class * extends android.service.voice.VoiceInteractionService {
    public <init>(...);
}
-keepclassmembers class * extends android.service.voice.VoiceInteractionSession {
    public <init>(...);
}
