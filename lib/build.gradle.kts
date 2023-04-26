plugins {
    id("java-library")
    id("maven-publish")
    id("checkstyle")
    id("jacoco")
    id("pmd")
}

group = "com.lucimber"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("io.netty:netty-all:4.1.86.Final")
    implementation("io.netty:netty-transport-native-epoll:4.1.86.Final")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.1")
    testImplementation("org.apache.logging.log4j:log4j-api:2.17.1")
    testImplementation("org.apache.logging.log4j:log4j-core:2.17.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.0")
    testImplementation("org.mockito:mockito-core:3.12.4")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    val mavenUsername: String by project
    val mavenPassword: String by project
    maven {
        name = "LucimberRelease"
        url = uri("https://artifactory.lucimber.io/artifactory/gradle-release")
        mavenContent {
            releasesOnly()
        }
        credentials {
            username = mavenUsername
            password = mavenPassword
        }
    }
    maven {
        name = "LucimberSnapshot"
        url = uri("https://artifactory.lucimber.io/artifactory/gradle-dev")
        mavenContent {
            snapshotsOnly()
        }
        credentials {
            username = mavenUsername
            password = mavenPassword
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("library") {
            artifactId = "dbus-app"
            from(components["java"])
        }
    }
    repositories {
        val mavenUsername: String by project
        val mavenPassword: String by project
        maven {
            name = "LucimberLocal"
            val releaseUrl = uri("https://artifactory.lucimber.io/artifactory/gradle-release-local")
            val snapshotUrl = uri("https://artifactory.lucimber.io/artifactory/gradle-dev-local")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotUrl else releaseUrl
            credentials {
                username = mavenUsername
                password = mavenPassword
            }
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
}

tasks.named<Javadoc>("javadoc") {
    options.windowTitle = rootProject.name
    options.encoding = "UTF-8"
    isVerbose = true
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf("Implementation-Title" to project.name,
            "Implementation-Version" to project.version))
    }
}

checkstyle {
    toolVersion = "9.1"
}

pmd {
    rulesMinimumPriority.set(2)
}
