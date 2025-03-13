plugins {
    id("kotlin")
}

group = "pers.wpf.kotlin.utils.fileReplace"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
kotlin {
    jvmToolchain(8)
}

tasks.register("FileReplace打包", Jar::class) {
    group = "jar"
    archiveFileName = "FileReplace.jar"
    destinationDirectory.set(file("D:\\Android\\ShareFile\\tools"))
    manifest {
        attributes["Main-Class"] = "com.wpf.utils.MainKt"
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
        "META-INF/versions/9/module-info.class",
        "module-info.class",
        "META-INF/INDEX.LIST"
    )
}