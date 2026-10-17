package bootstrap

import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Test
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.test.*

class StackRenderingTest {
    private val temp = Files.createTempDirectory("bootstrap-stack-render-")
    private val out = temp.resolve("app")
    @After fun cleanup() { temp.toFile().deleteRecursively() }
    private fun spec(vararg fields: Pair<String, Any?>) = value(mapOf("name" to "Aurora", "package" to "com.example.aurora") + fields).jsonObject
    private fun GenerationPlan.file(path: String) = rendered.files.getValue(path).toString(Charsets.UTF_8)
    private fun plan(vararg fields: Pair<String, Any?>): GenerationPlan {
        val result = planProject(spec(*fields), out)
        assertTrue(result.valid, result.issues.toString()); return result.plan!!
    }
    @Test fun androidHostsAlwaysCompileComposableEntryPointsAndIosRunSelectsSimulator() {
        for (kind in listOf("android", "kmp")) for (data in listOf(false, true)) {
            val p = plan("kind" to kind, "network" to if (data) "ktor" else "none", "storage" to if (data) "settings" else "none")
            assertTrue("  compose:\n    enabled: true" in p.file("androidApp/module.yaml"))
            if (kind == "kmp") assertTrue("run -m iosApp --platform iosSimulatorArm64" in p.file("README.md"))
        }
        val native = plan("ui" to "swiftui", "targets" to listOf("android", "ios"))
        assertTrue("  compose:\n    enabled: true" in native.file("androidApp/module.yaml"))
        assertFalse("  compose:" in native.file("shared/module.yaml"))
    }
    @Test fun xcodeIntegrationMatchesArm64TargetsAndSupportsIdeBuildGuard() {
        for (ui in listOf("shared-compose", "swiftui")) {
            val p = plan("targets" to listOf("ios"), "ui" to ui)
            val project = p.file("iosApp/module.xcodeproj/project.pbxproj")
            assertEquals(2, Regex("ARCHS = arm64;").findAll(project).count())
            assertTrue("alwaysOutOfDate = 1;" in project)
            assertTrue("KOTLIN_CLI_WRAPPER_PATH" in project)
            assertTrue("tool xcode-integration" in project)
            assertFalse("ENABLE_USER_SCRIPT_SANDBOXING = YES;" in project)
            assertTrue(project.indexOf("/* Build Kotlin */,", project.indexOf("buildPhases =")) < project.indexOf("/* Sources */,", project.indexOf("buildPhases =")))
        }
    }
    @Test fun nativeMobileKeepsLogicUiFreeAndGeneratesActualSwiftUiWithBothGraphs() {
        for (di in listOf("manual", "koin")) for (nav in listOf("none", "platform-native")) for (targets in listOf(listOf("ios"), listOf("android", "ios"))) {
            val p = plan("ui" to "swiftui", "di" to di, "navigation" to nav, "ios_deployment_target" to "16.0", "targets" to targets)
            assertFalse(p.rendered.files.keys.any { it.startsWith("shared/src@ios/") || it.endsWith("GreetingTest.kt") })
            val sharedBuild = p.file("shared/module.yaml")
            assertFalse("org.jetbrains.compose" in sharedBuild); assertFalse("  compose:" in sharedBuild)
            assertTrue("jvm" in sharedBuild)
            val swift = p.file("iosApp/src/ContentView.swift")
            assertTrue("CounterController()" in swift); assertTrue("controller.increment()" in swift)
            assertFalse("UIViewControllerRepresentable" in swift); assertFalse("MainViewController" in swift)
            assertEquals(nav != "none", "NavigationStack" in swift)
            assertEquals(nav != "none" && "android" in targets, "navigation3-ui" in p.rendered.files.filterKeys { it.endsWith("module.yaml") }.values.joinToString { it.toString(Charsets.UTF_8) })
            if (nav != "none" && "android" in targets) assertTrue("junit: junit-4" in p.file("androidApp/module.yaml"))
            assertTrue("iosSimulatorArm64" in p.document().getValue("verification").toString())
            val logic = p.file("shared/src/com/example/aurora/CounterController.kt")
            assertEquals(di == "koin", "koinApplication" in logic)
            assertTrue("CounterTest.kt" in p.rendered.files.keys.joinToString())
            assertFalse(out.exists())
        }
    }
    @Test fun composeIntegrationsUseCorrectModulesAndExplicitNonJvmSerializers() {
        for (kind in listOf("android", "kmp")) for (di in listOf("manual", "koin")) for (nav in listOf("none", "navigation3")) {
            if (di == "manual" && nav == "none") continue // Preserved baseline recipe.
            val p = plan("kind" to kind, "di" to di, "navigation" to nav)
            val module = if (kind == "kmp") "shared" else "androidApp"
            val main = "$module/src/com/example/aurora"
            val app = p.file("$main/App.kt")
            assertTrue("controller.increment()" in app); assertTrue("controller.close()" in app)
            assertEquals(nav == "navigation3", "NavDisplay" in app)
            assertEquals(di == "koin", "io.insert-koin:koin-core" in p.file("$module/module.yaml"))
            if (nav == "navigation3") {
                assertTrue("polymorphic(NavKey::class)" in p.file("$main/Routes.kt"))
                assertTrue("routeSerializers" in app); assertTrue("backStack.size > 1" in app)
                assertTrue("serialization:" in p.file("$module/module.yaml"))
            }
        }
    }
    @Test fun everyComposePlatformSubsetRendersAndNewStacksReplayWithLocks() {
        for (mask in 1..15) {
            val targets = platforms.filterIndexed { i, _ -> mask and (1 shl i) != 0 }
            val p = plan("targets" to targets, "di" to "koin", "navigation" to "navigation3")
            assertTrue("navigation3-ui" in p.file("shared/module.yaml"))
        }
        for (ui in listOf("shared-compose", "swiftui")) {
            val p = plan("ui" to ui, "di" to "koin", "navigation" to if (ui == "swiftui") "platform-native" else "navigation3", "ios_deployment_target" to "16.0")
            val copy = planProject(p.spec, temp.resolve("replay"), p.lock).plan!!
            assertEquals(p.rendered.files.mapValues { sha256(it.value) }, copy.rendered.files.mapValues { sha256(it.value) })
            val changed = JsonObject(p.spec + ("di" to JsonPrimitive("manual")))
            assertEquals("lock_mismatch", planProject(changed, out, p.lock).issues.single().code)
        }
    }
}
