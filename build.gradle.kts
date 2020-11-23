plugins {
    java
    kotlin("jvm") version "1.4.10"
}

group = "xyz.acrylicstyle"
version = "0.0.1"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://repo.acrylicstyle.xyz/") }
    maven { url = uri("https://repo2.acrylicstyle.xyz/") }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("xyz.acrylicstyle:sequelize4j:0.3.7a") {
        exclude("xyz.acrylicstyle", "java-util-all")
    }
    compileOnly("xyz.acrylicstyle:java-util-all:0.11.40a")
    compileOnly("net.md-5:bungeecord-api:1.16-R0.4-SNAPSHOT")
    compileOnly("xyz.acrylicstyle:UniversalLanguageSelector:0.0.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.4"
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf(
            "-Xjsr305=strict"
        )
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "utf-8"
    }

    withType<ProcessResources> {
        filteringCharset = "UTF-8"
        from(sourceSets.main.get().resources.srcDirs) {
            include("**")

            val tokenReplacementMap = mapOf(
                "version" to project.version,
                "name" to project.rootProject.name
            )

            filter<org.apache.tools.ant.filters.ReplaceTokens>("tokens" to tokenReplacementMap)
        }

        from(projectDir) { include("LICENSE") }
    }

    withType<Jar> {
        from(configurations.getByName("implementation").apply { isCanBeResolved = true }.map { if (it.isDirectory) it else zipTree(it) })
    }
}
