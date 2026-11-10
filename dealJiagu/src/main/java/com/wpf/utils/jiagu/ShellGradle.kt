package com.wpf.utils.jiagu

import java.io.File
import java.net.URI
import java.util.Properties

/** Read-only discovery of existing installations. Never rewrites wrapper settings or cache files. */
internal object ShellGradle {
    data class Launch(val command: List<String>, val environment: Map<String, String>, val description: String)

    fun prepare(project: File, jdkPath: String, tasks: List<String>,
                environment: Map<String, String> = System.getenv(),
                properties: Properties = System.getProperties()): Launch {
        val wrapper = Properties().apply {
            File(project, "gradle/wrapper/gradle-wrapper.properties").inputStream().use { load(it) }
        }
        val url = wrapper.getProperty("distributionUrl") ?: error("壳工程缺少 Gradle distributionUrl")
        val version = Regex("gradle-(.+)-(bin|all)\\.zip").matchEntire(URI(url).path.substringAfterLast('/'))?.groupValues?.get(1)
        val home = File(properties.getProperty("gradle.user.home")?.takeIf { it.isNotBlank() }
            ?: environment["GRADLE_USER_HOME"]?.takeIf { it.isNotBlank() }
            ?: File(properties.getProperty("user.home"), ".gradle").path).absoluteFile
        val javaHome = File(jdkPath.ifBlank { properties.getProperty("java.home") }).absoluteFile
        val java = File(javaHome, "bin/" + if (File.separatorChar == '\\') "java.exe" else "java")
        require(java.isFile) { "壳构建 JDK 不可用：$javaHome" }
        val childEnvironment = mapOf("GRADLE_USER_HOME" to home.path, "JAVA_HOME" to javaHome.path)
        val arguments = listOf("--gradle-user-home", home.path) +
            listOf("-Dorg.gradle.java.home=${javaHome.path}") + tasks
        fun launcher(installation: File): File? = version?.let {
            File(installation, "lib/gradle-launcher-$it.jar").takeIf { file -> file.isFile && file.length() > 0L }
        }
        val configured = environment["GRADLE_HOME"]?.takeIf { it.isNotBlank() }?.let(::File)
        val installed = configured?.takeIf { launcher(it) != null } ?: version?.let { required ->
            listOf("bin", "all").asSequence().flatMap { type ->
                val distribution = "gradle-$required-$type"
                File(home, "wrapper/dists/$distribution").listFiles()?.sortedBy { it.name }.orEmpty().asSequence()
                    .filter { it.isDirectory && File(it, "$distribution.zip.ok").isFile }
                    .map { File(it, "gradle-$required") }
            }.firstOrNull { launcher(it) != null }
        }
        return if (installed != null) {
            // This is the same launcher entry point as bin/gradle. Bypass Wrapper URL hashing,
            // but retain the exact required Gradle version and selected dependency cache home.
            val agent = File(installed, "lib/agents/gradle-instrumentation-agent-$version.jar")
            val agentOptions = if (agent.isFile) listOf("-javaagent:${agent.absolutePath}") else emptyList()
            Launch(listOf(java.path, "-Xmx64m", "-Xms64m") + agentOptions + listOf("-Dorg.gradle.appname=gradle", "-classpath",
                requireNotNull(launcher(installed)).absolutePath, "org.gradle.launcher.GradleMain") + arguments,
                childEnvironment, "复用本地 Gradle $version：${installed.absolutePath}；缓存目录：${home.path}")
        } else {
            val script = File(project, if (File.separatorChar == '\\') "gradlew.bat" else "gradlew")
            Launch(listOf(script.absolutePath) + arguments, childEnvironment,
                "本地未找到完整的 Gradle ${version ?: "所需版本"} 分发，使用壳 Wrapper；缓存目录：${home.path}，可能下载分发包")
        }
    }
}
