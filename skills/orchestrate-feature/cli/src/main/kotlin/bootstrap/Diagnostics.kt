package bootstrap

import kotlinx.serialization.json.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

val windows get() = System.getProperty("os.name").startsWith("Windows")
val mac get() = System.getProperty("os.name").startsWith("Mac")
data class ProcessResult(val code: Int, val detail: String = "")
fun execute(command: List<String>, timeout: Long, log: Path, directory: Path? = null): ProcessResult {
    val actual = if (windows && command.first().endsWith(".bat", ignoreCase = true)) listOf("cmd.exe", "/d", "/c") + command else command
    return try {
        val builder = ProcessBuilder(actual).redirectErrorStream(true).redirectOutput(log.toFile())
        if (directory != null) builder.directory(directory.toFile())
        val process = builder.start()
        if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
            process.toHandle().descendants().use { children -> children.forEach { it.destroyForcibly() } }
            process.destroyForcibly(); process.waitFor(5, TimeUnit.SECONDS)
            ProcessResult(-1, "Timed out after $timeout seconds")
        } else ProcessResult(process.exitValue())
    } catch (e: Exception) { ProcessResult(-1, e.message ?: e.javaClass.simpleName) }
}
/** Toolchain 0.13 locks its default simulator only within one JVM, then shuts it down. */
fun withIosSimulatorLock(timeout: Long, action: (Long) -> ProcessResult): ProcessResult {
    val name = sha256(System.getProperty("user.name").toByteArray()).take(16)
    val path = Path.of(System.getProperty("java.io.tmpdir"), "bootstrap-ios-simulator-$name.lock")
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeout)
    return try {
        FileChannel.open(path, CREATE, WRITE, NOFOLLOW_LINKS).use { channel ->
            while (true) {
                val lock = try { channel.tryLock() } catch (_: OverlappingFileLockException) { null }
                if (lock != null) return lock.use {
                    val remaining = deadline - System.nanoTime()
                    if (remaining <= 0) ProcessResult(-1, "Timed out waiting for the iOS simulator")
                    else action((remaining + 999_999_999L) / 1_000_000_000L)
                }
                if (System.nanoTime() >= deadline) return ProcessResult(-1, "Timed out waiting for the iOS simulator")
                Thread.sleep(100)
            }
            @Suppress("UNREACHABLE_CODE")
            error("unreachable")
        }
    } catch (e: Exception) { ProcessResult(-1, e.message ?: e.javaClass.simpleName) }
}
fun probe(command: List<String>): ProcessResult {
    val log = Files.createTempFile("bootstrap-probe-", ".log")
    return try {
        val result = execute(command, 15, log)
        val output = Files.newBufferedReader(log).use { reader ->
            val buffer = CharArray(2000); val count = reader.read(buffer)
            if (count <= 0) "" else String(buffer, 0, count).trim()
        }
        result.copy(detail = listOf(output, result.detail).filter { it.isNotEmpty() }.joinToString("\n"))
    } finally { Files.deleteIfExists(log) }
}
fun inspect(targets: List<String>, sdkLevel: Int, env: Map<String, String> = System.getenv()): JsonObject {
    val checks = mutableListOf<Map<String, Any>>()
    fun add(name: String, status: String, detail: String) { checks += mapOf("check" to name, "status" to status, "detail" to detail) }
    val javaHome = Path.of(System.getProperty("java.home"))
    val java = probe(listOf(javaHome.resolve("bin/${if (windows) "java.exe" else "java"}").toString(), "-version"))
    val javac = probe(listOf(javaHome.resolve("bin/${if (windows) "javac.exe" else "javac"}").toString(), "-version"))
    add("jdk", if (java.code == 0 && Runtime.version().feature() >= 17 && javac.code == 0) "PASS" else "FAIL", java.detail + "\n" + javac.detail)
    if ("android" in targets) {
        val home = System.getProperty("user.home")
        val candidates = listOfNotNull(env["ANDROID_HOME"], env["ANDROID_SDK_ROOT"], "$home/Library/Android/sdk", "$home/Android/Sdk", env["LOCALAPPDATA"]?.let { "$it/Android/Sdk" })
        val sdk = candidates.map { Path.of(it) }.firstOrNull { it.resolve("platforms").isDirectory() }
        val installed = sdk != null && listOf("$sdkLevel", "$sdkLevel.0").any { sdk.resolve("platforms/android-$it/android.jar").isRegularFile() }
        add("android-sdk", if (installed) "PASS" else "SKIPPED", "Platform API $sdkLevel; SDK: ${sdk ?: "not found"}. Kotlin Toolchain can provision the SDK during builds; this read-only check does not download tools or accept licenses.")
        val android = probe(listOf("android", "--help"))
        val modern = android.code == 0 && listOf("skills", "studio").all { it in android.detail.lowercase() }
        add("android-cli", if (modern) "PASS" else "SKIPPED", if (modern) "Modern agent CLI detected" else "Optional; unavailable or unrecognized. Use Kotlin Toolchain/adb or developer.android.com/tools/agents/android-cli")
    }
    if ("ios" in targets) {
        if (!mac) add("xcode", "SKIPPED", "iOS requires macOS + full Xcode") else {
            val xcode = probe(listOf("xcodebuild", "-version"))
            add("xcode", if (xcode.code == 0) "PASS" else "FAIL", xcode.detail)
            val sdk = probe(listOf("xcrun", "--sdk", "iphonesimulator", "--show-sdk-path"))
            add("ios-simulator-sdk", if (sdk.code == 0) "PASS" else "FAIL", sdk.detail)
        }
    }
    if ("web" in targets) add("browser", "SKIPPED", "WasmGC browser execution must be verified separately; Toolchain provisions browser testing tools")
    return value(mapOf("host" to mapOf("os" to System.getProperty("os.name"), "arch" to System.getProperty("os.arch")), "checks" to checks)).jsonObject
}
fun doctor(args: List<String>): Int {
    val a = Arguments(args, setOf("targets", "compile-sdk"))
    require(a.positional.isEmpty()) { "doctor takes no positional arguments" }
    val targets = selection(a.get("targets", "android,ios"), platforms)
    val sdk = a.integer("compile-sdk", 37); require(sdk > 0) { "compile-sdk must be positive" }
    val report = inspect(targets, sdk)
    print(encode(report))
    return if (report.getValue("checks").jsonArray.any { it.jsonObject.text("status") == "FAIL" }) 1 else 0
}
fun verify(args: List<String>): Int {
    val a = Arguments(args, setOf("targets", "timeout")); val root = a.path()
    val timeout = a.integer("timeout", 1200); require(timeout > 0) { "timeout must be positive" }
    val meta = Json.parseToJsonElement(root.resolve("bootstrap.json").readText()).jsonObject
    require(meta["build_system"]?.jsonPrimitive?.content == "kotlin-toolchain") { "Use the matching old CLI to verify a retired Gradle project" }
    val checks = meta.getValue("checks").jsonObject
    val targets = selection(a.get("targets", meta.strings("targets").joinToString(",")), platforms)
    require(targets.all { it in checks }) { "Requested target is not present in this project" }
    val commands = linkedMapOf<String, List<List<String>>>()
    if (meta["ui"]?.jsonPrimitive?.content == "swiftui") commands["shared-logic"] = listOf(listOf("build", "--platform", "jvm"), listOf("test", "--platform", "jvm"))
    targets.forEach { target ->
        commands[target] = checks.getValue(target).jsonArray.map { command ->
            command.jsonArray.map { it.jsonPrimitive.content }.also { arguments ->
                require(arguments.size == 3 && arguments[0] in listOf("build", "test") && arguments[1] == "--platform" && arguments[2] in listOf("android", "jvm", "wasmJs", "iosSimulatorArm64")) { "Invalid Toolchain verification command" }
            }
        }
    }
    val logs = root.resolve(".bootstrap"); require(!Files.isSymbolicLink(logs)) { "Log directory must not be a symlink" }; Files.createDirectories(logs)
    val wrapper = root.resolve(if (windows) "kotlin.bat" else "kotlin").toString()
    val results = mutableListOf<Map<String, Any>>()
    for ((target, targetCommands) in commands) for (arguments in targetCommands) {
        if (target == "ios") {
            val xcode = if (mac) probe(listOf("xcodebuild", "-version")) else ProcessResult(1, "host is not macOS")
            if (xcode.code != 0) { results += mapOf("target" to target, "check" to arguments.first(), "status" to "SKIPPED", "reason" to "Full Xcode required: ${xcode.detail}"); continue }
        }
        val command = listOf(wrapper) + arguments
        val log = logs.resolve("$target-${arguments.first()}.log"); require(!Files.isSymbolicLink(log)) { "Log file must not be a symlink" }
        val started = System.nanoTime()
        System.err.println("Checking $target ${arguments.first()}; log: $log")
        val process = if (target == "ios" && arguments.first() == "test")
            withIosSimulatorLock(timeout.toLong()) { remaining -> execute(command, remaining, log, root) }
        else execute(command, timeout.toLong(), log, root)
        val result = linkedMapOf<String, Any>("target" to target, "check" to arguments.first(), "status" to if (process.code == 0) "PASS" else "FAIL", "exit_code" to process.code,
            "command" to command, "seconds" to ((System.nanoTime() - started) / 10_000_000L / 100.0), "log" to log.toString())
        if (process.detail.isNotEmpty()) result["reason"] = process.detail
        results += result
    }
    val report = encode(mapOf("project" to root.toString(), "checks" to results, "scope" to "Toolchain compilation, selected app hosts and platform unit/browser tests. Device UI interaction, live HTTP and signing require separate verification."))
    val reportPath = logs.resolve("verification.json"); require(!Files.isSymbolicLink(reportPath)) { "Report must not be a symlink" }
    reportPath.writeText(report); print(report)
    return if (results.any { it["status"] == "FAIL" }) 1 else if (results.any { it["status"] == "SKIPPED" }) 2 else 0
}
