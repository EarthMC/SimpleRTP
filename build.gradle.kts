plugins {
    java
    `maven-publish`
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
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    compileOnly("com.palmergames.bukkit.towny:towny:0.99.1.0")
}

group = "net.earthmc"
version = "0.0.5"

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()

        expand("version" to project.version)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = Charsets.UTF_8.name()
}
