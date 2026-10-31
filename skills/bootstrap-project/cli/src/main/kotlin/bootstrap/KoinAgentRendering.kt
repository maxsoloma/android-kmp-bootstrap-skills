package bootstrap

/** Offline, project-scoped installation; upstream skill trees remain byte-for-byte. */
fun configureKoinAgents(o: Options, files: MutableMap<String, ByteArray>) {
    if (o.di != "koin" || o.agents == listOf("none")) return
    fun put(path: String, content: String) { files[path] = content.toByteArray(Charsets.UTF_8) }
    val roots = linkedSetOf<String>()
    if (o.agents.any { it in listOf("codex", "gemini", "cursor") }) roots += ".agents/skills"
    if ("claude" in o.agents) roots += ".claude/skills"
    if ("copilot" in o.agents) roots += ".github/skills"
    val prefix = "koin-agent/skills/"
    for (resource in Resources.paths.filter { it.startsWith(prefix) }) {
        for (root in roots) files["$root/${resource.removePrefix(prefix)}"] = Resources.bytes(resource)
    }
    for (name in listOf("LICENSE", "PRIVACY.md", "SOURCE.md"))
        files[".agents/koin-upstream/$name"] = Resources.bytes("koin-agent/$name")

    val endpoint = "https://mcp.kotzilla.io/mcp"
    val server = mapOf("kotzilla" to mapOf("type" to "http", "url" to endpoint))
    if ("codex" in o.agents) put(".codex/config.toml", "[mcp_servers.kotzilla]\nurl = \"$endpoint\"\n")
    if ("claude" in o.agents || "copilot" in o.agents) put(".mcp.json", encode(mapOf("mcpServers" to server)))
    if ("copilot" in o.agents) put(".vscode/mcp.json", encode(mapOf("servers" to server)))
    if ("cursor" in o.agents) put(".cursor/mcp.json", encode(mapOf("mcpServers" to mapOf("kotzilla" to mapOf("url" to endpoint)))))
    if ("gemini" in o.agents) put(".gemini/settings.json", encode(mapOf("mcpServers" to mapOf("kotzilla" to mapOf("httpUrl" to endpoint)))))
    val guide = """

## Koin agent tooling

The official koin, koin-setup, di-migration and koin-diagnose skills are installed
locally with their complete references. Read the koin skill for DI changes and
koin-diagnose for Koin failures. Installed roots: ${roots.joinToString(", ")}.
Provenance and Apache-2.0 license: .agents/koin-upstream/SOURCE.md and LICENSE.

Project constraints take precedence over upstream Gradle examples: inspect
project.yaml and module.yaml, keep Kotlin Toolchain and the existing Koin DSL.
Do not introduce Gradle, KSP, compiler plugins or a DI migration automatically.

Kotzilla MCP is configured for the selected agents at $endpoint (HTTP/OAuth).
Trust/reload the project in your agent to discover local skills and MCP settings.
Authenticate in the agent before using Kotzilla (Codex: `codex mcp login kotzilla`;
Claude Code: `/mcp`, select kotzilla and sign in). Authentication and live MCP
access have not been verified by generation. Skills work without signing in.
Runtime measurements additionally require the Kotzilla SDK and recorded sessions;
do not add the SDK or upload application data without the user's authorization.
"""
    for (path in listOf("AGENTS.md", "README.md")) put(path, files.getValue(path).toString(Charsets.UTF_8) + guide)
}
