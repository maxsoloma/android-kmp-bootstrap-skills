package bootstrap

import kotlinx.serialization.json.*

fun defaultUi(kind: String) = if (kind == "android") "android-compose" else "shared-compose"
val componentPins by lazy { Json.parseToJsonElement(Resources.text("components.json")).jsonObject }

fun stackIssues(kind: String, targets: List<String>, ui: String, di: String, navigation: String, ios: String): List<Issue> = buildList {
    fun issue(field: String, message: String, code: String = "incompatible") { add(Issue(field, code, message)) }
    if (ui !in listOf("android-compose", "shared-compose", "swiftui")) issue("ui", "Use android-compose, shared-compose or swiftui", "unsupported_choice")
    else if (ui == "swiftui") {
        if (kind != "kmp" || "ios" !in targets || targets.any { it !in listOf("android", "ios") })
            issue("ui", "SwiftUI requires modern KMP with iOS, optionally Android; remove desktop/web or choose shared-compose")
    } else if (ui != defaultUi(kind)) issue("ui", "Kind $kind requires ${defaultUi(kind)}${if (kind == "kmp") " or swiftui for native mobile UI" else ""}")
    if (di !in listOf("manual", "koin")) issue("di", if (di == "hilt") "Hilt is not supported; use manual or Koin" else "Use manual or koin", "unsupported_choice")
    when (navigation) {
        "none" -> Unit
        "navigation3" -> if (ui == "swiftui") issue("navigation", "Navigation 3 owns Compose screens; choose platform-native for SwiftUI or none")
        "platform-native" -> {
            if (ui != "swiftui") issue("navigation", "platform-native requires SwiftUI; choose navigation3 for Compose screens")
            if ((ios.substringBefore('.').toIntOrNull() ?: 0) < 16) issue("ios_deployment_target", "SwiftUI NavigationStack requires iOS 16.0+; raise the minimum or choose navigation none")
        }
        else -> issue("navigation", "Use none, navigation3 or platform-native", "unsupported_choice")
    }
}

fun stackCatalog(): Map<String, Any> = mapOf(
    "ui" to listOf(
        mapOf("id" to "android-compose", "profile" to "modern", "kind" to "android", "targets" to listOf("android")),
        mapOf("id" to "shared-compose", "profile" to "modern", "kind" to "kmp", "targets" to platforms),
        mapOf("id" to "swiftui", "profile" to "modern", "kind" to "kmp", "targets" to listOf("android", "ios"), "requires" to listOf("ios"), "scope" to "Shared Kotlin logic; Android Compose host; native SwiftUI iOS host"),
    ),
    "di" to listOf(
        mapOf("id" to "manual", "profile" to "modern", "targets" to platforms, "scope" to "Shared logic or native Android"),
        mapOf("id" to "koin", "profile" to "modern", "targets" to platforms, "scope" to "Isolated shared logic graph or native Android", "version" to componentPins.text("koin")),
    ),
    "navigation" to listOf(
        mapOf("id" to "none", "profile" to "modern", "targets" to platforms),
        mapOf("id" to "navigation3", "profile" to "modern", "ui" to listOf("android-compose", "shared-compose"), "targets" to platforms, "version" to componentPins.text("navigation3"), "scope" to "Compose screen back stack; browser history integration is not included"),
        mapOf("id" to "platform-native", "profile" to "modern", "ui" to listOf("swiftui"), "targets" to listOf("android", "ios"), "ios_minimum" to "16.0", "scope" to "SwiftUI NavigationStack + Android Navigation 3"),
    ),
    "pins" to componentPins,
    "network" to listOf(
        mapOf("id" to "none", "profile" to "modern", "targets" to platforms),
        mapOf("id" to "ktor", "profile" to "modern", "targets" to platforms, "version" to componentPins.text("ktor"), "engines" to networkAdapters(platforms), "requires" to "HTTPS endpoint; browser CORS and connectivity checked at runtime"),
    ),
    "storage" to listOf(
        mapOf("id" to "none", "profile" to "modern", "targets" to platforms),
        mapOf("id" to "settings", "profile" to "modern", "targets" to platforms, "version" to componentPins.text("settings"), "adapters" to storageAdapters(platforms), "scope" to "Small non-secret key-value data; not SQL or credential storage"),
    ),
    "verification" to "Availability describes implemented recipes. Run generated checks; SwiftUI app compilation requires full Xcode. No build success is inferred from catalog.",
)
