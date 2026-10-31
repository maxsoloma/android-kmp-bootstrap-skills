package bootstrap

import kotlinx.serialization.json.*

private fun kotlinLiteral(s: String) = JsonPrimitive(s).toString().replace("$", "\\$")

fun renderDataStack(o: Options): Rendered {
    val base = renderStack(o.copy(network = "none", storage = "none", networkUrl = null))
    val files = base.files
    val kmp = o.kind == "kmp"; val native = o.ui == "swiftui"
    val network = o.network == "ktor"; val storage = o.storage == "settings"
    val logic = if (kmp) "shared" else "androidApp"
    val ui = if (kmp && !native) "shared" else "androidApp"
    val pkgPath = o.pkg.replace('.', '/')
    fun source(module: String, set: String): String { val folder = when (set) { "main", "commonMain" -> "src"; "test", "commonTest" -> "test"; "desktopMain", "logicMain" -> "src@jvm"; "desktopTest", "logicTest" -> "test@jvm"; else -> if (set.endsWith("Main")) "src@" + set.removeSuffix("Main") else "test@" + set.removeSuffix("Test") }; return "$module/$folder/$pkgPath" }
    val main = source(logic, if (kmp) "commonMain" else "main")
    val test = source(logic, if (kmp) "commonTest" else "test")
    fun put(path: String, content: String) { files[path] = content.toByteArray(Charsets.UTF_8) }
    fun edit(path: String, transform: (String) -> String) { put(path, transform(files.getValue(path).toString(Charsets.UTF_8))) }
    val tokens = o.tokens + mapOf("NETWORK_URL_LITERAL" to kotlinLiteral(o.networkUrl ?: demoEndpoint))
    fun template(name: String) = substitute(Resources.text("data/$name.template"), tokens)
    fun tested(content: String) = if (kmp) content else content.replace("import kotlin.test.Test", "import org.junit.Test").replace("import kotlin.test.assertEquals", "import org.junit.Assert.assertEquals").replace("import kotlin.test.assertTrue", "import org.junit.Assert.assertTrue").replace("import kotlin.test.assertFalse", "import org.junit.Assert.assertFalse").replace("import kotlin.test.assertNotNull", "import org.junit.Assert.assertNotNull")
    val controller = template("CounterController").replace("// NETWORK_METHODS", if (network) template("NetworkMethods").trim() else "")
    put("$main/CounterController.kt", controller)
    put("$main/CounterStore.kt", template("CounterStore") + if (storage) template("SettingsStore") else "\ninternal fun platformCounterStore(): CounterStore = MemoryCounterStore()\n")
    if (storage && kmp) edit("$main/CounterStore.kt") { it + "\ninternal expect fun platformCounterStore(): CounterStore\n" }
    val clientParameter = if (network) ", private val clientFactory: () -> io.ktor.client.HttpClient = ::platformHttpClient" else ""
    val remote = if (network) "private val remote = lazy { NetworkRepository(clientFactory()) }\n    val network: NetworkRepository get() = ${if (o.di == "koin") "application.koin.get()" else "remote.value"}\n    " else ""
    val closeRemote = if (network) "if (remote.isInitialized()) remote.value.close(); " else ""
    val graph = if (o.di == "koin") """
internal class AppGraph(store: CounterStore = platformCounterStore()$clientParameter) {
    private val repository = CounterRepository(store)
    ${remote}private val application = org.koin.dsl.koinApplication {
        modules(org.koin.dsl.module {
            single { repository }
            factory { CounterService(get()) }
            ${if (network) "single { remote.value }" else ""}
        })
    }
    val counter: CounterService = application.koin.get()
    fun close() { ${closeRemote}application.close() }
}
""" else """
internal class AppGraph(store: CounterStore = platformCounterStore()$clientParameter) {
    private val repository = CounterRepository(store)
    val counter = CounterService(repository)
    ${remote}fun close() { $closeRemote }
}
"""
    edit("$main/CounterController.kt") { it + graph }
    put("$test/CounterTest.kt", tested(template("CounterTest")))
    if (storage) {
        put("$test/PersistenceTest.kt", tested(template("PersistenceTest")))
        if ("web" in o.targets) put("${source(logic, "wasmJsTest")}/BrowserPersistenceTest.kt", template("BrowserPersistenceTest"))
        if ("ios" in o.targets) put("${source(logic, "iosTest")}/IosPersistenceTest.kt", template("IosPersistenceTest"))
    }
    if (network) {
        put("$main/NetworkRepository.kt", template("NetworkRepository") + if (kmp) "\ninternal expect fun platformHttpClient(): io.ktor.client.HttpClient\n" else "")
        put("$test/NetworkTest.kt", tested(template("NetworkTest")))
    }
    fun platform(target: String, set: String) {
        val actual = if (kmp) "actual " else ""
        var content = "package ${o.pkg}\n\n"
        if (storage) content += template(when (target) { "android" -> "AndroidStore"; "ios" -> "IosStore"; "web" -> "WebStore"; else -> "JvmStore" }).substringAfter("package ${o.pkg}\n")
        if (network) {
            val engine = when (target) { "ios" -> "darwin.Darwin"; "web" -> "js.Js"; else -> "cio.CIO" }
            content += "\ninternal ${actual}fun platformHttpClient() = io.ktor.client.HttpClient(io.ktor.client.engine.$engine) { configureNetwork() }\n"
        }
        if (!kmp) content = content.replace("internal actual fun", "internal fun")
        put("${source(logic, set)}/PlatformData.kt", content)
        if (storage && target in listOf("desktop", "logic")) put("${source(logic, set.removeSuffix("Main") + "Test")}/FilePersistenceTest.kt", template("FilePersistenceTest"))
    }
    if (kmp) {
        o.targets.forEach { platform(it, when (it) { "android" -> "androidMain"; "ios" -> "iosMain"; "web" -> "wasmJsMain"; else -> "desktopMain" }) }
        if (native) platform("logic", "logicMain")
    } else platform("android", "main")
    if (storage && "android" in o.targets) edit("androidApp/src/$pkgPath/MainActivity.kt") {
        it.replace("        enableEdgeToEdge()", "        initializeStorage(applicationContext)\n        enableEdgeToEdge()")
    }
    if (network && "android" in o.targets) edit("androidApp/src/AndroidManifest.xml") {
        it.replace("    <application", "    <uses-permission android:name=\"android.permission.INTERNET\" />\n    <application")
    }
    val uiMain = source(ui, if (ui == "shared") "commonMain" else "main")
    if (!native || "android" in o.targets) edit("$uiMain/App.kt") { app ->
        var result = app.replace("    var count by remember { mutableIntStateOf(controller.count) }", "    var count by remember { mutableIntStateOf(controller.count) }\n    var storageError by remember { mutableStateOf(controller.storageError) }")
            .replace("count = controller.increment()", "count = controller.increment(); storageError = controller.storageError")
            .replace("Button(onClick = { count", "Button(enabled = storageError == null, onClick = { count")
        if (network) result = result.replace("    MaterialTheme {", "    var networkState by remember { mutableStateOf(NetworkState()) }\n    MaterialTheme {")
        val widgets = "\n                storageError?.let { Text(\"Storage error: \" + it) }\n" + if (network) """
                Button(enabled = !networkState.loading, onClick = { controller.fetch { networkState = it } }) { Text(if (networkState.loading) "Loading…" else "Fetch response") }
                networkState.text?.let { Text(it) }
                networkState.error?.let { Text("Network error: " + it) }
""" else ""
        // Add controls to each generated screen, preserving its existing navigation owner.
        result.replace(Regex("(?m)^([ \\t]*)Text\\(\\\"(?:${o.name}|Details:)[^\\n]*\\n")) { match ->
            match.value + widgets.trim().lineSequence().joinToString("\n", postfix = "\n") { match.groupValues[1] + it.trim() }
        }
    }
    if (native) edit("iosApp/src/ContentView.swift") { swift ->
        var result = swift.replace("    func increment() { count = controller.increment() }", "    @Published private(set) var storageError: String?\n    init() { count = controller.count; storageError = controller.storageError }\n    func increment() { count = controller.increment(); storageError = controller.storageError }")
        if (network) result = result.replace("    deinit", """
    @Published private(set) var loading = false
    @Published private(set) var response: String?
    @Published private(set) var networkError: String?
    func fetch() {
        controller.fetch { [weak self] state in
            self?.loading = state.loading
            self?.response = state.text
            self?.networkError = state.error
        }
    }
    deinit
""".trimStart())
        val widgets = "\n            if let error = model.storageError { Text(\"Storage error: \\(error)\") }\n" + if (network) """
            Button(model.loading ? "Loading…" : "Fetch response") { model.fetch() }.disabled(model.loading)
            if let text = model.response { Text(text) }
            if let error = model.networkError { Text("Network error: \(error)") }
""" else ""
        result.replace(Regex("(?m)^([ \\t]*)Text\\(\\\"(?:${o.name}|Details:)[^\\n]*\\n")) { match ->
            match.value + widgets.trim().lineSequence().joinToString("\n", postfix = "\n") { match.groupValues[1] + it.trim() }
        }
            .replace("{ model.increment() }", "{ model.increment() }.disabled(model.storageError != nil)")
    }
    configureToolchain(o, files)
    val meta = JsonObject(projectMetadata(o) + mapOf("network" to value(o.network), "storage" to value(o.storage)) + (o.networkUrl?.let { mapOf("network_url" to value(it)) } ?: emptyMap()))
    put("bootstrap.json", encode(meta))
    val guide = "\n## Data integrations\n\nNetwork: ${o.network}; storage: ${o.storage}. Adapters: ${if (network) networkAdapters(o.targets) else "none"}; ${if (storage) storageAdapters(o.targets) else "none"}.\nThe counter persists only when Settings is selected. Networking is user-triggered, with timeout, error state and owner cancellation. Tests use MockEngine and temporary/test stores; no public API uptime dependency. Replace the demo endpoint for production. Browser CORS/quota and device persistence require runtime checks. Settings is for small non-secret values, not SQL or credential storage.\n"
    for (path in listOf("README.md", "AGENTS.md")) edit(path) { it.replace("no backend, credentials, billing, database, or unverified platform stubs", "no backend, credentials, billing, SQL database, or unverified platform stubs") + guide }
    return Rendered(files, meta)
}
