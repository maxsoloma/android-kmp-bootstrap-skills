package bootstrap

import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Test
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.test.*

class ConfigurationTest {
    private val temp = Files.createTempDirectory("bootstrap-config-")
    private val output = temp.resolve("app")
    @After fun cleanup() { temp.toFile().deleteRecursively() }
    private fun spec(vararg fields: Pair<String, Any?>): JsonObject = value(mapOf("name" to "Aurora", "package" to "com.example.aurora") + fields).jsonObject

    @Test fun everyPresetAndAllModernTargetSubsetsResolve() {
        for (preset in presets) {
            val result = resolveSpec(JsonObject(spec() + preset.defaults()), output)
            assertTrue(result.valid, result.issues.toString())
            assertEquals(preset.kind, result.options!!.kind)
        }
        for (mask in 1..15) {
            val targets = platforms.filterIndexed { i, _ -> mask and (1 shl i) != 0 }
            val result = resolveSpec(spec("targets" to targets), output)
            assertTrue(result.valid, result.issues.toString())
            assertEquals(targets, result.options!!.targets)
        }
        assertFalse(output.exists())
    }
    @Test fun incompleteConfigReportsAllMissingInputs() {
        val result = resolveSpec(JsonObject(emptyMap()), output)
        assertEquals(setOf("name", "package"), result.issues.filter { it.code == "required" }.map { it.field }.toSet())
        assertFalse(result.valid); assertFalse(output.exists())
    }
    @Test fun unknownFieldsAndWrongTypesNeverFallBackSilently() {
        for ((field, v) in listOf("name" to 123, "app_id" to false, "targets" to "android", "targets" to listOf(1), "agents" to null, "schema_version" to "1", "schema_version" to 3, "sdk" to "37", "sdk" to mapOf("compile" to "37"), "sdk" to mapOf("complie" to 37), "navigation" to "new-library")) {
            assertFalse(resolveSpec(spec(field to v), output).valid, "$field=$v")
        }
        assertFalse(output.exists())
    }
    @Test fun compatibilityIsScopedToProfileAndUi() {
        val bad = listOf(
            spec("profile" to "hardened", "targets" to listOf("web")),
            spec("kind" to "android", "targets" to listOf("ios")),
            spec("ui" to "uikit"), spec("agents" to listOf("none", "claude")),
            spec("modules" to listOf("billing")), spec("targets" to emptyList<String>()),
            spec("profile" to "hardened", "modules" to listOf("../bad")),
            spec("sdk" to mapOf("compile" to 36, "target" to 36)),
            spec("profile" to "hardened", "sdk" to mapOf("min" to 23)),
        )
        bad.forEach { assertFalse(resolveSpec(it, output).valid, it.toString()) }
        val good = resolveSpec(spec("kind" to "android", "sdk" to mapOf("min" to 23, "target" to 35)), output)
        assertTrue(good.valid, good.issues.toString())
    }
    @Test fun normalizedChoicesRoundTripAndPreserveAllOptions() {
        val result = resolveSpec(spec("kind" to "android", "app_id" to "org.example.release", "agents" to listOf("gemini", "cursor"), "sdk" to mapOf("min" to 23, "target" to 35)), output)
        assertTrue(result.valid)
        assertEquals(result.spec, resolveSpec(result.spec!!, output).spec)
        assertEquals("org.example.release", result.options!!.appId)
        assertEquals(35, result.options.target)
        assertEquals(listOf("gemini", "cursor"), result.options.agents)
    }
    @Test fun catalogExposesRealModulesDependenciesAndUnavailableRecipes() {
        val catalog = catalogDocument()
        assertEquals(listOf("name", "package"), catalog.strings("required_fields"))
        assertEquals(4, catalog.getValue("presets").jsonArray.size)
        assertEquals("kotlin-toolchain", catalog.text("build_system"))
        assertFalse("optional_modules" in catalog)
        assertTrue(resolveSpec(spec("profile" to "hardened"), output).issues.any { it.code == "retired_profile" })
        assertTrue(resolveSpec(spec("build_system" to "gradle"), output).issues.any { it.code == "retired_backend" })
    }
}
