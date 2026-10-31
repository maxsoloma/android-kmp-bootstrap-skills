package bootstrap

import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.*

class LauncherTest {
    private val temp = Files.createTempDirectory("launcher-test-")
    @After fun clean() = InstalledSkills.deleteReadOnly(temp)

    /** A skill with the shipped launchers and a fake wrapper that reports the selected CLI runtime. */
    private val fakeSkill by lazy {
        val skill = temp.resolve("fake skill")
        Files.createDirectories(skill.resolve("cli"))
        InstalledSkills.copyReadOnly(InstalledSkills.repo.resolve("skills/bootstrap-project/bin"), skill.resolve("bin"))
        if (windows) skill.resolve("cli/kotlin.bat").writeText("@echo java=%KOTLIN_CLI_JAVA_HOME%\r\n")
        else skill.resolve("cli/kotlin").also { it.writeText("#!/bin/sh\necho \"java=\${KOTLIN_CLI_JAVA_HOME:-}\"\n"); it.toFile().setExecutable(true) }
        skill
    }
    /** A fake POSIX `java` that reports [version] (or fails when null) and records each invocation. */
    private fun fakeJava(name: String, version: String?): Path {
        val home = temp.resolve(name); val java = home.resolve("bin/java"); Files.createDirectories(java.parent)
        java.writeText(if (version == null) "#!/bin/sh\ntouch \"$home/probed\"\nexit 1\n"
            else "#!/bin/sh\ntouch \"$home/probed\"\nprintf '    java.home = %s\\n    java.specification.version = %s\\n' '$home' '$version' >&2\n")
        java.toFile().setExecutable(true)
        return home
    }
    /** PATH with only the tools the launcher needs, so host Java installations cannot interfere. */
    private fun toolsPath(vararg first: Path): String {
        val tools = temp.resolve("tools"); Files.createDirectories(tools)
        for (tool in listOf("sed", "head", "uname", "mkdir", "dirname", "touch", "cksum", "cut")) {
            val real = listOf("/usr/bin", "/bin").map { Path.of(it, tool) }.first { it.isExecutable() }
            if (!tools.resolve(tool).exists()) Files.createSymbolicLink(tools.resolve(tool), real)
        }
        return (first.map { it.toString() } + tools.toString()).joinToString(":")
    }
    private fun selected(env: Map<String, String?>): String {
        val result = InstalledSkills.run(fakeSkill, listOf("--version"), temp, env)
        assertEquals(0, result.code, result.stderr)
        return result.stdout.lineSequence().first { it.startsWith("java=") }.removePrefix("java=").trim()
    }

    @Test fun explicitCliRuntimeIsPassedThroughWithoutProbing() {
        val other = if (windows) null else fakeJava("other", "25")
        val env = mutableMapOf<String, String?>("KOTLIN_CLI_JAVA_HOME" to temp.resolve("explicit").toString())
        if (other != null) { env["JAVA_HOME"] = other.toString(); env["PATH"] = toolsPath(other.resolve("bin")) }
        assertEquals(temp.resolve("explicit").toString(), selected(env))
        if (other != null) assertFalse(other.resolve("probed").exists())
    }
    @Test fun javaHomeMeetingTheMinimumIsReused() {
        val home = System.getProperty("java.home")
        assertTrue(Runtime.version().feature() >= 25, "Tests run on the provisioned compilation JDK")
        assertEquals(Path.of(home).toRealPath(), Path.of(selected(mapOf("KOTLIN_CLI_JAVA_HOME" to null, "JAVA_HOME" to home))).toRealPath())
    }
    @Test fun olderOrBrokenRuntimesFallBackToTheWrapperRuntime() {
        assumeFalse(windows)
        val old = fakeJava("jdk21", "21"); val stub = fakeJava("stub", null)
        assertEquals("", selected(mapOf("KOTLIN_CLI_JAVA_HOME" to null, "JAVA_HOME" to old.toString(), "PATH" to toolsPath(stub.resolve("bin")))))
        assertTrue(old.resolve("probed").exists()); assertTrue(stub.resolve("probed").exists())
        assertEquals("", selected(mapOf("KOTLIN_CLI_JAVA_HOME" to null, "JAVA_HOME" to null, "PATH" to toolsPath())))
    }
    @Test fun pathRuntimeIsUsedWhenJavaHomeIsUnsuitable() {
        assumeFalse(windows)
        val old = fakeJava("jdk17", "17"); val modern = fakeJava("jdk26", "26")
        assertEquals(modern.toString(), selected(mapOf("KOTLIN_CLI_JAVA_HOME" to null, "JAVA_HOME" to old.toString(), "PATH" to toolsPath(modern.resolve("bin")))))
    }

    @Test fun installedSkillPrintsOnlyCommandOutputAndPropagatesFailures() {
        val skill = InstalledSkills.skill("bootstrap-project")
        val before = InstalledSkills.digest(skill)
        val catalog = InstalledSkills.run(skill, listOf("catalog", "--json"), temp)
        assertEquals(0, catalog.code, catalog.stderr)
        assertEquals("kotlin-toolchain", Json.parseToJsonElement(catalog.stdout).jsonObject.text("build_system"))
        val failed = InstalledSkills.run(skill, listOf("plan", "--config", "missing.json", "--output", "out"), temp)
        assertNotEquals(0, failed.code)
        assertFalse(temp.resolve("out").exists())
        assertEquals(before, InstalledSkills.digest(skill))
    }
    @Test fun installedSkillResolvesRelativePathsAgainstTheCallerAndMatchesTheRepositoryBuild() {
        val skill = InstalledSkills.skill("bootstrap-project")
        val workspace = temp.resolve("caller workspace"); Files.createDirectories(workspace)
        workspace.resolve("config.json").writeText(encode(mapOf("name" to "Aurora", "package" to "com.example.aurora", "targets" to listOf("android", "desktop"), "di" to "koin", "agents" to listOf("none"))))
        val result = InstalledSkills.run(skill, listOf("generate", "--config", "config.json", "--output", "Aurora"), workspace)
        assertEquals(0, result.code, result.stderr + result.stdout)
        val expected = temp.resolve("expected")
        assertEquals(0, configCommand(listOf("--config", workspace.resolve("config.json").toString(), "--output", expected.toString()), generate = true))
        assertEquals(InstalledSkills.digest(expected), InstalledSkills.digest(workspace.resolve("Aurora")))
    }
    @Test fun callerInsideAnotherToolchainProjectKeepsThePinnedWrapperVersion() {
        val skill = InstalledSkills.skill("bootstrap-project")
        val app = temp.resolve("user app"); Files.createDirectories(app)
        app.resolve("project.yaml").writeText("product: jvm/app\n")
        for (name in listOf("kotlin", "kotlin.bat")) {
            val wrapper = app.resolve(name)
            wrapper.writeText(InstalledSkills.repo.resolve(name).readText().replace("kotlin_cli_version=0.13.0", "kotlin_cli_version=0.0.1"))
            wrapper.toFile().setExecutable(true)
        }
        val catalog = InstalledSkills.run(skill, listOf("catalog", "--json"), app)
        assertEquals(0, catalog.code, catalog.stderr)
        assertFalse("0.0.1" in catalog.stderr, catalog.stderr)
        assertEquals("kotlin-toolchain", Json.parseToJsonElement(catalog.stdout).jsonObject.text("build_system"))
    }
    @Test fun separateInstallationsRunInParallelWithCleanOutput() {
        val copies = (1..2).map { InstalledSkills.copyReadOnly(InstalledSkills.repo.resolve("skills/bootstrap-project"), temp.resolve("copy $it")) }
        val runs = (copies + copies).map { copy -> java.util.concurrent.CompletableFuture.supplyAsync { InstalledSkills.run(copy, listOf("catalog", "--json"), temp) } }
        for (result in runs.map { it.get() }) {
            assertEquals(0, result.code, result.stderr)
            assertEquals("kotlin-toolchain", Json.parseToJsonElement(result.stdout).jsonObject.text("build_system"), result.stdout)
        }
    }
    @Test fun installedSkillRunsDoctorAndVerify() {
        val skill = InstalledSkills.skill("bootstrap-project")
        val doctor = InstalledSkills.run(skill, listOf("doctor", "--targets", "desktop"), temp)
        assertTrue(doctor.code in 0..1, doctor.stderr)
        assertTrue(Json.parseToJsonElement(doctor.stdout).jsonObject.getValue("checks").jsonArray.isNotEmpty())
        val project = temp.resolve("checked app")
        val generated = InstalledSkills.run(skill, listOf("generate", project.toString(), "--name", "Demo", "--package", "org.example.demo", "--targets", "desktop"), temp)
        assertEquals(0, generated.code, generated.stdout + generated.stderr)
        val wrapper = project.resolve(if (windows) "kotlin.bat" else "kotlin")
        wrapper.writeText(if (windows) "@exit /b 0\r\n" else "#!/bin/sh\nexit 0\n")
        val verify = InstalledSkills.run(skill, listOf("verify", project.toString()), temp)
        assertEquals(0, verify.code, verify.stdout + verify.stderr)
        assertTrue(project.resolve(".bootstrap/verification.json").isRegularFile())
    }
}
