package bootstrap

import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.system.exitProcess

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
val json = Json { prettyPrint = true; prettyPrintIndent = "  " }
fun value(v: Any?): JsonElement = when (v) {
    null -> JsonNull
    is JsonElement -> v
    is Map<*, *> -> JsonObject(v.entries.associate { it.key.toString() to value(it.value) })
    is Iterable<*> -> JsonArray(v.map(::value))
    is Number -> JsonPrimitive(v)
    is Boolean -> JsonPrimitive(v)
    else -> JsonPrimitive(v.toString())
}
fun encode(v: Any?) = json.encodeToString(JsonElement.serializer(), value(v)) + "\n"
fun JsonObject.text(key: String) = getValue(key).jsonPrimitive.content
fun JsonObject.strings(key: String) = getValue(key).jsonArray.map { it.jsonPrimitive.content }
object Resources {
    private val loader = Resources::class.java.classLoader
    fun bytes(path: String): ByteArray = loader.getResourceAsStream(path)?.use { it.readBytes() }
        ?: error("Missing packaged resource: $path")
    fun text(path: String) = bytes(path).toString(Charsets.UTF_8)
    val paths by lazy { text("resources.index").lineSequence().filter { it.isNotEmpty() }.toList() }
}
val toolchain by lazy { Json.parseToJsonElement(Resources.text("toolchain.json")).jsonObject }
val platforms = listOf("android", "ios", "desktop", "web")
val keywords = "as break class continue do else false for fun if in interface is null object package return super this throw true try typealias typeof val var when while import by constructor delegate dynamic field file finally get init param property receiver set setparam where actual abstract annotation companion const crossinline data enum expect external final infix inline inner internal lateinit noinline open operator out override private protected public reified sealed suspend tailrec vararg value".split(" ").toSet()
class Arguments(args: List<String>, allowed: Set<String>, flags: Set<String> = emptySet()) {
    val options = linkedMapOf<String, String>()
    val positional = mutableListOf<String>()
    init {
        var i = 0
        while (i < args.size) {
            val arg = args[i++]
            if (!arg.startsWith("--")) positional += arg else {
                val key = arg.removePrefix("--").substringBefore('=')
                require(key in allowed || key in flags) { "Unknown option --$key" }
                require(key !in options) { "Duplicate option --$key" }
                options[key] = if (key in flags) {
                    require('=' !in arg) { "--$key does not take a value" }; "true"
                } else if ('=' in arg) arg.substringAfter('=') else {
                    require(i < args.size && !args[i].startsWith("--")) { "Missing value for --$key" }; args[i++]
                }
            }
        }
    }
    fun get(key: String, default: String? = null): String = options[key] ?: default ?: error("Required: --$key")
    fun integer(key: String, default: Int) = get(key, default.toString()).toIntOrNull() ?: error("--$key must be an integer")
    fun path(): Path { require(positional.size == 1) { "Exactly one output/project path is required" }; return Path.of(positional.single()).toAbsolutePath().normalize() }
}
fun selection(text: String, allowed: Collection<String>): List<String> = text.split(',').distinct().also {
    require(it.isNotEmpty() && it.all { s -> s in allowed }) { "Invalid selection '$text'; expected ${allowed.joinToString(",")}" }
}
data class Options(
    val output: Path, val name: String, val pkg: String, val appId: String,
    val kind: String, val targets: List<String>, val agents: List<String>,
    val min: Int, val target: Int, val compile: Int, val ios: String,
    val dryRun: Boolean,
    val ui: String = defaultUi(kind), val di: String = "manual",
    val navigation: String = "none",
    val network: String = "none", val storage: String = "none",
    val networkUrl: String? = if (network == "ktor") demoEndpoint else null,
) {
    val hasData get() = (network != "none" || storage != "none")
    val customStack get() = (ui == "swiftui" || di != "manual" || navigation != "none" || hasData)
    val tokens get() = linkedMapOf("PROJECT_NAME" to name, "PACKAGE_BASE" to pkg,
        "PACKAGE_PATH" to pkg.replace('.', '/'), "APP_ID" to appId, "MIN_SDK" to "$min",
        "TARGET_SDK" to "$target", "COMPILE_SDK" to "$compile", "IOS_DEPLOYMENT_TARGET" to ios,
        "KIND" to kind, "TARGET_LIST" to targets.joinToString(", "))
}
fun parseGenerate(args: List<String>): Options {
    val a = Arguments(args, setOf("name", "package", "app-id", "kind", "targets", "agents", "min-sdk", "target-sdk", "compile-sdk", "ios-deployment-target", "profile", "feature", "modules", "ui", "di", "navigation", "network", "storage", "network-url"), setOf("dry-run"))
    require(a.get("profile", "modern") == "modern") { "Hardened profiles were removed in 3.0; choose a modern recipe" }
    require(listOf("feature", "modules").none { it in a.options }) { "Hardened feature/modules options were removed in 3.0" }
    val kind = a.get("kind", "kmp").also { require(it in listOf("kmp", "android")) { "Unknown kind" } }
    val targets = selection(a.get("targets", if (kind == "android") "android" else "android,ios"), platforms)
    require(kind != "android" || targets == listOf("android")) { "Native Android supports only android" }
    val name = a.get("name"); val pkg = a.get("package"); val appId = a.get("app-id", pkg)
    require(name.matches(Regex("[A-Z][A-Za-z0-9]*"))) { "Project name must be a PascalCase identifier" }
    for (s in listOf(pkg, appId)) require(s.matches(Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+")) && s.split('.').none { it in keywords }) { "Package/application ID must have at least two lowercase non-keyword segments" }
    require("ios" !in targets || '_' !in appId) { "iOS application ID cannot contain underscores" }
    val agents = selection(a.get("agents", "codex,claude"), listOf("codex", "claude", "gemini", "copilot", "cursor", "none"))
    require("none" !in agents || agents.size == 1) { "Agent none cannot be combined" }
    val min = a.integer("min-sdk", 26); val target = a.integer("target-sdk", 37)
    val compile = a.integer("compile-sdk", 37); val ios = a.get("ios-deployment-target", "15.0")
    require(min >= 23 && min <= target && target <= compile && compile <= 37) { "Require 23 <= min-sdk <= target-sdk <= compile-sdk <= 37" }
    require("android" !in targets || compile == 37) { "Pinned Compose libraries require compile-sdk 37" }
    require(ios.matches(Regex("(?:1[5-9]|2[0-9])\\.\\d+"))) { "iOS deployment target must be >=15.0" }
    val output = a.path(); validateOutput(output)
    val ui = a.get("ui", defaultUi(kind)); val di = a.get("di", "manual")
    val navigation = a.get("navigation", "none")
    val network = a.get("network", "none"); val storage = a.get("storage", "none")
    val networkUrl = a.options["network-url"] ?: demoEndpoint.takeIf { network == "ktor" }
    val conflicts = stackIssues(kind, targets, ui, di, navigation, ios) + dataIssues(network, storage, networkUrl)
    require(conflicts.isEmpty()) { conflicts.joinToString("; ") { "[${it.field}] ${it.message}" } }
    return Options(output, name, pkg, appId, kind, targets, agents, min, target, compile, ios, "dry-run" in a.options, ui, di, navigation, network, storage, networkUrl)
}
fun validateOutput(path: Path) {
    require(!Files.isSymbolicLink(path)) { "Output must not be a symlink" }
    require(!Files.exists(path, NOFOLLOW_LINKS) || (path.isDirectory() && Files.list(path).use { it.findAny().isEmpty })) { "Output must be absent or empty; existing files are never overwritten" }
}
fun substitute(text: String, tokens: Map<String, String>): String {
    var result = text
    tokens.forEach { (key, v) -> result = result.replace("{{$key}}", v) }
    require(!Regex("\\{\\{[A-Z_]+}}").containsMatchIn(result)) { "Unresolved template token" }
    return result
}
data class Rendered(val files: LinkedHashMap<String, ByteArray>, val metadata: JsonObject)
fun generate(args: List<String>): Int {
    if (args.any { it == "--config" || it.startsWith("--config=") }) return configCommand(args, generate = true)
    val o = parseGenerate(args)
    val rendered = renderModern(o)
    if (o.dryRun) {
        print(encode(mapOf("output" to o.output.toString(), "kind" to o.kind, "targets" to o.targets, "files" to rendered.files.keys.sorted(), "checks" to rendered.metadata["checks"])))
        return 0
    }
    publish(o.output, rendered.files)
    print(encode(mapOf("output" to o.output.toString(), "files" to rendered.files.size, "targets" to o.targets, "build_status" to "NOT_RUN")))
    return 0
}
fun publish(output: Path, files: Map<String, ByteArray>) {
    validateOutput(output)
    Files.createDirectories(output.parent)
    val staging = Files.createTempDirectory(output.parent, ".bootstrap-")
    try {
        val project = Files.createDirectory(staging.resolve("project"))
        files.forEach { (name, bytes) ->
            val path = project.resolve(name).normalize()
            require(path.startsWith(project)) { "Unsafe resource path $name" }
            Files.createDirectories(path.parent); Files.write(path, bytes)
        }
        project.resolve("kotlin").toFile().setExecutable(true, false)
        validateOutput(output)
        if (Files.exists(output, NOFOLLOW_LINKS)) Files.delete(output)
        // No REPLACE_EXISTING: never clobber a destination created by another process.
        Files.move(project, output)
    } finally { staging.toFile().deleteRecursively() }
}
val help = """
Android & KMP Bootstrap 3.0.0 — Kotlin/JVM CLI (Java 17+)
  init [OUTPUT]                  Interactive terminal wizard
  catalog --json
  plan --config SPEC --output OUTPUT [--lock LOCK]
  generate --config SPEC --output OUTPUT [--lock LOCK] [--dry-run]
  generate OUTPUT --name Aurora --package com.example.aurora [options]
  doctor [--targets android,ios] [--compile-sdk 37]
  verify PROJECT [--targets android,ios] [--timeout 1200]
Generation options:
  --kind kmp|android             Default: kmp
  --targets android,ios,desktop,web  Default: android,ios (native: android)
  --agents codex,claude,gemini,copilot,cursor|none  Default: codex,claude
  --app-id ID                    Default: package
  --min-sdk 26 --target-sdk 37 --compile-sdk 37
  --ios-deployment-target 15.0
  --ui android-compose|shared-compose|swiftui  Default: determined by kind
  --di manual|koin              default manual
  --navigation none|navigation3|platform-native  default none
  --network none|ktor --storage none|settings  defaults none
  --network-url HTTPS_URL        Ktor only; default demo https://httpbin.org/uuid
  --dry-run                     Validate and print plan without writes
Exit codes: 0 success, 1 failure, 2 verification with skipped checks.
""".trimIndent()
fun runCli(args: List<String>): Int {
    if (args.isEmpty() || args.any { it in listOf("--help", "-h") }) { println(help); return 0 }
    return when (args.first()) {
        "generate" -> generate(args.drop(1))
        "catalog" -> catalog(args.drop(1))
        "init" -> init(args.drop(1))
        "plan" -> configCommand(args.drop(1), generate = false)
        "doctor" -> doctor(args.drop(1))
        "verify" -> verify(args.drop(1))
        "--version" -> { println(CLI_VERSION); 0 }
        else -> error("Unknown command: ${args.first()}")
    }
}
fun main(args: Array<String>) {
    val code = try { runCli(args.toList()) } catch (e: Exception) { System.err.println("ERROR: ${e.message}"); 1 }
    exitProcess(code)
}
