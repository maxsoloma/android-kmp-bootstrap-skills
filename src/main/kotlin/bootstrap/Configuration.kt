package bootstrap

import kotlinx.serialization.json.*
import java.nio.file.Path

const val SPEC_VERSION = 2
const val CLI_VERSION = "3.1.0"
val agentChoices = listOf("codex", "claude", "gemini", "copilot", "cursor", "none")

data class Issue(val field: String, val code: String, val message: String) {
    fun document() = mapOf("field" to field, "code" to code, "message" to message)
}
data class Resolution(val options: Options?, val spec: JsonObject?, val issues: List<Issue>) {
    val valid get() = issues.isEmpty() && options != null
    fun failure() = mapOf("valid" to false, "missing_fields" to issues.filter { it.code == "required" }.map { it.field }, "issues" to issues.map { it.document() })
}
fun normalizedSpec(o: Options): JsonObject = value(linkedMapOf<String, Any>(
    "schema_version" to SPEC_VERSION, "build_system" to "kotlin-toolchain", "name" to o.name, "package" to o.pkg, "app_id" to o.appId,
    "kind" to o.kind, "ui" to o.ui, "di" to o.di, "navigation" to o.navigation,
    "network" to o.network, "storage" to o.storage,
    "targets" to o.targets, "agents" to o.agents,
    "sdk" to mapOf("min" to o.min, "target" to o.target, "compile" to o.compile),
    "ios_deployment_target" to o.ios,
).apply {
    o.networkUrl?.let { put("network_url", it) }
    o.firebase?.let { put("firebase", it.document()) }
}).jsonObject

fun resolveSpec(input: JsonObject, output: Path): Resolution {
    val issues = mutableListOf<Issue>()
    fun issue(field: String, code: String, message: String) { issues += Issue(field, code, message) }
    fun checkKeys(obj: JsonObject, allowed: Set<String>, prefix: String = "") {
        (obj.keys - allowed).forEach { issue(prefix + it, "unknown_field", "Unknown configuration field: $prefix$it") }
    }
    checkKeys(input, setOf("schema_version", "build_system", "name", "package", "app_id", "profile", "kind", "targets", "agents", "sdk", "ios_deployment_target", "feature", "modules", "ui", "di", "navigation", "network", "storage", "network_url", "firebase"))
    fun string(key: String, default: String? = null): String? {
        val v = input[key] ?: return default
        if (v !is JsonPrimitive || !v.isString) { issue(key, "invalid_type", "$key must be a string"); return default }
        return v.content
    }
    fun integer(obj: JsonObject, key: String, default: Int, prefix: String = ""): Int {
        val v = obj[key] ?: return default
        if (v !is JsonPrimitive || v.isString || v.intOrNull == null) { issue(prefix + key, "invalid_type", "$prefix$key must be an integer"); return default }
        return v.int
    }
    fun list(key: String, default: List<String>): List<String> {
        val v = input[key] ?: return default
        if (v !is JsonArray || v.any { it !is JsonPrimitive || !it.isString }) { issue(key, "invalid_type", "$key must be an array of strings"); return default }
        return v.map { it.jsonPrimitive.content }.distinct()
    }
    if (integer(input, "schema_version", SPEC_VERSION) !in 1..SPEC_VERSION) issue("schema_version", "unsupported_version", "Supported schema versions: 1 (modern choices), 2")
    if (string("build_system", "kotlin-toolchain") != "kotlin-toolchain") issue("build_system", "retired_backend", "Only Kotlin Toolchain is supported; Gradle generation was removed in 3.0")
    if (string("profile", "modern") != "modern") issue("profile", "retired_profile", "Hardened profiles were removed in 3.0; choose a modern recipe")
    for (field in listOf("feature", "modules")) if (field in input) issue(field, "retired_option", "Hardened feature/modules options were removed in 3.0")
    val name = string("name"); val pkg = string("package")
    for ((key, v) in listOf("name" to name, "package" to pkg)) if (v.isNullOrBlank() && issues.none { it.field == key }) issue(key, "required", "Provide $key")
    val kind = string("kind", "kmp")!!
    if (kind !in listOf("android", "kmp")) issue("kind", "unsupported_choice", "Use android or kmp")
    val targets = list("targets", if (kind == "android") listOf("android") else listOf("android", "ios"))
    val agents = list("agents", listOf("codex", "claude"))
    if (targets.isEmpty() || targets.any { it !in platforms }) issue("targets", "unsupported_choice", "Select one or more of ${platforms.joinToString(",")}")
    if (kind == "android" && targets != listOf("android")) issue("targets", "incompatible", "Native Android supports only android; use kind kmp for other platforms")
    if (agents.isEmpty() || agents.any { it !in agentChoices } || "none" in agents && agents.size != 1) issue("agents", "incompatible", "Select coding agents or none on its own")
    val ui = string("ui", defaultUi(kind))!!; val di = string("di", "manual")!!
    val navigation = string("navigation", "none")!!; val network = string("network", "none")!!; val storage = string("storage", "none")!!
    val networkUrl = string("network_url", demoEndpoint.takeIf { network == "ktor" })
    issues += dataIssues(network, storage, networkUrl)
    val sdk = input["sdk"]?.let { if (it !is JsonObject) { issue("sdk", "invalid_type", "sdk must be an object"); JsonObject(emptyMap()) } else it } ?: JsonObject(emptyMap())
    checkKeys(sdk, setOf("min", "target", "compile"), "sdk.")
    val min = integer(sdk, "min", 26, "sdk."); val target = integer(sdk, "target", 37, "sdk."); val compile = integer(sdk, "compile", 37, "sdk.")
    val ios = string("ios_deployment_target", "15.0")!!
    issues += stackIssues(kind, targets, ui, di, navigation, ios)
    if (min < 23 || min > target || target > compile || compile > 37) issue("sdk", "incompatible", "Require 23 <= min <= target <= compile <= 37")
    if ("android" in targets && compile != 37) issue("sdk.compile", "incompatible", "Pinned Compose libraries require compile SDK 37")
    val appId = string("app_id", pkg)
    val firebase = resolveFirebase(input["firebase"], targets, appId ?: "", issues)
    if (issues.isNotEmpty()) return Resolution(null, null, issues)
    val flags = mutableListOf(output.toString(), "--name", name!!, "--package", pkg!!, "--app-id", appId!!,
        "--kind", kind, "--targets", targets.joinToString(","), "--agents", agents.joinToString(","),
        "--min-sdk", "$min", "--target-sdk", "$target", "--compile-sdk", "$compile", "--ios-deployment-target", ios,
        "--ui", ui, "--di", di, "--navigation", navigation, "--network", network, "--storage", storage)
    networkUrl?.let { flags += listOf("--network-url", it) }
    return try { val options = parseGenerate(flags).copy(firebase = firebase); Resolution(options, normalizedSpec(options), emptyList()) }
    catch (e: Exception) { Resolution(null, null, listOf(Issue("configuration", "invalid_configuration", e.message ?: "Invalid configuration"))) }
}

data class Preset(val id: String, val label: String, val kind: String, val targets: List<String>, val ui: String = defaultUi(kind)) {
    fun defaults(): JsonObject = value(mapOf("kind" to kind, "targets" to targets, "ui" to ui)).jsonObject
}
val presets = listOf(
    Preset("android", "Android / Jetpack Compose", "android", listOf("android")),
    Preset("mobile-shared-ui", "Mobile / Shared Compose UI", "kmp", listOf("android", "ios")),
    Preset("universal-compose", "Universal Compose / choose platforms", "kmp", platforms),
    Preset("native-mobile-ui", "Native Mobile UI / Android Compose + SwiftUI", "kmp", listOf("android", "ios"), "swiftui"),
)
fun catalogDocument(): JsonObject = value(mapOf(
    "schema_version" to SPEC_VERSION, "generator_version" to CLI_VERSION, "build_system" to "kotlin-toolchain", "toolchain" to toolchain,
    "required_fields" to listOf("name", "package"),
    "fields" to mapOf(
        "schema_version" to "integer; default 2; modern v1 choices accepted", "build_system" to "kotlin-toolchain only",
        "name" to "required PascalCase identifier", "package" to "required dotted lowercase non-keyword identifiers", "app_id" to "defaults to package; no underscores for iOS",
        "kind" to "kmp (default) or android", "targets" to "array; default android,ios for kmp; android for native Android",
        "ui" to "android-compose for android; shared-compose (default) or swiftui for kmp",
        "di" to "manual (default) or koin", "navigation" to "none (default), navigation3 for Compose, platform-native for SwiftUI (iOS 16+)",
        "network" to "none (default) or ktor", "storage" to "none (default) or settings", "network_url" to "Ktor-only HTTPS URL, no userinfo/fragment; defaults to demo endpoint",
        "agents" to "array; codex,claude by default; none alone", "sdk" to "object {min,target,compile}; defaults 26/37/37; Android compile 37",
        "ios_deployment_target" to "string; default 15.0; NavigationStack needs 16.0+",
        "firebase" to "optional object; services array, project_id, android_config/ios_config paths or embedded android/ios configs"),
    "firebase" to firebaseCatalog(),
    "presets" to presets.map { mapOf("id" to it.id, "label" to it.label, "defaults" to it.defaults()) },
    "agents" to agentChoices, "stack_choices" to stackCatalog(),
    "retired" to listOf("Gradle backend", "hardened profiles", "feature/modules options"),
)).jsonObject
fun catalog(args: List<String>): Int {
    val a = Arguments(args, emptySet(), setOf("json")); require(a.positional.isEmpty()) { "catalog takes no positional arguments" }
    print(encode(catalogDocument())); return 0
}
