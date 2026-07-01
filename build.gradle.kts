plugins {
    application
    kotlin("jvm") version "2.4.0"
}

val ktorVersion = "3.5.1"

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")

    runtimeOnly("org.slf4j:slf4j-simple:2.0.18")
}

application {
    mainClass.set("com.example.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    register<Jar>("shadowJar") {
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes["Main-Class"] = application.mainClass.get()
        }

        val runtimeClasspath = configurations.runtimeClasspath.get()
        from(sourceSets.main.get().output)
        dependsOn(runtimeClasspath)
        from({
            runtimeClasspath.filter { it.name.endsWith(".jar") }.map { zipTree(it) }
        })
    }
}
