package com.wpf.utils.jiagu

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.net.Socket
import java.nio.charset.StandardCharsets

/** ADB smart socket avoids host adb executable/home differences; all commands target the fixture only. */
internal class FixtureAdb(private val serial: String) {
    fun shell(command: String): String = request("shell:$command")
    fun install(apk: File, update: Boolean): String {
        if (shell("getprop ro.build.version.sdk").trim().toInt() >= 24) return request(
            "exec:cmd package install ${if (update) "-r " else ""}-t -S ${apk.length()}", apk)
        val created = shell("pm install-create ${if (update) "-r " else ""}-t -S ${apk.length()}")
        val session = Regex("\\[(\\d+)\\]").find(created)?.groupValues?.get(1)
            ?: error("Cannot create legacy fixture install session: $created")
        try {
            val written = request("exec:pm install-write -S ${apk.length()} $session base -", apk)
            check("Success" in written) { written }
            return shell("pm install-commit $session")
        } finally { shell("pm install-abandon $session") }
    }
    private fun request(service: String, upload: File? = null): String = Socket("127.0.0.1", 5037).use { socket ->
        socket.soTimeout = 120000
        val input = java.io.DataInputStream(socket.getInputStream()); val output = socket.getOutputStream()
        fun send(value: String) {
            val bytes = value.toByteArray(StandardCharsets.UTF_8)
            output.write("%04x".format(bytes.size).toByteArray(StandardCharsets.US_ASCII)); output.write(bytes); output.flush()
            val status = ByteArray(4); input.readFully(status)
            check(String(status, StandardCharsets.US_ASCII) == "OKAY") { "ADB rejected fixture service on $serial" }
        }
        send("host:transport:$serial"); send(service)
        upload?.inputStream()?.use { it.copyTo(output); output.flush() }
        input.readBytes().toString(StandardCharsets.UTF_8)
    }
}

fun main(args: Array<String>) {
    val root = File(args[0]); val serial = args[1]
    val rootsOnly = args.getOrNull(2) == "--native-roots-only"
    val folder = File(root, "build/compatibility/device-${serial.replace(Regex("[^A-Za-z0-9_.-]"), "_")}" + if (rootsOnly) "-native-roots" else "").apply { mkdirs() }
    val adb = FixtureAdb(serial)
    val pkg = "com.wpf.jiagu.compat"
    val apk = File(root, "build/compatibility/fixture/" + if (rootsOnly) "compat-roots_jiagu_sign.apk" else "compat_jiagu_sign.apk")
    val results = mutableListOf<Map<String, Any>>()
    val report = linkedMapOf<String, Any>("schemaVersion" to 1, "scope" to "device_fixture", "serial" to serial,
        "startedAtUtc" to java.time.Instant.now().toString(),
        "fixtureSha256" to ProtectionReport.read(apk)["outputSha256"].asString,
        "packageName" to pkg, "status" to "running", "scenarios" to results,
        "limits" to "Independent fixture only; not validation of a business APK, every OEM, API or ABI")
    var installedByThisRun = false
    var failure: Throwable? = null
    val log = StringBuilder()
    fun save() {
        folder.resolve("results.json").writeText(GsonBuilder().setPrettyPrinting().create().toJson(report), Charsets.UTF_8)
        folder.resolve("evidence.log").writeText(log.toString(), Charsets.UTF_8)
    }
    fun shell(command: String): String = adb.shell(command).also { log.append("$ $command\n$it\n") }
    fun processPid(name: String): Int {
        if (report["sdk"].toString().toInt() >= 26) return shell("pidof $name").trim().split(' ').first().toIntOrNull()
            ?: error("Fixture process did not remain alive: $name")
        return shell("ps | grep '$pkg'").lineSequence().map { it.trim().split(Regex("\\s+")) }
            .firstOrNull { it.size >= 2 && it.last() == name }?.get(1)?.toIntOrNull()
            ?: error("Fixture process did not remain alive: $name")
    }
    fun records(pid: Int): List<JsonObject> {
        val deadline = System.nanoTime() + 30_000_000_000L
        while (true) {
            val lines = shell("logcat -d -v raw -s NDK_JIAGU_PERF:I JIAGU_COMPAT:I '*:S'")
            val parsed = lines.lineSequence().filter { it.startsWith('{') }.mapNotNull { line ->
                runCatching { JsonParser.parseString(line).asJsonObject }.getOrNull()
            }.filter { it["pid"]?.asInt == pid && it.has("phasesMs") }.toList()
            check(parsed.none { it["status"].asString == "failed" }) { "Shell startup reported a failure" }
            if (parsed.any { it["stage"].asString == "application" }) return parsed
            check(System.nanoTime() < deadline) { "Missing completed Application timing for pid=$pid" }
            // Some framework versions return am start -W with UNKNOWN before Application finishes.
            Thread.sleep(200)
        }
    }
    fun launch(name: String, cacheHit: Int? = null, resourceCacheHit: Int = 1, nativeCacheHit: Int = 1) {
        shell("am force-stop $pkg")
        val start = shell("am start -W -n $pkg/.FixtureActivity")
        check("Status: ok" in start && "Error" !in start) { "Activity launch failed: $start" }
        val pid = processPid(pkg)
        val metrics = records(pid)
        check(metrics.none { it["status"].asString == "failed" }) { "Shell startup reported a failure" }
        val complete = metrics.lastOrNull { it["stage"].asString == "application" } ?: error("Missing completed Application timing")
        check(complete["status"].asString == "ok")
        cacheHit?.let { check(complete.getAsJsonObject("phasesMs")["so_cache_hit"].asInt == it) { "Unexpected SO cache state for $name" } }
        check(complete.getAsJsonObject("phasesMs")["resource_cache_hit"].asInt == resourceCacheHit) { "Unexpected resource cache state for $name" }
        check(complete.getAsJsonObject("phasesMs")["native_cache_hit"].asInt == nativeCacheHit) { "Unexpected native cache state for $name" }
        val markerDeadline = System.nanoTime() + 30_000_000_000L
        while (true) {
            val evidence = shell("logcat -d -v raw -s JIAGU_COMPAT:I '*:S'")
            if ("COMPAT_ACTIVITY_PASSED pid=$pid" in evidence && "COMPAT_APPLICATION_PASSED pid=$pid" in evidence &&
                "COMPAT_PROVIDER_PASSED pid=$pid" in evidence && "COMPAT_RESOURCES_PASSED pid=$pid" in evidence && "COMPAT_NATIVE_PASSED pid=$pid" in evidence) break
            check(System.nanoTime() < markerDeadline) { "Fixture Application/Provider/Activity assertions did not all finish" }
            Thread.sleep(200)
        }
        val activityTimes = Regex("(?m)^(ThisTime|TotalTime|WaitTime): (\\d+)").findAll(start)
            .associate { it.groupValues[1] to it.groupValues[2].toLong() }
        results.add(mapOf("name" to name, "status" to "passed", "pid" to pid, "shellStartup" to complete,
            "activityStartCommandMs" to activityTimes, "resourceReads" to "passed"))
        save()
    }
    try {
        save()
        report["sdk"] = shell("getprop ro.build.version.sdk").trim()
        report["abis"] = shell("getprop ro.product.cpu.abilist").trim()
        check(report["sdk"].toString().toInt() >= 21) { "Fixture requires API 21+" }
        check(shell("pm path $pkg").trim().isEmpty()) { "Fixture package already exists; refusing to overwrite an existing installation" }
        val install = adb.install(apk, false); log.append("install: $install\n")
        check("Success" in install) { install }; installedByThisRun = true
        launch("fresh_install_cold_process", 0, 0, 0)
        launch("cold_process_reuses_so_cache", 1)

        shell("am force-stop $pkg")
        val nativeVersion = Regex("[0-9a-f]{64}").find(shell("run-as $pkg ls no_backup/jiagu-native"))?.value
            ?: error("No native cache version found")
        val nativeAbi = shell("run-as $pkg ls no_backup/jiagu-native/$nativeVersion").trim()
        check(nativeAbi in setOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a"))
        val nativeFile = "no_backup/jiagu-native/$nativeVersion/$nativeAbi/libcompat_entry.so"
        check("NATIVE_CACHE_DAMAGED" in shell("run-as $pkg sh -c 'chmod 600 $nativeFile && echo CORRUPT > $nativeFile && echo NATIVE_CACHE_DAMAGED'"))
        launch("damaged_native_cache_recovered", 1, 1, 0)

        shell("am force-stop $pkg")
        val resourceFiles = shell("run-as $pkg ls no_backup/jiagu-resources")
        val resourceFile = Regex("resources-[0-9a-f]{64}\\.apk").find(resourceFiles)?.value ?: error("No resource cache found")
        val damagedResource = shell("run-as $pkg sh -c 'chmod 600 no_backup/jiagu-resources/$resourceFile && echo CORRUPT > no_backup/jiagu-resources/$resourceFile && echo RESOURCE_CACHE_DAMAGED'")
        check("RESOURCE_CACHE_DAMAGED" in damagedResource)
        launch("damaged_resource_cache_recovered", 1, 0)

        shell("am force-stop $pkg")
        val names = shell("run-as $pkg ls .jiagu")
        val file = Regex("libjiaguv3_[0-9]+_[0-9]+_(x86_64|x86|arm64-v8a|armeabi-v7a)\\.so").find(names)?.value
            ?: error("No fixture cached shell SO found")
        val damaged = shell("run-as $pkg sh -c 'echo CORRUPT > .jiagu/$file && echo CACHE_DAMAGED'")
        check("CACHE_DAMAGED" in damaged) { "Cannot create fixture cache corruption case" }
        launch("damaged_so_cache_recovered", 0)

        val query = shell("content query --uri content://$pkg.remote/probe")
        check("probe=ok" in query) { "Remote Provider query failed: $query" }
        val remotePid = processPid("$pkg:probe")
        val remote = records(remotePid).lastOrNull { it["stage"].asString == "application" }
            ?: error("Missing remote process startup record")
        check(remote["status"].asString == "ok")
        check(remote.getAsJsonObject("phasesMs")["resource_cache_hit"].asInt == 1)
        check(remote.getAsJsonObject("phasesMs")["native_cache_hit"].asInt == 1)
        results.add(mapOf("name" to "remote_process_provider", "status" to "passed", "pid" to remotePid, "shellStartup" to remote,
            "resourceReads" to "passed"))
        save()

        shell("am force-stop $pkg")
        val update = adb.install(apk, true); log.append("update: $update\n"); check("Success" in update) { update }
        launch("same_certificate_update_install", 0)
        report["status"] = "passed"
    } catch (error: Throwable) {
        failure = error; report["status"] = "failed"; report["error"] = error.message.orEmpty()
    } finally {
        if (installedByThisRun) {
            val cleanup = runCatching { shell("am force-stop $pkg"); shell("pm uninstall $pkg") }
            report["fixtureCleanup"] = if (cleanup.getOrNull()?.contains("Success") == true) "passed" else "failed"
            if (report["fixtureCleanup"] == "failed") {
                report["status"] = "failed"; failure = failure ?: IllegalStateException("Fixture uninstall failed")
            }
        } else report["fixtureCleanup"] = "not_installed_by_this_run"
        save()
    }
    failure?.let { throw IllegalStateException("Device regression failed; see ${folder.resolve("results.json")}", it) }
    println("DEVICE_COMPATIBILITY_PASSED: $serial; ${results.size} scenarios; ${folder.resolve("results.json")}")
}
