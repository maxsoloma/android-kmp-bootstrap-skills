package bootstrap

import org.junit.After
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.*

/** Filesystem discovery/portability checks; native host discovery has a separate live receipt. */
class SkillDiscoveryTest {
    private val repo = Path.of(System.getProperty("bootstrap.repo", ".")).toAbsolutePath()
    private val temp = Files.createTempDirectory("skill-discovery-")
    @After fun clean() { temp.toFile().deleteRecursively() }
    private fun copySkill(name: String): Path {
        val source = repo.resolve("skills/$name"); val target = temp.resolve(name)
        Files.walk(source).use { paths -> paths.forEach { from ->
            val to = target.resolve(source.relativize(from).toString())
            if (from.isDirectory()) Files.createDirectories(to) else Files.copy(from, to)
        } }
        return target
    }
    private fun validate(root: Path): Set<Path> {
        val entry = root.resolve("SKILL.md"); val body = entry.readText()
        require(body.startsWith("---\n")) { "Missing skill frontmatter" }
        val front = body.substringAfter("---\n").substringBefore("\n---")
        val name = Regex("(?m)^name: ([a-z0-9-]+)$").find(front)?.groupValues?.get(1)
        require(name == root.fileName.toString()) { "Missing/mismatched discovery name" }
        require(Regex("(?m)^description: .+\\S.*$").containsMatchIn(front)) { "Missing skill description" }
        val ui = root.resolve("agents/openai.yaml").readText()
        for (field in listOf("display_name", "short_description", "default_prompt")) require(Regex("(?m)^  $field: \".+\"$").containsMatchIn(ui)) { "Missing interface field $field" }
        require(ui.contains("\$$name")) { "Prompt does not invoke discovered skill" }
        for (file in listOf("bin/bootstrap", "bin/bootstrap.cmd", "cli/kotlin", "cli/kotlin.bat", "cli/module.yaml", "cli/project.yaml")) require(root.resolve(file).isRegularFile()) { "Missing portable runtime file $file" }
        val visited = mutableSetOf<Path>()
        fun links(file: Path) {
            if (!visited.add(file)) return
            for (match in Regex("\\[[^]]*]\\(([^)]+)\\)").findAll(file.readText())) {
                val link = match.groupValues[1].substringBefore('#')
                if (link.isEmpty() || "://" in link) continue
                val target = file.parent.resolve(link).normalize()
                require(target.startsWith(root) && target.isRegularFile()) { "Broken/nonportable reference $link from $file" }
                if (target.extension == "md") links(target)
            }
        }
        links(entry)
        return visited
    }
    @Test fun everyDiscoveredSkillWorksAsAnIndependentCopyWithItsOwnReferences() {
        val names = Files.list(repo.resolve("skills")).use { paths -> paths.filter { it.resolve("SKILL.md").isRegularFile() }.map { it.fileName.toString() }.toList().toSet() }
        assertEquals(setOf("bootstrap-project", "orchestrate-feature"), names)
        for (name in names) {
            val copied = copySkill(name)
            val reached = validate(copied)
            if (name == "orchestrate-feature") assertTrue(copied.resolve("references/testing.md") in reached, "Tester guide must be discoverable from the portable entrypoint")
        }
    }
    @Test fun missingTesterGuideAndMalformedEntrypointCannotPassDiscoveryValidation() {
        val copied = copySkill("orchestrate-feature")
        copied.resolve("references/testing.md").deleteExisting()
        assertFails { validate(copied) }
        Files.copy(repo.resolve("skills/orchestrate-feature/references/testing.md"), copied.resolve("references/testing.md"))
        val entry = copied.resolve("SKILL.md"); entry.writeText(entry.readText().replace("name: orchestrate-feature", "name: wrong-name"))
        assertFails { validate(copied) }
    }
}
