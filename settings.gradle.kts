rootProject.name = "MCAPI"
include("MCAPI-client")
include("MCAPI-common")
include("MCAPI-fabric")

pluginManagement {
    repositories {
        maven(url = "http://maven.fabricmc.net"){
            name = "Fabric"
        }
        gradlePluginPortal()
    }
}
