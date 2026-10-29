package bootstrap

import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Test
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.test.*

class DataConfigurationTest {
    private val temp = Files.createTempDirectory("bootstrap-data-config-")
    private val out = temp.resolve("app")
    @After fun cleanup() { temp.toFile().deleteRecursively() }
    private fun spec(vararg fields: Pair<String, Any?>) = value(mapOf("name" to "Aurora", "package" to "com.example.aurora") + fields).jsonObject
    @Test fun allPlatformSubsetsAndDiChoicesSupportIndependentDataSelections() {
        for (mask in 1..15) for (di in listOf("manual", "koin")) for (network in listOf("none", "ktor")) for (storage in listOf("none", "settings")) {
            val targets = platforms.filterIndexed { i, _ -> mask and (1 shl i) != 0 }
            val r = resolveSpec(spec("targets" to targets, "di" to di, "network" to network, "storage" to storage), out)
            assertTrue(r.valid, r.issues.toString()); assertEquals(r.spec, resolveSpec(r.spec!!, out).spec)
            assertEquals(network, r.options!!.network); assertEquals(storage, r.options.storage)
        }
        for (kind in listOf("android", "kmp")) assertTrue(resolveSpec(spec("kind" to kind, "network" to "ktor", "storage" to "settings"), out).valid)
        assertTrue(resolveSpec(spec("ui" to "swiftui", "targets" to listOf("ios"), "network" to "ktor", "storage" to "settings"), out).valid)
        assertFalse(out.exists())
    }
    @Test fun invalidProvidersTypesAndEndpointsFailBeforeWrites() {
        val invalid = listOf(
            spec("network" to "retrofit"), spec("storage" to "room"), spec("storage" to "datastore"),
            spec("network" to false), spec("storage" to null), spec("network_url" to 42),
            spec("network_url" to "https://example.com/"),
            spec("profile" to "hardened", "network" to "ktor"), spec("profile" to "hardened", "storage" to "none"),
        ) + listOf("http://example.com", "file:///tmp/data", "https:///bad", "https://u:p@example.com", "https://example.com/#fragment", "https://example.com:70000", "\nhttps://example.com", "").map { spec("network" to "ktor", "network_url" to it) }
        invalid.forEach { assertFalse(resolveSpec(it, out).valid, it.toString()) }
        assertFalse(out.exists())
    }
    @Test fun defaultsAndFlatFlagsRetainPriorSemantics() {
        val modern = resolveSpec(spec(), out).options!!
        assertEquals("none", modern.network); assertEquals("none", modern.storage); assertFalse(modern.hasData)
        assertFalse(resolveSpec(spec("profile" to "hardened"), out).valid)
        val flags = listOf(out.toString(), "--name", "Aurora", "--package", "com.example.aurora")
        val parsed = parseGenerate(flags + listOf("--network", "ktor", "--storage", "settings", "--network-url", "https://example.com/data"))
        assertEquals("https://example.com/data", parsed.networkUrl)
        assertFails { parseGenerate(flags + listOf("--network-url", "https://example.com")) }
    }
    @Test fun catalogAndPlanDescribeRealAdaptersAndSelectedPins() {
        val catalog = catalogDocument().getValue("stack_choices").jsonObject
        assertTrue(catalog.getValue("network").jsonArray.any { it.jsonObject.text("id") == "ktor" })
        val o = resolveSpec(spec("network" to "ktor", "storage" to "settings", "targets" to platforms), out).options!!
        assertEquals(setOf("ktor", "settings", "coroutines"), selectedPins(o).keys)
        assertEquals("Darwin", networkAdapters(o.targets)["ios"])
        assertTrue("localStorage" in storageAdapters(o.targets).getValue("web"))
    }
    @Test fun everyStackCombinationRendersWithoutRetiredBackendFiles() {
        for (mask in 1..15) for (di in listOf("manual", "koin")) for (nav in listOf("none", "navigation3")) for (network in listOf("none", "ktor")) for (storage in listOf("none", "settings")) {
            val targets = platforms.filterIndexed { i, _ -> mask and (1 shl i) != 0 }
            val p = planProject(spec("targets" to targets, "di" to di, "navigation" to nav, "network" to network, "storage" to storage), out)
            assertTrue(p.valid, p.issues.toString())
            assertFalse(p.plan!!.rendered.files.keys.any { "gradle" in it.lowercase() && !it.endsWith("/references/gradle.md") })
            assertEquals("kotlin-toolchain", p.plan.spec.text("build_system"))
        }
    }
}
