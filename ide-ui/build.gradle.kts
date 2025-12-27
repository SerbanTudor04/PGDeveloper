plugins {
    id("java")
    id("org.openjfx.javafxplugin")

}

javafx {
    version = "23" // <--- Must match your JDK and Launcher version
    modules("javafx.controls", "javafx.fxml", "javafx.graphics")
}
dependencies {
    implementation(project(":ide-core"))

    // Advanced Text Editor (We will use this for the SQL Editor later)
    implementation("org.fxmisc.richtext:richtextfx:0.11.2")
}

tasks.test {
    useJUnitPlatform()
}