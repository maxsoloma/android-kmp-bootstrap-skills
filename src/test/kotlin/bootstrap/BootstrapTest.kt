package bootstrap

import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletableFuture

class BootstrapTest {
    private val temp = Files.createTempDirectory("bootstrap-test-")
    private val output = temp.resolve("project with spaces")
    private val repo = Path.of(System.getProperty("bootstrap.repo", ".")).toAbsolutePath()
    @After fun clean() { temp.toFile().deleteRecursively() }
    private fun args(vararg extra: String) = listOf(output.toString(), "--name", "Aurora", "--package", "dev.example.aurora") + extra
    private fun options(vararg extra: String) = parseGenerate(args(*extra))
    private fun cli(vararg args: String) = InstalledSkills.run(InstalledSkills.skill("bootstrap-project"), args.toList(), temp).also { temp.resolve("cli.log").writeText(it.stdout + it.stderr) }
    @Test fun everyTargetSubsetHasOnlySelectedHostsAndPinnedWrappers() {
        for (mask in 1..15) {
            val targets = platforms.filterIndexed { i, _ -> mask and (1 shl i) != 0 }
            val rendered = renderModern(options("--targets", targets.joinToString(",")))
            assertEquals(targets.toSet(), rendered.metadata.getValue("checks").jsonObject.keys)
            assertTrue("shared/module.yaml" in rendered.files)
            for (target in platforms) {
                val module = mapOf("android" to "androidApp", "ios" to "iosApp", "desktop" to "desktopApp", "web" to "webApp").getValue(target)
                assertEquals(target in targets, "$module/module.yaml" in rendered.files)
            }
            assertEquals("8e5972c57ee6cb277ea3ec48a7b5ace1f19983e5f4373f3f29752b31082d340b", sha256(rendered.files.getValue("kotlin")))
            assertFalse(rendered.files.keys.any { "gradle" in it.lowercase() })
            assertEquals(rendered.files.mapValues { sha256(it.value) }, renderModern(options("--targets", targets.joinToString(","))).files.mapValues { sha256(it.value) })
        }
    }
    @Test fun retiredFlagsFailBeforePublication() {
        for (flags in listOf(listOf("--profile", "hardened"), listOf("--feature", "Home"), listOf("--modules", "billing"))) {
            assertFailsWith<IllegalArgumentException> { parseGenerate(args(*flags.toTypedArray())) }
            assertFalse(output.exists())
        }
    }
    @Test fun dryRunCreatesNoOutputOrParentDirectory() {
        assertEquals(0, cli("generate", *args("--dry-run").toTypedArray()).code, temp.resolve("cli.log").readText())
        assertFalse(output.exists())
        val nested = temp.resolve("missing/parent/project")
        assertEquals(0, cli("generate", nested.toString(), "--name", "Test", "--package", "com.example.test", "--dry-run").code)
        assertFalse(nested.parent.exists())
    }
    @Test fun existingFilesAndSymlinksAreNeverOverwritten() {
        Files.createDirectory(output); output.resolve("notes.txt").writeText("keep me")
        assertFails { options() }; assertEquals("keep me", output.resolve("notes.txt").readText())
        output.resolve("notes.txt").deleteExisting(); output.deleteExisting()
        if (!windows) { Files.createSymbolicLink(output, temp.resolve("absent")); assertFails { options() }; assertTrue(Files.isSymbolicLink(output)) }
    }
    @Test fun invalidOptionsFailBeforeWrites() {
        val invalid = listOf(listOf("--targets", ""), listOf("--name", "Bad\"Name"), listOf("--package", "../../bad"), listOf("--package", "com.object.app"), listOf("--targets", "android,watchos"), listOf("--kind", "android", "--targets", "web"), listOf("--agents", "none,claude"), listOf("--min-sdk", "38"), listOf("--compile-sdk", "36", "--target-sdk", "36"), listOf("--ios-deployment-target", "14.0"), listOf("--app-id", "com.example_bad.app"), listOf("--profile", "unknown"))
        for (extra in invalid) {
            val base = args().toMutableList()
            for ((key, v) in extra.chunked(2)) { val i = base.indexOf(key); if (i >= 0) base[i + 1] = v else base.addAll(listOf(key, v)) }
            assertFails(extra.toString()) { parseGenerate(base) }; assertFalse(output.exists())
        }
    }
    @Test fun customSdkAppIdAndAgentNoneArePreserved() {
        val rendered = renderModern(options("--kind", "android", "--min-sdk", "23", "--target-sdk", "35", "--agents", "none", "--app-id", "org.other.product"))
        val yaml = rendered.files.getValue("androidApp/module.yaml").toString(Charsets.UTF_8)
        assertTrue("minSdk: 23" in yaml); assertTrue("targetSdk: 35" in yaml); assertTrue("applicationId: org.other.product" in yaml)
        assertTrue("AGENTS.md" in rendered.files); assertFalse("CLAUDE.md" in rendered.files); assertFalse("shared/module.yaml" in rendered.files)
    }
    @Test fun installedSkillWorksAloneOutsideRepository() {
        for ((i, flags) in listOf(listOf("--targets", "ios"), listOf("--kind", "android"), listOf("--targets", "desktop,web", "--network", "ktor", "--storage", "settings"), listOf("--targets", "desktop", "--di", "koin")).withIndex()) {
            val out = temp.resolve("generated $i")
            assertEquals(0, cli("generate", out.toString(), "--name", "Demo", "--package", "org.example.demo", *flags.toTypedArray()).code, temp.resolve("cli.log").readText())
            assertContentEquals(repo.resolve("kotlin").readBytes(), out.resolve("kotlin").readBytes())
            if (!windows) assertTrue(Files.isExecutable(out.resolve("kotlin")))
            assertEquals("@AGENTS.md\n", out.resolve("CLAUDE.md").readText())
            if ("koin" in flags) {
                assertTrue(out.resolve(".agents/skills/koin/SKILL.md").exists())
                assertTrue(out.resolve(".claude/skills/koin/references/rules.md").exists())
                assertTrue("https://mcp.kotzilla.io/mcp" in out.resolve(".codex/config.toml").readText())
            }
        }
    }
    @Test fun verifierContinuesAfterFailureAndStoresPerCommandLogs() {
        publish(output, renderModern(options("--targets", "desktop,web")).files)
        val wrapper = output.resolve(if (windows) "kotlin.bat" else "kotlin")
        wrapper.writeText(if (windows) "@echo off\r\nif \"%~1%~3\"==\"buildjvm\" exit /b 7\r\nexit /b 0\r\n" else "#!/bin/sh\nif [ \"$1$3\" = buildjvm ]; then exit 7; fi\nexit 0\n")
        assertEquals(1, verify(listOf(output.toString())))
        val checks = readObject(output.resolve(".bootstrap/verification.json")).getValue("checks").jsonArray
        assertEquals(listOf("FAIL", "PASS", "PASS", "PASS"), checks.map { it.jsonObject.text("status") })
        assertTrue(output.resolve(".bootstrap/web-test.log").exists())
        assertFails { verify(listOf(output.toString(), "--targets", "ios")) }; assertFails { verify(listOf(output.toString(), "--timeout", "0")) }
    }
    @Test fun verifierTimeoutMissingExecutableAndUnsafeMetadataFail() {
        publish(output, renderModern(options("--targets", "desktop")).files)
        val wrapper = output.resolve(if (windows) "kotlin.bat" else "kotlin")
        wrapper.writeText(if (windows) "@ping -n 8 127.0.0.1 >nul\r\n" else "#!/bin/sh\nsleep 8\n")
        assertEquals(1, verify(listOf(output.toString(), "--timeout", "1")))
        assertTrue("Timed out" in output.resolve(".bootstrap/verification.json").readText())
        wrapper.deleteExisting(); assertEquals(1, verify(listOf(output.toString())))
        val meta = readObject(output.resolve("bootstrap.json")); output.resolve("bootstrap.json").writeText(encode(JsonObject(meta + ("checks" to value(mapOf("desktop" to listOf(listOf("clean", "--platform", "jvm"))))))))
        assertFails { verify(listOf(output.toString())) }
    }
    @Test fun doctorReportsJvmAndBrowserVerificationSeparately() {
        val checks = inspect(listOf("desktop", "web"), 37).getValue("checks").jsonArray.map { it.jsonObject }
        assertEquals("PASS", checks.first { it.text("check") == "jdk" }.text("status"))
        assertEquals("SKIPPED", checks.first { it.text("check") == "browser" }.text("status"))
        assertFails { doctor(listOf("--targets", "watchos")) }
    }
    @Test fun simulatorUseIsExclusiveAndWaitingHasABoundedTimeout() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val first = CompletableFuture.supplyAsync {
            withIosSimulatorLock(10) {
                entered.countDown()
                check(release.await(8, TimeUnit.SECONDS))
                ProcessResult(0)
            }
        }
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val blocked = withIosSimulatorLock(1) { error("Concurrent simulator use") }
            assertEquals(-1, blocked.code)
            assertTrue("Timed out waiting" in blocked.detail)
        } finally { release.countDown() }
        assertEquals(0, first.get(5, TimeUnit.SECONDS).code)
        assertEquals(0, withIosSimulatorLock(1) { ProcessResult(0) }.code)
    }
    @Test fun pluginMetadataAndSurvivingSkillStayAligned() {
        fun read(path: String) = readObject(repo.resolve(path))
        val claude = read(".claude-plugin/plugin.json"); val codex = read(".codex-plugin/plugin.json")
        assertEquals(CLI_VERSION, claude.text("version")); assertEquals(claude.text("version"), codex.text("version")); assertEquals(claude.text("name"), codex.text("name"))
        assertTrue(repo.resolve(codex.text("skills")).isDirectory())
        assertEquals(true, codex["requires_local_executor"]?.jsonPrimitive?.boolean)
        val listing = codex.getValue("interface").jsonObject
        assertTrue(listing.text("displayName").length <= 30)
        assertTrue(listing.text("shortDescription").length <= 30)
        assertTrue(listing.text("longDescription").isNotBlank())
        for (field in listOf("logo", "composerIcon")) assertTrue(repo.resolve(listing.text(field)).isRegularFile(), field)
        assertTrue(repo.resolve("skills/bootstrap-project/agents/openai.yaml").isRegularFile())
        assertTrue(repo.resolve("skills/orchestrate-feature/agents/openai.yaml").isRegularFile())
        assertEquals(InstalledSkills.digest(repo.resolve("skills/bootstrap-project/cli")), InstalledSkills.digest(repo.resolve("skills/orchestrate-feature/cli")))
        assertFalse(Files.walk(repo.resolve("skills")).use { paths -> paths.anyMatch { it.extension == "jar" } })
        assertFalse(repo.resolve("skills/bootstrap-android-project").exists()); assertFalse(repo.resolve("skills/bootstrap-kmp-project").exists())
    }
}
