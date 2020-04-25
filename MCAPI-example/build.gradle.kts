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
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":MCAPI-client"))
    implementation("org.apache.logging.log4j:log4j-core:2.13.2")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
