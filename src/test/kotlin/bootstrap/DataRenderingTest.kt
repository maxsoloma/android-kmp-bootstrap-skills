package bootstrap

import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Test
import java.nio.file.Files
import kotlin.test.*

class DataRenderingTest {
    private val temp = Files.createTempDirectory("bootstrap-data-render-")
    @After fun cleanup() { temp.toFile().deleteRecursively() }
    private fun plan(vararg fields: Pair<String, Any?>): GenerationPlan {
        val spec = value(mapOf("name" to "Aurora", "package" to "com.example.aurora") + fields).jsonObject
        val result = planProject(spec, temp.resolve("app"))
        assertTrue(result.valid, result.issues.toString()); return result.plan!!
    }
    private fun GenerationPlan.file(path: String) = rendered.files.getValue(path).toString(Charsets.UTF_8)
    @Test fun allSubsetsRenderWithCorrectPlatformEnginesAndStores() {
        for (mask in 1..15) {
            val targets = platforms.filterIndexed { i, _ -> mask and (1 shl i) != 0 }
            val p = plan("targets" to targets, "network" to "ktor", "storage" to "settings", "di" to "koin", "navigation" to "navigation3")
            for (target in targets) {
                val set = when (target) { "android" -> "android"; "ios" -> "ios"; "web" -> "wasmJs"; else -> "jvm" }
                val actual = p.file("shared/src@$set/com/example/aurora/PlatformData.kt")
                assertTrue("platformHttpClient" in actual); assertTrue("platformCounterStore" in actual)
            }
            val app = p.file("shared/src/com/example/aurora/App.kt")
            assertTrue("controller.fetch" in app); assertTrue("Network error:" in app); assertTrue("Storage error:" in app)
            assertTrue("ktor-client-core" in p.file("shared/module.yaml"))
            if ("android" in targets) {
                assertTrue("android.permission.INTERNET" in p.file("androidApp/src/AndroidManifest.xml"))
                assertTrue("initializeStorage(applicationContext)" in p.file("androidApp/src/com/example/aurora/MainActivity.kt"))
            }
            if ("ios" in targets) {
                val test = p.file("shared/test@ios/com/example/aurora/IosPersistenceTest.kt")
                assertTrue("NSUserDefaultsSettings" in test)
                assertTrue("CounterRepository(platformCounterStore())" in test)
                assertTrue("finally" in test)
            }
        }
    }
    @Test fun independentChoicesDoNotPullInUnselectedProviders() {
        val net = plan("network" to "ktor")
        assertFalse("multiplatform-settings" in net.file("shared/module.yaml"))
        assertTrue("Fetch response" in net.file("shared/src/com/example/aurora/App.kt"))
        val store = plan("storage" to "settings", "kind" to "android")
        assertFalse("ktor-client-core" in store.file("androidApp/module.yaml"))
        assertFalse("INTERNET" in store.file("androidApp/src/AndroidManifest.xml"))
        assertTrue("SharedPreferencesSettings" in store.file("androidApp/src/com/example/aurora/PlatformData.kt"))
        assertTrue("test-dependencies:" in store.file("androidApp/module.yaml"))
        assertTrue("multiplatform-settings-test" in store.file("androidApp/module.yaml"))
    }
    @Test fun swiftUiIntegratesBothChoicesWithoutAComposeBridge() {
        val p = plan("ui" to "swiftui", "di" to "koin", "navigation" to "platform-native", "ios_deployment_target" to "16.0", "network" to "ktor", "storage" to "settings")
        val swift = p.file("iosApp/src/ContentView.swift")
        assertTrue("[weak self]" in swift); assertTrue("controller.fetch" in swift)
        assertTrue("model.fetch()" in swift); assertTrue("controller.count" in swift)
        assertFalse("UIViewControllerRepresentable" in swift)
        assertTrue("fileCounterStore" in p.file("shared/src@jvm/com/example/aurora/PlatformData.kt"))
        assertTrue("ktor-client-cio" in p.file("shared/module.yaml"))
    }
    @Test fun endpointLiteralsAreEscapedAndNewRecipesReplayExactly() {
        val url = "https://example.com/?value=\"\$token\""
        val p = plan("network" to "ktor", "storage" to "settings", "network_url" to url.replace("\"", "%22"))
        assertTrue("\\\$token" in p.file("shared/src/com/example/aurora/NetworkRepository.kt"))
        val copy = planProject(p.spec, temp.resolve("replay"), p.lock).plan!!
        assertEquals(p.rendered.files.mapValues { sha256(it.value) }, copy.rendered.files.mapValues { sha256(it.value) })
        assertFalse(planProject(JsonObject(p.spec + ("storage" to JsonPrimitive("none"))), temp.resolve("different"), p.lock).valid)
    }
}
