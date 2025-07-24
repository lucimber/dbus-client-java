# Documentation Website Generation

This directory contains files used to generate the documentation website for GitHub Pages.

## Files

- `index-template.html` - Dynamic landing page template with version selector
- `version-switcher.js` - JavaScript for version switching in Javadoc pages

## Purpose

These files are used by the GitHub Actions workflow (`.github/workflows/deploy-docs.yml`) to generate a versioned documentation website with:

- Version selector on the main page
- Floating version switcher on all Javadoc pages
- Navigation between documentation versions
- Support for both release versions and the latest development version

## Usage

These files are automatically used during the documentation deployment process:

1. When pushing to the main branch, the workflow updates the `latest/` documentation
2. When creating a release tag (v*.*), the workflow creates a new versioned directory
3. The `index-template.html` is copied to the root of the documentation site
4. The `version-switcher.js` is injected into all Javadoc HTML pages

## Local Testing

To test the documentation website locally:

```bash
# From project root
./gradlew javadoc
mkdir -p _site/latest
cp -r lib/build/docs/javadoc/* _site/latest/
cp scripts/docs-website/index-template.html _site/index.html
echo '{"versions":[{"name":"latest","url":"./latest/","title":"Latest (Development)"}]}' > _site/versions.json

# Serve locally
cd _site
python -m http.server 8080
```

Then open http://localhost:8080 in your browser.