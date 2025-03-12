plugins {
    id("kotlin")
}

group = "com.wpf.tools.fileupload"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.wpf.utils:tools:+")
    implementation("net.dongliu:apk-parser:2.6.10")
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
    archiveFileName = "FileUpload.jar"
    destinationDirectory.set(file("D:\\Android\\ShareFile\\tools"))
    manifest {
        attributes["Main-Class"] = "com.wpf.tools.fileupload.MainKt"
        attributes["Manifest-Version"] = "1.0.0"
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
        "META-INF/versions/9/module-info.class",
        "module-info.class",
        "META-INF/INDEX.LIST"
    )
}