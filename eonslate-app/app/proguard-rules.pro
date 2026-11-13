# Keep kotlinx.serialization metadata for navigation destinations.
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class dev.qtremors.eonslate.**$$serializer { *; }
-keepclassmembers class dev.qtremors.eonslate.** {
    *** Companion;
}
