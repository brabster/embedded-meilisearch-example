plugins {
    application
}

val ktorVersion = "2.3.12"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25")
    }
}

apply(plugin = "org.jetbrains.kotlin.jvm")

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

    runtimeOnly("org.slf4j:slf4j-simple:2.0.16")
}

application {
    mainClass.set("com.example.ApplicationKt")
}

extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>("kotlin") {
    jvmToolchain(17)
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
