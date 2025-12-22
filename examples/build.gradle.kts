/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */


plugins {
    id("java")
    id("application")
    id("checkstyle")
    id("pmd")
    id("com.diffplug.spotless") version "8.1.0"
}

dependencies {
    implementation(project(":lib"))
    implementation("org.slf4j:slf4j-api:2.0.17")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.23")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Tool versions
checkstyle {
    toolVersion = "10.20.2"
    configFile = file("../config/checkstyle/checkstyle.xml")
    // Examples focus on clarity over strict style compliance
    isIgnoreFailures = true
}

pmd {
    toolVersion = "7.8.0"
    ruleSetFiles = files("../config/pmd/ruleset.xml")
    // Examples are meant to be educational, not necessarily production-perfect
    // Allow unused parameters and variables for demonstration purposes
    isIgnoreFailures = true
}

// Spotless configuration
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

// Configure the application plugin
application {
    mainClass.set("com.lucimber.dbus.examples.BasicClientExample")
}

// Create individual run tasks for each example
tasks.register<JavaExec>("runBasicClient") {
    group = "examples"
    description = "Runs the basic D-Bus client example"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.lucimber.dbus.examples.BasicClientExample")

    // Allow passing system properties
    systemProperties = System.getProperties().toMap() as Map<String, Any>

    // Allow passing command line arguments
    args = if (project.hasProperty("args")) {
        (project.property("args") as String).split(" ")
    } else {
        emptyList()
    }
}

tasks.register<JavaExec>("runSignalHandling") {
    group = "examples"
    description = "Runs the signal handling example"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.lucimber.dbus.examples.SignalHandlingExample")

    // Allow passing system properties
    systemProperties = System.getProperties().toMap() as Map<String, Any>

    // Allow passing command line arguments
    args = if (project.hasProperty("args")) {
        (project.property("args") as String).split(" ")
    } else {
        emptyList()
    }
}

tasks.register<JavaExec>("runServiceDiscovery") {
    group = "examples"
    description = "Runs the service discovery example"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.lucimber.dbus.examples.ServiceDiscoveryExample")

    // Allow passing system properties
    systemProperties = System.getProperties().toMap() as Map<String, Any>

    // Allow passing command line arguments
    args = if (project.hasProperty("args")) {
        (project.property("args") as String).split(" ")
    } else {
        emptyList()
    }
}

tasks.register<JavaExec>("runAuthentication") {
    group = "examples"
    description = "Runs the authentication example"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.lucimber.dbus.examples.AuthenticationExample")

    // Allow passing system properties
    systemProperties = System.getProperties().toMap() as Map<String, Any>

    // Allow passing command line arguments
    args = if (project.hasProperty("args")) {
        (project.property("args") as String).split(" ")
    } else {
        emptyList()
    }
}

// Create a task to list all available examples
tasks.register("listExamples") {
    group = "examples"
    description = "Lists all available examples"

    doLast {
        println(
            """
        |Available D-Bus Examples:
        |
        |./gradlew :examples:runBasicClient
        |  Basic D-Bus client operations (method calls, connection management)
        |
        |./gradlew :examples:runSignalHandling
        |  Signal subscription and handling patterns
        |
        |./gradlew :examples:runServiceDiscovery
        |  Service discovery and introspection
        |
        |./gradlew :examples:runAuthentication
        |  Authentication mechanisms and security
        |
        |Configuration options:
        |  -Dargs="--help"                    Show example help
        |  -Dargs="--system-bus"              Use system bus instead of session bus
        |  -Dargs="--timeout=30"              Set timeout in seconds
        |  -Dargs="--verbose"                 Enable verbose logging
        |  -Dlogging.level.com.lucimber.dbus=DEBUG   Enable debug logging
        |
        |Required Environment Variables:
        |  DBUS_SESSION_BUS_ADDRESS          Session bus socket address (required for session bus)
        |  DBUS_SYSTEM_BUS_ADDRESS           System bus socket address (optional, defaults to /var/run/dbus/system_bus_socket)
        |
        |SASL Authentication Requirements:
        |  user.name (system property)        Username for DBUS_COOKIE_SHA1 and EXTERNAL mechanisms
        |  user.home (system property)        Home directory for .dbus-keyrings directory (DBUS_COOKIE_SHA1)
        |  os.name (system property)          Operating system for EXTERNAL mechanism (Unix UID vs Windows SID)
        |
        |Examples:
        |  ./gradlew :examples:runBasicClient -Dargs="--help"
        |  ./gradlew :examples:runSignalHandling -Dargs="--timeout=60 --verbose"
        |  ./gradlew :examples:runBasicClient -Dargs="--system-bus"
        |  ./gradlew :examples:runServiceDiscovery -Dlogging.level.com.lucimber.dbus=DEBUG
        |
        |Environment Setup Example:
        |  export DBUS_SESSION_BUS_ADDRESS=unix:path=/tmp/dbus-session
        |  ./gradlew :examples:runBasicClient
            """.trimMargin(),
        )
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
}

// Helper task to run all examples in sequence
tasks.register("runAllExamples") {
    group = "examples"
    description = "Runs all examples in sequence"

    doLast {
        println("Running all examples...")
        println("Note: Each example will run for a short time to demonstrate functionality")
    }

    // Run with short timeouts to demonstrate all examples
    finalizedBy("runBasicClientDemo", "runSignalHandlingDemo", "runServiceDiscoveryDemo", "runAuthenticationDemo")
}

// Demo versions with shorter timeouts
tasks.register<JavaExec>("runBasicClientDemo") {
    group = "examples"
    description = "Runs basic client example in demo mode"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.lucimber.dbus.examples.BasicClientExample")
    args = listOf("--timeout=10", "--mode=demo")
}

tasks.register<JavaExec>("runSignalHandlingDemo") {
    group = "examples"
    description = "Runs signal handling example in demo mode"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.lucimber.dbus.examples.SignalHandlingExample")
    args = listOf("--timeout=10", "--mode=demo")
}

tasks.register<JavaExec>("runServiceDiscoveryDemo") {
    group = "examples"
    description = "Runs service discovery example in demo mode"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.lucimber.dbus.examples.ServiceDiscoveryExample")
    args = listOf("--timeout=10", "--mode=demo")
}

tasks.register<JavaExec>("runAuthenticationDemo") {
    group = "examples"
    description = "Runs authentication example in demo mode"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.lucimber.dbus.examples.AuthenticationExample")
    args = listOf("--timeout=10", "--mode=demo")
}
