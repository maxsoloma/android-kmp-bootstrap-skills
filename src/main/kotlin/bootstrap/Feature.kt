package bootstrap

import kotlinx.serialization.json.*
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*
import java.nio.file.StandardOpenOption.*
import java.time.Instant
import kotlin.io.path.*

/** Cooperative task admission and evidence journal. It never launches models or executes checks. */
private val featureId = Regex("[a-z0-9][a-z0-9-]{0,63}")
private val featureRoles = setOf("developer", "tester", "reviewer", "mechanical")
private fun JsonObject.number(key: String) = getValue(key).jsonPrimitive.int
private fun JsonObject.update(vararg fields: Pair<String, Any?>) = JsonObject(this + fields.associate { it.first to value(it.second) })
private fun nonempty(s: String) = s.also { require(it.isNotBlank()) { "Nonempty text required" } }
private fun safeScope(s: String): Path {
    val p = Path.of(s)
    require(s.isNotBlank() && !p.isAbsolute && p.normalize() == p && p.nameCount > 0 &&
        p.none { it.toString() in setOf("..", ".", ".git", ".feature-runs", "build", ".kotlin", ".amper") }) { "Unsafe source scope: $s" }
    return p
}
private fun regular(path: Path) {
    require(Files.isRegularFile(path, NOFOLLOW_LINKS)) { "Expected regular file: $path" }
}
private fun readFeatureObject(path: Path): JsonObject { regular(path); return Json.parseToJsonElement(path.readText()).jsonObject }
private fun featurePlan(plan: JsonObject): JsonObject {
    require(plan.keys == setOf("goal", "kind", "tasks", "limits")) { "Plan requires goal, kind, tasks, limits only" }
    nonempty(plan.text("goal"))
    require(plan.text("kind") in setOf("behavioral", "mechanical")) { "Unknown feature kind" }
    val limits = plan.getValue("limits").jsonObject
    require(limits.keys == setOf("attempts", "repairs", "seconds", "packet_bytes")) { "Unknown/missing limits" }
    limits.keys.forEach { require(limits.number(it) > 0) { "Limits must be positive integers" } }
    val tasks = plan.getValue("tasks").jsonArray.map { it.jsonObject }
    require(tasks.isNotEmpty() && tasks.size <= 100) { "Require 1..100 tasks" }
    val ids = tasks.map { it.text("id") }
    require(ids.distinct().size == ids.size && ids.all { featureId.matches(it) }) { "Invalid/duplicate task ID" }
    tasks.forEach { task ->
        require(task.keys == setOf("id", "role", "depends_on", "files", "acceptance", "checks")) { "Unknown/missing task fields" }
        require(task.text("role") in featureRoles) { "Unknown role" }
        require(task.strings("files").isNotEmpty()) { "Source scope required" }
        task.strings("files").forEach(::safeScope)
        require(task.strings("acceptance").isNotEmpty()) { "Acceptance required" }
        task.strings("acceptance").forEach(::nonempty)
        task.strings("checks").forEach(::nonempty)
        require(task.strings("depends_on").all { it in ids && it != task.text("id") }) { "Unknown/self dependency" }
    }
    val visited = mutableSetOf<String>(); val visiting = mutableSetOf<String>()
    fun visit(id: String) {
        if (id in visited) return
        require(visiting.add(id)) { "Cyclic task dependencies" }
        tasks.single { it.text("id") == id }.strings("depends_on").forEach(::visit)
        visiting.remove(id); visited.add(id)
    }
    ids.forEach(::visit)
    require(tasks.count { it.text("role") == "reviewer" } == 1) { "One final reviewer required" }
    if (plan.text("kind") == "behavioral") require(tasks.any { it.text("role") == "developer" } && tasks.any { it.text("role") == "tester" }) { "Behavioral work requires developer and tester tasks" }
    return plan
}

/** Bind evidence to declared source bytes, including uncommitted/new/deleted files. */
private fun featureSource(project: Path, plan: JsonObject): String {
    val paths = sortedSetOf<String>()
    val scopes = plan.getValue("tasks").jsonArray.flatMap { it.jsonObject.strings("files") } + listOf("project.yaml", "kotlin", "kotlin.bat", "bootstrap.json")
    scopes.forEach { name ->
        val relative = safeScope(name); val target = project.resolve(relative)
        var ancestor = project
        relative.forEach { part -> ancestor = ancestor.resolve(part); require(!Files.isSymbolicLink(ancestor)) { "Symlink in source scope: $name" } }
        if (Files.isDirectory(target, NOFOLLOW_LINKS)) Files.walk(target).use { walk ->
            walk.forEach { file ->
                require(!Files.isSymbolicLink(file)) { "Symlink in source scope: $file" }
                if (Files.isRegularFile(file, NOFOLLOW_LINKS)) paths += project.relativize(file).toString().replace('\\', '/')
            }
        } else paths += name
    }
    return sha256(encode(paths.map { name ->
        val path = project.resolve(name)
        listOf(name, if (Files.exists(path, NOFOLLOW_LINKS)) { regular(path); sha256(path.readBytes()) } else "MISSING")
    }).toByteArray())
}
private fun overlap(a: String, b: String): Boolean = Path.of(a).startsWith(Path.of(b)) || Path.of(b).startsWith(Path.of(a))
private fun saveFeature(path: Path, state: JsonObject) {
    val temp = Files.createTempFile(path.parent, ".state-", ".tmp")
    try {
        temp.writeText(encode(state))
        Files.move(temp, path, ATOMIC_MOVE, REPLACE_EXISTING)
    } finally { Files.deleteIfExists(temp) }
}

fun feature(args: List<String>): Int {
    require(args.isNotEmpty()) { "feature init|status|claim|settle|finish PROJECT --run ID; see the skill's references/ledger.md" }
    val command = args.first()
    val allowed = when (command) {
        "init" -> setOf("run", "plan")
        "status" -> setOf("run")
        "claim" -> setOf("run", "task", "owner", "packet", "reason")
        "settle" -> setOf("run", "task", "owner", "outcome", "evidence")
        "finish" -> setOf("run", "outcome", "evidence")
        else -> error("Unknown feature operation: $command")
    }
    val a = Arguments(args.drop(1), allowed)
    val project = a.path().toRealPath()
    regular(project.resolve("project.yaml"))
    require(Files.isRegularFile(project.resolve("kotlin"), NOFOLLOW_LINKS) || Files.isRegularFile(project.resolve("kotlin.bat"), NOFOLLOW_LINKS)) { "Existing Kotlin Toolchain project required; no migration is performed" }
    val run = a.get("run").also { require(featureId.matches(it)) { "Invalid run ID" } }
    val root = project.resolve(".feature-runs")
    require(!Files.isSymbolicLink(root)) { "State directory must not be a symlink" }
    if (command == "init") Files.createDirectories(root)
    require(root.isDirectory()) { "No feature runs; initialize an explicit plan first" }
    val runs = root.resolve("runs")
    require(!Files.isSymbolicLink(runs)) { "Run directory must not be a symlink" }
    if (command == "init") Files.createDirectories(runs)
    require(runs.isDirectory()) { "No saved run directory" }
    val lockPath = root.resolve(".lock")
    require(!Files.isSymbolicLink(lockPath)) { "Lock must not be a symlink" }
    FileChannel.open(lockPath, CREATE, WRITE).use { channel -> channel.lock().use {
        val statePath = runs.resolve("$run.json")
        require(!Files.isSymbolicLink(statePath)) { "State must not be a symlink" }
        if (command == "init") {
            require(!statePath.exists()) { "Run already exists; use status and resume it" }
            val plan = featurePlan(readFeatureObject(Path.of(a.get("plan"))))
            val source = featureSource(project, plan)
            val state = value(mapOf("schema_version" to 1, "cli_version" to CLI_VERSION, "run" to run, "project" to project.toString(), "created" to Instant.now().epochSecond,
                "status" to "active", "plan" to plan, "initial_source" to source, "attempts" to emptyList<Any>())).jsonObject
            saveFeature(statePath, state); print(encode(state)); return 0
        }
        var state = readFeatureObject(statePath)
        require(state.number("schema_version") == 1 && state.text("cli_version") == CLI_VERSION && state.text("project") == project.toString()) { "Run schema/version/project mismatch; reconcile explicitly" }
        val plan = featurePlan(state.getValue("plan").jsonObject)
        val tasks = plan.getValue("tasks").jsonArray.map { task -> task.jsonObject }
        val attempts = state.getValue("attempts").jsonArray.map { attempt -> attempt.jsonObject }.toMutableList()
        fun latest(id: String) = attempts.lastOrNull { attempt -> attempt.text("task") == id }
        fun passed(id: String) = latest(id)?.text("outcome") == "passed"
        if (command == "status") {
            print(encode(state.update("current_source" to featureSource(project, plan), "ready" to tasks.filter { task ->
                !passed(task.text("id")) && latest(task.text("id"))?.text("outcome") != "running" && task.strings("depends_on").all(::passed)
            }.map { task -> task.text("id") }))); return 0
        }
        require(state.text("status") == "active") { "Terminal run cannot be reopened" }
        val source = featureSource(project, plan)
        if (command == "claim") {
            val limits = plan.getValue("limits").jsonObject
            require(Instant.now().epochSecond - state.getValue("created").jsonPrimitive.long < limits.number("seconds")) { "Run deadline exhausted" }
            require(attempts.size < limits.number("attempts")) { "Shared attempt allowance exhausted" }
            val id = a.get("task"); val task = tasks.single { task -> task.text("id") == id }
            val owner = nonempty(a.get("owner")); val packet = Path.of(a.get("packet")); regular(packet)
            val bytes = packet.readBytes()
            require(bytes.isNotEmpty() && bytes.size <= limits.number("packet_bytes")) { "Packet exceeds allowance or is empty" }
            val packetHash = sha256(bytes)
            require(latest(id)?.text("outcome") != "running") { "Task already running; reconcile existing owner" }
            require(task.strings("depends_on").all(::passed)) { "Dependencies have not passed" }
            if (task.text("role") != "reviewer") {
                require(attempts.none { attempt -> attempt.text("owner") == owner && attempt.text("role") == "reviewer" }) { "A reviewer cannot become an author in this run" }
            }
            if (task.text("role") == "reviewer") {
                require(tasks.filter { task -> task.text("role") != "reviewer" }.all { task -> passed(task.text("id")) }) { "Review requires all author/check tasks passed" }
                require(attempts.none { attempt -> attempt.text("owner") == owner && attempt.text("role") != "reviewer" }) { "Reviewer must be independent of authors" }
            }
            if (latest(id) != null) {
                nonempty(a.get("reason"))
                require(attempts.count { attempt -> attempt.getValue("repair").jsonPrimitive.boolean } < limits.number("repairs")) { "Shared repair allowance exhausted" }
                require(attempts.none { attempt -> attempt.text("task") == id && attempt.text("source") == source && attempt.text("packet_sha256") == packetHash }) { "Unchanged retry rejected" }
            }
            // Serialize all runs in this project when their active write scopes overlap.
            Files.list(runs).use { records -> records.filter { path -> path.extension == "json" }.forEach { path ->
                val peer = readFeatureObject(path)
                val peerTasks = peer.getValue("plan").jsonObject.getValue("tasks").jsonArray.map { task -> task.jsonObject }
                peer.getValue("attempts").jsonArray.map { attempt -> attempt.jsonObject }.filter { attempt -> attempt.text("outcome") == "running" }.forEach { attempt ->
                    val active = peerTasks.single { task -> task.text("id") == attempt.text("task") }
                    require(task.strings("files").none { file -> active.strings("files").any { other -> overlap(file, other) } }) { "Scope owned by active run ${peer.text("run")}/${attempt.text("task")}" }
                }
            } }
            attempts += value(mapOf("task" to id, "role" to task.text("role"), "owner" to owner, "source" to source, "packet_sha256" to packetHash,
                "repair" to (latest(id) != null), "reason" to a.options["reason"], "outcome" to "running", "usage" to null)).jsonObject
        } else if (command == "settle") {
            val id = a.get("task"); val index = attempts.indexOfLast { attempt -> attempt.text("task") == id }
            require(index >= 0) { "Task has not been claimed" }
            val previous = attempts[index]
            require(previous.text("outcome") == "running" && previous.text("owner") == a.get("owner")) { "Only the recorded running owner can settle" }
            val outcome = a.get("outcome").also { require(it in setOf("passed", "failed", "blocked")) { "Invalid outcome" } }
            val evidencePath = Path.of(a.get("evidence")).toAbsolutePath()
            val evidence = readFeatureObject(evidencePath); nonempty(evidence.text("summary"))
            if (outcome == "passed") {
                val checks = evidence.getValue("checks").jsonArray
                require(checks.isNotEmpty() && checks.all { check -> check.jsonObject.text("status") == "passed" && check.jsonObject.text("command").isNotBlank() && check.jsonObject.text("log").isNotBlank() }) { "Passing evidence requires actual passed checks with command/log references" }
                if (previous.text("role") == "reviewer") {
                    require(evidence.text("verdict") == "approved" && source == previous.text("source")) { "Review must approve an unchanged candidate" }
                }
            }
            attempts[index] = previous.update("outcome" to outcome, "final_source" to source, "evidence_path" to evidencePath.toString(), "evidence_sha256" to sha256(evidencePath.readBytes()), "evidence" to evidence)
        } else {
            val outcome = a.get("outcome").also { require(it in setOf("completed", "blocked")) { "Invalid finish outcome" } }
            require(attempts.none { attempt -> attempt.text("outcome") == "running" }) { "Reconcile active attempts before finishing" }
            if (outcome == "completed") {
                require(tasks.all { task -> passed(task.text("id")) }) { "Incomplete tasks" }
                require(tasks.filter { task -> task.text("role") in setOf("tester", "reviewer") }.all { task -> latest(task.text("id"))?.text("final_source") == source }) { "Test/review evidence is stale; recheck the current candidate" }
            }
            val evidence = Path.of(a.get("evidence")); regular(evidence)
            state = state.update("status" to outcome, "final_source" to source, "handoff" to nonempty(evidence.readText()))
        }
        state = state.update("attempts" to attempts)
        saveFeature(statePath, state); print(encode(state)); return 0
    } }
}
