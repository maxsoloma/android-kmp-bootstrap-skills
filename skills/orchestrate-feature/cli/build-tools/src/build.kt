package bootstrap.build

import org.jetbrains.amper.plugins.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap
import kotlin.io.path.*

@TaskAction
fun prepareResources(@Input assets: Path, @Input toolchain: Path, @Input sourceResources: Path, @Output output: Path) =
    writeResources(assets, toolchain, sourceResources, output, copySources = false)

// Skill copies keep resources outside the standard layout and copy them here, because
// the Toolchain copies resource directory permissions and fails in read-only installs.
@TaskAction
fun prepareSkillResources(@Input assets: Path, @Input toolchain: Path, @Input sourceResources: Path, @Output output: Path) =
    writeResources(assets, toolchain, sourceResources, output, copySources = true)

private fun writeResources(assets: Path, toolchain: Path, sourceResources: Path, output: Path, copySources: Boolean) {
    output.toFile().deleteRecursively()
    Files.createDirectories(output)
    fun copyTree(from: Path, to: Path) {
        Files.walk(from).use { paths -> paths.filter { Files.isRegularFile(it) }.forEach {
            val target = to.resolve(from.relativize(it).toString()); Files.createDirectories(target.parent)
            Files.copy(it, target)
        } }
    }
    copyTree(assets, output.resolve("assets"))
    if (copySources) copyTree(sourceResources, output)
    Files.copy(toolchain, output.resolve("toolchain.json"))
    val names = sortedSetOf<String>()
    for (root in listOf(output, sourceResources)) {
        Files.walk(root).use { paths -> paths.filter { Files.isRegularFile(it) }.forEach { names += root.relativize(it).toString().replace('\\', '/') } }
    }
    names.remove("resources.index")
    output.resolve("resources.index").writeText(names.joinToString("\n", postfix = "\n"))
}

@TaskAction
fun bundle(
    @Input projectFile: Path, @Input moduleFile: Path, @Input sources: Path, @Input pluginModule: Path, @Input pluginSource: Path,
    @Input assets: Path, @Input toolchain: Path, @Input wrapper: Path, @Input windowsWrapper: Path, @Input launchers: Path,
    @Output skill: Path, @Output skillBin: Path, @Output orchestrator: Path, @Output orchestratorBin: Path,
) {
    val version = Regex("const val CLI_VERSION = \"([^\"]+)\"").find(sources.resolve("kotlin/bootstrap/Configuration.kt").readText())!!.groupValues[1]
    val files = TreeMap<String, ByteArray>()
    fun tree(from: Path, prefix: String) = Files.walk(from).use { paths ->
        paths.filter { Files.isRegularFile(it) && it.name != ".DS_Store" }.forEach { files[prefix + from.relativize(it).toString().replace('\\', '/')] = it.readBytes() }
    }
    tree(sources.resolve("kotlin"), "src/main/kotlin/")
    tree(sources.resolve("resources"), "resources/")
    tree(assets, "assets/")
    files["build-tools/module.yaml"] = pluginModule.readBytes()
    files["build-tools/src/build.kt"] = pluginSource.readBytes()
    files["build-tools/plugin.yaml"] = SKILL_PLUGIN.toByteArray()
    files["project.yaml"] = projectFile.readBytes()
    files["module.yaml"] = moduleFile.readText().replace(Regex("(?m)^test-settings:\\n(?:[ \\t].*\\n|\\n)*"), "").toByteArray()
    files["toolchain.json"] = toolchain.readBytes()
    files["kotlin"] = wrapper.readBytes()
    files["kotlin.bat"] = windowsWrapper.readBytes()
    for ((target, bin) in listOf(skill to skillBin, orchestrator to orchestratorBin)) {
        target.toFile().deleteRecursively()
        for ((name, bytes) in files) {
            val file = target.resolve(name); Files.createDirectories(file.parent); file.writeBytes(bytes)
        }
        target.resolve("kotlin").toFile().setExecutable(true, false)
        bin.toFile().deleteRecursively(); Files.createDirectories(bin)
        for (name in listOf("bootstrap", "bootstrap.cmd")) bin.resolve(name).writeText(launchers.resolve(name).readText()
            .replace("@SKILL@", target.parent.name).replace("@VERSION@", version).replace("@MIN_JAVA@", MIN_CLI_JAVA.toString()))
        bin.resolve("bootstrap").toFile().setExecutable(true, false)
    }
    println("Synced CLI sources: $skill, $orchestrator")
}

// Kotlin Toolchain 0.13.0 starts its CLI with JVM options that need Java 25.
private const val MIN_CLI_JAVA = 25

// The skill copy builds from its own directory and has no bundle command.
private val SKILL_PLUGIN = """
tasks:
  resources:
    action: !bootstrap.build.prepareSkillResources
      assets: ${'$'}{project.rootDir}/assets
      toolchain: ${'$'}{project.rootDir}/toolchain.json
      sourceResources: ${'$'}{project.rootDir}/resources
      output: ${'$'}{taskOutputDir}

generated:
  resources:
    - directory: ${'$'}{tasks.resources.action.output}
""".trimStart()
