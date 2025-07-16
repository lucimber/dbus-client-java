# Copyright Management Scripts

This directory contains scripts for managing copyright headers in the project.

## Copyright Check Script

The `copyright-check.py` script validates and updates SPDX-compliant copyright headers in source files.

### Features

- **SPDX Header Validation**: Checks for proper SPDX-FileCopyrightText and SPDX-License-Identifier format
- **Automatic Year Updates**: Updates copyright years to current year
- **Non-SPDX Conversion**: Converts old-style copyright headers to SPDX format
- **Dry-Run Mode**: Preview changes without modifying files
- **Multiple File Types**: Supports Java, Kotlin, and other file extensions
- **Recursive Processing**: Scans directories recursively
- **Detailed Reporting**: Shows summary of changes made

### Usage

#### Direct Python Usage

```bash
# Check copyright headers (dry-run)
python3 scripts/copyright-check.py java kt kts --recursive --dry-run --summary

# Update copyright headers
python3 scripts/copyright-check.py java kt kts --recursive --summary

# Check specific directory
python3 scripts/copyright-check.py java --path src/main/java --recursive --dry-run

# Use custom fallback values
python3 scripts/copyright-check.py java --fallback-license MIT --fallback-owner "Your Company" --dry-run
```

#### Gradle Integration

The script is integrated into the Gradle build system with the following tasks:

```bash
# Check copyright headers (shows detailed output)
./gradlew checkCopyright

# Update copyright headers
./gradlew updateCopyright

# Quiet check (for CI/CD)
./gradlew checkCopyrightQuiet

# Check specific subproject
./gradlew :lib:checkCopyrightLocal
./gradlew :examples:checkCopyrightLocal

# Update specific subproject
./gradlew :lib:updateCopyrightLocal
./gradlew :examples:updateCopyrightLocal
```

### Gradle Tasks

| Task | Description | Output |
|------|-------------|---------|
| `checkCopyright` | Validate copyright headers with detailed output | Console output with diffs |
| `updateCopyright` | Update copyright headers in all files | Updates files + summary report |
| `checkCopyrightQuiet` | Quiet validation for CI/CD | Minimal output, fails on issues |
| `checkCopyrightLocal` | Check headers in current subproject only | Subproject-specific validation |
| `updateCopyrightLocal` | Update headers in current subproject only | Subproject-specific updates |

### Configuration

The Gradle tasks are configured with the following defaults:

- **File Extensions**: `.java`, `.kt`, `.kts`
- **Fallback License**: `Apache-2.0`
- **Fallback Owner**: `Lucimber UG`
- **Processing Mode**: Recursive
- **Report Location**: `build/reports/copyright-summary.txt`

### Expected Header Format

The script expects and generates SPDX-compliant headers:

```java
/*
 * SPDX-FileCopyrightText: 2023-2025 Lucimber UG
 * SPDX-License-Identifier: Apache-2.0
 */
```

### CI/CD Integration

The `checkCopyrightQuiet` task is automatically run as part of the `check` task, making it part of your CI/CD pipeline:

```yaml
# GitHub Actions example
- name: Build and Test
  run: ./gradlew check
  # This will fail if copyright headers are missing or outdated
```

### Troubleshooting

#### Python Not Found
If you get "Python 3 is required but not found in PATH":
- Install Python 3: `brew install python3` (macOS) or `apt-get install python3` (Ubuntu)
- Ensure `python3` is in your PATH

#### Permission Denied
If you get permission errors:
- Make sure the script is executable: `chmod +x scripts/copyright-check.py`
- Check file permissions on the source files

#### Encoding Issues
If you encounter encoding problems:
- The script uses UTF-8 encoding by default
- Ensure your source files are UTF-8 encoded

### Customization

You can customize the script behavior by modifying the Gradle task parameters:

```kotlin
// In build.gradle.kts
tasks.named<Exec>("checkCopyright") {
    commandLine(
        "python3", 
        "scripts/copyright-check.py",
        "java", "kt", "kts", "py", // Add more extensions
        "--recursive",
        "--path", ".",
        "--fallback-license", "MIT", // Change license
        "--fallback-owner", "Your Company", // Change owner
        "--dry-run",
        "--summary"
    )
}
```

### Script Arguments

| Argument | Description | Default |
|----------|-------------|---------|
| `extensions` | File extensions to process | Required |
| `--recursive` | Process subdirectories | False |
| `--path` | Starting directory | Current directory |
| `--fallback-license` | License for new headers | Apache-2.0 |
| `--fallback-owner` | Copyright owner | Lucimber UG |
| `--dry-run` | Preview changes only | False |
| `--summary` | Show summary | False |
| `--summary-file` | Write summary to file | None |