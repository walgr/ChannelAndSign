plugins {
    id("kotlin")
    application
}

group = "com.wpf.server"
version = "0.0.1"

application {
    mainClass.set("com.wpf.server.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:2.3.2")
    implementation("io.ktor:ktor-server-cio-jvm:2.3.2")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.2")
}


kotlin {
    jvmToolchain(8)
}
