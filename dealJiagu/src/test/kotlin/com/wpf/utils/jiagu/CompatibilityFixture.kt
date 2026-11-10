package com.wpf.utils.jiagu

import com.wpf.utils.ResourceManager
import java.io.File
import java.util.zip.*

fun main(args: Array<String>) {
    val root = File(args[0]); val sdk = File(args[1])
    val work = File(root, "build/compatibility/fixture").apply { mkdirs() }
    ResourceManager.cachePath = File(root, "desktop").path
    ResourceManager.tempFolderPath = File(work, "tools").apply { mkdirs() }.path + File.separator
    val manifest = File(work, "AndroidManifest.xml")
    manifest.writeText("""<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.wpf.jiagu.compat" android:versionCode="1">
        <uses-sdk android:minSdkVersion="26" android:targetSdkVersion="33"/>
        <application android:name="com.wpf.jiagu.compat.FixtureApplication" android:debuggable="true" android:label="Jiagu regression">
          <activity android:name="com.wpf.jiagu.compat.FixtureActivity" android:exported="true"/>
          <provider android:name="com.wpf.jiagu.compat.FixtureProvider" android:authorities="com.wpf.jiagu.compat.main" android:exported="false"/>
          <provider android:name="com.wpf.jiagu.compat.RemoteFixtureProvider" android:authorities="com.wpf.jiagu.compat.remote" android:process=":probe" android:exported="true"/>
        </application></manifest>""".trimIndent())
    val aapt = File(sdk, "build-tools/35.0.0/aapt2.exe")
    val resources = File(work, "resources.apk")
    val process = ProcessBuilder(aapt.path, "link", "--manifest", manifest.path, "-I",
        File(sdk, "platforms/android-33/android.jar").path, "-o", resources.path).redirectErrorStream(true).start()
    val result = process.inputStream.bufferedReader().readText(); check(process.waitFor() == 0) { result }
    val input = File(work, "compat.apk")
    ZipOutputStream(input.outputStream()).use { out ->
        ZipFile(resources).use { zip -> zip.entries().asSequence().forEach { entry ->
            val bytes = zip.getInputStream(entry).use { it.readBytes() }
            val copied = ZipEntry(entry.name)
            if (entry.name == "resources.arsc") {
                copied.method = ZipEntry.STORED; copied.size = bytes.size.toLong()
                copied.crc = CRC32().apply { update(bytes) }.value
            }
            out.putNextEntry(copied); out.write(bytes); out.closeEntry()
        } }
        out.putNextEntry(ZipEntry("classes.dex"))
        File(root, "build/hybrid-app/original/classes.dex").inputStream().use { it.copyTo(out) }; out.closeEntry()
    }
    JiaGu.deal(input.path, "0123456789abcdef", "fedcba9876543210", sdk.path, System.getProperty("java.home"),
        File(root, "jiagulibrary/jiagu.jks").path, "jiagu", "abc12345678", "abc12345678", showLog = true)
    val output = File(work, "compat_jiagu_sign.apk")
    val report = ProtectionReport.read(output)
    check(report.getAsJsonArray("d2cMethods").size() > 0 && report.getAsJsonArray("vmpMethods").size() > 0)
    check(report.getAsJsonArray("extractedCodeItems").size() > 0)
    println("COMPATIBILITY_FIXTURE_READY: $output")
}
