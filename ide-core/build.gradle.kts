plugins {
    id("java")
}

group = "ro.fintechpro"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // The Database Driver
    implementation("org.postgresql:postgresql:42.7.2")

    // The Connection Pool (High performance connection manager)
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Logging (Required by HikariCP to see errors/info)
    implementation("org.slf4j:slf4j-simple:2.0.9")

    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.test {
    useJUnitPlatform()
}