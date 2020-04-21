val maven_group: String by project
val mod_version: String by project
val archives_base_name: String by project
val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project
val fabric_version: String by project

val common = project(":MCAPI-common")

plugins {
    id("fabric-loom") version "0.2.6-SNAPSHOT"
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

base {
    archivesBaseName = archives_base_name
}

group = maven_group
version = mod_version

repositories {
    mavenLocal()
    maven(url = "http://maven.fabricmc.net") {
        name = "Fabric"
    }
}

minecraft {
}

dependencies {
    //to change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${minecraft_version}")
    mappings("net.fabricmc:yarn:${yarn_mappings}:v2")
    modCompile("net.fabricmc:fabric-loader:${loader_version}")

    // Fabric API. This is technically optional, but you probably want it anyway.
    modCompile("net.fabricmc.fabric-api:fabric-api:${fabric_version}")
    compile(common)

    // PSA: Some older mods, compiled on Loom 0.2.1, might have outdated Maven POMs.
    // You may need to force-disable transitiveness on them.
    testCompile("junit:junit:4.12")
}

tasks.getByName<ProcessResources>("processResources") {
    filesMatching("fabric.mod.json") {
        expand(
            mutableMapOf("version" to version)
        )
    }
}

// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
// if it is present.
// If you remove this task, sources will not be generated.
val sourcesJar = tasks.create<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

val buildFinal = tasks.create<Zip>("buildFinal") {
    from(zipTree((common.tasks.get("jar") as Jar).archiveFile))
    from(zipTree((tasks.get("remapJar") as Zip).archiveFile))

    archiveFileName.set("$archives_base_name-$mod_version.jar")
    destinationDirectory.set(file("$buildDir/dist"))
}

buildFinal.dependsOn("remapJar")
tasks.get("build").dependsOn(buildFinal)
