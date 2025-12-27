plugins {
    id("java")
    id("org.openjfx.javafxplugin")

}

group = "ro.fintechpro"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

javafx {
    version = "23" // <--- Must match your JDK and Launcher version
    modules("javafx.controls", "javafx.fxml", "javafx.graphics")
}
dependencies {
    // The Database Driver
    implementation("org.postgresql:postgresql:42.7.2")

    // The Connection Pool (High performance connection manager)
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Logging (Required by HikariCP to see errors/info)
    implementation("org.slf4j:slf4j-simple:2.0.9")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.h2database:h2:2.2.224")
}

tasks.test {
    useJUnitPlatform()
}