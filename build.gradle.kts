plugins {
    java
    `maven-publish`
    id("net.earthmc.conventions.publishing") version "1.0.7"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://repo.glaremasters.me/repository/towny/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("com.palmergames.bukkit.towny:towny:0.100.0.0")
}

tasks {
    processResources {
        expand("version" to project.version)
    }
}

earthmc {
    publishing {
        public = true
    }
}
