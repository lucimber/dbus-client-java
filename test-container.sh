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

echo "🐳 Running D-Bus integration tests via Gradle..."
echo "📋 This script now uses the single entry point: ./gradlew integrationTest"
echo ""

# Build the Gradle command based on options
GRADLE_ARGS=("integrationTest")

if [ "$VERBOSE" = true ]; then
    echo "📊 Verbose mode enabled - showing full test output"
    GRADLE_ARGS+=("-PshowOutput")
else
    echo "💡 Use -v or --verbose to see detailed test output"
fi

if [ "$SHOW_BUILD_OUTPUT" = true ]; then
    echo "📦 Build output enabled"
    GRADLE_ARGS+=("-Pdebug")
fi

echo ""
echo "🧪 Running integration tests..."
echo "⚙️  Command: ./gradlew ${GRADLE_ARGS[*]}"
echo ""

# Run the Gradle task
./gradlew "${GRADLE_ARGS[@]}"
GRADLE_EXIT_CODE=$?

echo ""
if [ $GRADLE_EXIT_CODE -eq 0 ]; then
    echo "✅ Integration tests completed successfully!"
    echo "📋 D-Bus SASL authentication working correctly in Linux container"
    echo "📊 Test results: lib/build/test-results/integrationTest/"
    echo "📋 Test reports: lib/build/reports/tests/integrationTest/"
else
    echo "❌ Integration tests failed with exit code: $GRADLE_EXIT_CODE"
    echo "📋 Check the test output above for details"
fi

echo ""
echo "🎯 For more details, see: docs/testing-guide.md"
echo ""

exit $GRADLE_EXIT_CODE