plugins {
    java
    `maven-publish`
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
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
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("com.palmergames.bukkit.towny:towny:0.99.1.0")
}

group = "net.earthmc"
version = "0.0.3"

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

tasks.withType<JavaCompile>() {
    options.encoding = Charsets.UTF_8.name()
}
