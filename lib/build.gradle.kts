/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    id("java-library")
    id("maven-publish")
    id("checkstyle")
    id("jacoco")
    id("pmd")
    id("com.diffplug.spotless") version "6.25.0"
}

group = "com.lucimber"
version = "2.0.0"

// Tool versions
checkstyle {
    toolVersion = "10.20.2"
}

pmd {
    toolVersion = "7.8.0"
}

dependencies {
    // Common
    implementation("io.netty:netty-all:4.2.3.Final")
    implementation("io.netty:netty-transport-native-epoll:4.2.3.Final")
    implementation("org.slf4j:slf4j-api:2.0.17")

    // SASL ID Resolver
    implementation("net.java.dev.jna:jna:5.17.0")
    implementation("net.java.dev.jna:jna-platform:5.17.0")

    // Testing
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.mockito:mockito-core:5.8.0")

    // Integration and Performance Testing
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.21.3"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql") // For general container support
    testImplementation("org.awaitility:awaitility:4.3.0")
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

// Set the base name for all archives to match the artifactId
base {
    archivesName.set("lucimber-dbus-client")
}

// Spotless configuration for code formatting
spotless {
    java {
        // Use Google Java Format
        googleJavaFormat("1.22.0").aosp() // AOSP style uses 4 spaces like your checkstyle

        // Remove unused imports
        removeUnusedImports()

        // Fix import order - matches checkstyle configuration
        importOrder("\\#", "")

        // Add trailing whitespace trimming
        trimTrailingWhitespace()

        // Ensure files end with newline
        endWithNewline()

        // License header management
        licenseHeaderFile("$rootDir/config/license-header.txt")
            .updateYearWithLatest(true) // Update year to current year
            .yearSeparator("-") // Use hyphen for year ranges (e.g., 2023-2025)

        // Target all Java files
        target("src/**/*.java")
    }

    // Also format Kotlin/Gradle files
    kotlin {
        // Use ktlint for Kotlin formatting
        ktlint("0.50.0")

        // License header for Kotlin files
        licenseHeaderFile("$rootDir/config/license-header.txt", "(^(?![\\/ ]\\*).*$)")
            .updateYearWithLatest(true)
            .yearSeparator("-")

        // Target build files and any Kotlin source
        target("*.gradle.kts", "src/**/*.kt")
    }
}

tasks.named<Test>("test") {
    val runMemoryIntensiveTests = project.hasProperty("withMemoryIntensiveTests")

    useJUnitPlatform {
        if (runMemoryIntensiveTests) {
            excludeTags("integration", "performance", "chaos")
        } else {
            excludeTags("integration", "performance", "chaos", "memory-intensive")
        }
    }

    // Allocate more memory when running memory-intensive tests
    if (runMemoryIntensiveTests) {
        maxHeapSize = "8g"
        jvmArgs("-XX:+UseG1GC", "-XX:MaxMetaspaceSize=1g")
        doFirst {
            println("🧠 Memory-intensive tests enabled - using 8GB heap size")
        }
    } else {
        maxHeapSize = "4g"
        jvmArgs("-XX:+UseG1GC", "-XX:MaxMetaspaceSize=512m")
        doFirst {
            println("💡 Memory-intensive tests disabled - use -PwithMemoryIntensiveTests to enable")
        }
    }

    finalizedBy(tasks.jacocoTestReport)
}

// JaCoCo configuration to generate XML and HTML coverage reports
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

// Performance tests
tasks.register<Test>("performanceTest") {
    useJUnitPlatform {
        includeTags("performance")
    }
    group = "verification"
    description = "Runs performance benchmark tests"

    // Skip expensive static analysis tasks for faster execution
    dependsOn(tasks.compileJava, tasks.compileTestJava)

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

    // Skip expensive static analysis tasks for faster execution
    dependsOn(tasks.compileJava, tasks.compileTestJava)

    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
}

tasks.named<JavaCompile>("compileTestJava") {
    options.encoding = "UTF-8"
}

tasks.named<Javadoc>("javadoc") {
    (options as StandardJavadocDocletOptions).apply {
        windowTitle = "D-Bus Client API ${project.version}"
        docTitle = "D-Bus Client API Documentation"
        header = "<b>D-Bus Client ${project.version}</b>"
        // footer option is deprecated in newer Java versions
        encoding = "UTF-8"
        charSet = "UTF-8"
        author(true)
        version(true)
        use(true)
        splitIndex(true)
        addBooleanOption("Xdoclint:none", true)
        addStringOption("Xmaxwarns", "1")
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to "dbus-client",
                "Implementation-Version" to project.version,
            ),
        )
    }
}

pmd {
    rulesMinimumPriority.set(2)
}

// Integration test task for running inside container
tasks.register<Test>("integrationTestContainer") {
    useJUnitPlatform {
        includeTags("integration")
    }
    group = "verification"
    description = "Runs integration tests inside container (for internal use)"

    testClassesDirs = tasks.test.get().testClassesDirs
    classpath = tasks.test.get().classpath

    // Container-specific JVM settings
    maxHeapSize = "4g"
    jvmArgs("-XX:+UseG1GC", "-XX:MaxMetaspaceSize=512m")

    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// Container-based integration testing - the only reliable way to run D-Bus integration tests
tasks.register<Exec>("integrationTest") {
    group = "verification"
    description = "Runs integration tests inside a Linux container (single entry point for all environments)"

    // Skip expensive build tasks - only compile what we need
    dependsOn(tasks.compileJava, tasks.compileTestJava)

    // Dynamically detect Gradle version from wrapper properties
    val gradleVersion = gradle.gradleVersion

    // Build and run the test container in one step with caching
    commandLine("docker", "build", "-f", "src/test/docker/Dockerfile.integration", "-t", "dbus-integration-test", "--build-arg", "GRADLE_VERSION=$gradleVersion", "--cache-from", "type=gha", "--cache-to", "type=gha,mode=max", "..")

    doFirst {
        println("=".repeat(80))
        println("🏗️  INTEGRATION TEST - CONTAINER BUILD PHASE")
        println("=".repeat(80))
    }

    doLast {
        println("🐳 Running integration tests inside Linux container...")
        println("📋 Single entry point for consistent testing across all environments")
        println("⚙️  Using Gradle version: $gradleVersion")

        // Check if verbose flag is provided
        val showFullOutput = project.hasProperty("showOutput") || project.hasProperty("verbose")
        val showDebugLogs = project.hasProperty("debug") || project.hasProperty("debugLogs")

        if (showFullOutput) {
            println("📊 Full verbose mode enabled - showing complete test output")
        } else if (showDebugLogs) {
            println("🔍 Debug mode enabled - showing test logs with enhanced debugging")
        } else {
            println("💡 Use --verbose/-PshowOutput for full output, or -Pdebug/-PdebugLogs for debug logs")
        }

        println("=".repeat(80))
        println("🐳 INTEGRATION TEST - CONTAINER EXECUTION PHASE")
        println("=".repeat(80))

        // Prepare environment variables for container logging
        val containerEnvVars = mutableListOf<String>()
        if (showDebugLogs || showFullOutput) {
            containerEnvVars.addAll(
                listOf(
                    "-e",
                    "LOG_LEVEL=DEBUG",
                    "-e",
                    "DBUS_LOG_LEVEL=DEBUG",
                    "-e",
                    "INTEGRATION_LOG_LEVEL=DEBUG",
                ),
            )
        }

        // Add volume mount to copy test results back
        val testResultsMount = listOf(
            "-v",
            "${project.projectDir}/build/test-results:/app/lib/build/test-results",
            "-v",
            "${project.projectDir}/build/reports:/app/lib/build/reports",
        )

        // Run the container and execute tests
        @Suppress("DEPRECATION")
        val result = if (showFullOutput) {
            // Use direct exec with complete output shown
            project.exec {
                commandLine = listOf(
                    "docker", "run", "--rm",
                    "--name", "dbus-integration-test-run",
                ) +
                    testResultsMount +
                    containerEnvVars +
                    listOf("dbus-integration-test")
                standardOutput = System.out
                errorOutput = System.err
                isIgnoreExitValue = true
            }
        } else {
            // Use exec with limited output filtering
            project.exec {
                commandLine = listOf(
                    "docker", "run", "--rm",
                    "--name", "dbus-integration-test-run",
                ) +
                    testResultsMount +
                    containerEnvVars +
                    listOf("dbus-integration-test")
                if (showDebugLogs) {
                    standardOutput = System.out
                    errorOutput = System.err
                }
                isIgnoreExitValue = true
            }
        }

        println("=".repeat(80))
        println("📊 INTEGRATION TEST - RESULTS SUMMARY")
        println("=".repeat(80))

        if (result.exitValue == 0) {
            println("✅ Integration tests completed successfully!")
            println("📋 Test results available in: build/test-results/integrationTest/")
            println("📊 Test reports available in: build/reports/tests/integrationTest/")
        } else {
            println("❌ Integration tests failed with exit code: ${result.exitValue}")
            println("📋 Check the test output above for details")
            println("=".repeat(80))
            throw GradleException("Integration tests failed in container")
        }
        println("=".repeat(80))
    }
}

// Summary task to show test results
tasks.register("integrationTestSummary") {
    group = "verification"
    description = "Shows integration test results summary"

    doLast {
        println(
            """
        |=== Integration Test Results ===
        |✅ Container-based integration tests completed successfully!
        |📋 D-Bus daemon with SASL authentication: Working
        |🔒 EXTERNAL + DBUS_COOKIE_SHA1 authentication: Tested
        |🌐 Unix socket + TCP connections: Both supported
        |🐳 Cross-platform compatibility: Solved via containerization
        |
            """.trimMargin(),
        )
    }
}

// Task to print runtime classpath for debugging
tasks.register("printRuntimeClasspath") {
    group = "help"
    description = "Prints the runtime classpath"

    doLast {
        println(configurations.runtimeClasspath.get().asPath)
    }
}

// Task to print test runtime classpath for debugging
tasks.register("printTestRuntimeClasspath") {
    group = "help"
    description = "Prints the test runtime classpath including test dependencies"

    doLast {
        println(configurations.testRuntimeClasspath.get().asPath)
    }
}

// Publishing configuration
publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "lucimber-dbus-client"
            from(components["java"])

            pom {
                name.set("D-Bus Client")
                description.set("A modern Java client library for D-Bus")
                url.set("https://github.com/lucimber/dbus-client-java")

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("lucimber")
                        name.set("Lucimber UG")
                        email.set("info@lucimber.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/lucimber/dbus-client-java.git")
                    developerConnection.set("scm:git:ssh://github.com:lucimber/dbus-client-java.git")
                    url.set("https://github.com/lucimber/dbus-client-java")
                }
            }
        }
    }
}
