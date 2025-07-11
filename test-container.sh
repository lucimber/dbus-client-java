#!/bin/bash

# Parse command line arguments
VERBOSE=false
SHOW_BUILD_OUTPUT=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        --show-build)
            SHOW_BUILD_OUTPUT=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  -v, --verbose      Show detailed test output"
            echo "  --show-build       Show Docker build output"
            echo "  -h, --help         Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo "🐳 Building and running D-Bus integration tests in container..."
echo ""

# Build the container
echo "📦 Building Docker container..."
if [ "$SHOW_BUILD_OUTPUT" = true ]; then
    docker build -f Dockerfile.test -t dbus-integration-test .
else
    docker build -f Dockerfile.test -t dbus-integration-test . --quiet
fi

if [ $? -eq 0 ]; then
    echo "✅ Container built successfully"
else
    echo "❌ Container build failed"
    exit 1
fi

echo ""
echo "🧪 Running integration tests..."
if [ "$VERBOSE" = true ]; then
    echo "📊 Verbose mode enabled - showing full test output"
else
    echo "💡 Use -v or --verbose to see detailed test output"
fi
echo ""

# Run the container and capture output
if [ "$VERBOSE" = true ]; then
    docker run --rm --name dbus-integration-test-run dbus-integration-test
    CONTAINER_EXIT_CODE=$?
else
    # Use a temporary file to capture the full output, then filter it
    TEMP_OUTPUT=$(mktemp)
    docker run --rm --name dbus-integration-test-run dbus-integration-test > "$TEMP_OUTPUT" 2>&1
    CONTAINER_EXIT_CODE=$?
    
    # Show filtered output
    grep -E "(=== D-Bus Integration Test|✓|❌|ERROR|FAILED|BUILD)" "$TEMP_OUTPUT"
    
    # Clean up temp file
    rm "$TEMP_OUTPUT"
fi

echo ""
if [ $CONTAINER_EXIT_CODE -eq 0 ]; then
    echo "✅ Integration tests completed successfully!"
    echo "📋 D-Bus SASL authentication working correctly in Linux container"
else
    echo "❌ Integration tests failed with exit code: $CONTAINER_EXIT_CODE"
    echo "📋 Check the test output above for details"
fi

echo ""
echo "🎯 For more details, see: docs/testing-guide.md"
echo ""

exit $CONTAINER_EXIT_CODE