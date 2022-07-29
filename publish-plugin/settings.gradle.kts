//this plugin is includeBuild so it requires explicit dependency on the toml file
//https://gradle-community.slack.com/archives/CAH4ZP3GX/p1645056433577989
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../libs.versions.toml"))
        }
    }
}