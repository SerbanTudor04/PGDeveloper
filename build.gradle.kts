plugins {
    id("java")
    id("org.openjfx.javafxplugin") version "0.1.0" apply false
}

allprojects {
    group = "ro.fintechpro"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(23))
        }
    }

    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.10.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
    }

    tasks.test {
        useJUnitPlatform()
    }
}
