package bootstrap

import kotlinx.serialization.json.jsonObject
import org.junit.Test
import java.nio.file.Path
import kotlin.test.*

class KoinAgentRenderingTest {
    private fun render(di: String = "koin", agents: List<String> = listOf("codex", "claude"), data: Boolean = false): Rendered {
        val fields = mapOf("name" to "Aurora", "package" to "com.example.aurora", "targets" to listOf("desktop"),
            "di" to di, "agents" to agents, "network" to if (data) "ktor" else "none")
        val resolved = resolveSpec(value(fields).jsonObject, Path.of("build/koin-agent-test-unused").toAbsolutePath())
        assertTrue(resolved.valid, resolved.issues.toString())
        return renderModern(resolved.options!!)
    }
    private fun Rendered.text(path: String) = files.getValue(path).toString(Charsets.UTF_8)

    @Test fun selectedAgentsReceiveCompleteUnmodifiedSkillsAndOwnMcpConfiguration() {
        val adapters = mapOf("codex" to (".agents/skills" to ".codex/config.toml"),
            "claude" to (".claude/skills" to ".mcp.json"),
            "gemini" to (".agents/skills" to ".gemini/settings.json"),
            "copilot" to (".github/skills" to ".vscode/mcp.json"),
            "cursor" to (".agents/skills" to ".cursor/mcp.json"))
        val prefix = "koin-agent/skills/"
        val resources = Resources.paths.filter { it.startsWith(prefix) }
        assertEquals(setOf("koin", "koin-setup", "di-migration", "koin-diagnose"),
            resources.filter { it.endsWith("/SKILL.md") }.map { it.removePrefix(prefix).substringBefore('/') }.toSet())
        for ((agent, paths) in adapters) for (data in listOf(false, true)) {
            val (root, config) = paths
            val rendered = render(agents = listOf(agent), data = data)
            for (resource in resources) assertContentEquals(Resources.bytes(resource), rendered.files.getValue("$root/${resource.removePrefix(prefix)}"))
            assertTrue("https://mcp.kotzilla.io/mcp" in rendered.text(config))
            for ((other, otherPaths) in adapters) if (other != agent && otherPaths.second != ".mcp.json")
                assertFalse(otherPaths.second in rendered.files, "$agent emitted ${otherPaths.second}")
            assertTrue("keep Kotlin Toolchain and the existing Koin DSL" in rendered.text("AGENTS.md"))
            assertTrue("have not been verified" in rendered.text("README.md"))
            assertContentEquals(Resources.bytes("koin-agent/LICENSE"), rendered.files.getValue(".agents/koin-upstream/LICENSE"))
        }
    }
    @Test fun manualDiAndNoAgentSelectionDoNotInstallKoinTooling() {
        for (rendered in listOf(render(di = "manual"), render(agents = listOf("none")))) {
            assertFalse(rendered.files.keys.any { it.contains("/skills/") || "mcp" in it || it == ".codex/config.toml" })
            assertFalse("Kotzilla" in rendered.text("AGENTS.md"))
        }
    }
    @Test fun defaultAndCombinedSelectionsKeepEveryAdapter() {
        val default = render()
        assertTrue(".agents/skills/koin/SKILL.md" in default.files)
        assertTrue(".claude/skills/koin/SKILL.md" in default.files)
        val all = render(agents = listOf("codex", "claude", "gemini", "copilot", "cursor"))
        assertTrue("httpUrl" in all.text(".gemini/settings.json"))
        assertTrue("mcpServers" in all.text(".mcp.json"))
        assertTrue("servers" in all.text(".vscode/mcp.json"))
        assertTrue("[mcp_servers.kotzilla]" in all.text(".codex/config.toml"))
    }
}
