package bootstrap

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

class LauncherResult(val code: Int, val stdout: String, val stderr: String)

/** Read-only copies of the shipped skills, compiled once per test JVM into a shared temporary build cache. */
object InstalledSkills {
    val repo: Path = Path.of(System.getProperty("bootstrap.repo", ".")).toAbsolutePath()
    private val root = Files.createTempDirectory("installed-skills-").also { dir ->
        Runtime.getRuntime().addShutdownHook(Thread { deleteReadOnly(dir) })
    }
    val buildCache: Path = root.resolve("build cache")
    private val copies = mutableMapOf<String, Path>()

    @Synchronized fun skill(name: String): Path = copies.getOrPut(name) { copyReadOnly(repo.resolve("skills/$name"), root.resolve("installed $name")) }

    fun copyReadOnly(source: Path, target: Path): Path {
        Files.walk(source).use { paths -> paths.forEach { from ->
            val to = target.resolve(source.relativize(from).toString())
            if (from.isDirectory()) Files.createDirectories(to) else Files.copy(from, to, java.nio.file.StandardCopyOption.COPY_ATTRIBUTES)
        } }
        target.toFile().walkBottomUp().forEach { it.setWritable(false, false) }
        return target
    }

    fun launcher(skill: Path): List<String> =
        if (windows) listOf("cmd.exe", "/d", "/c", skill.resolve("bin/bootstrap.cmd").toString()) else listOf(skill.resolve("bin/bootstrap").toString())

    /** Runs a skill launcher with separate stdout/stderr, a working directory and environment overrides (null removes a variable). */
    fun run(skill: Path, args: List<String>, directory: Path, env: Map<String, String?> = emptyMap(), launcher: List<String> = launcher(skill)): LauncherResult {
        val out = Files.createTempFile(root, "stdout-", ".log"); val err = Files.createTempFile(root, "stderr-", ".log")
        val builder = ProcessBuilder(launcher + args).directory(directory.toFile()).redirectOutput(out.toFile()).redirectError(err.toFile())
        builder.environment()["BOOTSTRAP_BUILD_DIR"] = buildCache.resolve(skill.fileName.toString()).toString()
        for ((key, value) in env) if (value == null) builder.environment().remove(key) else builder.environment()[key] = value
        val process = builder.start()
        if (!process.waitFor(900, TimeUnit.SECONDS)) { process.destroyForcibly(); error("Launcher timed out: $args") }
        return LauncherResult(process.exitValue(), out.readText(), err.readText())
    }

    /** Restores write access top-down (deleting needs a writable parent), then deletes [dir]. */
    fun deleteReadOnly(dir: Path) {
        dir.toFile().walkTopDown().forEach { it.setWritable(true) }
        dir.toFile().deleteRecursively()
    }

    /** SHA-256 of every file under [dir], keyed by relative path. */
    fun digest(dir: Path): Map<String, String> = Files.walk(dir).use { paths ->
        paths.filter { it.isRegularFile() }.toList().associate { dir.relativize(it).toString() to sha256(it.readBytes()) }
    }
}
