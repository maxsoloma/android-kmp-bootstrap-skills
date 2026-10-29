package bootstrap

import kotlinx.serialization.json.*

fun renderModern(o: Options): Rendered {
    if (o.customStack) return renderStack(o)
    val files = linkedMapOf<String, ByteArray>()
    fun put(path: String, content: String) { files[path] = content.toByteArray() }
    fun template(path: String) = substitute(Resources.text("modern/$path.template"), o.tokens)
    val kmp = o.kind == "kmp"; val pkg = o.pkg.replace('.', '/')
    val logic = if (kmp) "shared" else "androidApp"
    put("$logic/src/$pkg/App.kt", template("shared/src/commonMain/kotlin/{{PACKAGE_PATH}}/App.kt"))
    put("$logic/test/$pkg/GreetingTest.kt", template("shared/src/commonTest/kotlin/{{PACKAGE_PATH}}/GreetingTest.kt"))
    if ("android" in o.targets) {
        put("androidApp/src/$pkg/MainActivity.kt", template("androidApp/src/main/kotlin/{{PACKAGE_PATH}}/MainActivity.kt"))
        put("androidApp/src/AndroidManifest.xml", template("androidApp/src/main/AndroidManifest.xml"))
    }
    if ("desktop" in o.targets) put("desktopApp/src/$pkg/Main.kt", template("desktopApp/src/main/kotlin/{{PACKAGE_PATH}}/Main.kt"))
    if ("web" in o.targets) put("webApp/src/$pkg/Main.kt", template("webApp/src/wasmJsMain/kotlin/{{PACKAGE_PATH}}/Main.kt"))
    if ("ios" in o.targets) {
        put("shared/src@ios/$pkg/MainViewController.kt", template("shared/src/iosMain/kotlin/{{PACKAGE_PATH}}/MainViewController.kt"))
        val prefix = "assets/ios/iosApp/iosApp/"
        for (resource in Resources.paths.filter { it.startsWith(prefix) }) {
            val path = "iosApp/src/" + resource.removePrefix(prefix).removeSuffix(".template")
            if (resource.endsWith(".template")) put(path, substitute(Resources.text(resource), o.tokens).replace("import Shared", "import KotlinModules"))
            else files[path] = Resources.bytes(resource)
        }
        put("iosApp/module.xcodeproj/project.pbxproj", substitute(Resources.text("assets/toolchain/project.pbxproj.template"), o.tokens))
        put("iosApp/module.xcodeproj/xcshareddata/xcschemes/app.xcscheme", substitute(Resources.text("assets/toolchain/app.xcscheme.template"), o.tokens))
    }
    for (name in listOf("kotlin", "kotlin.bat")) files[name] = Resources.bytes("assets/toolchain/$name")
    put(".gitignore", "build/\n.kotlin/\n.bootstrap/\nlocal.properties\n.DS_Store\n")
    put("AGENTS.md", "# ${o.name}\n\nUse the pinned Kotlin Toolchain wrapper (`./kotlin`, Windows `kotlin.bat`).\nKeep platform APIs in src@android, src@ios, src@jvm or src@wasmJs; common code is in src.\nKeep shared as a library and Android/iOS app entry points in their own hosts. Gradle/AGP files are not part of this recipe.\nUse Context7 when available, otherwise official vendor documentation. Pin dependency versions.\nNever infer app/runtime success from generation. See bootstrap.json and README.md for checks.\n\nDelegate only independent bounded work when useful. Start workers with fresh context (Codex spawn_agent fork_turns=none when available); pass the objective, allowed files, constraints, acceptance checks and requested report. Send paths/symbols and short findings, not the parent transcript or full docs/logs. Workers read applicable AGENTS.md and only needed sources. Give each file one writer; the coordinator owns integration and test evidence. Do not delegate project/account selection, credentials or cloud mutations. Report changed files, commands/results and blockers concisely. When delegation tools are absent, work sequentially.\n")
    if ("claude" in o.agents) put("CLAUDE.md", "@AGENTS.md\n")
    if ("gemini" in o.agents) put("GEMINI.md", "Follow AGENTS.md.\n")
    if ("copilot" in o.agents) put(".github/copilot-instructions.md", "Follow AGENTS.md.\n")
    if ("cursor" in o.agents) put(".cursor/rules/bootstrap.mdc", "---\nalwaysApply: true\n---\nFollow AGENTS.md.\n")
    configureToolchain(o, files)
    val metadata = projectMetadata(o)
    put("bootstrap.json", encode(metadata))
    put("README.md", buildString {
        append("# ${o.name}\n\nKotlin Toolchain ${toolchain.text("kotlin_toolchain")}; ${o.targets.joinToString(", ")}.\nUse the checked-in wrapper; a global Kotlin CLI is not needed. First builds download dependencies and SDK tools.\n\n## Build and test\n\n`./kotlin build`\n\n`./kotlin test`\n\n")
        if ("ios" in o.targets) append("Xcode host check: `$xcodeAppCommand`. Device signing requires your own team.\n")
        append("\n## Run\n\n")
        if ("android" in o.targets) append("Android: `./kotlin run -m androidApp` (use `--device-id ID` to select a device).\n\n")
        if ("ios" in o.targets) append("iOS simulator: `./kotlin run -m iosApp --platform iosSimulatorArm64` (use `--device-id UDID` to select a simulator).\n\n")
        if ("desktop" in o.targets) append("Desktop: `./kotlin run -m desktopApp`.\n\n")
        if ("web" in o.targets) append("Web: `./kotlin run -m webApp`.\n\n")
        append("See AGENTS.md and bootstrap.json. Generation never runs builds or calls a public network endpoint.\n")
    })
    return Rendered(files, metadata)
}
