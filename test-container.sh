#!/bin/bash

echo "🐳 Building and running D-Bus integration tests in container..."
echo ""

# Build the container
echo "📦 Building Docker container..."
docker build -f Dockerfile.test -t dbus-integration-test . --quiet

if [ $? -eq 0 ]; then
    echo "✅ Container built successfully"
else
    echo "❌ Container build failed"
    exit 1
fi

echo ""
echo "🧪 Running integration tests..."
echo ""

# Run the container and capture output
docker run --rm --name dbus-integration-test-run dbus-integration-test

CONTAINER_EXIT_CODE=$?

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