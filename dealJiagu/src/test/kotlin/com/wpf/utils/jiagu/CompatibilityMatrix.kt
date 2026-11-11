package com.wpf.utils.jiagu

import com.google.gson.GsonBuilder
import com.wpf.utils.ResourceManager
import java.io.File
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal data class MatrixProfile(val id: String, val resources: Boolean, val native: Boolean)
internal val matrixProfiles = listOf(
    MatrixProfile("code", false, false), MatrixProfile("resources", true, false),
    MatrixProfile("native", false, true), MatrixProfile("full", true, true))
internal fun matrixPackage(id: String) = "com.wpf.jiagu.compat.matrix.${if (id == "native") "jni" else id}"
internal fun matrixFolder(root: File) = File(root, "build/compatibility/matrix")
internal fun matrixSha(file: File): String {
    val hash = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(65536)
        while (true) { val count = input.read(buffer); if (count < 0) break; hash.update(buffer, 0, count) }
    }
    return hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
}

/** Four independent apps, with identical baseline/hardened fixture code within each profile. */
fun main(args: Array<String>) {
    val root = File(args[0]); val sdk = File(args[1])
    val folder = matrixFolder(root).apply { mkdirs() }
    ResourceManager.cachePath = File(root, "desktop").path
    ResourceManager.tempFolderPath = File(folder, "tools").apply { mkdirs() }.path + File.separator
    val artifacts = mutableListOf<Map<String, Any>>()
    val inventory = linkedMapOf<String, Any>("schemaVersion" to 1, "status" to "running",
        "generatedAtUtc" to java.time.Instant.now().toString(), "deviceValidation" to "not_run",
        "artifacts" to artifacts, "abis" to listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"),
        "minSdk" to 21, "targetSdk" to 33)
    fun save() = File(folder, "inventory.json").writeText(GsonBuilder().setPrettyPrinting().create().toJson(inventory))
    save() // Invalidate any successful inventory from a previous, interrupted generation.
    try {
        val store = File(root, "jiagulibrary/jiagu.jks")
        fun sign(input: File, output: File) = PackageSigner.sign(store.path, "jiagu", "abc12345678", "abc12345678",
            outApkPath = output.path, inputApkPath = input.path, androidSdkPath = sdk.path)
        var certificate: String? = null
        for (profile in matrixProfiles) {
            val pkg = matrixPackage(profile.id)
            val fixtureWork = File(folder, "work/${profile.id}-sources").apply { mkdirs() }
            val dex = compileMatrixDex(root, sdk, pkg, fixtureWork)
            val native = if (profile.native) nativeFixtures(root, sdk, packageName = pkg) else null
            val rules = File(fixtureWork, "matrix.d2c.txt").apply { writeText(listOf(
                "L${pkg.replace('.', '/')}/FixtureChecks;->straight(I)I",
                "L${pkg.replace('.', '/')}/FixtureChecks;->branching(I)I").joinToString("\n")) }
            for (version in 1..2) {
                // Version 1 has a signed unprotected control. Version 2 proves a real version upgrade.
                for (hardened in if (version == 1) listOf(false, true) else listOf(true)) {
                    val kind = if (hardened) "hardened" else "baseline"
                    val work = File(folder, "work/${profile.id}-v$version-$kind").apply { mkdirs() }
                    val manifest = File(work, "AndroidManifest.xml").apply { writeText("""
                        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="$pkg" android:versionCode="$version" android:versionName="$version.0">
                          <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="33"/>
                          <application android:name="com.wpf.jiagu.compat.FixtureApplication" android:debuggable="true" android:label="Jiagu ${profile.id} $kind v$version"
                              android:extractNativeLibs="false" android:icon="@drawable/bootstrap_icon" android:theme="@style/BootstrapTheme"
                              android:appComponentFactory="com.wpf.jiagu.compat.FixtureComponentFactory">
                            <meta-data android:name="fixture.hardened" android:value="$hardened"/>
                            <meta-data android:name="fixture.native" android:value="${profile.native}"/>
                            <service android:name="com.wpf.jiagu.compat.FixtureService" android:exported="false"/>
                            <activity android:name="com.wpf.jiagu.compat.FixtureActivity" android:exported="true">
                              <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
                            </activity>
                            <provider android:name="com.wpf.jiagu.compat.FixtureProvider" android:authorities="$pkg.main" android:exported="false"/>
                            <provider android:name="com.wpf.jiagu.compat.RemoteFixtureProvider" android:authorities="$pkg.remote" android:process=":probe" android:exported="true"/>
                          </application>
                        </manifest>
                    """.trimIndent().replace("com.wpf.jiagu.compat.Fixture", "$pkg.Fixture")
                        .replace("com.wpf.jiagu.compat.Remote", "$pkg.Remote")) }
                    val resources = File(work, "resources.apk")
                    compileResourceFixture(work, sdk, manifest, resources)
                    val input = File(work, "input.apk")
                    ZipOutputStream(input.outputStream()).use { out ->
                        fun put(name: String, bytes: ByteArray, stored: Boolean = false) {
                            val entry = ZipEntry(name)
                            if (stored) { entry.method = ZipEntry.STORED; entry.size = bytes.size.toLong(); entry.crc = CRC32().apply { update(bytes) }.value }
                            out.putNextEntry(entry); out.write(bytes); out.closeEntry()
                        }
                        ZipFile(resources).use { zip -> zip.entries().asSequence().forEach { entry ->
                            put(entry.name, zip.getInputStream(entry).use { it.readBytes() }, entry.method == ZipEntry.STORED)
                        } }
                        put("classes.dex", dex.first.readBytes())
                        put("classes2.dex", dex.second.readBytes())
                        if (native != null) native.walkTopDown().filter { it.extension == "so" }.forEach {
                            put("lib/" + it.relativeTo(native).invariantSeparatorsPath, it.readBytes(), true)
                        }
                    }
                    val output = File(folder, "${profile.id}-v$version-$kind.apk")
                    if (hardened) {
                        // Never allow an old APK to masquerade as successful output after JiaGu's logged failure.
                        val built = File(work, "input_jiagu.apk")
                        built.delete(); File(built.path + ".protection.json").delete()
                        JiaGu.deal(input.path, "0123456789abcdef", "fedcba9876543210", sdk.path, System.getProperty("java.home"),
                            d2cConfigPath = rules.path, obfuscateResourcePaths = profile.resources,
                            encryptResourceContents = profile.resources, encryptNativeLibraries = profile.native)
                        check(built.isFile && File(built.path + ".protection.json").isFile) { "Hardening failed for ${profile.id}" }
                        val report = ProtectionReport.read(built)
                        check(report.getAsJsonArray("d2cMethods").size() > 0 && report.getAsJsonArray("vmpMethods").size() > 0)
                        check(report.getAsJsonArray("extractedCodeItems").any { it.asJsonObject["dex"].asString == "classes2.dex" })
                        if (profile.resources) check(report.getAsJsonObject("resourceEncryption").getAsJsonArray("encryptedFiles").size() >= 5)
                        if (profile.native) check(report.getAsJsonObject("nativeProtection").getAsJsonArray("encryptedFiles").size() == 8)
                        val signStarted = System.nanoTime()
                        sign(built, output)
                        val signingMs = (System.nanoTime() - signStarted) / 1_000_000
                        report.addProperty("outputName", output.name); report.addProperty("outputSha256", matrixSha(output))
                        report.addProperty("signed", true); report.addProperty("outputBytes", output.length())
                        report.addProperty("sizeDeltaBytes", output.length() - input.length())
                        report.addProperty("packagingMs", report["packagingMs"].asLong + signingMs)
                        report.getAsJsonObject("phasesMs").addProperty("signing", signingMs)
                        File(output.path + ".protection.json").writeText(GsonBuilder().setPrettyPrinting().create().toJson(report))
                    } else sign(input, output)
                    val actualCertificate = PackageSigner.certificate(output)
                    if (certificate == null) certificate = actualCertificate
                    check(certificate == actualCertificate) { "Matrix signing certificates differ" }
                    artifacts.add(linkedMapOf("profile" to profile.id, "packageName" to pkg, "versionCode" to version,
                        "kind" to kind, "file" to output.name, "sha256" to matrixSha(output), "bytes" to output.length(),
                        "certificateSha256" to actualCertificate, "resourceEncryption" to (hardened && profile.resources),
                        "nativeEncryption" to (hardened && profile.native)))
                    save()
                }
            }
        }
        inventory["status"] = "passed"
    } catch (error: Throwable) {
        inventory["status"] = "failed"; inventory["error"] = error.message.orEmpty(); throw error
    } finally { save() }
    val bundle = File(folder, "compatibility-test-apks.zip")
    ZipOutputStream(bundle.outputStream()).use { zip ->
        val files = artifacts.flatMap { artifact ->
            val apk = File(folder, artifact["file"].toString())
            listOf(apk) + File(apk.path + ".protection.json").takeIf { it.isFile }.let { listOfNotNull(it) }
        } + File(folder, "inventory.json")
        for (file in files) {
            zip.putNextEntry(ZipEntry(file.name)); file.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
        }
        zip.putNextEntry(ZipEntry("README.md"))
        File(root, "dealJiagu/COMPATIBILITY_MATRIX.md").inputStream().use { it.copyTo(zip) }; zip.closeEntry()
    }
    println("COMPATIBILITY_MATRIX_READY: ${artifacts.size} signed APKs; ${File(folder, "inventory.json")}")
}
