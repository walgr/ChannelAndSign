package com.wpf.utils.jiagu

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.util.UUID

/** Device evidence is tied to exact APK hashes; no business apps are installed, stopped or removed. */
fun main(args: Array<String>) {
    val root = File(args[0]); val serial = args[1]; val iterations = args[2].toInt()
    require(serial.matches(Regex("[A-Za-z0-9_.:-]+"))) { "Invalid device serial" }
    require(iterations in 1..1000)
    val requestedProfiles = args.getOrNull(3)?.split(',')?.map { it.trim() }?.toSet()
        ?: matrixProfiles.map { it.id }.toSet()
    require(requestedProfiles.isNotEmpty() && requestedProfiles.all { id -> matrixProfiles.any { it.id == id } }) {
        "matrixProfiles must contain only code,resources,native,full"
    }
    val selectedProfiles = matrixProfiles.filter { it.id in requestedProfiles }
    val source = matrixFolder(root)
    val inventory = JsonParser.parseString(File(source, "inventory.json").readText()).asJsonObject
    check(inventory["status"].asString == "passed") { "Generate a complete matrix first" }
    val artifacts = inventory.getAsJsonArray("artifacts").map { it.asJsonObject }
    check(artifacts.size == matrixProfiles.size * 3)
    for (profile in matrixProfiles) for ((version, kind) in listOf(1 to "baseline", 1 to "hardened", 2 to "hardened")) {
        val name = "${profile.id}-v$version-$kind.apk"
        val artifact = artifacts.single { it["file"].asString == name }
        check(artifact["packageName"].asString == matrixPackage(profile.id))
        check(matrixSha(File(source, name)) == artifact["sha256"].asString) { "APK changed; regenerate inventory: $name" }
    }
    check(artifacts.map { it["certificateSha256"].asString }.distinct().size == 1)
    val runId = UUID.randomUUID().toString().replace("-", "")
    val folder = File(source, "device-${serial.replace(':', '_')}/$runId").apply { mkdirs() }
    val archivedApks = File(folder, "apks").apply { mkdirs() }
    File(source, "inventory.json").copyTo(File(archivedApks, "inventory.json"))
    for (artifact in artifacts) {
        val apk = File(source, artifact["file"].asString)
        apk.copyTo(File(archivedApks, apk.name))
        File(apk.path + ".protection.json").takeIf { it.isFile }?.let { it.copyTo(File(archivedApks, it.name)) }
    }
    val adb = FixtureAdb(serial)
    val results = mutableListOf<Map<String, Any>>()
    val profiles = mutableListOf<Map<String, Any>>()
    val report = linkedMapOf<String, Any>("schemaVersion" to 1, "scope" to "device_matrix", "status" to "running",
        "serial" to serial, "runId" to runId, "startedAtUtc" to java.time.Instant.now().toString(),
        "iterationsPerVariant" to iterations, "selectedProfiles" to selectedProfiles.map { it.id },
        "artifacts" to artifacts, "scenarios" to results, "profiles" to profiles,
        "limits" to "Synthetic fixtures; results apply only to this device/API/ABI. am start -W is not first-frame or long-duration soak measurement.")
    val log = File(folder, "evidence.log")
    val gson = GsonBuilder().setPrettyPrinting().create()
    fun save() {
        File(folder, "results.json").writeText(gson.toJson(report))
        File(folder, "results.md").writeText("# 多应用加固签名设备测试\n\n设备：$serial；API：${report["sdk"]}；ABI：${report["abis"]}；状态：${report["status"]}\n\n" +
            "重复冷进程启动：原始签名包和加固签名包各 $iterations 次/应用。\n\n" +
            "| 应用 | 状态 | 已通过场景 |\n| --- | --- | --- |\n" + profiles.joinToString("\n") {
                "| ${it["profile"]} | ${it["status"]} | ${it["passedScenarios"]} |"
            } + "\n\n详细启动耗时、APK 哈希、错误和清理结果见 results.json，原始设备输出见 evidence.log。\n" +
            "\n此结果仅覆盖本次设备/API/ABI；不代表全部机型兼容或长期稳定性。\n")
    }
    fun shell(command: String): String = adb.shell(command).also { log.appendText("$ $command\n$it\n") }
    var sdk = 0
    var activeScenario = "preflight"
    var deviceStartedAt = ""
    fun captureCrash(pkg: String) {
        val since = if (deviceStartedAt.isEmpty()) "" else "-T '$deviceStartedAt' "
        val crash = adb.shell("logcat -d -b crash -v threadtime ${since}-t 300")
        val lines = crash.lines()
        // Persist only this fixture's crash records, including debuggerd records for native crashes.
        val crashLogPids = lines.filter { "Process: $pkg," in it || ">>> $pkg <<<" in it }
            .mapNotNull { it.trim().split(Regex("\\s+")).getOrNull(2) }.toSet()
        val filtered = lines.filter { it.trim().split(Regex("\\s+")).getOrNull(2) in crashLogPids }.joinToString("\n")
        log.appendText("fixture crash evidence: $pkg\n$filtered\n")
        if (sdk >= 24) for (crashedPid in crashLogPids.filter { it.matches(Regex("[0-9]+")) }) {
            val nativeLog = adb.shell("logcat -d -v threadtime --pid=$crashedPid ${since}-s NDK_JIAGU:D NDK_JIAGU_PERF:I '*:S'")
            log.appendText("fixture native diagnostics pid=$crashedPid\n$nativeLog\n")
        }
    }
    fun pid(name: String): Int {
        val candidates = if (sdk >= 26) shell("pidof $name").trim().split(Regex("\\s+"))
            .mapNotNull { it.toIntOrNull() }.distinct() else shell("ps").lineSequence()
            .map { it.trim().split(Regex("\\s+")) }.filter { it.size >= 2 && it.last() == name }
            .mapNotNull { it[1].toIntOrNull() }.distinct().toList()
        check(candidates.isNotEmpty()) { "Process is not alive: $name" }
        if (candidates.size == 1) return candidates.single()
        // OEM processes can share a command name. Match Android's component owner, never pidof's arbitrary order.
        val record = Regex("ProcessRecord\\{[^}\\r\\n]*\\s(\\d+):${Regex.escape(name)}/")
        for (component in listOf("activities", "providers")) {
            val owners = record.findAll(adb.shell("dumpsys activity $component"))
                .map { it.groupValues[1].toInt() }.filter { it in candidates }.distinct().toList()
            if (owners.size == 1) {
                log.appendText("resolved component PID name=$name candidates=$candidates owner=${owners.single()} via=$component\n")
                return owners.single()
            }
        }
        error("Ambiguous fixture process ownership: $name candidates=$candidates")
    }
    fun readLogs(process: Int): String = adb.shell("logcat -d -v raw " +
        (if (sdk >= 24) "--pid=$process " else "") + "-s JIAGU_COMPAT:I NDK_JIAGU_PERF:I NDK_JIAGU:D AndroidRuntime:E '*:S'")
    fun timing(evidence: String, process: Int): JsonObject? = evidence.lineSequence().filter { it.startsWith('{') }
        .mapNotNull { runCatching { JsonParser.parseString(it).asJsonObject }.getOrNull() }
        .filter { it["pid"]?.asInt == process && it["stage"]?.asString == "application" }.lastOrNull()
    fun launch(profile: MatrixProfile, kind: String, scenario: String, version: Int = 1, recreate: Boolean = false,
               expectedCacheMiss: String? = null) {
        val pkg = matrixPackage(profile.id)
        activeScenario = "$kind/$scenario"
        val token = UUID.randomUUID().toString().replace("-", "")
        shell("am force-stop $pkg")
        val start = shell("am start -W -f 0x10008000 -n $pkg/.FixtureActivity --es matrix_run $token " +
            "--es matrix_state $runId --ei matrix_version $version --ez matrix_recreate $recreate " +
            "--ez matrix_require_state ${scenario != "fresh_install" && scenario != "fresh_data"}")
        check("Status: ok" in start && "Error" !in start) { "Activity launch failed: $start" }
        val process = pid(pkg)
        val deadline = System.nanoTime() + 30_000_000_000L
        var metrics: JsonObject?
        var evidence: String
        while (true) {
            evidence = readLogs(process)
            metrics = timing(evidence, process)
            if ("MATRIX_ACTIVITY_PASSED token=$token pid=$process" in evidence &&
                (kind == "baseline" || metrics?.get("status")?.asString == "ok")) break
            if ("FATAL EXCEPTION" in evidence || metrics?.get("status")?.asString == "failed" || System.nanoTime() > deadline) {
                log.appendText("launch failure pid=$process\n$evidence\n")
                if ("FATAL EXCEPTION" !in evidence) {
                    val activities = adb.shell("dumpsys activity activities")
                    val resumed = Regex("(?m)^\\s*(?:mResumedActivity|topResumedActivity)\\s*[:=].*$")
                        .findAll(activities).map { it.value.trim() }.toList()
                    log.appendText("foreground at assertion failure: ${resumed.joinToString("; ")}\n")
                    if (metrics?.get("status")?.asString == "ok" && resumed.isNotEmpty() && resumed.none { "$pkg/" in it })
                        error("Test environment interrupted fixture foreground: ${resumed.joinToString("; ")}")
                }
                error("Missing successful fixture assertion for ${profile.id}/$scenario")
            }
            Thread.sleep(200)
        }
        log.appendText("launch evidence pid=$process\n$evidence\n")
        check(pid(pkg) == process) { "Fixture restarted unexpectedly" }
        expectedCacheMiss?.let {
            check(metrics?.getAsJsonObject("phasesMs")?.get(it)?.asInt == 0) { "Damaged cache was not rebuilt: $it" }
        }
        for (marker in listOf("APPLICATION", "PROVIDER", "RESOURCES") + if (profile.native) listOf("NATIVE") else emptyList())
            check("COMPAT_${marker}_PASSED pid=$process" in evidence) { "Missing $marker assertion" }
        val times = Regex("(?m)^(ThisTime|TotalTime|WaitTime): (\\d+)").findAll(start)
            .associate { it.groupValues[1] to it.groupValues[2].toLong() }
        val result = linkedMapOf<String, Any>("profile" to profile.id, "kind" to kind, "scenario" to scenario,
            "versionCode" to version, "status" to "passed", "pid" to process, "activityStartCommandMs" to times)
        Regex("(?m)^processAbi=([^,]+),").find(evidence)?.groupValues?.get(1)?.let { result["processAbi"] = it }
        metrics?.let { result["shellStartup"] = it }
        results.add(result); save()
    }
    fun remote(profile: MatrixProfile, kind: String) {
        val pkg = matrixPackage(profile.id)
        activeScenario = "$kind/remote_process_provider"
        check("probe=ok" in shell("content query --uri content://$pkg.remote/probe")) { "Remote Provider failed" }
        val process = pid("$pkg:probe")
        val deadline = System.nanoTime() + 30_000_000_000L
        var evidence: String
        var metrics: JsonObject?
        while (true) {
            evidence = readLogs(process); metrics = timing(evidence, process)
            if ("COMPAT_APPLICATION_PASSED pid=$process" in evidence &&
                (kind == "baseline" || metrics?.get("status")?.asString == "ok")) break
            check(System.nanoTime() < deadline && "FATAL EXCEPTION" !in evidence) { "Remote Application did not complete" }
            Thread.sleep(200)
        }
        log.appendText("remote evidence pid=$process\n$evidence\n")
        val result = linkedMapOf<String, Any>("profile" to profile.id, "kind" to kind,
            "scenario" to "remote_process_provider", "status" to "passed", "pid" to process)
        metrics?.let { result["shellStartup"] = it }
        results.add(result); save()
    }
    var failed = false
    try {
        save()
        sdk = shell("getprop ro.build.version.sdk").trim().toInt()
        deviceStartedAt = adb.shell("date '+%m-%d %H:%M:%S.000'").trim().takeIf {
            it.matches(Regex("[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\\.000"))
        }.orEmpty()
        report["sdk"] = sdk; report["abis"] = shell("getprop ro.product.cpu.abilist").trim()
        report["model"] = shell("getprop ro.product.model").trim()
        report["fingerprint"] = shell("getprop ro.build.fingerprint").trim()
        report["pageSize"] = shell("getconf PAGESIZE").trim()
        check(sdk >= 21) { "Matrix requires API 21+" }
        for (profile in selectedProfiles) {
            val pkg = matrixPackage(profile.id)
            val before = results.size
            var owned = false
            val status = linkedMapOf<String, Any>("profile" to profile.id, "status" to "running")
            profiles.add(status)
            try {
                activeScenario = "preflight"
                check(shell("pm path $pkg").trim().isEmpty()) { "Test package already installed; refusing to overwrite: $pkg" }
                fun install(version: Int, kind: String, update: Boolean) {
                    activeScenario = "$kind/install_v$version"
                    val file = File(source, "${profile.id}-v$version-$kind.apk")
                    val result = adb.install(file, update)
                    log.appendText("install ${file.name}, update=$update: $result\n")
                    check("Success" in result) { "Install failed: $result" }
                    owned = true
                }
                install(1, "baseline", false)
                repeat(iterations) { launch(profile, "baseline", if (it == 0) "fresh_install" else "repeated_cold_$it") }
                launch(profile, "baseline", "activity_recreation", recreate = true)
                remote(profile, "baseline")
                install(1, "hardened", true)
                launch(profile, "hardened", "baseline_to_hardened_update_preserves_state")
                activeScenario = "hardened/reset_fixture_data"
                shell("am force-stop $pkg")
                val cleared = shell("pm clear $pkg")
                if ("Success" in cleared) {
                    status["freshDataReset"] = "pm_clear"
                } else {
                    check("SecurityException" in cleared && "android.permission.CLEAR_APP_USER_DATA" in cleared) {
                        "Cannot reset owned fixture data: $cleared"
                    }
                    // Only this run's fixture is removed. Verify removal before a fresh signed install.
                    check(owned && "Success" in shell("pm uninstall $pkg")) { "Cannot uninstall owned fixture for data reset" }
                    owned = false
                    check(shell("pm path $pkg").trim().isEmpty()) { "Fixture still installed after data reset uninstall" }
                    install(1, "hardened", false)
                    status["freshDataReset"] = "uninstall_reinstall_after_clear_permission_denied"
                }
                repeat(iterations) { launch(profile, "hardened", if (it == 0) "fresh_data" else "repeated_cold_$it") }
                fun damage(path: String, scenario: String, cacheFlag: String) {
                    shell("am force-stop $pkg")
                    check("MATRIX_CACHE_DAMAGED" in shell("run-as $pkg sh -c 'chmod 600 $path && echo CORRUPT > $path && echo MATRIX_CACHE_DAMAGED'"))
                    launch(profile, "hardened", scenario, expectedCacheMiss = cacheFlag)
                }
                val shellCache = Regex("libjiaguv3_[0-9]+_[0-9]+_(x86_64|x86|arm64-v8a|armeabi-v7a)\\.so")
                    .find(shell("run-as $pkg ls .jiagu"))?.value ?: error("Shell cache missing")
                damage(".jiagu/$shellCache", "damaged_shell_cache_recovered", "so_cache_hit")
                if (profile.resources) {
                    val resourceCache = Regex("resources-[0-9a-f]{64}\\.apk")
                        .find(shell("run-as $pkg ls no_backup/jiagu-resources"))?.value ?: error("Resource cache missing")
                    damage("no_backup/jiagu-resources/$resourceCache", "damaged_resource_cache_recovered", "resource_cache_hit")
                }
                if (profile.native) {
                    val nativeVersion = Regex("[0-9a-f]{64}").find(shell("run-as $pkg ls no_backup/jiagu-native"))?.value
                        ?: error("Native cache missing")
                    val nativeAbi = shell("run-as $pkg ls no_backup/jiagu-native/$nativeVersion").trim()
                    check(nativeAbi in setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
                    damage("no_backup/jiagu-native/$nativeVersion/$nativeAbi/libcompat_entry.so", "damaged_native_cache_recovered", "native_cache_hit")
                }
                launch(profile, "hardened", "activity_recreation", recreate = true)
                remote(profile, "hardened")
                install(2, "hardened", true)
                launch(profile, "hardened", "version_upgrade_preserves_state", version = 2)
                status["status"] = "passed"
            } catch (error: Throwable) {
                failed = true; status["status"] = "failed"; status["error"] = error.message.orEmpty()
                status["failedScenario"] = activeScenario
                status["failureCategory"] = when {
                    error is java.net.SocketException || error is java.net.SocketTimeoutException || error is java.io.EOFException ->
                        "adb_transport_failure"
                    error.message.orEmpty().startsWith("Test environment interrupted") -> "test_environment_interference"
                    else -> "fixture_failure"
                }
                runCatching { captureCrash(pkg) }
            } finally {
                if (owned) {
                    val cleanup = runCatching { shell("am force-stop $pkg"); shell("pm uninstall $pkg") }
                    status["cleanup"] = if (cleanup.getOrNull()?.contains("Success") == true) "passed" else "failed"
                    if (status["cleanup"] == "failed") { failed = true; status["status"] = "failed" }
                } else status["cleanup"] = "not_installed_by_this_run"
                status["passedScenarios"] = results.size - before
                save()
                println("MATRIX_PROFILE: ${profile.id}: ${status["status"]}; ${status["passedScenarios"]} scenarios")
            }
        }
    } catch (error: Throwable) {
        failed = true; report["error"] = error.message.orEmpty()
    } finally {
        report["status"] = if (failed) "failed" else "passed"
        report["finishedAtUtc"] = java.time.Instant.now().toString()
        // Compare only repeated cold-process samples; first-run extraction and upgrade are reported separately.
        report["startupComparison"] = selectedProfiles.map { profile ->
            val summary = linkedMapOf<String, Any>("profile" to profile.id)
            for (kind in listOf("baseline", "hardened")) {
                val samples = results.filter { it["profile"] == profile.id && it["kind"] == kind &&
                    it["scenario"].toString().startsWith("repeated_cold_") }
                    .mapNotNull { (it["activityStartCommandMs"] as? Map<*, *>)?.get("TotalTime") as? Long }.sorted()
                summary[kind] = if (samples.isEmpty()) mapOf("status" to "not_measured") else mapOf(
                    "samples" to samples.size, "minMs" to samples.first(), "medianMs" to samples[samples.size / 2],
                    "p95Ms" to samples[kotlin.math.ceil(samples.size * 0.95).toInt() - 1], "maxMs" to samples.last())
            }
            summary
        }
        save()
    }
    check(!failed) { "Matrix device testing failed; see ${File(folder, "results.json")}" }
    println("COMPATIBILITY_MATRIX_PASSED: ${results.size} scenarios; ${File(folder, "results.json")}")
}
