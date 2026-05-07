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
        // ── Social Media ─────────────────────────────────────────────
        "com.instagram" to "SOCIAL",
        "com.twitter" to "SOCIAL",
        "com.facebook.katana" to "SOCIAL",
        "com.facebook.lite" to "SOCIAL",
        "com.facebook.orca" to "SOCIAL",      // Messenger
        "com.snapchat" to "SOCIAL",
        "com.reddit" to "SOCIAL",
        "com.tumblr" to "SOCIAL",
        "com.linkedin" to "SOCIAL",
        "com.pinterest" to "SOCIAL",
        "com.tiktok" to "SOCIAL",
        "com.zhiliaoapp.musically" to "SOCIAL", // TikTok alternate package
        "app.bsky" to "SOCIAL",                // Bluesky
        "com.bereal" to "SOCIAL",

        // ── Communication ────────────────────────────────────────────
        "com.whatsapp" to "COMMUNICATION",
        "com.whatsapp.w4b" to "COMMUNICATION", // WhatsApp Business
        "org.telegram.messenger" to "COMMUNICATION",
        "org.thunderdog.chalern" to "COMMUNICATION", // Telegram X
        "com.google.android.apps.messaging" to "COMMUNICATION",
        "com.discord" to "COMMUNICATION",
        "com.skype.raider" to "COMMUNICATION",
        "com.skype.m2" to "COMMUNICATION",
        "us.zoom.videomeetings" to "COMMUNICATION",
        "com.google.android.apps.meet" to "COMMUNICATION",
        "com.google.android.apps.tachyon" to "COMMUNICATION", // Google Duo
        "com.Slack" to "COMMUNICATION",
        "com.microsoft.teams" to "COMMUNICATION",
        "com.viber.voip" to "COMMUNICATION",
        "com.imo.android.imoim" to "COMMUNICATION",
        "jp.naver.line.android" to "COMMUNICATION",
        "com.google.android.gm" to "COMMUNICATION", // Gmail
        "com.microsoft.office.outlook" to "COMMUNICATION",
        "com.yahoo.mobile.client.android.mail" to "COMMUNICATION",

        // ── Browsers ─────────────────────────────────────────────────
        "com.android.chrome" to "BROWSER",
        "com.chrome.beta" to "BROWSER",
        "com.chrome.canary" to "BROWSER",
        "org.mozilla.firefox" to "BROWSER",
        "com.opera.browser" to "BROWSER",
        "com.opera.mini.native" to "BROWSER",
        "com.brave.browser" to "BROWSER",
        "com.microsoft.emmx" to "BROWSER",     // Edge
        "com.UCMobile.intl" to "BROWSER",       // UC Browser
        "com.sec.android.app.sbrowser" to "BROWSER", // Samsung Internet
        "com.vivaldi.browser" to "BROWSER",
        "com.duckduckgo.mobile.android" to "BROWSER",

        // ── Entertainment ────────────────────────────────────────────
        "com.netflix.mediaclient" to "ENTERTAINMENT",
        "com.amazon.avod.thirdpartyclient" to "ENTERTAINMENT", // Prime Video
        "com.disney.disneyplus" to "ENTERTAINMENT",
        "com.hbo.hbonow" to "ENTERTAINMENT",
        "in.startv.hotstar" to "ENTERTAINMENT", // Hotstar/JioCinema
        "com.jio.jioplay.tv" to "ENTERTAINMENT",
        "com.sonyliv" to "ENTERTAINMENT",
        "com.voot.android" to "ENTERTAINMENT",
        "com.balaji.alt" to "ENTERTAINMENT",    // ALTBalaji
        "com.mxtech.videoplayer" to "ENTERTAINMENT", // MX Player
        "com.zee5.hiburan" to "ENTERTAINMENT",

        // ── Music & Audio ────────────────────────────────────────────
        "com.spotify.music" to "MUSIC",
        "com.google.android.apps.youtube.music" to "MUSIC",
        "com.apple.android.music" to "MUSIC",
        "com.gaana.android" to "MUSIC",
        "com.jio.media.jiobeats" to "MUSIC",    // JioSaavn
        "com.jiosaavn" to "MUSIC",
        "com.soundcloud.android" to "MUSIC",
        "com.audible.application" to "MUSIC",
        "com.amazon.mp3" to "MUSIC",
        "com.shazam.android" to "MUSIC",
        "fm.castbox.audiobook.radio.podcast" to "MUSIC",

        // ── Video ────────────────────────────────────────────────────
        "com.google.android.youtube" to "VIDEO",
        "com.vimeo.android.videoapp" to "VIDEO",
        "tv.twitch.android.app" to "VIDEO",
        "com.dailymotion.dailymotion" to "VIDEO",
        "com.google.android.apps.youtube.creator" to "VIDEO",

        // ── Productivity ─────────────────────────────────────────────
        "com.google.android.apps.docs" to "PRODUCTIVITY",
        "com.google.android.apps.docs.editors.docs" to "PRODUCTIVITY",
        "com.google.android.apps.docs.editors.sheets" to "PRODUCTIVITY",
        "com.google.android.apps.docs.editors.slides" to "PRODUCTIVITY",
        "com.microsoft.office.word" to "PRODUCTIVITY",
        "com.microsoft.office.excel" to "PRODUCTIVITY",
        "com.microsoft.office.powerpoint" to "PRODUCTIVITY",
        "com.microsoft.office.onenote" to "PRODUCTIVITY",
        "com.notion.android" to "PRODUCTIVITY",
        "com.todoist" to "PRODUCTIVITY",
        "com.google.android.keep" to "PRODUCTIVITY",
        "com.google.android.calendar" to "PRODUCTIVITY",
        "com.google.android.apps.tasks" to "PRODUCTIVITY",
        "com.google.android.deskclock" to "PRODUCTIVITY",
        "com.google.android.apps.drive" to "PRODUCTIVITY",

        // ── Education ────────────────────────────────────────────────
        "com.google.android.apps.classroom" to "EDUCATION",
        "com.duolingo" to "EDUCATION",
        "com.byjus.thelearningapp" to "EDUCATION",
        "com.khan" to "EDUCATION",
        "com.udemy.android" to "EDUCATION",
        "com.coursera.app" to "EDUCATION",
        "com.unacademy.unacademylearningapp" to "EDUCATION",
        "co.extramarks" to "EDUCATION",

        // ── Finance & Payments ───────────────────────────────────────
        "com.google.android.apps.walletnfcrel" to "FINANCE", // Google Wallet
        "com.google.android.apps.nbu.paisa.user" to "FINANCE", // Google Pay
        "net.one97.paytm" to "FINANCE",
        "com.phonepe.app" to "FINANCE",
        "in.amazon.mShop.android.shopping" to "FINANCE", // Amazon (also shopping)
        "com.cred.android" to "FINANCE",
        "com.groww.android" to "FINANCE",
        "com.zerodha.kite3" to "FINANCE",
        "com.mymoneykarma" to "FINANCE",

        // ── Shopping ─────────────────────────────────────────────────
        "com.flipkart.android" to "SHOPPING",
        "in.amazon.mShop.android" to "SHOPPING",
        "com.myntra.android" to "SHOPPING",
        "com.ajio.retail" to "SHOPPING",
        "club.cred" to "SHOPPING",
        "com.meesho.supply" to "SHOPPING",
        "com.ebay.mobile" to "SHOPPING",

        // ── Food & Delivery ──────────────────────────────────────────
        "com.application.zomato" to "FOOD",
        "in.swiggy.android" to "FOOD",
        "com.ubercab.eats" to "FOOD",
        "com.done.faasos" to "FOOD",            // EatSure
        "com.blinkit.groceriesorder" to "FOOD",
        "com.bigbasket.mobileapp" to "FOOD",
        "com.ninjacart.supply" to "FOOD",

        // ── Navigation & Travel ──────────────────────────────────────
        "com.google.android.apps.maps" to "NAVIGATION",
        "com.waze" to "NAVIGATION",
        "com.ubercab" to "NAVIGATION",
        "com.olacabs.customer" to "NAVIGATION",
        "com.rapido.passenger" to "NAVIGATION",
        "in.redbus.android" to "NAVIGATION",
        "com.cleartrip.android" to "NAVIGATION",
        "com.makemytrip" to "NAVIGATION",

        // ── Health & Fitness ─────────────────────────────────────────
        "com.google.android.apps.fitness" to "HEALTH",
        "com.strava" to "HEALTH",
        "com.myfitnesspal.android" to "HEALTH",
        "com.cure.fit.cult" to "HEALTH",
        "com.practo.fabric" to "HEALTH",

        // ── Photography ──────────────────────────────────────────────
        "com.google.android.apps.photos" to "PHOTOGRAPHY",
        "com.android.camera2" to "PHOTOGRAPHY",
        "com.sec.android.app.camera" to "PHOTOGRAPHY",  // Samsung Camera
        "com.canva.editor" to "PHOTOGRAPHY",
        "com.adobe.lrmobile" to "PHOTOGRAPHY",           // Lightroom

        // ── News ─────────────────────────────────────────────────────
        "com.google.android.apps.magazines" to "NEWS",   // Google News
        "com.inshorts.android" to "NEWS",
        "com.dailyhunt.tv" to "NEWS",
        "com.bbc.mundo" to "NEWS",

        // ── Gaming ───────────────────────────────────────────────────
        "com.supercell" to "GAMING",
        "com.kiloo" to "GAMING",
        "com.king" to "GAMING",
        "com.miniclip" to "GAMING",
        "com.rovio" to "GAMING",
        "com.epicgames.fortnite" to "GAMING",
        "com.garena.game.freefire" to "GAMING",
        "com.tencent.ig" to "GAMING",           // BGMI/PUBG
        "com.dts.freefireth" to "GAMING",
        "com.activision.callofduty.shooter" to "GAMING",

        // ── System ───────────────────────────────────────────────────
        "com.android.settings" to "SYSTEM",
        "com.android.phone" to "SYSTEM",
        "com.android.camera" to "SYSTEM",
        "com.google.android.dialer" to "SYSTEM",
        "com.google.android.contacts" to "SYSTEM",
        "com.samsung.android.app.phone" to "SYSTEM",     // Samsung Phone
        "com.samsung.android.contacts" to "SYSTEM"        // Samsung Contacts
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
            packageName.contains("bank", ignoreCase = true) -> "FINANCE"
            packageName.contains("learn", ignoreCase = true) -> "EDUCATION"
            packageName.contains("fitness", ignoreCase = true) -> "HEALTH"
            packageName.contains("news", ignoreCase = true) -> "NEWS"
            packageName.contains("browser", ignoreCase = true) -> "BROWSER"
            packageName.contains("shop", ignoreCase = true) -> "SHOPPING"
            packageName.contains("food", ignoreCase = true) -> "FOOD"
            packageName.contains("photo", ignoreCase = true) -> "PHOTOGRAPHY"
            packageName.contains("camera", ignoreCase = true) -> "PHOTOGRAPHY"
            packageName.contains("travel", ignoreCase = true) -> "NAVIGATION"
            packageName.contains("map", ignoreCase = true) -> "NAVIGATION"
            else -> "OTHER"
        }
    }

    /**
     * Returns a human-readable label for a category.
     */
    fun getCategoryDisplayName(category: String): String = when (category) {
        "SOCIAL" -> "Social Media"
        "COMMUNICATION" -> "Communication"
        "BROWSER" -> "Browser"
        "ENTERTAINMENT" -> "Entertainment"
        "MUSIC" -> "Music & Audio"
        "VIDEO" -> "Video"
        "PRODUCTIVITY" -> "Productivity"
        "EDUCATION" -> "Education"
        "FINANCE" -> "Finance"
        "SHOPPING" -> "Shopping"
        "FOOD" -> "Food & Delivery"
        "NAVIGATION" -> "Navigation"
        "HEALTH" -> "Health & Fitness"
        "PHOTOGRAPHY" -> "Photography"
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
