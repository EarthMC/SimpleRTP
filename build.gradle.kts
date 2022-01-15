plugins {
    java
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.2.0"
    id("com.github.johnrengelman.shadow") version "7.1.2" apply true
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }

    maven {
        url = uri("https://repo.glaremasters.me/repository/towny/")
    }

    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    implementation("net.kyori:adventure-text-minimessage:4.2.0-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.17.1-R0.1-SNAPSHOT")
    compileOnly("com.palmergames.bukkit.towny:towny:0.97.5.11")
    paperDevBundle("1.17.1-R0.1-SNAPSHOT")
}

group = "net.earthmc"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_17

tasks {
    build {
        dependsOn(reobfJar)
    }

    shadowJar {
        dependencies {
            include(dependency("net.kyori:adventure-text-minimessage"))
        }

        relocate("net.kyori.adventure.text.minimessage", "net.earthmc.simplertp.libs.minimessage")
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(16)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()

        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}
