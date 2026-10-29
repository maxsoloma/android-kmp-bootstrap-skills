package bootstrap.build

import org.jetbrains.amper.plugins.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.TreeMap
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.io.path.*

@TaskAction
fun prepareResources(@Input assets: Path, @Input toolchain: Path, @Input sourceResources: Path, @Output output: Path) {
    output.toFile().deleteRecursively()
    Files.createDirectories(output)
    fun copyTree(from: Path, to: Path) {
        Files.walk(from).use { paths -> paths.filter { Files.isRegularFile(it) }.forEach {
            val target = to.resolve(from.relativize(it).toString()); Files.createDirectories(target.parent)
            Files.copy(it, target)
        } }
    }
    copyTree(assets, output.resolve("assets"))
    Files.copy(toolchain, output.resolve("toolchain.json"))
    val names = sortedSetOf<String>()
    for (root in listOf(output, sourceResources)) {
        Files.walk(root).use { paths -> paths.filter { Files.isRegularFile(it) }.forEach { names += root.relativize(it).toString().replace('\\', '/') } }
    }
    names.remove("resources.index")
    output.resolve("resources.index").writeText(names.joinToString("\n", postfix = "\n"))
}

@TaskAction
fun bundle(@Input app: CompilationArtifact, @Input dependencies: Classpath, @Output output: Path, @Output skill: Path, @Output orchestrator: Path) {
    val entries = TreeMap<String, ByteArray>()
    for (file in listOf(app.artifact) + dependencies.resolvedFiles.sortedBy { it.toString() }) {
        if (!Files.isRegularFile(file) || !file.toString().endsWith(".jar")) continue
        JarFile(file.toFile()).use { jar ->
            jar.entries().asSequence().filter { !it.isDirectory }.forEach { entry ->
                val name = entry.name
                if (name != "META-INF/MANIFEST.MF" && !Regex("META-INF/[^/]+\\.(SF|RSA|DSA)", RegexOption.IGNORE_CASE).matches(name))
                    entries.putIfAbsent(name, jar.getInputStream(entry).use { it.readBytes() })
            }
        }
    }
    val manifest = Manifest().apply {
        mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        mainAttributes[Attributes.Name.MAIN_CLASS] = "bootstrap.MainKt"
    }
    entries["META-INF/MANIFEST.MF"] = java.io.ByteArrayOutputStream().also { manifest.write(it) }.toByteArray()
    Files.createDirectories(output.parent)
    JarOutputStream(Files.newOutputStream(output)).use { jar ->
        for ((name, bytes) in entries) {
            jar.putNextEntry(JarEntry(name).apply { time = 0L }); jar.write(bytes); jar.closeEntry()
        }
    }
    Files.createDirectories(skill.parent)
    Files.copy(output, skill, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    Files.createDirectories(orchestrator.parent)
    Files.copy(output, orchestrator, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    println("Bundled standalone CLI: $output")
}
