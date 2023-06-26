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
    implementation("io.netty:netty-all:4.1.94.Final")
    implementation("io.netty:netty-transport-native-epoll:4.1.94.Final")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")
    testImplementation("org.apache.logging.log4j:log4j-api:2.20.0")
    testImplementation("org.apache.logging.log4j:log4j-core:2.20.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
    testImplementation("org.mockito:mockito-core:4.11.0")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
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
