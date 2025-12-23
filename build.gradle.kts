plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.chiraitori"
version = "0.2"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("com.maxmind.geoip2:geoip2:4.2.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("com.maxmind", "com.chiraitori.ipblock.libs.maxmind")
        relocate("com.fasterxml", "com.chiraitori.ipblock.libs.fasterxml")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to version)
        }
    }
}
