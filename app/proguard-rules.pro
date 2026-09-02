# ProGuard & R8 Obfuscation Rules for DEVID CHANGER

# Repackage all internal classes into root package to hide structure
-repackageclasses ''
-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses

# Remove debug info and source file names
-renamesourcefileattribute ""
-keepattributes !SourceFile,!LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable

# Default optimizations. Kotlin metadata & signature are needed for Compose runtime.
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!class/unboxing/enum

# Keep Compose Runtime methods
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Android System Entry Points
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

# Keep ViewModel base (needs default no-arg constructor via reflection in navigation3)
-keep public class * extends androidx.lifecycle.ViewModel {
    public <init>();
    public void onCleared();
}

# Keep serialization plugin generated serializers for data models
-keepclassmembers class com.warungerik.devidchanger.model.** {
    *** Companion;
}
-keepattributes *Annotation*
-keepclassmembers class com.warungerik.devidchanger.model.** {
    *** kotlinx.serialization.**;
}

# --- Anti-reverse-engineering hardening ---

# Rename (obfuscate) the internal engine & network classes that contain the
# injection logic and the sensitive API endpoint. This makes static
# decompilation substantially harder because the class/method names become
# meaningless (a, b, c...) and the bytecode is harder to trace by hand.
-keep,allowobfuscation class com.warungerik.devidchanger.engine.** { *; }
-keep,allowobfuscation class com.warungerik.devidchanger.root.** { *; }
-keep,allowobfuscation class com.warungerik.devidchanger.network.** { *; }

# Force obfuscation to also rename methods and fields (not just classes) inside
# the sensitive packages. Without this, R8 may keep them at their original name.
-keepclassmembers,allowobfuscation class com.warungerik.devidchanger.engine.** { *; }
-keepclassmembers,allowobfuscation class com.warungerik.devidchanger.root.** { *; }
-keepclassmembers,allowobfuscation class com.warungerik.devidchanger.network.** { *; }

