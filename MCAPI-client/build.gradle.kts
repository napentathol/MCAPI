plugins {
    kotlin("jvm") version "1.3.72"
}

group = "us.sodiumlabs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile("com.github.Steveice10:MCProtocolLib:1.15-1")
    // https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j
    compile("org.apache.logging.log4j:log4j-api:2.13.2")
    compile(project(":MCAPI-common"))
}

val compileKotlin by tasks.getting(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    kotlinOptions.jvmTarget = "1.8"
}
val compileTestKotlin by tasks.getting(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    kotlinOptions.jvmTarget = "1.8"
}
