package dev.qtremors.eonslate

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArchitectureTest {
    private val root = generateSequence(File(".").canonicalFile) { it.parentFile }
        .first { it.resolve("settings.gradle.kts").isFile }
    private val modulePattern = Regex("""include\("(:[^"]+)"\)""")
    private val projectPattern = Regex("""project\("(:[^"]+)"\)""")
    private val modules = modulePattern.findAll(root.resolve("settings.gradle.kts").readText()).map { it.groupValues[1] }.toSet()

    @Test
    fun featureAndCoreDependenciesPointInward() {
        val invalid = modules.flatMap { owner ->
            val script = directory(owner).resolve("build.gradle.kts").readText()
            projectPattern.findAll(script).map { it.groupValues[1] }.filter { dependency ->
                dependency !in modules || when {
                    owner.startsWith(":core:") -> !dependency.startsWith(":core:")
                    owner.startsWith(":feature:") -> !dependency.startsWith(":core:")
                    else -> dependency == owner
                }
            }.map { "$owner depends on $it" }.toList()
        }
        assertTrue(invalid.joinToString("\n"), invalid.isEmpty())
    }

    @Test
    fun appSourcesDoNotRequestNetworkAccess() {
        val forbidden = Regex("""android\.permission\.(INTERNET|ACCESS_NETWORK_STATE)\b""")
        val invalid = modules.flatMap { module ->
            directory(module).resolve("src").listFiles().orEmpty().map { it.resolve("AndroidManifest.xml") }
                .filter { it.isFile && forbidden.containsMatchIn(it.readText()) }
                .map { it.relativeTo(root).invariantSeparatorsPath }
        }
        assertTrue(invalid.joinToString("\n"), invalid.isEmpty())
    }

    private fun directory(module: String) = root.resolve(module.trimStart(':').replace(':', '/'))
}
