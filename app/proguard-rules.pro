# Add project specific ProGuard rules here.
# Keep Room entities and DAOs
-keep class com.phonewhisperer.data.local.db.entity.** { *; }
-keep class com.phonewhisperer.data.local.db.dao.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep data classes used in Room queries
-keepclassmembers class com.phonewhisperer.data.local.db.dao.EventTypeCount { *; }
-keepclassmembers class com.phonewhisperer.data.local.db.dao.AppUsageSummary { *; }
-keepclassmembers class com.phonewhisperer.data.local.db.dao.AppFrequencyInfo { *; }
