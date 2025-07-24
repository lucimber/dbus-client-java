/*
 * SPDX-FileCopyrightText: 2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */

// Root build file for multi-module project structure
// Individual module configurations are in lib/build.gradle.kts and examples/build.gradle.kts

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}