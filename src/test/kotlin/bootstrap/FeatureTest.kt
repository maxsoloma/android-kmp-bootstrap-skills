package bootstrap

import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.*

class FeatureTest {
    private val temp = Files.createTempDirectory("feature-test-")
    private val app = temp.resolve("app with spaces").also { Files.createDirectories(it); it.resolve("project.yaml").writeText("product: jvm/app\n"); it.resolve("kotlin").writeText("preserved wrapper"); it.resolve("src").createDirectory(); it.resolve("test").createDirectory() }
    private val packet = temp.resolve("packet.md").also { it.writeText("Implement the requested behavior using the specified interfaces.") }
    private val evidence = temp.resolve("evidence.json").also { it.writeText(encode(mapOf("summary" to "Observed checks", "checks" to listOf(mapOf("command" to "actual check", "status" to "passed", "log" to "/tmp/check.log")), "verdict" to "approved"))) }
    private val handoff = temp.resolve("handoff.md").also { it.writeText("Actual outcome and remaining gaps.") }
    @After fun clean() { temp.toFile().deleteRecursively() }
    private fun task(id: String, role: String, dependencies: List<String>, files: List<String>) = mapOf("id" to id, "role" to role, "depends_on" to dependencies, "files" to files, "acceptance" to listOf("Observable outcome"), "checks" to listOf("Execute relevant checks"))
    private fun plan(attempts: Int = 12, repairs: Int = 6, tasks: List<Map<String, Any>> = listOf(task("code", "developer", emptyList(), listOf("src")), task("tests", "tester", listOf("code"), listOf("test")), task("review", "reviewer", listOf("tests"), listOf("src", "test")))): Path = temp.resolve("plan.json").also { it.writeText(encode(mapOf("goal" to "Implement feature", "kind" to "behavioral", "limits" to mapOf("attempts" to attempts, "repairs" to repairs, "seconds" to 7200, "packet_bytes" to 16384), "tasks" to tasks))) }
    private fun op(command: String, vararg rest: String, run: String = "example") = feature(listOf(command, app.toString(), "--run", run) + rest)
    private fun init(attempts: Int = 12, repairs: Int = 6) = op("init", "--plan", plan(attempts, repairs).toString())
    private fun claim(id: String, owner: String = id, reason: String? = null, run: String = "example") = op("claim", "--task", id, "--owner", owner, "--packet", packet.toString(), *(if (reason == null) emptyArray() else arrayOf("--reason", reason)), run = run)
    private fun settle(id: String, outcome: String = "passed", owner: String = id) = op("settle", "--task", id, "--owner", owner, "--outcome", outcome, "--evidence", evidence.toString())
    private fun finish(outcome: String = "completed") = op("finish", "--outcome", outcome, "--evidence", handoff.toString())
    private fun journal() = app.resolve(".feature-runs/runs/example.json").readText()
    private fun completeAuthors() { claim("code"); settle("code"); claim("tests"); settle("tests") }

    @Test fun workflowSurvivesReloadAndKeepsAppUntouched() {
        init(); assertFails { claim("tests") }; completeAuthors()
        assertFails { claim("review", owner = "code") }
        claim("review"); settle("review"); finish()
        val before = journal(); op("status"); assertEquals(before, journal())
        assertFails { claim("code", reason = "more") }; assertFails { finish("blocked") }; assertFails { init() }
        assertEquals("preserved wrapper", app.resolve("kotlin").readText())
        assertEquals("completed", Json.parseToJsonElement(before).jsonObject.text("status"))
    }
    @Test fun failedAttemptsCannotBeDuplicatedAndBudgetsSurviveRestart() {
        init(attempts = 2); claim("code"); settle("code", "failed")
        val before = journal(); assertFails { claim("code", reason = "retry") }; assertEquals(before, journal())
        packet.writeText("New evidence: handle the negative outcome.")
        claim("code", reason = "Changed requirements packet after diagnosis"); settle("code")
        assertFails { claim("tests") }; finish("blocked")
        assertEquals(2, Json.parseToJsonElement(journal()).jsonObject.getValue("attempts").jsonArray.size)
    }
    @Test fun crossRunOwnershipAndSettlementAreExclusive() {
        init(); claim("code", owner = "worker-1")
        assertFails { settle("code", owner = "worker-2") }; assertFails { claim("code") }
        op("init", "--plan", plan().toString(), run = "other")
        assertFails { claim("code", run = "other") }
        settle("code", owner = "worker-1")
        claim("code", run = "other")
    }
    @Test fun sourceChangesInvalidateTestsAndReviewIncludingUncommittedNewFiles() {
        init(); completeAuthors(); claim("review"); settle("review")
        app.resolve("src/new.kt").writeText("changed behavior")
        val before = journal(); assertFails { finish() }; assertEquals(before, journal())
        claim("tests", reason = "Verify changed source"); settle("tests")
        claim("review", reason = "Review changed source"); settle("review"); finish()
    }
    @Test fun reviewCannotEditCandidateAndSkippedChecksCannotPass() {
        init(); completeAuthors(); claim("review")
        app.resolve("src/change.kt").writeText("reviewer edit")
        assertFails { settle("review") }
        settle("review", "failed")
        evidence.writeText("""{"summary":"Unavailable host","checks":[{"command":"check","status":"skipped","log":"log"}]}""")
        claim("tests", reason = "Check changed candidate"); assertFails { settle("tests") }
        settle("tests", "blocked"); finish("blocked")
    }
    @Test fun malformedPlansCyclesAndUnsafeScopesFailBeforeStatePublication() {
        val cycle = listOf(task("code", "developer", listOf("tests"), listOf("src")), task("tests", "tester", listOf("code"), listOf("test")), task("review", "reviewer", listOf("tests"), listOf("src")))
        assertFails { op("init", "--plan", plan(tasks = cycle).toString()) }
        for (scope in listOf("../escape", "/absolute", ".feature-runs", ".", "src/../test")) {
            val invalid = listOf(task("code", "developer", emptyList(), listOf(scope)), task("tests", "tester", listOf("code"), listOf("test")), task("review", "reviewer", listOf("tests"), listOf("src")))
            assertFails(scope) { op("init", "--plan", plan(tasks = invalid).toString()) }
        }
        assertFalse(app.resolve(".feature-runs/runs/example.json").exists())
    }
    @Test fun nonToolchainProjectAndPacketOverageRejectBeforeWork() {
        app.resolve("project.yaml").deleteExisting(); assertFails { init() }; assertFalse(app.resolve(".feature-runs").exists())
        app.resolve("project.yaml").writeText("product: jvm/app"); init()
        packet.writeText("x".repeat(16385)); assertFails { claim("code") }
        assertTrue(Json.parseToJsonElement(journal()).jsonObject.getValue("attempts").jsonArray.isEmpty())
    }
    @Test fun rootedAndDriveQualifiedScopesRejectBeforeJournalPublicationOnEveryHost() {
        val unsafeScopes = listOf(
            "/absolute", "\\rooted", "C:drive-relative", "C:/absolute", "C:\\absolute",
            "c:drive-relative", "c:/absolute", "c:\\absolute", "\\\\server\\share", "//server/share",
        )
        for ((index, scope) in unsafeScopes.withIndex()) {
            val run = "unsafe-path-$index"
            val tasks = listOf(task("code", "developer", emptyList(), listOf(scope)), task("tests", "tester", listOf("code"), listOf("test")), task("review", "reviewer", listOf("tests"), listOf("src")))
            assertFails(scope) { op("init", "--plan", plan(tasks = tasks).toString(), run = run) }
            assertFalse(app.resolve(".feature-runs/runs/$run.json").exists(), scope)
        }
    }
    @Test fun ordinaryProjectRelativeScopesWithNestedPathsAndSpacesRemainAdmitted() {
        val nested = app.resolve("src/nested directory").also { it.createDirectory() }
        nested.resolve("example.kt").writeText("// Existing project source")
        val scopes = listOf("src", "src/nested directory", "src/nested directory/example.kt")
        val tasks = listOf(task("code", "developer", emptyList(), scopes), task("tests", "tester", listOf("code"), listOf("test")), task("review", "reviewer", listOf("tests"), listOf("src")))
        op("init", "--plan", plan(tasks = tasks).toString())
        assertTrue(app.resolve(".feature-runs/runs/example.json").isRegularFile())
        assertEquals("// Existing project source", nested.resolve("example.kt").readText())
    }
    @Test fun sharedRepairLimitDoesNotResetAcrossTasks() {
        init(repairs = 1); claim("code"); settle("code", "failed")
        packet.writeText("Diagnosed failure; bounded repair")
        claim("code", reason = "repair"); settle("code"); claim("tests"); settle("tests", "failed")
        packet.writeText("New test diagnosis")
        assertFails { claim("tests", reason = "repair tests") }
    }
    @Test fun artifactsBesideRunsDoNotBecomeTaskJournalsAndDeadlineDoesNotBlockSettlement() {
        init()
        app.resolve(".feature-runs/evidence.json").writeText("{}")
        app.resolve(".feature-runs/plan.json").writeText("{}")
        claim("code")
        val statePath = app.resolve(".feature-runs/runs/example.json")
        val state = Json.parseToJsonElement(statePath.readText()).jsonObject
        statePath.writeText(encode(JsonObject(state + ("created" to JsonPrimitive(0)))))
        settle("code")
        assertFails { claim("tests") }
        op("status"); finish("blocked")
    }
    @Test fun symlinkSourceAndStateDirectoriesRejectWithoutFollowingThem() {
        if (windows) return
        val external = temp.resolve("outside").also { it.createDirectory(); it.resolve("keep").writeText("unchanged") }
        app.resolve("src").deleteExisting(); Files.createSymbolicLink(app.resolve("src"), external)
        assertFails { init() }
        assertEquals("unchanged", external.resolve("keep").readText())
        app.resolve("src").deleteExisting(); app.resolve("src").createDirectory()
        app.resolve(".feature-runs/runs").deleteExisting()
        Files.createSymbolicLink(app.resolve(".feature-runs/runs"), external)
        assertFails { init() }
        assertFalse(external.resolve("example.json").exists())
    }
    @Test fun behavioralRoleGapsRejectBeforePublishingJournal() {
        val good = listOf(task("code", "developer", emptyList(), listOf("src")), task("tests", "tester", emptyList(), listOf("test")), task("review", "reviewer", emptyList(), listOf("src", "test")))
        val invalid = listOf(
            good.filterNot { it["role"] == "developer" },
            good.filterNot { it["role"] == "tester" },
            good.filterNot { it["role"] == "reviewer" },
            good + task("second-review", "reviewer", emptyList(), listOf("src")),
            good + task("unknown", "invented", emptyList(), listOf("src")),
        )
        for (tasks in invalid) {
            assertFails { op("init", "--plan", plan(tasks = tasks).toString()) }
            assertFalse(app.resolve(".feature-runs/runs/example.json").exists())
        }
    }
    @Test fun sequentialFallbackCanUseOneAuthorButNeverSelfApproveReview() {
        init(); claim("code", owner = "host"); settle("code", owner = "host")
        claim("tests", owner = "host"); settle("tests", owner = "host")
        assertFails { claim("review", owner = "host") }
    }
    @Test fun standaloneOrchestratorJarRunsOutsideRepository() {
        val repo = Path.of(System.getProperty("bootstrap.repo", ".")).toAbsolutePath()
        val jar = temp.resolve("portable skill/bin/bootstrap.jar"); Files.createDirectories(jar.parent)
        Files.copy(repo.resolve("skills/orchestrate-feature/bin/bootstrap.jar"), jar)
        val java = Path.of(System.getProperty("java.home"), "bin", if (windows) "java.exe" else "java").toString()
        val result = execute(listOf(java, "-jar", jar.toString(), "feature", "init", app.toString(), "--run", "portable", "--plan", plan().toString()), 60, temp.resolve("cli.log"), temp)
        assertEquals(0, result.code, temp.resolve("cli.log").readText())
        assertTrue(app.resolve(".feature-runs/runs/portable.json").isRegularFile())
    }
}
