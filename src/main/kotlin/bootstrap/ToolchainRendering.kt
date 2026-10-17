package bootstrap

import kotlinx.serialization.json.*

fun projectMetadata(o: Options): JsonObject = value(mapOf(
    "schema_version" to 2, "generator_version" to CLI_VERSION, "build_system" to "kotlin-toolchain",
    "kind" to o.kind, "name" to o.name, "package" to o.pkg, "app_id" to o.appId,
    "targets" to o.targets, "agents" to o.agents, "ui" to o.ui, "di" to o.di, "navigation" to o.navigation,
    "network" to o.network, "storage" to o.storage, "network_url" to o.networkUrl,
    "toolchain" to toolchain, "sdk" to mapOf("min" to o.min, "target" to o.target, "compile" to o.compile),
    "ios_deployment_target" to o.ios,
    "checks" to o.targets.associateWith { target ->
        val platform = when (target) { "ios" -> "iosSimulatorArm64"; "desktop" -> "jvm"; "web" -> "wasmJs"; else -> target }
        listOf(listOf("build", "--platform", platform), listOf("test", "--platform", platform))
    },
)).jsonObject

fun configureToolchain(o: Options, files: MutableMap<String, ByteArray>) {
    fun put(path: String, content: String) { files[path] = content.toByteArray() }
    val kmp = o.kind == "kmp"; val native = o.ui == "swiftui"; val data = o.hasData
    val logic = if (kmp) "shared" else "androidApp"
    val ui = if (kmp && !native) "shared" else "androidApp"
    val modules = (if (kmp) listOf("shared") else emptyList()) + o.targets.map { when (it) { "android" -> "androidApp"; "ios" -> "iosApp"; "desktop" -> "desktopApp"; else -> "webApp" } }
    put("project.yaml", "modules:\n" + modules.joinToString("\n", postfix = "\n") { "  - $it" })
    for (module in modules) {
        val compose = module == ui || module in listOf("androidApp", "desktopApp", "webApp") || module == "iosApp" && !native
        val nav = (o.navigation == "navigation3" || o.navigation == "platform-native" && "android" in o.targets) && module == ui
        val deps = mutableListOf<String>(); val tests = mutableListOf<String>()
        val platformDeps = linkedMapOf<String, MutableList<String>>()
        fun platform(name: String, dep: String) { platformDeps.getOrPut(name) { mutableListOf() } += dep }
        if (module != "shared" && kmp) deps += "//shared"
        if (compose) {
            deps += listOf("org.jetbrains.compose.runtime:runtime:${toolchain.text("compose_multiplatform")}", "org.jetbrains.compose.foundation:foundation:${toolchain.text("compose_multiplatform")}", "org.jetbrains.compose.material3:material3:${toolchain.text("compose_material3")}", "org.jetbrains.compose.ui:ui:${toolchain.text("compose_multiplatform")}")
            if (module == "shared") deps += "org.jetbrains.compose.components:components-resources:${toolchain.text("compose_multiplatform")}"
        }
        if (module == "desktopApp") deps += "\$compose.desktop.currentOs"
        if (module == "androidApp") deps += "androidx.activity:activity-compose:${toolchain.text("activity_compose")}"
        if (nav) deps += listOf("org.jetbrains.androidx.navigation3:navigation3-ui:${componentPins.text("navigation3")}", "org.jetbrains.kotlinx:kotlinx-serialization-json:${componentPins.text("serialization")}")
        if (module == logic) {
            if (o.di == "koin") deps += "io.insert-koin:koin-core:${componentPins.text("koin")}"
            if (data) {
                deps += "org.jetbrains.kotlinx:kotlinx-coroutines-core:${componentPins.text("coroutines")}"
                tests += "org.jetbrains.kotlinx:kotlinx-coroutines-test:${componentPins.text("coroutines")}"
            }
            if (o.storage == "settings") {
                deps += "com.russhwolf:multiplatform-settings:${componentPins.text("settings")}"
                tests += "com.russhwolf:multiplatform-settings-test:${componentPins.text("settings")}"
            }
            if (o.network == "ktor") {
                deps += "io.ktor:ktor-client-core:${componentPins.text("ktor")}"
                tests += "io.ktor:ktor-client-mock:${componentPins.text("ktor")}"
                if ("android" in o.targets) platform("android", "io.ktor:ktor-client-cio:${componentPins.text("ktor")}")
                if ("ios" in o.targets) platform("ios", "io.ktor:ktor-client-darwin:${componentPins.text("ktor")}")
                if ("desktop" in o.targets || native) platform("jvm", "io.ktor:ktor-client-cio:${componentPins.text("ktor")}")
                if ("web" in o.targets) platform("wasmJs", "io.ktor:ktor-client-js:${componentPins.text("ktor")}")
                if ("android" in o.targets) platform("android", "org.jetbrains.kotlinx:kotlinx-coroutines-android:${componentPins.text("coroutines")}")
                if ("desktop" in o.targets) platform("jvm", "org.jetbrains.kotlinx:kotlinx-coroutines-swing:${componentPins.text("coroutines")}")
            }
        }
        if (!kmp) { deps += platformDeps.values.flatten(); platformDeps.clear() }
        val product = when (module) { "shared" -> "kmp/lib"; "androidApp" -> "android/app"; "iosApp" -> "ios/app"; "desktopApp" -> "jvm/app"; else -> "wasm-js/app" }
        val yaml = buildString {
            if (module == "shared") {
                val targets = o.targets.flatMap { when (it) { "ios" -> listOf("iosArm64", "iosSimulatorArm64"); "desktop" -> listOf("jvm"); "web" -> listOf("wasmJs"); else -> listOf(it) } }.toMutableList()
                if (native && "jvm" !in targets) targets += "jvm"
                append("product:\n  type: $product\n  platforms: [${targets.joinToString(", ")}]\n")
            } else append("product: $product\n")
            if (deps.isNotEmpty()) append("\ndependencies:\n" + deps.joinToString("\n", postfix = "\n") { "  - $it" })
            if (tests.isNotEmpty()) append("\ntest-dependencies:\n" + tests.joinToString("\n", postfix = "\n") { "  - $it" })
            platformDeps.forEach { (platform, dependencies) ->
                val qualifier = if (kmp) "@$platform" else ""
                append("\ndependencies$qualifier:\n" + dependencies.joinToString("\n", postfix = "\n") { "  - $it" })
            }
            append("\nsettings:\n  kotlin:\n    version: ${toolchain.text("kotlin")}\n")
            if (nav) append("    serialization:\n      format: json\n      version: ${componentPins.text("serialization")}\n")
            if (module == "androidApp" || module == "desktopApp" || module == "shared" && ("android" in o.targets || "desktop" in o.targets || native)) append("  junit: junit-4\n")
            if (compose) append("  compose:\n    enabled: true\n    version: ${toolchain.text("compose_multiplatform")}\n")
            if (module == "androidApp" || module == "shared" && "android" in o.targets) append("  android:\n    namespace: ${o.pkg}${if (module == "shared") ".shared" else ""}\n${if (module == "androidApp") "    applicationId: ${o.appId}\n" else ""}    compileSdk: ${o.compile}\n    targetSdk: ${o.target}\n    minSdk: ${o.min}\n")
            if (module == "desktopApp" || module == "shared" && ("desktop" in o.targets || native)) append("  jvm:\n    release: 17\n    jdk:\n      version: 21\n")
        }
        put("$module/module.yaml", yaml)
    }
}
