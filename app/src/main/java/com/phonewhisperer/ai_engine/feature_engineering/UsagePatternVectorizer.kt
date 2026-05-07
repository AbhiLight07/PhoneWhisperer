package com.phonewhisperer.ai_engine.feature_engineering

/**
 * Converts raw Android package names into behavioral categories.
 *
 * Instead of clustering on raw package names (com.spotify.music, com.instagram.android),
 * we vectorize usage into semantic categories:
 *   SOCIAL, PRODUCTIVITY, ENTERTAINMENT, MUSIC, EDUCATION, COMMUNICATION, etc.
 *
 * This dramatically improves DBSCAN cluster quality because semantically
 * similar apps (WhatsApp + Telegram) group together instead of being treated
 * as entirely different signals.
 */
object UsagePatternVectorizer {

    /**
     * App category definitions.
     * Maps known package prefixes to semantic categories.
     */
    private val CATEGORY_MAP = mapOf(
        // Social Media
        "com.instagram" to "SOCIAL",
        "com.twitter" to "SOCIAL",
        "com.facebook" to "SOCIAL",
        "com.snapchat" to "SOCIAL",
        "com.reddit" to "SOCIAL",
        "com.tumblr" to "SOCIAL",
        "com.linkedin" to "SOCIAL",
        "com.pinterest" to "SOCIAL",
        "org.telegram" to "SOCIAL",
        "com.tiktok" to "SOCIAL",

        // Communication
        "com.whatsapp" to "COMMUNICATION",
        "com.google.android.apps.messaging" to "COMMUNICATION",
        "com.discord" to "COMMUNICATION",
        "com.skype" to "COMMUNICATION",
        "us.zoom" to "COMMUNICATION",
        "com.google.android.apps.meet" to "COMMUNICATION",
        "com.Slack" to "COMMUNICATION",
        "com.microsoft.teams" to "COMMUNICATION",

        // Entertainment
        "com.netflix" to "ENTERTAINMENT",
        "com.amazon.avod" to "ENTERTAINMENT",
        "com.disney" to "ENTERTAINMENT",
        "com.hbo" to "ENTERTAINMENT",
        "in.startv.hotstar" to "ENTERTAINMENT",
        "com.jio" to "ENTERTAINMENT",

        // Music & Audio
        "com.spotify" to "MUSIC",
        "com.google.android.apps.youtube.music" to "MUSIC",
        "com.apple.android.music" to "MUSIC",
        "com.gaana" to "MUSIC",
        "com.jiosaavn" to "MUSIC",
        "com.soundcloud" to "MUSIC",
        "com.audible" to "MUSIC",

        // Video
        "com.google.android.youtube" to "VIDEO",
        "com.vimeo" to "VIDEO",
        "tv.twitch" to "VIDEO",

        // Productivity
        "com.google.android.apps.docs" to "PRODUCTIVITY",
        "com.google.android.apps.sheets" to "PRODUCTIVITY",
        "com.google.android.apps.slides" to "PRODUCTIVITY",
        "com.microsoft.office" to "PRODUCTIVITY",
        "com.notion" to "PRODUCTIVITY",
        "com.todoist" to "PRODUCTIVITY",
        "com.google.android.keep" to "PRODUCTIVITY",

        // Education
        "com.google.android.apps.classroom" to "EDUCATION",
        "com.duolingo" to "EDUCATION",
        "com.byjus" to "EDUCATION",
        "com.khan" to "EDUCATION",
        "com.udemy" to "EDUCATION",
        "com.coursera" to "EDUCATION",

        // Finance
        "com.google.android.apps.walletnfcrel" to "FINANCE",
        "net.one97.paytm" to "FINANCE",
        "com.phonepe" to "FINANCE",
        "com.google.android.apps.nbu" to "FINANCE",

        // Navigation
        "com.google.android.apps.maps" to "NAVIGATION",
        "com.waze" to "NAVIGATION",
        "com.uber" to "NAVIGATION",
        "com.olacabs" to "NAVIGATION",

        // Health
        "com.google.android.apps.fitness" to "HEALTH",
        "com.strava" to "HEALTH",

        // Games
        "com.supercell" to "GAMING",
        "com.kiloo" to "GAMING",
        "com.king" to "GAMING",
        "com.miniclip" to "GAMING",
        "com.rovio" to "GAMING",

        // System
        "com.android.settings" to "SYSTEM",
        "com.android.phone" to "SYSTEM",
        "com.android.camera" to "SYSTEM",
        "com.google.android.dialer" to "SYSTEM",
        "com.google.android.contacts" to "SYSTEM"
    )

    /**
     * Returns the behavioral category for a package name.
     * Uses prefix matching to handle variant packages (e.g., com.instagram.lite).
     */
    fun categorize(packageName: String): String {
        // Exact or prefix match
        for ((prefix, category) in CATEGORY_MAP) {
            if (packageName.startsWith(prefix)) return category
        }

        // Heuristic fallback based on common package patterns
        return when {
            packageName.contains("game", ignoreCase = true) -> "GAMING"
            packageName.contains("music", ignoreCase = true) -> "MUSIC"
            packageName.contains("video", ignoreCase = true) -> "VIDEO"
            packageName.contains("chat", ignoreCase = true) -> "COMMUNICATION"
            packageName.contains("social", ignoreCase = true) -> "SOCIAL"
            packageName.contains("pay", ignoreCase = true) -> "FINANCE"
            packageName.contains("learn", ignoreCase = true) -> "EDUCATION"
            packageName.contains("fitness", ignoreCase = true) -> "HEALTH"
            packageName.contains("news", ignoreCase = true) -> "NEWS"
            else -> "OTHER"
        }
    }

    /**
     * Returns a human-readable label for a category.
     */
    fun getCategoryDisplayName(category: String): String = when (category) {
        "SOCIAL" -> "Social Media"
        "COMMUNICATION" -> "Communication"
        "ENTERTAINMENT" -> "Entertainment"
        "MUSIC" -> "Music & Audio"
        "VIDEO" -> "Video"
        "PRODUCTIVITY" -> "Productivity"
        "EDUCATION" -> "Education"
        "FINANCE" -> "Finance"
        "NAVIGATION" -> "Navigation"
        "HEALTH" -> "Health & Fitness"
        "GAMING" -> "Gaming"
        "NEWS" -> "News"
        "SYSTEM" -> "System"
        else -> "Other"
    }

    /**
     * Returns a category similarity score between two packages.
     * Same category = 0.0, different = 1.0.
     */
    fun categoryDistance(pkg1: String, pkg2: String): Double {
        val cat1 = categorize(pkg1)
        val cat2 = categorize(pkg2)
        return if (cat1 == cat2) 0.0 else 1.0
    }
}
