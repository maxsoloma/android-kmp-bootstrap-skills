package bootstrap

import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.*

class WizardTest {
    private val temp = Files.createTempDirectory("bootstrap-wizard-")
    private val out = temp.resolve("app")
    @After fun cleanup() { temp.toFile().deleteRecursively() }
    private class ScriptedIO(answers: List<String>, val beforeRead: (String) -> Unit = {}) : WizardIO {
        private val input = ArrayDeque(answers)
        val transcript = StringBuilder()
        override fun say(message: String) { transcript.appendLine(message) }
        override fun read(prompt: String): String? {
            transcript.appendLine(prompt); beforeRead(prompt)
            return input.removeFirstOrNull()
        }
    }
    private fun inputs(recipe: Int = 2, di: String = "", navigation: String = "", ios: String = "", network: String = "", storage: String = "") =
        listOf("", "Aurora", "com.example.aurora", "", "$recipe", "", "", "", "", ios) +
            (listOf("", di, navigation, network) + (if (network == "ktor") listOf("") else emptyList()) + listOf(storage)) + listOf("")
    private fun run(input: List<String>, output: Path = out, beforeRead: (String) -> Unit = {}): ScriptedIO {
        val io = ScriptedIO(input, beforeRead)
        assertEquals(0, InitWizard(io, output).run(), io.transcript.toString())
        return io
    }
    @Test fun everyAvailablePresetGeneratesWithTheSamePlanAsConfigMode() {
        for (recipe in 1..4) {
            val output = temp.resolve("recipe-$recipe")
            val io = run(inputs(recipe) + "create", output)
            assertTrue(output.resolve("bootstrap.spec.json").exists(), io.transcript.toString())
            val spec = readObject(output.resolve("bootstrap.spec.json")); val lock = readObject(output.resolve("bootstrap.lock.json"))
            val expected = planProject(spec, temp.resolve("unused-$recipe"), lock)
            assertTrue(expected.valid)
            expected.plan!!.rendered.files.forEach { (path, bytes) -> assertContentEquals(bytes, output.resolve(path).readBytes(), path) }
            assertTrue("NOT_RUN" in io.transcript)
        }
    }
    @Test fun eofAtEveryStepAndExplicitCancelLeaveNoFiles() {
        val all = inputs()
        for (count in 0..all.size) {
            val io = run(all.take(count))
            assertFalse(out.exists()); assertTrue("Cancelled" in io.transcript)
        }
        run(inputs() + "cancel"); assertFalse(out.exists())
        assertTrue(Files.list(temp).use { it.findAny().isEmpty })
    }
    @Test fun invalidAnswersRecoverWithoutAbandoningTheWizard() {
        val io = run(listOf("", "bad name", "Aurora", "wrong", "com.example.aurora", "", "99", "1", "web", "android", "oops", "26", "", "", "") + listOf("", "", "", "", "") + listOf("none,claude", "none", "create"))
        assertTrue(out.exists(), io.transcript.toString())
        assertTrue("Use a PascalCase" in io.transcript)
        assertTrue("Native Android supports only android" in io.transcript)
        assertTrue("Enter an integer" in io.transcript)
        assertEquals(listOf("none"), readObject(out.resolve("bootstrap.spec.json")).strings("agents"))
    }
    @Test fun recipeChangesRetainAndExplainIncompatibleAnswers() {
        val io = run(inputs(3) + listOf("edit recipe", "1", "create", "edit targets", "android", "edit ui", "android-compose", "create"))
        assertTrue(out.exists(), io.transcript.toString())
        assertTrue("Existing targets" in io.transcript); assertTrue("Correct these choices" in io.transcript)
        val spec = readObject(out.resolve("bootstrap.spec.json"))
        assertEquals("android", spec.text("kind")); assertEquals(listOf("android"), spec.strings("targets"))
    }
    @Test fun backAndTargetedEditDoNotResetOtherAnswers() {
        // Back from platforms returns to the recipe menu, preserving basics.
        val answers = listOf("", "Aurora", "com.example.aurora", "", "2", "back", "1", "", "", "", "", "") + listOf("", "", "", "", "", "", "edit agents", "gemini,cursor", "create")
        val io = run(answers)
        assertTrue(out.exists(), io.transcript.toString())
        val spec = readObject(out.resolve("bootstrap.spec.json"))
        assertEquals("Aurora", spec.text("name")); assertEquals("android", spec.text("kind"))
        assertEquals(listOf("gemini", "cursor"), spec.strings("agents"))
    }
    @Test fun saveIsExplicitOutsideOutputAndNeverReplacesFiles() {
        val config = temp.resolve("recipe.json")
        val io = run(inputs() + listOf("save", config.toString()))
        assertTrue(config.exists(), io.transcript.toString()); assertFalse(out.exists())
        val before = config.readText()
        run(inputs() + listOf("save", config.toString(), "cancel"))
        assertEquals(before, config.readText()); assertFalse(out.exists())
        run(inputs() + listOf("save", out.resolve("recipe.json").toString(), "cancel"))
        assertFalse(out.exists())
        if (!windows) {
            val alias = temp.resolve("alias"); Files.createSymbolicLink(alias, temp)
            assertFails { saveSpec(alias.resolve("app/recipe.json"), out, readObject(config)) }
            val link = temp.resolve("recipe-link.json"); Files.createSymbolicLink(link, config)
            assertFails { saveSpec(link, out, readObject(config)) }; assertEquals(before, config.readText())
        }
    }
    @Test fun destinationPopulatedDuringReviewIsPreserved() {
        var reviewSeen = false
        val io = run(inputs() + listOf("create", "cancel"), beforeRead = { prompt ->
            if (prompt == "> " && !reviewSeen) {
                reviewSeen = true; Files.createDirectory(out); out.resolve("notes").writeText("keep")
            }
        })
        assertEquals("keep", out.resolve("notes").readText())
        assertFalse(out.resolve("project.yaml").exists())
        assertTrue("existing files are never overwritten" in io.transcript)
    }
    @Test fun noConsoleReturnsImmediatelyWithoutReadingInput() {
        assertEquals(1, init(listOf(out.toString()), console = null))
        assertFalse(out.exists())
    }
    @Test fun swiftUiMenusGenerateTheSameLockedProjectAsConfig() {
        val io = run(inputs(4, di = "koin", navigation = "platform-native", ios = "16.0") + "create")
        assertTrue(out.exists(), io.transcript.toString())
        val spec = readObject(out.resolve("bootstrap.spec.json"))
        assertEquals("swiftui", spec.text("ui")); assertEquals("koin", spec.text("di"))
        assertEquals("platform-native", spec.text("navigation"))
        val replay = planProject(spec, temp.resolve("copy"), readObject(out.resolve("bootstrap.lock.json"))).plan!!
        replay.rendered.files.forEach { (path, bytes) -> assertContentEquals(bytes, out.resolve(path).readBytes(), path) }
    }
    @Test fun navigationMinimumIsExplainedAndExplicitSdkEditPreservesChoices() {
        val io = run(inputs(4, di = "koin", navigation = "platform-native") + listOf("create", "edit sdk", "", "", "", "16.0", "edit di", "manual", "create"))
        assertTrue(out.exists(), io.transcript.toString()); assertTrue("requires iOS 16.0+" in io.transcript)
        val spec = readObject(out.resolve("bootstrap.spec.json"))
        assertEquals("swiftui", spec.text("ui")); assertEquals("manual", spec.text("di"))
        assertEquals("platform-native", spec.text("navigation")); assertEquals("16.0", spec.text("ios_deployment_target"))
    }
    @Test fun uiAndTargetEditsRetainConflictingNavigationUntilCorrected() {
        val io = run(inputs(2, di = "koin", navigation = "navigation3", ios = "16.0") + listOf(
            "edit ui", "swiftui", "create", "edit navigation", "platform-native",
            "edit targets", "android,ios,desktop", "create", "edit targets", "android,ios", "create",
        ))
        assertTrue(out.exists(), io.transcript.toString())
        assertTrue("Navigation 3 owns Compose screens" in io.transcript)
        assertTrue("remove desktop/web" in io.transcript)
        val spec = readObject(out.resolve("bootstrap.spec.json"))
        assertEquals("koin", spec.text("di")); assertEquals("swiftui", spec.text("ui"))
        assertEquals("platform-native", spec.text("navigation"))
    }
    @Test fun dataChoicesPersistAndDisablingNetworkRequiresExplicitEndpointRemoval() {
        val io = run(inputs(4, di = "koin", network = "ktor", storage = "settings") + listOf(
            "edit network", "none", "create", "edit endpoint", "clear", "create",
        ))
        assertTrue(out.exists(), io.transcript.toString())
        assertTrue("remove it explicitly" in io.transcript)
        val spec = readObject(out.resolve("bootstrap.spec.json"))
        assertEquals("none", spec.text("network")); assertEquals("settings", spec.text("storage"))
        assertFalse("network_url" in spec); assertEquals("koin", spec.text("di"))
    }
    @Test fun networkOnlyAndStorageOnlyWorkIndependentlyAndCancellationDoesNotWrite() {
        for ((network, storage) in listOf("ktor" to "none", "none" to "settings")) {
            val output = temp.resolve("$network-$storage")
            val io = run(inputs(network = network, storage = storage) + "create", output)
            assertTrue(output.exists(), io.transcript.toString())
            val spec = readObject(output.resolve("bootstrap.spec.json"))
            assertEquals(network, spec.text("network")); assertEquals(storage, spec.text("storage"))
        }
        run(inputs(network = "ktor", storage = "settings") + "cancel")
        assertFalse(out.exists())
    }
    @Test fun recipeChangeRetainsDataAndEndpointChoices() {
        val io = run(inputs(network = "ktor", storage = "settings") + listOf("edit recipe", "1", "edit targets", "android", "edit ui", "android-compose", "create"))
        assertTrue(out.exists(), io.transcript.toString())
        val spec = readObject(out.resolve("bootstrap.spec.json"))
        assertEquals("ktor", spec.text("network")); assertEquals("settings", spec.text("storage"))
        assertEquals(demoEndpoint, spec.text("network_url"))
    }
}
