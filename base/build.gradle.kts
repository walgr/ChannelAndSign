plugins {
    id("kotlin")
}

group = "com.wpf.util"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dongliu:apk-parser:2.6.10")
//    api("com.wpf.utils:tools:1.1.18")
    api(project(":Tools"))
    api("com.android:zipflinger:7.3.1")              //压缩包操作
//    implementation(project(":AXMLEditor3"))
//    implementation("co.touchlab:kermit:2.0.0-RC4")            //日志保存
    implementation(kotlin("stdlib-jdk8"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
kotlin {
    jvmToolchain(8)
}

tasks.register("打包", Jar::class) {
    group = "jar"
    archiveFileName = "打渠道包并签名.jar"
    destinationDirectory.set(file("D:\\Android\\ShareFile\\tools"))
    manifest {
        attributes["Main-Class"] = "com.wpf.base.dealfile.MainKt"
        attributes["Manifest-Version"] = "1.0.1"
    }
    from(
        sourceSets.main.get().output,
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    )
    exclude(
        "META-INF/*.RSA",
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/LICENSE.txt",
        "META-INF/NOTICE.txt",
        "META-INF/versions/9/module-info.class",
        "module-info.class",
        "META-INF/INDEX.LIST"
    )
}