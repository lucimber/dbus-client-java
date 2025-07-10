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
    // Common
    implementation("io.netty:netty-all:4.2.2.Final")
    implementation("io.netty:netty-transport-native-epoll:4.2.2.Final")
    implementation("org.slf4j:slf4j-api:2.0.17")

    // SASL ID Resolver
    implementation("net.java.dev.jna:jna:5.17.0")
    implementation("net.java.dev.jna:jna-platform:5.17.0")

    // Testing
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation(platform("org.junit:junit-bom:5.13.3"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.mockito:mockito-core:5.8.0")
    
    // Integration and Performance Testing
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql") // For general container support
    testImplementation("org.awaitility:awaitility:4.2.2")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration", "performance", "chaos")
    }
    finalizedBy(tasks.jacocoTestReport)
}

// Integration tests
tasks.register<Test>("integrationTest") {
    useJUnitPlatform {
        includeTags("integration")
    }
    group = "verification"
    description = "Runs integration tests"
    
    systemProperty("testcontainers.reuse.enable", "true")
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Performance tests
tasks.register<Test>("performanceTest") {
    useJUnitPlatform {
        includeTags("performance")
    }
    group = "verification"
    description = "Runs performance benchmark tests"
    
    // Allocate more memory for performance tests
    maxHeapSize = "2g"
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Chaos engineering tests
tasks.register<Test>("chaosTest") {
    useJUnitPlatform {
        includeTags("chaos")
    }
    group = "verification"
    description = "Runs chaos engineering tests"
    
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
}

tasks.named<Javadoc>("javadoc") {
    options.windowTitle = project.name
    options.encoding = "UTF-8"
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(mapOf("Implementation-Title" to project.name,
            "Implementation-Version" to project.version))
    }
}

pmd {
    rulesMinimumPriority.set(2)
}
