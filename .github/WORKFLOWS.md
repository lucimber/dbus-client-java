# GitHub Workflows & Configuration

This directory contains GitHub-specific configuration files for automation, templates, and workflows.

## 📁 Structure

```
.github/
├── workflows/              # GitHub Actions workflows
│   ├── ci.yml             # 🔄 Continuous Integration (build, test, coverage)
│   ├── security.yml       # 🔒 Security scanning (OWASP dependency check)
│   ├── codeql.yml         # 🔍 Code Analysis (CodeQL security scanning)
│   └── release.yml        # 🚀 Release automation (tags → GitHub releases)
├── ISSUE_TEMPLATE/         # Issue templates for bug reports and features
│   ├── bug_report.yml     # 🐛 Bug report template
│   └── feature_request.yml # ✨ Feature request template
├── PULL_REQUEST_TEMPLATE.md # 📝 Pull request template
└── dependabot.yml         # 🤖 Dependabot configuration
```

## 🔄 Workflows

### **Continuous Integration** (`ci.yml`)
- **Triggers**: Push/PR to main branch
- **Actions**: Build, test, integration tests, coverage reporting
- **Optimizations**: Gradle caching, Docker layer caching
- **Duration**: ~3-5 minutes (with caching)

### **Security Scanning** (`security.yml`)
- **Triggers**: Push/PR to main, weekly schedule
- **Actions**: OWASP dependency vulnerability scanning
- **Optimizations**: Vulnerability database caching
- **Duration**: ~2-3 minutes (with caching)

### **Code Analysis** (`codeql.yml`)
- **Triggers**: Push/PR to main, weekly schedule
- **Actions**: GitHub CodeQL security analysis
- **Languages**: Java
- **Duration**: ~4-6 minutes

### **Release Automation** (`release.yml`)
- **Triggers**: Version tags (v*)
- **Actions**: Full test suite, build artifacts, create GitHub release
- **Artifacts**: JAR files, JavaDoc, source archives
- **Duration**: ~8-12 minutes

## 🎯 Features

- ✅ **Comprehensive Testing**: Unit, integration, and security tests
- ✅ **Performance Optimized**: Extensive caching for faster builds
- ✅ **Security Focused**: Multiple scanning tools and vulnerability checks
- ✅ **Release Ready**: Automated release pipeline with artifacts
- ✅ **Developer Friendly**: Clear templates and automated workflows

## 📊 Status Badges

All workflow statuses are displayed in the main [README.md](../README.md) for quick visibility.

## 🔧 Maintenance

- **Dependabot**: Automatically updates dependencies weekly
- **Workflows**: Self-updating through GitHub Actions marketplace
- **Security**: Weekly automated scans for vulnerabilities
- **Performance**: Cached builds reduce CI time by 40-60%