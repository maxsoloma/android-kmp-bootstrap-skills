package bootstrap

import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.*

class PlanningTest {
    private val temp = Files.createTempDirectory("bootstrap-plan-")
    private val out = temp.resolve("missing/parent/app")
    @After fun cleanup() { temp.toFile().deleteRecursively() }
    private fun spec(vararg fields: Pair<String, Any?>) = value(mapOf("name" to "Aurora", "package" to "com.example.aurora") + fields).jsonObject
    private fun hashes(files: Map<String, ByteArray>) = files.mapValues { sha256(it.value) }
    @Test fun planningAndConfigGenerationPreserveEveryRecipe() {
        val choices = (1..15).map { mask -> spec("targets" to platforms.filterIndexed { i, _ -> mask and (1 shl i) != 0 }) } +
            listOf(spec("kind" to "android"))
        for (choice in choices) {
            val result = planProject(choice, out)
            assertTrue(result.valid, result.issues.toString()); val plan = result.plan!!
            val original = renderModern(plan.options)
            assertEquals(hashes(original.files), hashes(plan.rendered.files.filterKeys { it !in listOf("bootstrap.spec.json", "bootstrap.lock.json") }))
            assertEquals("NOT_RUN", plan.document().getValue("verification").jsonObject.text("status"))
            assertFalse(out.parent.exists())
        }
    }
    @Test fun planShowsDependenciesPinsModulesAndRealVerification() {
        val plan = planProject(spec("targets" to listOf("desktop", "web"), "di" to "koin", "network" to "ktor", "storage" to "settings"), out).plan!!
        val document = plan.document()
        assertEquals(listOf("desktopApp", "shared", "webApp"), document.strings("modules"))
        assertTrue("0.13.0" in document.getValue("toolchain").toString())
        assertTrue("./kotlin test --platform wasmJs" in document.getValue("verification").toString())
        assertFalse("gradle_wrapper" in document.getValue("toolchain").jsonObject)
    }
    @Test fun lockReplaysWithoutPathsAndDetectsChanges() {
        val first = planProject(spec(), out).plan!!
        val second = planProject(first.spec, temp.resolve("other"), first.lock).plan!!
        assertEquals(hashes(first.rendered.files), hashes(second.rendered.files))
        assertFalse(encode(first.spec).contains(temp.toString())); assertFalse(encode(first.lock).contains(temp.toString()))
        assertFalse(planProject(spec("name" to "Changed"), out, first.lock).valid)
        for (key in first.lock.keys) {
            val changed = JsonObject(first.lock + (key to JsonPrimitive("changed")))
            assertEquals("lock_mismatch", planProject(first.spec, out, changed).issues.single().code)
        }
        assertFalse(planProject(first.spec, out, JsonObject(first.lock + ("unknown" to JsonPrimitive(1)))).valid)
        assertFalse(out.parent.exists())
    }
    @Test fun configCommandAutomaticallyLoadsSiblingLockAndNeverOverwrites() {
        val first = planProject(spec("targets" to listOf("desktop")), out).plan!!
        val config = temp.resolve("bootstrap.spec.json"); config.writeText(encode(first.spec))
        temp.resolve("bootstrap.lock.json").writeText(encode(first.lock))
        val args = listOf("--config", config.toString(), "--output", out.toString())
        assertEquals(0, configCommand(args, false)); assertFalse(out.parent.exists())
        assertEquals(0, configCommand(args + "--dry-run", true)); assertFalse(out.parent.exists())
        config.writeText(encode(JsonObject(first.spec + ("name" to JsonPrimitive("Other")))))
        assertEquals(1, configCommand(args, true)); assertFalse(out.parent.exists())
        config.writeText(encode(first.spec))
        assertEquals(0, configCommand(args, true)); assertTrue(out.resolve("bootstrap.spec.json").exists())
        val before = out.resolve("bootstrap.json").readBytes()
        assertEquals(1, configCommand(args, true)); assertContentEquals(before, out.resolve("bootstrap.json").readBytes())
    }
    @Test fun malformedMissingMixedAndUnsafeInputsFailWithoutWrites() {
        val config = temp.resolve("config.json")
        val args = listOf("--config", config.toString(), "--output", out.toString())
        for (content in listOf("{", "[]", "null", "{}", "{\"name\":false}")) {
            config.writeText(content); assertEquals(1, configCommand(args, true)); assertFalse(out.parent.exists())
        }
        config.writeText(encode(spec()))
        assertEquals(1, configCommand(args + listOf("--kind", "android"), true))
        Files.createDirectories(out); out.resolve("notes").writeText("keep")
        assertEquals(1, configCommand(args, true)); assertEquals("keep", out.resolve("notes").readText())
        if (!windows) {
            val link = temp.resolve("symlink"); Files.createSymbolicLink(link, temp.resolve("absent"))
            assertFalse(planProject(spec(), link).valid); assertTrue(Files.isSymbolicLink(link))
        }
    }
}
