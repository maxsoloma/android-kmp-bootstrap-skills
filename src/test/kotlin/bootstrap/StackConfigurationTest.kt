package bootstrap

import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Test
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.test.*

class StackConfigurationTest {
    private val temp = Files.createTempDirectory("bootstrap-stack-config-")
    private val out = temp.resolve("app")
    @After fun cleanup() { temp.toFile().deleteRecursively() }
    private fun spec(vararg fields: Pair<String, Any?>) = value(mapOf("name" to "Aurora", "package" to "com.example.aurora") + fields).jsonObject
    @Test fun allComposeTargetsSupportEachImplementedDiAndNavigationChoice() {
        for (mask in 1..15) for (di in listOf("manual", "koin")) for (nav in listOf("none", "navigation3")) {
            val targets = platforms.filterIndexed { i, _ -> mask and (1 shl i) != 0 }
            val r = resolveSpec(spec("targets" to targets, "di" to di, "navigation" to nav), out)
            assertTrue(r.valid, r.issues.toString()); assertEquals(di, r.options!!.di)
            assertEquals(nav, r.options.navigation); assertEquals(r.spec, resolveSpec(r.spec!!, out).spec)
        }
        for (di in listOf("manual", "koin")) for (nav in listOf("none", "navigation3")) {
            assertTrue(resolveSpec(spec("kind" to "android", "di" to di, "navigation" to nav), out).valid)
        }
        assertFalse(out.exists())
    }
    @Test fun nativeUiAllowsIosAndMobileButNotOtherPlatformsOrProfiles() {
        for (targets in listOf(listOf("ios"), listOf("android", "ios"))) for (di in listOf("manual", "koin")) for (nav in listOf("none", "platform-native")) {
            val r = resolveSpec(spec("ui" to "swiftui", "targets" to targets, "di" to di, "navigation" to nav, "ios_deployment_target" to "16.0"), out)
            assertTrue(r.valid, r.issues.toString()); assertEquals(r.spec, resolveSpec(r.spec!!, out).spec)
        }
        for (targets in listOf(listOf("android"), listOf("desktop", "ios"), listOf("ios", "web"), platforms)) {
            assertTrue(resolveSpec(spec("ui" to "swiftui", "targets" to targets), out).issues.any { it.field == "ui" })
        }
        assertFalse(resolveSpec(spec("ui" to "swiftui", "kind" to "android"), out).valid)
        assertFalse(resolveSpec(spec("ui" to "swiftui", "profile" to "hardened"), out).valid)
    }
    @Test fun componentConflictsAreStructuredAndNeverSilentlyReplaced() {
        for ((fields, field) in listOf(
            listOf("ui" to "swiftui", "navigation" to "navigation3") to "navigation",
            listOf("navigation" to "platform-native") to "navigation",
            listOf("ui" to "swiftui", "navigation" to "platform-native") to "ios_deployment_target",
            listOf("di" to "hilt") to "di", listOf("di" to "dagger") to "di",
            listOf("profile" to "hardened", "di" to "manual") to "profile",
            listOf("profile" to "hardened", "navigation" to "navigation3") to "profile",
            listOf("di" to true) to "di", listOf("navigation" to emptyList<String>()) to "navigation",
        )) {
            val r = resolveSpec(spec(*fields.toTypedArray()), out)
            assertFalse(r.valid, "$fields"); assertTrue(r.issues.any { it.field == field }, r.issues.toString())
        }
        assertFalse(out.exists())
    }
    @Test fun legacyDefaultsAndNewFlagsUseTheSameCompatibilityRules() {
        val defaults = resolveSpec(spec(), out).options!!
        assertEquals("shared-compose", defaults.ui); assertEquals("manual", defaults.di); assertEquals("none", defaults.navigation)
        assertFalse(defaults.customStack)
        for (kind in listOf("android", "kmp")) assertFalse(resolveSpec(spec("profile" to "hardened", "kind" to kind), out).valid)
        val flags = listOf(out.toString(), "--name", "Aurora", "--package", "com.example.aurora")
        assertEquals("swiftui", parseGenerate(flags + listOf("--ui", "swiftui", "--di", "koin")).ui)
        assertFails { parseGenerate(flags + listOf("--ui", "swiftui", "--targets", "web")) }
        assertFails { parseGenerate(flags + listOf("--ui", "swiftui", "--navigation", "navigation3")) }
    }
    @Test fun catalogContainsConstraintsAndPublishedComponentPins() {
        val catalog = catalogDocument().getValue("stack_choices").jsonObject
        assertEquals("4.2.2", catalog.getValue("pins").jsonObject.text("koin"))
        assertEquals("1.1.2", catalog.getValue("pins").jsonObject.text("navigation3"))
        assertTrue(catalog.getValue("ui").jsonArray.any { it.jsonObject.text("id") == "swiftui" })
        assertEquals("16.0", catalog.getValue("navigation").jsonArray.first { it.jsonObject.text("id") == "platform-native" }.jsonObject.text("ios_minimum"))
    }
}
