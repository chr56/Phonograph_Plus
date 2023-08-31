dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/plugins.versions.toml"))
        }
    }
}