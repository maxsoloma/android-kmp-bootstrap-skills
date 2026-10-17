package bootstrap

import kotlinx.serialization.json.*
import java.io.Console
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.StandardOpenOption.WRITE
import kotlin.io.path.writeText

interface WizardIO {
    fun read(prompt: String): String?
    fun say(message: String)
}
private class ConsoleIO(private val console: Console) : WizardIO {
    override fun read(prompt: String): String? = console.readLine("%s", prompt)
    override fun say(message: String) { console.printf("%s%n", message) }
}
private class Back : RuntimeException()
private class Cancel : RuntimeException()

/** Resolve existing ancestors so config saving cannot escape its guard via a directory symlink. */
private fun physicalPath(path: Path): Path {
    val absolute = path.toAbsolutePath().normalize()
    var ancestor = absolute
    while (!Files.exists(ancestor, NOFOLLOW_LINKS)) ancestor = ancestor.parent ?: error("No existing ancestor")
    return ancestor.toRealPath().resolve(ancestor.relativize(absolute)).normalize()
}
fun saveSpec(path: Path, output: Path, spec: JsonObject) {
    require(!physicalPath(path).startsWith(physicalPath(output))) { "Save the config outside the project output directory" }
    // No parent creation, no replacement, including symlinks and existing empty files.
    path.writeText(encode(spec), Charsets.UTF_8, CREATE_NEW, WRITE)
}

class InitWizard(private val io: WizardIO, output: Path) {
    private var output = output.toAbsolutePath().normalize()
    private val answers = linkedMapOf<String, JsonElement>()
    private var preset = presets[1]
    private var uiSelected = false
    private var componentEdit: String? = null
    private val steps = listOf("basics", "recipe", "targets", "sdk", "components", "agents")
    private fun spec() = JsonObject(answers)
    private fun string(key: String) = (answers[key] as? JsonPrimitive)?.content
    private fun put(key: String, v: Any?) { answers[key] = value(v) }
    private fun raw(prompt: String): String {
        val answer = io.read(prompt)?.trim() ?: throw Cancel()
        when (answer.lowercase()) { "back" -> throw Back(); "cancel", "quit" -> throw Cancel() }
        return answer
    }
    private fun text(label: String, default: String? = null, validate: (String) -> String? = { null }): String {
        while (true) {
            val entered = raw("$label${default?.let { " [$it]" } ?: ""}: ")
            val answer = entered.ifEmpty { default ?: "" }
            val error = if (answer.isEmpty()) "A value is required" else validate(answer)
            if (error == null) return answer
            io.say(error)
        }
    }
    private fun choices(label: String, allowed: List<String>, defaults: List<String>): List<String> {
        io.say(allowed.mapIndexed { i, name -> "${i + 1}) $name" }.joinToString("  "))
        while (true) {
            val entered = raw("$label (IDs or comma-separated numbers) [${defaults.joinToString(",").ifEmpty { "none" }}]: ")
            val selected = if (entered.isEmpty()) defaults else entered.split(',').map { item ->
                val token = item.trim(); token.toIntOrNull()?.let { allowed.getOrNull(it - 1) ?: "<invalid:$token>" } ?: token
            }.distinct()
            if (selected.isNotEmpty() && selected.all { it in allowed }) return selected
            io.say("Choose ${allowed.joinToString(",")}.")
        }
    }
    private fun basics() {
        output = Path.of(text("Output directory", output.toString()) { s ->
            try { validateOutput(Path.of(s).toAbsolutePath().normalize()); null } catch (e: Exception) { e.message ?: "Invalid output" }
        }).toAbsolutePath().normalize()
        put("name", text("Project name", string("name")) { if (it.matches(Regex("[A-Z][A-Za-z0-9]*"))) null else "Use a PascalCase identifier" })
        val identifier: (String) -> String? = {
            if (it.matches(Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+")) && it.split('.').none { p -> p in keywords }) null else "Use dotted lowercase non-keyword identifiers, e.g. com.example.app"
        }
        put("package", text("Package", string("package"), identifier))
        put("app_id", text("Application / bundle ID", string("app_id") ?: string("package"), identifier))
    }
    private fun recipe() {
        io.say("Choose a starting recipe:")
        presets.forEachIndexed { index, p -> io.say("${index + 1}) ${p.label}") }
        val selected = text("Recipe number", "${presets.indexOf(preset) + 1}") {
            if (it.toIntOrNull() in 1..presets.size) null else "Choose a number from 1 to ${presets.size}"
        }.toInt()
        val previous = preset; preset = presets[selected - 1]
        put("kind", preset.kind)
        if (!uiSelected) put("ui", preset.ui)
        if (previous != preset && answers.keys.any { it in listOf("targets", "sdk") }) {
            io.say("Recipe changed. Existing targets and SDKs are retained; incompatible answers must be corrected.")
            io.say("Explicit UI, DI and navigation answers are also retained.")
            showIssues(resolveSpec(spec(), output).issues)
        }
        io.say("UI: ${string("ui")}. Modern UI, DI and navigation are editable in components.")
        io.say("Architecture: compact Kotlin Toolchain starter with selectable DI/navigation.")
    }
    private fun targets() {
        val current = answers["targets"]?.jsonArray?.map { it.jsonPrimitive.content } ?: preset.targets
        io.say(if (preset.kind == "kmp") "Select any nonempty platform subset." else "This recipe requires ${preset.targets.joinToString(",")}.")
        var defaults = current
        while (true) {
            val selected = choices("Platforms", platforms, defaults)
            put("targets", selected)
            val errors = resolveSpec(spec(), output).issues.filter { it.field == "targets" }
            if (errors.isEmpty()) break
            showIssues(errors); defaults = selected
        }
        // Other fields can become incompatible with iOS or Android; retain and explain them.
        showIssues(resolveSpec(spec(), output).issues)
    }
    private fun sdk() {
        val old = (answers["sdk"] as? JsonObject)?.toMutableMap() ?: linkedMapOf()
        val upper = 37
        io.say("Defaults: min 26, target/compile 37. Android compile must be 37.")
        for ((key, default) in listOf("min" to 26, "target" to upper, "compile" to upper)) {
            val answer = text("SDK $key", old[key]?.jsonPrimitive?.content ?: "$default") {
                if (it.toIntOrNull() != null) null else "Enter an integer SDK level"
            }
            old[key] = JsonPrimitive(answer.toInt()); answers["sdk"] = JsonObject(old.toMap())
        }
        put("ios_deployment_target", text("iOS minimum", string("ios_deployment_target") ?: "15.0") {
            if (it.matches(Regex("(?:1[5-9]|2[0-9])\\.\\d+"))) null else "Use a version from 15.0 through 29.x"
        })
        showIssues(resolveSpec(spec(), output).issues)
    }
    private fun stackChoice(field: String, allowed: List<String>, default: String) {
        io.say("$field compatibility for the current answers:")
        allowed.forEach { candidate ->
            val errors = resolveSpec(JsonObject(answers + (field to value(candidate))), output).issues
                .filter { it.field == field || field == "navigation" && it.field == "ios_deployment_target" }
            io.say("  $candidate: ${if (errors.isEmpty()) "compatible" else errors.joinToString("; ") { it.message }}")
        }
        while (true) {
            val selected = choices(field, allowed, listOf(string(field) ?: default))
            if (selected.size != 1) { io.say("Choose exactly one $field."); continue }
            put(field, selected.single())
            if (field == "ui") uiSelected = true
            showIssues(resolveSpec(spec(), output).issues)
            return
        }
    }
    private fun components() {
        if (componentEdit == "endpoint") { endpoint(); return }

        if (componentEdit == null || componentEdit == "ui") stackChoice("ui", listOf("android-compose", "shared-compose", "swiftui"), preset.ui)
        if (componentEdit == null || componentEdit == "di") stackChoice("di", listOf("manual", "koin", "hilt"), "manual")
        if (componentEdit == null || componentEdit == "navigation") stackChoice("navigation", listOf("none", "navigation3", "platform-native"), "none")
        if (componentEdit == null || componentEdit == "network") {
            stackChoice("network", listOf("none", "ktor", "retrofit"), "none")
            if (string("network") == "ktor") endpoint()
        }
        if (componentEdit == null || componentEdit == "storage") stackChoice("storage", listOf("none", "settings", "room", "datastore"), "none")
    }
    private fun endpoint() {
        val answer = text("HTTPS endpoint (clear removes the explicit URL)", string("network_url") ?: if (string("network") == "ktor") demoEndpoint else "clear") {
            if (it == "clear") null else dataIssues("ktor", "none", it).firstOrNull()?.message
        }
        if (answer == "clear") answers.remove("network_url") else put("network_url", answer)
        io.say("Networking happens only on the Fetch action. Browser endpoints require CORS permission; URL validation does not check it.")
        showIssues(resolveSpec(spec(), output).issues)
    }
    private fun agents() {
        var current = answers["agents"]?.jsonArray?.map { it.jsonPrimitive.content } ?: listOf("codex", "claude")
        io.say("AGENTS.md is always emitted; choose additional agent adapters.")
        while (true) {
            current = choices("Agents", agentChoices, current)
            put("agents", current)
            val errors = resolveSpec(spec(), output).issues.filter { it.field == "agents" }
            if (errors.isEmpty()) return
            showIssues(errors)
        }
    }
    private fun showIssues(issues: List<Issue>) { issues.forEach { io.say("[${it.field}] ${it.message}") } }
    private fun review(result: PlanningResult) {
        io.say("\nReview — $output")
        if (!result.valid) {
            showIssues(result.issues)
            io.say("Correct these choices before create or save.")
            return
        }
        val plan = result.plan!!; val doc = plan.document()
        io.say("${plan.options.name}: ${preset.label}; targets ${plan.options.targets.joinToString(",")}")
        io.say("UI: ${plan.spec.text("ui")}; SDK ${plan.options.min}/${plan.options.target}/${plan.options.compile}; iOS ${plan.options.ios}")
        io.say("DI: ${plan.options.di}; navigation: ${plan.options.navigation}; component pins: ${doc.getValue("stack").jsonObject.getValue("pins")}")
        io.say("Network: ${plan.options.network}; storage: ${plan.options.storage}; endpoint: ${plan.options.networkUrl ?: "none"}")
        io.say("Data adapters: ${doc.getValue("stack").jsonObject.getValue("network_adapters")}; ${doc.getValue("stack").jsonObject.getValue("storage_adapters")}")
        io.say("Agents: ${plan.options.agents.joinToString(",")}")
        io.say("Modules: ${doc.strings("modules").joinToString(",")}")
        io.say("${plan.rendered.files.size} files, including portable bootstrap.spec.json and bootstrap.lock.json.")
        doc.strings("requirements").forEach(io::say)
        io.say("Build checks: NOT_RUN. Generation does not install SDKs or run builds.")
        doc.getValue("verification").jsonObject.getValue("commands").jsonArray.forEach { io.say(it.jsonObject.text("command") + " (" + it.jsonObject.text("requires") + ")") }
        io.say("Use details for the full file plan, exact module configuration and lock.")
    }
    fun run(): Int {
        io.say("Android & KMP Bootstrap — init\nEnter accepts the shown value. Use back or cancel at any prompt. Nothing is written before create/save.")
        var step = 0; var editing = false
        try {
            while (true) {
                try {
                    if (step < steps.size) {
                        io.say("\n${step + 1}/${steps.size}: ${steps[step]}")
                        when (step) { 0 -> basics(); 1 -> recipe(); 2 -> targets(); 3 -> sdk(); 4 -> components(); 5 -> agents() }
                        componentEdit = null
                        step = if (editing) steps.size else step + 1
                        editing = false
                        continue
                    }
                    val result = planProject(spec(), output)
                    review(result)
                    io.say("Actions: create, details, save, edit <basics|recipe|targets|sdk|components|ui|di|navigation|network|storage|endpoint|agents>, back, cancel")
                    when (val action = raw("> ")) {
                        "create" -> if (result.valid) {
                            // Revalidate before publication, including a destination populated during review.
                            val fresh = planProject(spec(), output)
                            if (!fresh.valid) showIssues(fresh.issues) else {
                                io.say(encode(publishPlan(fresh.plan!!)))
                                return 0
                            }
                        }
                        "details" -> io.say(encode(result.document()))
                        "save" -> if (result.valid) {
                            val path = Path.of(text("Save config path (outside output; existing parent required)" )).toAbsolutePath().normalize()
                            saveSpec(path, output, result.plan!!.spec)
                            io.say("Saved config: $path. Project not created.")
                            return 0
                        }
                        else -> {
                            val target = action.removePrefix("edit ")
                            val index = if (target in listOf("ui", "di", "navigation", "network", "storage", "endpoint")) steps.indexOf("components") else steps.indexOf(target)
                            if (action.startsWith("edit ") && index >= 0) {
                                componentEdit = target.takeIf { it in listOf("ui", "di", "navigation", "network", "storage", "endpoint") }
                                step = index; editing = true
                            }
                            else io.say("Choose one of the listed actions.")
                        }
                    }
                } catch (_: Back) {
                    componentEdit = null
                    step = (step - 1).coerceAtLeast(0); editing = false
                } catch (e: Exception) {
                    if (e is Cancel) throw e
                    io.say("Cannot complete this action: ${e.message}. Correct the input or cancel.")
                }
            }
        } catch (_: Cancel) {
            io.say("Cancelled. No project or config was written.")
            return 0
        }
    }
}
fun init(args: List<String>, console: Console? = System.console()): Int {
    val a = Arguments(args, emptySet())
    require(a.positional.size <= 1) { "Usage: init [OUTPUT]" }
    if (console == null) {
        print(encode(mapOf("valid" to false, "issues" to listOf(Issue("terminal", "interactive_required", "init requires a terminal. Agents/CI: use catalog --json, collect a config, then plan --config SPEC --output OUTPUT and generate --config SPEC --output OUTPUT.").document()))))
        return 1
    }
    return InitWizard(ConsoleIO(console), Path.of(a.positional.singleOrNull() ?: ".")).run()
}
