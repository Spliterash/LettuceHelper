import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm") version "1.7.21"
    `maven-publish`
}

group = "ru.spliterash"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    api("com.redis:lettucemod:3.1.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")


}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "ru.spliterash"
            artifactId = rootProject.name

            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "nexus"
            url = uri("https://repo.spliterash.ru/" + rootProject.name)
            credentials {
                username = findProperty("SPLITERASH_NEXUS_USR")?.toString()
                password = findProperty("SPLITERASH_NEXUS_PSW")?.toString()
            }
        }
    }
}