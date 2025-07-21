# Documentation

This directory contains documentation files for the D-Bus Client Java library with support for multiple versions.

## Contents

- `index-template.html` - Dynamic landing page template with version selector
- `version-switcher.js` - JavaScript for version switching in Javadoc pages
- `README.md` - This file

## Versioned Documentation System

The documentation system supports multiple versions:

### URL Structure
- `https://{username}.github.io/{repository-name}/` - Main landing page with version selector
- `https://{username}.github.io/{repository-name}/latest/` - Latest development documentation
- `https://{username}.github.io/{repository-name}/v1.0.0/` - Version 1.0.0 documentation
- `https://{username}.github.io/{repository-name}/v1.1.0/` - Version 1.1.0 documentation

### Automatic Version Management

**When pushing to main branch:**
- Updates `latest/` directory with current Javadoc
- Preserves all existing version directories

**When creating a release tag (v*.*):**
- Creates new versioned directory (e.g., `v1.0.0/`)
- Updates `latest/` directory
- Updates version index with new release

### Features

1. **Version Selector**: Dynamic version list on the main page
2. **Version Switcher**: Floating version selector on all Javadoc pages
3. **Navigation**: Easy navigation between documentation home and API docs
4. **Persistence**: All previous versions remain accessible
5. **API**: JSON endpoint (`/versions.json`) for programmatic access

## GitHub Pages Setup

To enable GitHub Pages for this repository:

1. Go to repository Settings
2. Navigate to Pages section  
3. Select "GitHub Actions" as the source
4. The documentation will be available at: `https://{username}.github.io/{repository-name}/`

## Release Process

To create a new documented version:

1. Ensure your code is ready for release
2. Create and push a git tag: `git tag v1.0.0 && git push origin v1.0.0`
3. The GitHub Action will automatically:
   - Generate Javadoc for the tagged version
   - Create a new version directory
   - Update the version selector
   - Deploy to GitHub Pages

## Local Development

To generate and preview documentation locally:

```bash
# Generate Javadoc
./gradlew javadoc

# Create a local version structure
mkdir -p _site/latest
cp -r lib/build/docs/javadoc/* _site/latest/
cp docs/index-template.html _site/index.html
echo '{"versions":[{"name":"latest","url":"./latest/","title":"Latest (Development)"}]}' > _site/versions.json

# Serve the documentation locally
cd _site
python -m http.server 8080

# Open http://localhost:8080 in your browser
```

## Version JSON API

The `versions.json` file provides programmatic access to available versions:

```json
{
  "versions": [
    {
      "name": "latest",
      "url": "./latest/",
      "title": "Latest (Development)"
    },
    {
      "name": "v1.0.0", 
      "url": "./v1.0.0/",
      "title": "Version v1.0.0"
    }
  ]
}
```