# Keep Native OCR JNI Bridge
-keep class com.yuanshi.moldocr.recognition.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Room schemas and entities
-keep class androidx.room.** { *; }
-keep class com.seniordict.app.data.db.** { *; }

# Keep ONNX Runtime
-keep class ai.onnxruntime.** { *; }

# Keep Compose Runtime
-keep class androidx.compose.** { *; }
