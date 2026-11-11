package com.wpf.utils.jiagu

import com.wpf.utils.ResourceManager
import java.io.File
import java.util.zip.*

fun main(args: Array<String>) {
    val root = File(args[0]); val sdk = File(args[1])
    val rootsOnly = args.getOrNull(2) == "--native-roots-only"
    val work = File(root, "build/compatibility/fixture").apply { mkdirs() }
    ResourceManager.cachePath = File(root, "desktop").path
    ResourceManager.tempFolderPath = File(work, "tools").apply { mkdirs() }.path + File.separator
    val manifest = File(work, "AndroidManifest.xml")
    manifest.writeText("""<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.wpf.jiagu.compat" android:versionCode="1">
        <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="33"/>
        <application android:name="com.wpf.jiagu.compat.FixtureApplication" android:debuggable="true" android:label="Jiagu regression" android:extractNativeLibs="false"
          android:icon="@drawable/bootstrap_icon" android:theme="@style/BootstrapTheme"
          android:appComponentFactory="com.wpf.jiagu.compat.FixtureComponentFactory">
          <meta-data android:name="fixture.replaceLoader" android:value="true"/>
          <service android:name="com.wpf.jiagu.compat.FixtureService" android:exported="false"/>
          <activity android:name="com.wpf.jiagu.compat.FixtureActivity" android:exported="true"/>
          <provider android:name="com.wpf.jiagu.compat.FixtureProvider" android:authorities="com.wpf.jiagu.compat.main" android:exported="false"/>
          <provider android:name="com.wpf.jiagu.compat.RemoteFixtureProvider" android:authorities="com.wpf.jiagu.compat.remote" android:process=":probe" android:exported="true"/>
        </application></manifest>""".trimIndent())
    val resources = File(work, "resources.apk")
    compileResourceFixture(work, sdk, manifest, resources)
    val input = File(work, if (rootsOnly) "compat-roots.apk" else "compat.apk")
    val native = nativeFixtures(root, sdk, rootsOnly)
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
        out.putNextEntry(ZipEntry("classes2.dex"))
        File(root, "build/compatibility/secondary/classes.dex").inputStream().use { it.copyTo(out) }; out.closeEntry()
        native.walkTopDown().filter { it.extension == "so" && (!rootsOnly || it.name != "libcompat_dependency.so") }.forEach { lib ->
            out.putNextEntry(ZipEntry("lib/" + lib.relativeTo(native).invariantSeparatorsPath))
            lib.inputStream().use { it.copyTo(out) }; out.closeEntry()
        }
    }
    JiaGu.deal(input.path, "0123456789abcdef", "fedcba9876543210", sdk.path, System.getProperty("java.home"),
        File(root, "jiagulibrary/jiagu.jks").path, "jiagu", "abc12345678", "abc12345678", showLog = true,
        obfuscateResourcePaths = true, encryptResourceContents = true, encryptNativeLibraries = true)
    val output = File(work, input.nameWithoutExtension + "_jiagu_sign.apk")
    val report = ProtectionReport.read(output)
    val outputManifest = com.wpf.utils.jiagu.utils.ApplicationHelper.manifestXml(output)
    check("android:minSdkVersion=\"21\"" in outputManifest && "android:targetSdkVersion=\"33\"" in outputManifest)
    check(report.getAsJsonArray("d2cMethods").size() > 0 && report.getAsJsonArray("vmpMethods").size() > 0)
    check(report.getAsJsonArray("extractedCodeItems").size() > 0)
    check(report.getAsJsonArray("extractedCodeItems").any { it.asJsonObject["dex"].asString == "classes2.dex" })
    check(report.getAsJsonObject("resources")["renamedFiles"].asInt >= 3)
    val encryption = report.getAsJsonObject("resourceEncryption")
    check(report.getAsJsonObject("nativeProtection").getAsJsonArray("encryptedFiles").size() == 8)
    if (rootsOnly) ZipFile(output).use { zip ->
        check(zip.entries().asSequence().filter { NativeProtection.isLibrary(it.name) }.all { it.name.endsWith("/libjiagu_abi_marker.so") })
    }
    check(encryption.getAsJsonArray("encryptedFiles").size() >= 5 && encryption.getAsJsonArray("bootstrapFiles").size() == 4)
    println("COMPATIBILITY_FIXTURE_READY: $output")
}
