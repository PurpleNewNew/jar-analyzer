import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.GradleException
import org.gradle.api.file.DuplicatesStrategy

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    `maven-publish`
}

group = "me.n1ar4"
version = "6.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://packages.jetbrains.team/maven/p/kpm/public")
    maven("https://www.jetbrains.com/intellij-repository/releases")
}

configurations.configureEach {
    resolutionStrategy {
        // Use locally cached AndroidX artifacts to avoid external Google Maven fetches.
        force("androidx.annotation:annotation:1.9.1")
        force("androidx.collection:collection:1.6.0-beta01")
        dependencySubstitution {
            substitute(module("androidx.lifecycle:lifecycle-common"))
                .using(module("androidx.lifecycle:lifecycle-common-jvm:2.9.4"))
            substitute(module("androidx.lifecycle:lifecycle-runtime"))
                .using(module("androidx.lifecycle:lifecycle-runtime-desktop:2.9.4"))
            substitute(module("androidx.lifecycle:lifecycle-viewmodel"))
                .using(module("androidx.lifecycle:lifecycle-viewmodel-desktop:2.9.4"))
        }
    }
}

dependencies {
    implementation(compose.desktop.currentOs) {
        exclude(group = "org.jetbrains.compose.material")
    }
    implementation("org.jetbrains.jewel:jewel-int-ui-standalone-243:0.27.0")
    implementation("org.jetbrains.jewel:jewel-int-ui-decorated-window-243:0.27.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("androidx.lifecycle:lifecycle-common-jvm:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-desktop:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-desktop:2.9.4")

    // Build from Maven package phase against already compiled core classes.
    compileOnly(files("../target/classes"))
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

java {
    withSourcesJar()
}

tasks.jar {
    archiveBaseName.set("jar-analyzer-ui-jewel")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    from(
        configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) }
    )
}

tasks.withType<KotlinCompile>().configureEach {
    doFirst {
        val coreClasses = file("../target/classes")
        if (!coreClasses.exists()) {
            throw GradleException("missing ../target/classes, run `mvn -DskipTests compile` first")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "jar-analyzer-ui-jewel"
        }
    }
}
