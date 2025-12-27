plugins {
    id("java")
    application
    id("org.openjfx.javafxplugin")
}


javafx {
    version = "23" // Matches your Java 23 Toolchain
    modules("javafx.controls", "javafx.fxml", "javafx.graphics")
}

application {
    // Points to the wrapper class to avoid "Module not found" errors
    mainClass.set("ro.fintechpro.launcher.Main")
    applicationName = "PgDeveloper"
}

dependencies {
    // Links the other modules so the launcher can see them
    implementation(project(":ide-ui"))
    implementation(project(":ide-core"))
    implementation("io.github.mkpaz:atlantafx-base:2.0.1")
}