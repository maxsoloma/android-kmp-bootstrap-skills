package bootstrap

import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

val firebaseServices = listOf("analytics", "messaging", "remote-config")

data class FirebaseOptions(val services: List<String>, val projectId: String?, val android: JsonObject?, val ios: JsonObject?) {
    fun document(): JsonObject = value(linkedMapOf<String, Any>("services" to services).apply {
        projectId?.let { put("project_id", it) }; android?.let { put("android", it) }; ios?.let { put("ios", it) }
    }).jsonObject
}

fun firebaseCatalog() = mapOf(
    "services" to firebaseServices, "targets" to listOf("android", "ios"),
    "scope" to "Native mobile SDKs in app hosts; desktop/Wasm hosts remain independent",
    "configuration" to "Optional firebase object: services (nonempty array), project_id, android_config (JSON path), ios_config (plist path). Normalized specs embed public android/ios config for portable replay.",
    "provisioning" to "Agent uses Firebase MCP or optional Firebase CLI to select/create a project, register apps and fetch configs. Generation is offline; missing configs are reported CONFIG_MISSING.",
    "unsupported" to "Crashlytics needs build ID/mapping and dSYM upload integration; Auth/Firestore/Storage and desktop/web Firebase recipes are not implemented",
)

fun firebaseStatus(o: Options): Any = o.firebase?.let { f -> mapOf(
    "services" to f.services, "project_id" to f.projectId,
    "configuration" to o.targets.filter { it in listOf("android", "ios") }.associateWith {
        if (if (it == "android") f.android != null else f.ios != null) "CONFIGURED" else "CONFIG_MISSING"
    },
    "live_verification" to "NOT_RUN",
    "requirements" to buildList {
        add("Firebase/Google Analytics project setup and console receipt checks are separate from SDK configuration")
        if ("messaging" in f.services) add("Notification permission, FID registration with your backend and a real-device delivery check")
        if ("ios" in o.targets) add("Xcode 26.2+; Swift Package Manager resolves Firebase ${componentPins.text("firebase_apple")}")
        if ("ios" in o.targets && "messaging" in f.services) add("Apple team signing, Push Notifications capability and APNs key uploaded in Firebase Console")
    },
) } ?: "none"

/** File paths are input-only; the reviewed spec/lock can be replayed without the original files. */
fun readSpec(path: Path): JsonObject {
    val spec = readObject(path)
    val firebase = spec["firebase"] as? JsonObject ?: return spec
    return JsonObject(spec + ("firebase" to JsonObject(firebase.mapValues { (key, v) ->
        if (key in listOf("android_config", "ios_config") && v is JsonPrimitive && v.isString)
            JsonPrimitive(path.parent.resolve(v.content).normalize().toString()) else v
    })))
}

private fun readConfig(path: String): ByteArray {
    val file = Path.of(path)
    require(Files.isRegularFile(file) && Files.size(file) <= 1_048_576) { "Firebase config must be a regular file of at most 1 MiB" }
    return Files.readAllBytes(file)
}

private fun readPlist(bytes: ByteArray): JsonObject {
    val factory = DocumentBuilderFactory.newInstance().apply {
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
        setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        isXIncludeAware = false; isExpandEntityReferences = false
    }
    val root = factory.newDocumentBuilder().parse(bytes.inputStream()).documentElement
    require(root.tagName == "plist") { "Expected a Firebase XML plist" }
    val dict = root.getElementsByTagName("dict").item(0) as? Element ?: error("Expected a plist dictionary")
    val elements = (0 until dict.childNodes.length).mapNotNull { dict.childNodes.item(it) as? Element }
    require(elements.size % 2 == 0) { "Invalid plist dictionary" }
    val entries = linkedMapOf<String, JsonElement>()
    elements.chunked(2).forEach { (key, v) ->
        require(key.tagName == "key" && key.textContent !in entries) { "Invalid or duplicate plist key" }
        entries[key.textContent] = when (v.tagName) {
            "string" -> JsonPrimitive(v.textContent)
            "true" -> JsonPrimitive(true)
            "false" -> JsonPrimitive(false)
            else -> error("Firebase plist values must be strings or booleans")
        }
    }
    return JsonObject(entries)
}

fun resolveFirebase(input: JsonElement?, targets: List<String>, appId: String, issues: MutableList<Issue>): FirebaseOptions? {
    if (input == null) return null
    fun issue(field: String, message: String) { issues += Issue("firebase.$field", "invalid_configuration", message) }
    if (input !is JsonObject) { issue("", "firebase must be an object; omit it to disable Firebase"); return null }
    val allowed = setOf("services", "project_id", "android_config", "ios_config", "android", "ios")
    (input.keys - allowed).forEach { issue(it, "Unknown Firebase field: $it") }
    val services = (input["services"] as? JsonArray)?.takeIf { a -> a.all { it is JsonPrimitive && it.isString } }?.map { it.jsonPrimitive.content }?.distinct()
    if (services.isNullOrEmpty() || services.any { it !in firebaseServices }) issue("services", "Choose a nonempty array from ${firebaseServices.joinToString()}")
    if (targets.none { it == "android" || it == "ios" }) issue("services", "Firebase recipes require Android or iOS; desktop and Wasm SDKs are not implemented")
    val project = input["project_id"]?.let {
        if (it !is JsonPrimitive || !it.isString || !it.content.matches(Regex("[a-z][a-z0-9-]{4,28}[a-z0-9]"))) {
            issue("project_id", "Use the actual Firebase project ID (6-30 lowercase letters, digits and hyphens)"); null
        } else it.content
    }
    fun config(platform: String): JsonObject? = try {
        require("${platform}_config" !in input || platform !in input) { "Use either ${platform}_config or embedded $platform, not both" }
        val result = input["${platform}_config"]?.let {
            require(it is JsonPrimitive && it.isString) { "${platform}_config must be a file path" }
            val bytes = readConfig(it.content)
            if (platform == "android") Json.parseToJsonElement(bytes.toString(Charsets.UTF_8)) as? JsonObject ?: error("Expected a JSON object") else readPlist(bytes)
        } ?: input[platform]?.let { it as? JsonObject ?: error("$platform must be an object") }
        if (result != null) {
            require(platform in targets) { "Remove $platform config or select the platform" }
            require(project != null) { "project_id is required with a downloaded Firebase config" }
            if (platform == "android") {
                require(result.keys.all { it in listOf("project_info", "client", "configuration_version") }) { "Expected a public google-services.json, not service account credentials" }
                val info = result.getValue("project_info").jsonObject
                require(info.text("project_id") == project) { "Android config project does not match project_id" }
                require(info.text("project_number").matches(Regex("[0-9]+"))) { "Missing numeric project_number" }
                val clients = result.getValue("client").jsonArray.map { it.jsonObject }.filter {
                    it["client_info"]?.jsonObject?.get("android_client_info")?.jsonObject?.get("package_name")?.jsonPrimitive?.content == appId
                }
                require(clients.size == 1) { "google-services.json must contain exactly one client for application ID $appId" }
                val client = clients.single()
                require(client.getValue("client_info").jsonObject.text("mobilesdk_app_id").matches(Regex("1:[0-9]+:android:[A-Za-z0-9]+"))) { "Invalid Android mobilesdk_app_id" }
                require(client.getValue("client_info").jsonObject.text("mobilesdk_app_id").split(':')[1] == info.text("project_number")) { "Android app ID project number does not match project_number" }
                require(client.getValue("api_key").jsonArray.any { it.jsonObject["current_key"]?.jsonPrimitive?.content?.isNotBlank() == true }) { "Missing Android API key" }
            } else {
                require(result.values.all { it is JsonPrimitive && (it.isString || it.booleanOrNull != null) }) { "Invalid plist values" }
                require(result.keys.none { it.lowercase().contains("private") }) { "Use public GoogleService-Info.plist, not credentials" }
                require(result.text("PROJECT_ID") == project) { "iOS config project does not match project_id" }
                require(result.text("BUNDLE_ID") == appId) { "iOS config bundle ID does not match application ID $appId" }
                require(result.text("GOOGLE_APP_ID").matches(Regex("1:[0-9]+:ios:[A-Za-z0-9]+"))) { "Invalid iOS GOOGLE_APP_ID" }
                require(result.text("GCM_SENDER_ID").matches(Regex("[0-9]+")) && result.text("API_KEY").isNotBlank()) { "Missing iOS sender ID or API key" }
                require(result.text("GOOGLE_APP_ID").split(':')[1] == result.text("GCM_SENDER_ID")) { "iOS app ID project number does not match GCM_SENDER_ID" }
            }
        }
        result
    } catch (e: Exception) { issue(platform, e.message ?: "Invalid $platform config"); null }
    val android = config("android"); val ios = config("ios")
    if (android != null && ios != null && android.getValue("project_info").jsonObject.text("project_number") != ios.text("GCM_SENDER_ID"))
        issue("ios", "Android and iOS configs must use the same Firebase project number")
    return FirebaseOptions(services ?: emptyList(), project, android, ios)
}

internal fun xml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
