# Field configs are stored as JSON through kotlinx.serialization. Keep the generated serializers
# of the sealed config classes so release builds can still read and write them.
-keep,includedescriptorclasses class dev.qtremors.eonslate.core.tracker.domain.**$$serializer { *; }
-keepclassmembers class dev.qtremors.eonslate.core.tracker.domain.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class dev.qtremors.eonslate.core.tracker.domain.** {
    public static ** INSTANCE;
}
