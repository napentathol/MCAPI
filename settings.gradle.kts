rootProject.name = "MCAPI"
include("MCAPI-client")
include("MCAPI-common")
include("MCAPI-fabric")
include("MCAPI-example")

pluginManagement {
    repositories {
        maven(url = "http://maven.fabricmc.net"){
            name = "Fabric"
        }
        gradlePluginPortal()
    }
}
