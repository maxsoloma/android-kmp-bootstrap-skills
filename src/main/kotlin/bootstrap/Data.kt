package bootstrap

import java.net.URI

const val demoEndpoint = "https://httpbin.org/uuid"
fun dataIssues(network: String, storage: String, url: String?): List<Issue> = buildList {
        if (network !in listOf("none", "ktor")) add(Issue("network", "unsupported_choice", "Use none or ktor; a modern Retrofit recipe is not implemented"))
        if (storage !in listOf("none", "settings")) add(Issue("storage", "unsupported_choice", "Use none or settings for key-value data; modern SQL/DataStore recipes are not implemented"))
    if (url != null) {
        if (network != "ktor") add(Issue("network_url", "incompatible", "An endpoint requires network ktor; remove it explicitly when disabling networking"))
        val valid = runCatching { URI(url).let { it.scheme.equals("https", ignoreCase = true) && !it.host.isNullOrBlank() && it.rawUserInfo == null && it.rawFragment == null && (it.port == -1 || it.port in 1..65535) } }.getOrDefault(false)
        if (!valid) add(Issue("network_url", "invalid_endpoint", "Use an absolute HTTPS URL with a host, no credentials or fragment"))
    }
}
fun networkAdapters(targets: List<String>) = targets.associateWith { when (it) { "ios" -> "Darwin"; "web" -> "Js (WasmJS fetch)"; else -> "CIO" } }
fun storageAdapters(targets: List<String>) = targets.associateWith { when (it) { "android" -> "SharedPreferencesSettings (application context)"; "ios" -> "NSUserDefaultsSettings (app suite)"; "web" -> "StorageSettings (origin localStorage)"; else -> "PropertiesSettings (app-specific file)" } }
fun selectedPins(o: Options) = componentPins.filterKeys {
    (it == "koin" && o.di == "koin") ||
    (it in listOf("navigation3", "serialization") && (o.navigation == "navigation3" || o.navigation == "platform-native" && "android" in o.targets)) ||
    (it == "ktor" && o.network == "ktor") || (it == "settings" && o.storage == "settings") || (it == "coroutines" && o.hasData)
}
