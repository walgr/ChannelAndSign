package com.wpf.utils.jiagu

import com.google.gson.JsonParser
import com.wpf.utils.ResourceManager
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Real binary manifests, already signed APKs, nested resources, and failure transactions. */
fun main(args: Array<String>) {
    val root = File(args[0])
    val sdk = File(args[1])
    val work = File(root, "build/xapk-regression").apply { mkdirs() }
    ResourceManager.cachePath = File(root, "desktop").path
    ResourceManager.tempFolderPath = File(work, "tools").apply { mkdirs() }.path + File.separator
    val embeddedDex = "SDK asset DEX must stay byte-identical".toByteArray()
    val selectionFixture = File(work, "dex-selection.apk")
    ZipOutputStream(selectionFixture.outputStream()).use { out ->
        listOf("classes10.dex", "assets/audience_network/", "assets/audience_network/classes.dex",
            "assets/classes2.dex", "plugin.dex", "index", "classes999999999999999999999.dex",
            "classes1.dex", "classes02.dex", "classes.dex", "classes3.dex", "classes2.dex").forEach { name ->
            out.putNextEntry(ZipEntry(name))
            if (!name.endsWith('/')) out.write(name.toByteArray())
            out.closeEntry()
        }
    }
    fun selectedNames(entries: List<Pair<String, java.io.InputStream>>) = entries.map { (name, stream) ->
        stream.use { check(it.readBytes().contentEquals(name.toByteArray())) }
        name
    }
    val expectedDex = listOf("classes.dex", "classes2.dex", "classes3.dex", "classes10.dex")
    com.android.zipflinger.ZipArchive(selectionFixture.toPath()).use {
        check(selectedNames(com.wpf.utils.jiagu.utils.DexHelper.getApkSrcDexList(it)) == expectedDex)
    }
    net.lingala.zip4j.ZipFile(selectionFixture).use {
        check(selectedNames(com.wpf.utils.jiagu.utils.DexHelper.getApkSrcDexList(it)) == expectedDex)
    }
    println("DEX_SELECTION_PASSED: both ZIP readers, numeric multidex ordering, embedded DEX/index/invalid suffix exclusion")
    fun tool(vararg command: String) {
        val process = ProcessBuilder(command.toList()).redirectErrorStream(true).start()
        val result = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { result }
    }
    val aapt = File(sdk, "build-tools/35.0.0/aapt2.exe")
    val android = File(sdk, "platforms/android-33/android.jar")
    fun apk(name: String, split: String = ""): File {
        val dir = File(work, name).apply { mkdirs() }
        val manifest = File(dir, "AndroidManifest.xml")
        manifest.writeText("""<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.wpf.jiagu.d2c" android:versionCode="1" $split>
            <uses-sdk android:minSdkVersion="26" android:targetSdkVersion="33"/><application android:hasCode="${split.isEmpty()}"/></manifest>""")
        val result = File(dir, "$name.apk")
        tool(aapt.path, "link", "--manifest", manifest.path, "-I", android.path, "-o", result.path)
        if (split.isEmpty()) {
            val replacement = File(dir, "with-dex.apk")
            ZipOutputStream(replacement.outputStream()).use { out ->
                ZipFile(result).use { zip -> zip.entries().asSequence().forEach {
                    out.putNextEntry(ZipEntry(it.name)); zip.getInputStream(it).use { it.copyTo(out) }; out.closeEntry()
                } }
                out.putNextEntry(ZipEntry("classes.dex"))
                File(root, "build/hybrid-app/original/classes.dex").inputStream().use { it.copyTo(out) }; out.closeEntry()
                out.putNextEntry(ZipEntry("assets/audience_network/")); out.closeEntry()
                out.putNextEntry(ZipEntry("assets/audience_network/classes.dex"))
                out.write(embeddedDex); out.closeEntry()
            }
            XapkArchive.publish(replacement, result)
        }
        return result
    }
    val store = File(root, "jiagulibrary/jiagu.jks").path
    fun sign(input: File, output: File = File(input.parentFile, input.nameWithoutExtension + "_sign." + input.extension), pass: String = "abc12345678"): File =
        File(PackageSigner.sign(store, "jiagu", pass, pass, outApkPath = output.path, inputApkPath = input.path, androidSdkPath = sdk.path))
    val base = apk("base")
    val config = apk("config", "split=\"config.en\"")
    check(com.wpf.utils.jiagu.utils.ApplicationHelper.getName(base) == "android.app.Application")
    val signedBase = sign(base)
    val signedConfig = sign(config)
    val obb = ByteArray(1024 * 1024) { (it % 251).toByte() }
    val input = File(work, "sample.XAPK")
    fun archive(file: File, paths: List<Pair<String, ByteArray>>) {
        ZipOutputStream(file.outputStream()).use { out -> paths.forEach { (name, bytes) ->
            out.putNextEntry(ZipEntry(name)); out.write(bytes); out.closeEntry()
        } }
    }
    archive(input, listOf("apks/main.APK" to signedBase.readBytes(), "apks/config.en.apk" to signedConfig.readBytes(),
        "Android/obb/com.wpf.jiagu.d2c/main.1.obb" to obb,
        "icon.png" to byteArrayOf(1,2,3),
        "manifest.json" to """{"package_name":"com.wpf.jiagu.d2c","total_size":0,"unknown":{"preserve":true},"split_apks":[{"file":"apks/main.APK","size":0,"md5":"old"},{"file":"apks/config.en.apk","size":0}]}""".toByteArray()))
    val inputBytes = input.readBytes()
    val output = sign(input)
    check(input.readBytes().contentEquals(inputBytes))
    ZipFile(output).use { zip ->
        check(zip.size() == 5)
        check(zip.getInputStream(zip.getEntry("Android/obb/com.wpf.jiagu.d2c/main.1.obb")).readBytes().contentEquals(obb))
        val metadata = JsonParser.parseString(zip.getInputStream(zip.getEntry("manifest.json")).bufferedReader().readText()).asJsonObject
        check(metadata["unknown"].asJsonObject["preserve"].asBoolean)
        val certificates = listOf("apks/main.APK", "apks/config.en.apk").mapIndexed { index, name ->
            val file = File(work, "verify-$index.apk")
            zip.getInputStream(zip.getEntry(name)).use { source -> file.outputStream().use { source.copyTo(it) } }
            check(metadata["split_apks"].asJsonArray[index].asJsonObject["size"].asLong == file.length())
            tool(File(sdk, "build-tools/35.0.0/zipalign.exe").path, "-c", "-P", "16", "4", file.path)
            PackageSigner.certificate(file)
        }
        check(certificates.distinct().size == 1)
        check(metadata["split_apks"].asJsonArray[0].asJsonObject["md5"].asString.length == 32)
    }
    val previous = output.readBytes()
    check(runCatching { sign(input, output, "wrong-password") }.isFailure)
    check(output.readBytes().contentEquals(previous) && input.readBytes().contentEquals(inputBytes))
    val failed = File(work, "failed.xapk")
    check(runCatching { XapkArchive.transform(input, failed) { _, baseMember ->
        baseMember.file.writeBytes(byteArrayOf(0))
        error("injected hardener failure after modifying base")
    } }.isFailure)
    check(!failed.exists())
    check(input.readBytes().contentEquals(inputBytes))
    for ((name, entries) in listOf(
        "traversal" to listOf("../escape.apk" to byteArrayOf(0)),
        "duplicate" to listOf("main.apk" to signedBase.readBytes(), "MAIN.apk" to signedBase.readBytes()),
        "no-base" to listOf("split.apk" to signedConfig.readBytes()),
        "two-bases" to listOf("one.apk" to signedBase.readBytes(), "two.apk" to signedBase.readBytes())
    )) {
        val bad = File(work, "$name.xapk"); archive(bad, entries)
        var reachedAction = false
        check(runCatching { XapkArchive.transform(bad, failed) { _, _ -> reachedAction = true } }.isFailure)
        check(!reachedAction) { "Malformed XAPK reached processing: $name" }
        check(!failed.exists())
    }
    check(work.listFiles()!!.none { it.name.startsWith(".xapk-") || it.name.startsWith(".apk-sign-") })
    println("XAPK_REGRESSION_PASSED: all APK signatures/alignment, metadata, OBB, uppercase paths, source preservation, rollback and malformed archives")
    if (args.getOrNull(2) == "--harden") {
        JiaGu.deal(input.path, "0123456789abcdef", "fedcba9876543210", sdk.path,
            jdkPath = System.getProperty("java.home"), signFilePath = store, signAlias = "jiagu",
            keyStorePassword = "abc12345678", keyPassword = "abc12345678", showLog = true)
        val protected = File(work, "sample_jiagu_sign.xapk")
        XapkArchive.transform(protected, File(work, "validated_jiagu.xapk")) { members, main ->
            check(com.wpf.utils.jiagu.utils.ApplicationHelper.getName(main.file) == "com.wpf.util.jiagulibrary.StubApp")
            val integrityKey = IntegritySeal.key(File(ResourceManager.cachePath, "cache/jiagu-integrity"))
            com.wpf.util.jiagulibrary.utils.IntegrityVerifier.verify(main.file, IntegritySeal.publicHex(integrityKey))
            File(work, "integrity-public.hex").writeText(IntegritySeal.publicHex(integrityKey))
            check(members.map { PackageSigner.certificate(it.file) }.distinct().size == 1)
            ZipFile(main.file).use { zip ->
                check(zip.getInputStream(zip.getEntry("assets/audience_network/classes.dex")).readBytes().contentEquals(embeddedDex))
                check(zip.getEntry("assets/libjiagu_x86.so") != null)
                check(zip.getEntry("assets/libjiagu_x86_64.so") != null)
                check(zip.getInputStream(zip.getEntry("classes.dex")).readBytes().size >
                    File(root, "build/hybrid-app/original/classes.dex").length())
            }
            ZipFile(signedConfig).use { before -> ZipFile(members.single { it.split.isNotEmpty() }.file).use { after ->
                before.entries().asSequence().filter { !it.name.startsWith("META-INF/") }.forEach { entry ->
                    check(before.getInputStream(entry).readBytes().contentEquals(after.getInputStream(after.getEntry(entry.name)).readBytes()))
                }
            } }
        }
        println("XAPK_HARDENING_PASSED: main payload/manifest/Intel SO, preserved split content, all signatures")
    }
}
