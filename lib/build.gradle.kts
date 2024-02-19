plugins {
    id("java-library")
    id("maven-publish")
    id("checkstyle")
    id("jacoco")
    id("pmd")
}

group = "com.lucimber"
version = "2.0-SNAPSHOT"

dependencies {
    implementation("io.netty:netty-all:4.1.106.Final")
    implementation("io.netty:netty-transport-native-epoll:4.1.106.Final")
    implementation("org.slf4j:slf4j-api:2.0.11")
    testImplementation("ch.qos.logback:logback-classic:1.5.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("org.mockito:mockito-core:5.8.0")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
}

tasks.named<Javadoc>("javadoc") {
    options.windowTitle = project.name
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
