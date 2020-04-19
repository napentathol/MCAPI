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
    compile("us.sodiumlabs:MCAPI-common:1.0-SNAPSHOT")
}

val compileKotlin by tasks.getting(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    kotlinOptions.jvmTarget = "1.8"
}
val compileTestKotlin by tasks.getting(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class){
    kotlinOptions.jvmTarget = "1.8"
}
