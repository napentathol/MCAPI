plugins {
    java
    `maven-publish`
}

group = "us.sodiumlabs"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

dependencies {
    testCompile("junit:junit:4.12")
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.gradle.sample"
            artifactId = "project1-sample"
            version = "1.1"

            from(components["java"])
        }
    }

    // select the repositories you want to publish to
    repositories {
        // uncomment to publish to the local maven
        mavenLocal()
    }
}
