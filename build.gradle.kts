/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("java-library")
    id("maven-publish")
}

group = "com.lucimber"
version = "2.0-SNAPSHOT"

// Copyright validation and update tasks
tasks.register<Exec>("checkCopyright") {
    group = "verification"
    description = "Validate copyright headers in source files"
    
    // Run copyright check in dry-run mode
    commandLine(
        "python3", 
        "scripts/copyright-check.py",
        "java", "kt", "kts",
        "--recursive",
        "--path", ".",
        "--fallback-license", "Apache-2.0",
        "--fallback-owner", "Lucimber UG",
        "--dry-run",
        "--summary"
    )
    
    workingDir = rootDir
    
    // Fail build if there are files that need copyright updates
    isIgnoreExitValue = false
}

tasks.register<Exec>("updateCopyright") {
    group = "maintenance"
    description = "Update copyright headers in source files"
    
    // Run copyright update (no dry-run)
    commandLine(
        "python3", 
        "scripts/copyright-check.py",
        "java", "kt", "kts",
        "--recursive",
        "--path", ".",
        "--fallback-license", "Apache-2.0",
        "--fallback-owner", "Lucimber UG",
        "--summary",
        "--summary-file", "build/reports/copyright-summary.txt"
    )
    
    workingDir = rootDir
    
    // Create reports directory if it doesn't exist
    doFirst {
        file("build/reports").mkdirs()
    }
    
    doLast {
        println("Copyright update completed. Summary saved to build/reports/copyright-summary.txt")
    }
}

tasks.register<Exec>("checkCopyrightQuiet") {
    group = "verification"
    description = "Quietly check copyright headers (for CI/CD)"
    
    commandLine(
        "python3", 
        "scripts/copyright-check.py",
        "java", "kt", "kts",
        "--recursive",
        "--path", ".",
        "--fallback-license", "Apache-2.0",
        "--fallback-owner", "Lucimber UG",
        "--dry-run"
    )
    
    workingDir = rootDir
    isIgnoreExitValue = false
}

// Optional: Add copyright check to the standard check task
tasks.named("check") {
    dependsOn("checkCopyrightQuiet")
}

// Make sure check runs after compilation
tasks.named("checkCopyrightQuiet") {
    mustRunAfter("compileJava", "compileTestJava")
}

// Configure subprojects
subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    
    repositories {
        mavenCentral()
    }
    
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
    
    // Add copyright tasks to all subprojects
    tasks.register<Exec>("checkCopyrightLocal") {
        group = "verification"
        description = "Check copyright headers in this subproject only"
        
        commandLine(
            "python3", 
            "${rootDir}/scripts/copyright-check.py",
            "java", "kt", "kts",
            "--recursive",
            "--path", projectDir.toString(),
            "--fallback-license", "Apache-2.0",
            "--fallback-owner", "Lucimber UG",
            "--dry-run",
            "--summary"
        )
        
        workingDir = rootDir
    }
    
    tasks.register<Exec>("updateCopyrightLocal") {
        group = "maintenance"
        description = "Update copyright headers in this subproject only"
        
        commandLine(
            "python3", 
            "${rootDir}/scripts/copyright-check.py",
            "java", "kt", "kts",
            "--recursive",
            "--path", projectDir.toString(),
            "--fallback-license", "Apache-2.0",
            "--fallback-owner", "Lucimber UG",
            "--summary"
        )
        
        workingDir = rootDir
    }
}